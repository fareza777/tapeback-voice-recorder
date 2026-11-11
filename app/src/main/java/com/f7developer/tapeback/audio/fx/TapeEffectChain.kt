package com.f7developer.tapeback.audio.fx

import com.f7developer.tapeback.domain.model.EffectPreset
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tanh

/** Immutable effect parameters, swapped atomically into the chain. */
data class FxChainParams(val preset: EffectPreset = EffectPreset.CLEAN, val hiss: Float = 0f)

/** Receives per-buffer meter values (linear 0..1) measured after the effect. */
fun interface LevelSink {
    fun onLevels(peakLeft: Float, peakRight: Float, rmsLeft: Float, rmsRight: Float)
}

/**
 * Pure-Kotlin cassette effect chain operating on interleaved float PCM in [-1, 1].
 *
 * in → HPF → [lo-fi decimation] → tone peak → wow/flutter → saturation → LPF → +hiss → limit → meter
 *
 * Real-time safe: [process] does not allocate or lock. Parameters arrive through [params].
 */
class TapeEffectChain(private val levelSink: LevelSink? = null) {

    @Volatile
    var params: FxChainParams = FxChainParams()

    private var applied: FxChainParams? = null
    private var sampleRate = 44_100
    private var channels = 2

    private val hpf = Biquad()
    private val tone = Biquad()
    private val lpf = Biquad()

    // Lo-fi sample-and-hold
    private var holdFrames = 1
    private var holdCounter = 0
    private var held = FloatArray(2)

    // Wow & flutter: per-channel circular delay line
    private var delay = Array(2) { FloatArray(1) }
    private var delayWrite = 0
    private var wowPhase = 0.0
    private var flutterPhase = 0.0
    private var wowInc = 0.0
    private var flutterInc = 0.0
    private var baseDelaySamples = 0f
    private var wowDepthSamples = 0f
    private var flutterDepthSamples = 0f

    // Character
    private var drive = 1f
    private var driveNorm = 1f
    private var staticLevel = 0f
    private var hissGain = 0f

    // Hiss generator: xorshift32 + one-pole high-pass
    private var rng = 0x2545F491
    private var hissPrevIn = FloatArray(2)
    private var hissPrevOut = FloatArray(2)

    fun configure(sampleRate: Int, channels: Int) {
        this.sampleRate = sampleRate.coerceAtLeast(8_000)
        this.channels = channels.coerceIn(1, 8)
        hpf.resize(this.channels)
        tone.resize(this.channels)
        lpf.resize(this.channels)
        held = FloatArray(this.channels)
        hissPrevIn = FloatArray(this.channels)
        hissPrevOut = FloatArray(this.channels)
        val delayLength = (this.sampleRate * 0.012f).roundToInt().coerceAtLeast(8)
        delay = Array(this.channels) { FloatArray(delayLength) }
        applied = null
        reset()
    }

    fun reset() {
        hpf.reset()
        tone.reset()
        lpf.reset()
        held.fill(0f)
        hissPrevIn.fill(0f)
        hissPrevOut.fill(0f)
        delay.forEach { it.fill(0f) }
        delayWrite = 0
        holdCounter = 0
        wowPhase = 0.0
        flutterPhase = 0.0
    }

    private fun applyParams(p: FxChainParams) {
        val sr = sampleRate
        holdFrames = 1
        wowDepthSamples = 0f
        flutterDepthSamples = 0f
        staticLevel = 0f
        when (p.preset) {
            EffectPreset.CLEAN -> {
                hpf.bypass(); tone.bypass(); lpf.bypass()
                drive = 1f
            }
            EffectPreset.VINTAGE -> {
                hpf.highPass(sr, 45f)
                tone.peaking(sr, 140f, 0.8f, 2.5f)
                lpf.lowPass(sr, 9_500f, 0.8f)
                drive = 1.6f
                baseDelaySamples = sr * 0.004f
                wowDepthSamples = sr * 0.0014f
                flutterDepthSamples = sr * 0.00012f
            }
            EffectPreset.LOFI -> {
                hpf.highPass(sr, 90f)
                tone.peaking(sr, 900f, 0.9f, 2f)
                lpf.lowPass(sr, 4_200f, 0.9f)
                drive = 1.3f
                holdFrames = max(1, (sr / 11_025f).roundToInt())
            }
            EffectPreset.RADIO -> {
                hpf.highPass(sr, 380f, 0.9f)
                tone.peaking(sr, 1_600f, 1.1f, 5f)
                lpf.lowPass(sr, 3_200f, 0.9f)
                drive = 2.4f
                staticLevel = 0.006f
            }
        }
        driveNorm = tanh(drive)
        hissGain = p.hiss.coerceIn(0f, 1f) * 0.045f
        wowInc = 2.0 * PI * 0.55 / sr
        flutterInc = 2.0 * PI * 6.3 / sr
        applied = p
    }

    /** Process [frames] interleaved frames of [buffer] in place. */
    fun process(buffer: FloatArray, frames: Int) {
        val p = params
        if (p != applied) applyParams(p)
        val ch = channels
        val preset = p.preset
        val bypass = preset == EffectPreset.CLEAN
        val useDelay = wowDepthSamples > 0f
        val delayLen = delay[0].size

        var peakL = 0f
        var peakR = 0f
        var sumL = 0.0
        var sumR = 0.0

        for (f in 0 until frames) {
            val base = f * ch
            val takeSample = holdCounter == 0
            var readPos = 0f
            if (useDelay) {
                // Slow "wow" plus faster "flutter" modulate the read head position.
                val d = baseDelaySamples + (wowDepthSamples * sin(wowPhase) + flutterDepthSamples * sin(flutterPhase)).toFloat()
                readPos = delayWrite - d.coerceIn(1f, delayLen - 2f)
                if (readPos < 0f) readPos += delayLen
                wowPhase += wowInc
                flutterPhase += flutterInc
                if (wowPhase > 2 * PI) wowPhase -= 2 * PI
                if (flutterPhase > 2 * PI) flutterPhase -= 2 * PI
            }
            for (c in 0 until ch) {
                val i = base + c
                var x = buffer[i]
                if (!bypass) {
                    x = hpf.process(x, c)
                    if (holdFrames > 1) {
                        if (takeSample) held[c] = (x * 127f).roundToInt() / 127f
                        x = held[c]
                    }
                    x = tone.process(x, c)
                    if (useDelay) {
                        val line = delay[c]
                        line[delayWrite] = x
                        val i0 = readPos.toInt()
                        val frac = readPos - i0
                        val i1 = if (i0 + 1 >= delayLen) 0 else i0 + 1
                        x = line[i0] + (line[i1] - line[i0]) * frac
                    }
                    x = tanh(drive * x) / driveNorm
                    x = lpf.process(x, c)
                    if (staticLevel > 0f) x += noise() * staticLevel
                }
                if (hissGain > 0f) {
                    val white = noise()
                    val hp = 0.62f * (hissPrevOut[c] + white - hissPrevIn[c])
                    hissPrevIn[c] = white
                    hissPrevOut[c] = hp
                    x += hp * hissGain
                }
                if (x > 1f) x = 1f else if (x < -1f) x = -1f
                buffer[i] = x
                val a = abs(x)
                if (c == 0) {
                    if (a > peakL) peakL = a
                    sumL += x * x
                } else if (c == 1) {
                    if (a > peakR) peakR = a
                    sumR += x * x
                }
            }
            if (useDelay) {
                delayWrite++
                if (delayWrite >= delayLen) delayWrite = 0
            }
            if (holdFrames > 1) {
                holdCounter++
                if (holdCounter >= holdFrames) holdCounter = 0
            }
        }

        val sink = levelSink ?: return
        if (frames == 0) return
        val rmsL = sqrt(sumL / frames).toFloat()
        if (ch == 1) {
            sink.onLevels(peakL, peakL, rmsL, rmsL)
        } else {
            sink.onLevels(peakL, peakR, rmsL, sqrt(sumR / frames).toFloat())
        }
    }

    /** Uniform noise in [-1, 1] from xorshift32. */
    private fun noise(): Float {
        var x = rng
        x = x xor (x shl 13)
        x = x xor (x ushr 17)
        x = x xor (x shl 5)
        rng = x
        return (x.toFloat() / Int.MAX_VALUE.toFloat()).coerceIn(-1f, 1f)
    }
}

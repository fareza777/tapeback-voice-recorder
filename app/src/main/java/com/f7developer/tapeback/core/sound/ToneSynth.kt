package com.f7developer.tapeback.core.sound

import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.sin

/**
 * Procedural synthesis of the deck's mechanical sounds. Everything is generated from first
 * principles (decaying resonators + filtered noise), so the app ships no third-party audio.
 */
object ToneSynth {
    const val SAMPLE_RATE = 44_100

    private class Noise(seed: Int) {
        private var s = seed
        fun next(): Float {
            s = s xor (s shl 13); s = s xor (s ushr 17); s = s xor (s shl 5)
            return s.toFloat() / Int.MAX_VALUE
        }
    }

    private fun buffer(seconds: Float) = FloatArray((SAMPLE_RATE * seconds).toInt())

    /** Add a decaying sine resonator starting at [startSec]. */
    private fun FloatArray.resonator(startSec: Float, freq: Float, decaySec: Float, gain: Float, chirp: Float = 0f) {
        val start = (startSec * SAMPLE_RATE).toInt()
        var phase = 0.0
        for (i in start until size) {
            val t = (i - start).toFloat() / SAMPLE_RATE
            val env = exp(-t / decaySec)
            if (env < 0.0005f) break
            val f = freq * (1f + chirp * exp(-t / (decaySec * 0.5f)))
            phase += 2.0 * PI * f / SAMPLE_RATE
            this[i] += (sin(phase) * env * gain).toFloat()
        }
    }

    /** Add a burst of high-passed noise (the "tick" of plastic on plastic). */
    private fun FloatArray.click(startSec: Float, decaySec: Float, gain: Float, seed: Int) {
        val n = Noise(seed)
        val start = (startSec * SAMPLE_RATE).toInt()
        var prevIn = 0f
        var prevOut = 0f
        for (i in start until size) {
            val t = (i - start).toFloat() / SAMPLE_RATE
            val env = exp(-t / decaySec)
            if (env < 0.0005f) break
            val w = n.next()
            val hp = 0.85f * (prevOut + w - prevIn)
            prevIn = w
            prevOut = hp
            this[i] += hp * env * gain
        }
    }

    private fun FloatArray.normalized(peak: Float = 0.85f): FloatArray {
        var m = 0f
        for (v in this) m = max(m, kotlin.math.abs(v))
        if (m <= 0f) return this
        val g = peak / m
        for (i in indices) this[i] *= g
        return this
    }

    /** Light key press: short tick with a little plastic body. */
    fun keyClick(): FloatArray = buffer(0.09f).apply {
        click(0f, 0.0025f, 1f, 11)
        resonator(0f, 2_300f, 0.006f, 0.35f)
        resonator(0.002f, 210f, 0.02f, 0.45f)
    }.normalized(0.6f)

    /** Latching key (PLAY/REC): press tick, then the latch catching ~30 ms later. */
    fun keyLatch(): FloatArray = buffer(0.16f).apply {
        click(0f, 0.003f, 0.9f, 23)
        resonator(0f, 180f, 0.03f, 0.55f)
        click(0.032f, 0.002f, 1f, 37)
        resonator(0.032f, 1_650f, 0.009f, 0.45f)
        resonator(0.034f, 95f, 0.05f, 0.5f)
    }.normalized(0.75f)

    /** STOP: heavier clunk as the pinch roller releases. */
    fun stopClunk(): FloatArray = buffer(0.22f).apply {
        click(0f, 0.004f, 0.8f, 41)
        resonator(0f, 120f, 0.06f, 0.9f, chirp = 0.25f)
        resonator(0.004f, 860f, 0.015f, 0.3f)
        click(0.045f, 0.003f, 0.5f, 53)
    }.normalized(0.85f)

    /** EJECT: clunk followed by the door spring rattling open. */
    fun eject(): FloatArray = buffer(0.42f).apply {
        click(0f, 0.004f, 0.8f, 61)
        resonator(0f, 110f, 0.07f, 0.8f)
        resonator(0.05f, 640f, 0.12f, 0.35f, chirp = -0.15f)
        resonator(0.05f, 1_290f, 0.06f, 0.15f)
        click(0.19f, 0.006f, 0.4f, 71)
        resonator(0.19f, 300f, 0.05f, 0.35f)
    }.normalized(0.85f)

    /** Cassette pushed into the well: plastic slide then a snap. */
    fun insert(): FloatArray = buffer(0.3f).apply {
        val n = Noise(83)
        val slideEnd = (0.12f * SAMPLE_RATE).toInt()
        var lp = 0f
        for (i in 0 until slideEnd) {
            val t = i.toFloat() / slideEnd
            lp += 0.15f * (n.next() - lp)
            this[i] += lp * 0.35f * sin(PI.toFloat() * t)
        }
        click(0.13f, 0.003f, 1f, 89)
        resonator(0.13f, 150f, 0.05f, 0.7f)
        resonator(0.13f, 1_900f, 0.008f, 0.3f)
    }.normalized(0.8f)

    /**
     * Seamless 1-second loop of a fast-winding motor: harmonic whine at an integer number of
     * cycles per loop (no click at the seam) with tape friction noise and slight flutter.
     */
    fun windLoop(): FloatArray {
        val out = buffer(1f)
        val n = Noise(97)
        var lp = 0f
        val base = 196.0 // Hz, integer cycles in 1 s
        for (i in out.indices) {
            val t = i.toDouble() / SAMPLE_RATE
            val wobble = 1.0 + 0.08 * sin(2.0 * PI * 7.0 * t)
            val whine = sin(2.0 * PI * base * t) * 0.35 +
                sin(2.0 * PI * base * 2 * t) * 0.22 +
                sin(2.0 * PI * base * 3 * t) * 0.12 +
                sin(2.0 * PI * 1_176.0 * t) * 0.05
            lp += 0.35f * (n.next() - lp)
            out[i] = (whine * wobble * 0.6 + lp * 0.35).toFloat()
        }
        return out.normalized(0.4f)
    }
}

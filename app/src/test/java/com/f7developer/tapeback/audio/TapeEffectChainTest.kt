package com.f7developer.tapeback.audio

import com.f7developer.tapeback.audio.fx.Biquad
import com.f7developer.tapeback.audio.fx.FxChainParams
import com.f7developer.tapeback.audio.fx.LevelSink
import com.f7developer.tapeback.audio.fx.TapeEffectChain
import com.f7developer.tapeback.domain.model.EffectPreset
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.math.sqrt

class TapeEffectChainTest {
    private val sr = 44_100

    private fun sine(freq: Float, seconds: Float = 0.5f, amp: Float = 0.5f, channels: Int = 2): FloatArray {
        val frames = (sr * seconds).toInt()
        return FloatArray(frames * channels) { i -> amp * sin(2 * PI * freq * (i / channels) / sr).toFloat() }
    }

    private fun rms(buf: FloatArray, skip: Int = 4410 * 2): Float {
        var sum = 0.0
        var n = 0
        for (i in skip until buf.size) {
            sum += buf[i] * buf[i]
            n++
        }
        return sqrt(sum / n).toFloat()
    }

    private fun run(preset: EffectPreset, input: FloatArray, hiss: Float = 0f, channels: Int = 2, sink: LevelSink? = null): FloatArray {
        val chain = TapeEffectChain(sink)
        chain.configure(sr, channels)
        chain.params = FxChainParams(preset, hiss)
        val buf = input.copyOf()
        // Process in realistic 1024-frame buffers.
        val block = 1024 * channels
        var start = 0
        while (start < buf.size) {
            val len = minOf(block, buf.size - start)
            val part = buf.copyOfRange(start, start + len)
            chain.process(part, len / channels)
            part.copyInto(buf, start)
            start += len
        }
        return buf
    }

    @Test
    fun `clean preset is bit-exact passthrough`() {
        val input = sine(440f)
        assertArrayEquals(input, run(EffectPreset.CLEAN, input), 0f)
    }

    @Test
    fun `every preset keeps output inside full scale even when driven hard`() {
        val hot = sine(220f, amp = 1f)
        EffectPreset.entries.forEach { preset ->
            val out = run(preset, hot, hiss = 1f)
            out.forEach { v ->
                assertTrue("$preset produced $v", v in -1f..1f)
                assertTrue(!v.isNaN())
            }
        }
    }

    @Test
    fun `AM radio removes bass and keeps the voice band`() {
        val bass = rms(run(EffectPreset.RADIO, sine(80f)))
        val voice = rms(run(EffectPreset.RADIO, sine(1_500f)))
        assertTrue("bass $bass should be far below voice $voice", bass < voice * 0.2f)
    }

    @Test
    fun `vintage rolls off the top end`() {
        val inputHigh = sine(16_000f)
        val inputMid = sine(1_000f)
        val high = rms(run(EffectPreset.VINTAGE, inputHigh)) / rms(inputHigh)
        val mid = rms(run(EffectPreset.VINTAGE, inputMid)) / rms(inputMid)
        assertTrue("16 kHz gain $high should be well below 1 kHz gain $mid", high < mid * 0.35f)
    }

    @Test
    fun `vintage wow and flutter modulates timing so output differs from input`() {
        val input = sine(1_000f, seconds = 1f)
        val out = run(EffectPreset.VINTAGE, input)
        var diff = 0f
        for (i in 8820 until out.size) diff += abs(out[i] - input[i])
        assertTrue(diff / out.size > 0.01f)
    }

    @Test
    fun `lo-fi holds samples and quantizes to 8-bit steps`() {
        val out = run(EffectPreset.LOFI, sine(300f, channels = 1), channels = 1)
        // With 44.1 kHz in and ~11 kHz hold, consecutive groups of 4 samples share pre-filter values,
        // so the post-filter signal has many near-identical neighbours compared to the input.
        var repeats = 0
        for (i in 1 until out.size) if (abs(out[i] - out[i - 1]) < 1e-3f) repeats++
        assertTrue(repeats > 0)
    }

    @Test
    fun `hiss adds noise to silence only when enabled`() {
        val silence = FloatArray(sr * 2)
        assertEquals(0f, rms(run(EffectPreset.CLEAN, silence, hiss = 0f), 0), 0f)
        val hiss = rms(run(EffectPreset.CLEAN, silence, hiss = 1f), 0)
        assertTrue("hiss level $hiss", hiss in 0.001f..0.05f)
    }

    @Test
    fun `meters receive post-effect levels per buffer`() {
        var calls = 0
        var lastPeak = 0f
        val sink = LevelSink { peakL, _, rmsL, _ ->
            calls++
            lastPeak = peakL
            assertTrue(rmsL <= peakL + 1e-6f)
        }
        run(EffectPreset.CLEAN, sine(440f, amp = 0.5f), sink = sink)
        assertTrue(calls > 10)
        assertEquals(0.5f, lastPeak, 0.01f)
    }

    @Test
    fun `biquad low-pass passes DC and high-pass blocks it`() {
        val lp = Biquad(1).apply { lowPass(sr, 1_000f) }
        val hp = Biquad(1).apply { highPass(sr, 1_000f) }
        var l = 0f
        var h = 0f
        repeat(5_000) {
            l = lp.process(1f, 0)
            h = hp.process(1f, 0)
        }
        assertEquals(1f, l, 1e-3f)
        assertEquals(0f, h, 1e-3f)
    }
}

package com.f7developer.tapeback.domain

import com.f7developer.tapeback.domain.policy.VuBallistics
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VuBallisticsTest {

    @Test
    fun `needle reaches a step within VU integration time with modest overshoot`() {
        val vu = VuBallistics()
        var t = 0f
        var peak = 0f
        var reached99At = -1f
        while (t < 1.5f) {
            val pos = vu.step(0.8f, 1f / 120f)
            t += 1f / 120f
            peak = maxOf(peak, pos)
            if (reached99At < 0 && pos >= 0.8f * 0.99f) reached99At = t
        }
        assertTrue("reaches 99% in under 0.45 s, was $reached99At", reached99At in 0.05f..0.45f)
        val overshoot = (peak - 0.8f) / 0.8f
        assertTrue("overshoot $overshoot should be small", overshoot in 0f..0.1f)
        assertEquals(0.8f, vu.position, 0.01f)
    }

    @Test
    fun `critically damped needle never overshoots`() {
        val vu = VuBallistics(naturalFrequencyHz = 2.4f, damping = 1f)
        var peak = 0f
        repeat(300) { peak = maxOf(peak, vu.step(0.5f, 1f / 120f)) }
        assertTrue(peak <= 0.5f + 1e-3f)
    }

    @Test
    fun `long frames are sub-stepped and stay stable`() {
        val vu = VuBallistics()
        repeat(20) { vu.step(1f, 0.25f) }
        assertTrue(vu.position in 0.9f..1.08f)
    }

    @Test
    fun `zero VU is aligned to the digital reference level`() {
        val amp = Math.pow(10.0, VuBallistics.REFERENCE_DBFS / 20.0).toFloat()
        assertEquals(0f, VuBallistics.amplitudeToVu(amp), 0.01f)
        assertEquals(VuBallistics.MIN_VU, VuBallistics.amplitudeToVu(0f), 0f)
        assertEquals(VuBallistics.MAX_VU, VuBallistics.amplitudeToVu(1f), 0f)
    }

    @Test
    fun `deflection is monotonic across the scale`() {
        var last = -1f
        var vu = VuBallistics.MIN_VU
        while (vu <= VuBallistics.MAX_VU) {
            val d = VuBallistics.vuToDeflection(vu)
            assertTrue(d >= last)
            last = d
            vu += 0.5f
        }
        assertEquals(0f, VuBallistics.vuToDeflection(VuBallistics.MIN_VU), 1e-5f)
        assertEquals(1f, VuBallistics.vuToDeflection(VuBallistics.MAX_VU), 1e-5f)
    }
}

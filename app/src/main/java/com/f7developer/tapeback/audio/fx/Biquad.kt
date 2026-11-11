package com.f7developer.tapeback.audio.fx

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

/**
 * Multi-channel biquad filter (RBJ Audio EQ Cookbook), transposed direct form II.
 * Coefficient updates allocate nothing; channel state is sized once in [resize].
 */
class Biquad(channels: Int = 2) {
    private var b0 = 1f
    private var b1 = 0f
    private var b2 = 0f
    private var a1 = 0f
    private var a2 = 0f
    private var z1 = FloatArray(channels)
    private var z2 = FloatArray(channels)

    fun resize(channels: Int) {
        if (z1.size != channels) {
            z1 = FloatArray(channels)
            z2 = FloatArray(channels)
        }
    }

    fun reset() {
        z1.fill(0f)
        z2.fill(0f)
    }

    fun bypass() = set(1.0, 0.0, 0.0, 1.0, 0.0, 0.0)

    fun lowPass(sampleRate: Int, freq: Float, q: Float = 0.7071f) {
        val w0 = omega(sampleRate, freq)
        val alpha = sin(w0) / (2.0 * q)
        val c = cos(w0)
        set((1 - c) / 2, 1 - c, (1 - c) / 2, 1 + alpha, -2 * c, 1 - alpha)
    }

    fun highPass(sampleRate: Int, freq: Float, q: Float = 0.7071f) {
        val w0 = omega(sampleRate, freq)
        val alpha = sin(w0) / (2.0 * q)
        val c = cos(w0)
        set((1 + c) / 2, -(1 + c), (1 + c) / 2, 1 + alpha, -2 * c, 1 - alpha)
    }

    fun peaking(sampleRate: Int, freq: Float, q: Float, gainDb: Float) {
        val a = 10.0.pow(gainDb / 40.0)
        val w0 = omega(sampleRate, freq)
        val alpha = sin(w0) / (2.0 * q)
        val c = cos(w0)
        set(1 + alpha * a, -2 * c, 1 - alpha * a, 1 + alpha / a, -2 * c, 1 - alpha / a)
    }

    fun process(x: Float, channel: Int): Float {
        val y = b0 * x + z1[channel]
        z1[channel] = b1 * x - a1 * y + z2[channel]
        z2[channel] = b2 * x - a2 * y
        return y
    }

    private fun omega(sampleRate: Int, freq: Float): Double {
        val nyquistSafe = freq.coerceIn(10f, sampleRate * 0.45f)
        return 2.0 * PI * nyquistSafe / sampleRate
    }

    private fun set(nb0: Double, nb1: Double, nb2: Double, na0: Double, na1: Double, na2: Double) {
        b0 = (nb0 / na0).toFloat()
        b1 = (nb1 / na0).toFloat()
        b2 = (nb2 / na0).toFloat()
        a1 = (na1 / na0).toFloat()
        a2 = (na2 / na0).toFloat()
    }
}

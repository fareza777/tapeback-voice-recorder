package com.f7developer.tapeback.domain.policy

import kotlin.math.PI
import kotlin.math.log10
import kotlin.math.max

/**
 * Analog VU meter needle modelled as a damped mass-spring system.
 *
 * A standard VU meter reaches ~99% of a step in 300 ms with 1–1.5% overshoot. A natural
 * frequency of 2.1 Hz with damping ratio 0.62 gives a similar, slightly lively feel.
 */
class VuBallistics(
    naturalFrequencyHz: Float = 2.1f,
    private val damping: Float = 0.62f,
) {
    private val omega = (2.0 * PI * naturalFrequencyHz).toFloat()

    var position: Float = 0f
        private set
    var velocity: Float = 0f
        private set

    /** Advance the needle toward [target] (0..1 deflection) by [dtSeconds]. Returns the new position. */
    fun step(target: Float, dtSeconds: Float): Float {
        // Sub-step for stability when frames are long (e.g. after a jank or on resume).
        var remaining = dtSeconds.coerceIn(0f, 0.25f)
        while (remaining > 0f) {
            val dt = minOf(remaining, 1f / 240f)
            val accel = omega * omega * (target - position) - 2f * damping * omega * velocity
            velocity += accel * dt
            position += velocity * dt
            if (position < 0f) {
                position = 0f
                velocity = max(0f, velocity)
            }
            if (position > 1.08f) {
                position = 1.08f // the pin stop at the right of the scale
                velocity = minOf(0f, velocity)
            }
            remaining -= dt
        }
        return position
    }

    fun reset() {
        position = 0f
        velocity = 0f
    }

    companion object {
        /** Lowest marking on the scale, in VU. */
        const val MIN_VU = -20f

        /** Highest marking on the scale, in VU. */
        const val MAX_VU = 3f

        /** Digital reference: 0 VU is aligned to this dBFS value. */
        const val REFERENCE_DBFS = -14f

        /** Convert a linear amplitude (0..1 of full scale) to VU. */
        fun amplitudeToVu(amplitude: Float): Float {
            if (amplitude <= 0.00001f) return MIN_VU
            val dbfs = 20f * log10(amplitude)
            return (dbfs - REFERENCE_DBFS).coerceIn(MIN_VU, MAX_VU)
        }

        /**
         * Map VU to needle deflection 0..1. The real scale is non-linear (compressed at the
         * bottom), so we map through linear amplitude, which mimics the printed VU scale.
         */
        fun vuToDeflection(vu: Float): Float {
            val lin = Math.pow(10.0, (vu / 20f).toDouble()).toFloat()
            val minLin = Math.pow(10.0, (MIN_VU / 20f).toDouble()).toFloat()
            val maxLin = Math.pow(10.0, (MAX_VU / 20f).toDouble()).toFloat()
            return ((lin - minLin) / (maxLin - minLin)).coerceIn(0f, 1f)
        }

        fun amplitudeToDeflection(amplitude: Float): Float = vuToDeflection(amplitudeToVu(amplitude))
    }
}

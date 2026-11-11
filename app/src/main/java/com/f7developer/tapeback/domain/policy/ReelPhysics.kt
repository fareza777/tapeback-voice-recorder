package com.f7developer.tapeback.domain.policy

import kotlin.math.exp
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Tape transport physics for the animated cassette.
 *
 * Tape moves past the head at constant linear speed, so a reel's angular velocity is
 * ω = v / r. As tape winds from the supply reel to the take-up reel, the pack radii change while
 * the total tape area stays constant – which is why the empty reel spins fast at the start of a
 * side and slows down as it fills, exactly like a real deck.
 *
 * All radii are expressed as fractions of the maximum reel radius (1.0 = full pack).
 */
object ReelPhysics {
    /** Hub radius relative to a full pack (a C60 hub is roughly 40% of the full pack). */
    const val HUB_RADIUS = 0.42f

    /**
     * Linear tape speed in "full pack radii per second" for normal play. A compact cassette runs
     * at 4.76 cm/s with a ~2.6 cm full pack, i.e. ≈ 1.8 radii per second.
     */
    const val PLAY_SPEED = 1.8f

    /** Fast-wind multiplier used for REW/FF. */
    const val WIND_MULTIPLIER = 12f

    /** Tape thickness in the same units; chosen so a full side counts to ~600 like a real counter. */
    private const val TAPE_THICKNESS = (1f - HUB_RADIUS) / 600f

    /** Motor time constant in seconds (spin-up / spin-down inertia). */
    const val MOTOR_TAU_SECONDS = 0.18f

    data class Packs(val supply: Float, val takeUp: Float)

    /** Pack radii for a given progress (0 = start of tape, 1 = end). */
    fun packs(progress: Float): Packs {
        val p = progress.coerceIn(0f, 1f)
        val h2 = HUB_RADIUS * HUB_RADIUS
        val tapeArea = 1f - h2
        return Packs(
            supply = sqrt(h2 + (1f - p) * tapeArea),
            takeUp = sqrt(h2 + p * tapeArea),
        )
    }

    /** Angular velocity (radians per second) for a reel of [radius] at linear tape speed [linearSpeed]. */
    fun angularVelocity(linearSpeed: Float, radius: Float): Float {
        val r = radius.coerceAtLeast(HUB_RADIUS)
        return linearSpeed / r
    }

    /**
     * Mechanical 3-digit counter value. Real counters are geared to the take-up reel, so they
     * count turns, not seconds – the reading grows faster at the start of the tape.
     */
    fun counter(progress: Float): Int {
        val turns = (packs(progress).takeUp - HUB_RADIUS) / TAPE_THICKNESS
        return turns.roundToInt().coerceIn(0, 999)
    }

    /** Motor inertia: move [current] toward [target] over [dtSeconds]. */
    fun motorStep(current: Float, target: Float, dtSeconds: Float): Float {
        if (dtSeconds <= 0f) return current
        val alpha = 1f - exp(-dtSeconds / MOTOR_TAU_SECONDS)
        val next = current + (target - current) * alpha
        return if (kotlin.math.abs(next - target) < 0.0005f) target else next
    }
}

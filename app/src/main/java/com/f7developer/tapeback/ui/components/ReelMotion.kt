package com.f7developer.tapeback.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import com.f7developer.tapeback.domain.policy.ReelPhysics
import kotlinx.coroutines.flow.first
import kotlin.math.abs
import kotlin.math.sign

/** Reel angles in degrees, advanced by [rememberReelMotion]. Read them in draw lambdas only. */
@Stable
class ReelMotion {
    var left by mutableFloatStateOf(0f)
    var right by mutableFloatStateOf(0f)
}

/**
 * Drives both reels with ω = v / r and motor inertia.
 *
 * @param speed signed transport speed in multiples of normal play speed (1 = PLAY, 12 = FF, -12 = REW).
 * @param progress tape progress 0..1, which sets the pack radii.
 */
@Composable
fun rememberReelMotion(speed: Float, progress: () -> Float, reduceMotion: Boolean): ReelMotion {
    val motion = remember { ReelMotion() }
    val target by rememberUpdatedState(if (reduceMotion) speed.sign * minOf(abs(speed), 0.35f) else speed)
    val currentProgress by rememberUpdatedState(progress)
    LaunchedEffect(motion) {
        var current = 0f
        var last = 0L
        while (true) {
            if (current == 0f && target == 0f) {
                snapshotFlow { target }.first { it != 0f }
                last = 0L
            }
            withFrameNanos { now ->
                val dt = if (last == 0L) 0f else ((now - last) / 1_000_000_000f).coerceAtMost(0.1f)
                last = now
                current = ReelPhysics.motorStep(current, target, dt)
                if (current != 0f && dt > 0f) {
                    val packs = ReelPhysics.packs(currentProgress())
                    val v = ReelPhysics.PLAY_SPEED * current
                    val toDeg = 57.29578f
                    motion.left = (motion.left + ReelPhysics.angularVelocity(v, packs.supply) * toDeg * dt) % 360f
                    motion.right = (motion.right + ReelPhysics.angularVelocity(v, packs.takeUp) * toDeg * dt) % 360f
                }
            }
        }
    }
    return motion
}

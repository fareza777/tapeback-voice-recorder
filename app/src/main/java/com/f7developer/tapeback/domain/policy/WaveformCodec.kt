package com.f7developer.tapeback.domain.policy

import kotlin.math.log10
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Stores a recording's loudness envelope compactly (one byte per bin, ≤ [MAX_BINS] bins).
 * Values are perceptually scaled: 0 dBFS → 1.0, [FLOOR_DB] and below → 0.0.
 */
object WaveformCodec {
    const val MAX_BINS = 200
    private const val FLOOR_DB = -48f

    /** Convert a linear amplitude (0..1) to a perceptual 0..1 display level. */
    fun perceptual(amplitude: Float): Float {
        if (amplitude <= 0f) return 0f
        val db = 20f * log10(amplitude)
        return ((db - FLOOR_DB) / -FLOOR_DB).coerceIn(0f, 1f)
    }

    /** Down-sample a sequence of 0..1 levels into at most [bins] bins by peak-picking. */
    fun resample(levels: List<Float>, bins: Int = MAX_BINS): List<Float> {
        if (levels.isEmpty()) return emptyList()
        if (levels.size <= bins) return levels.map { it.coerceIn(0f, 1f) }
        val out = ArrayList<Float>(bins)
        val step = levels.size.toFloat() / bins
        for (i in 0 until bins) {
            val start = (i * step).toInt()
            val end = max(start + 1, ((i + 1) * step).toInt()).coerceAtMost(levels.size)
            var peak = 0f
            for (j in start until end) peak = max(peak, levels[j])
            out += peak.coerceIn(0f, 1f)
        }
        return out
    }

    fun encode(levels: List<Float>): ByteArray {
        val resampled = resample(levels)
        return ByteArray(resampled.size) { i -> (resampled[i] * 255f).roundToInt().coerceIn(0, 255).toByte() }
    }

    fun decode(bytes: ByteArray?): List<Float> {
        if (bytes == null || bytes.isEmpty()) return emptyList()
        return List(bytes.size) { i -> (bytes[i].toInt() and 0xFF) / 255f }
    }
}

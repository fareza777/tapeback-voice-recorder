package com.f7developer.tapeback.audio.fx

/**
 * Lock-free hand-off of meter levels from an audio thread to the UI frame loop.
 * Writers call [publish]; the UI polls [left]/[right] each frame and treats stale values as silence.
 */
class LevelBus(private val nanoClock: () -> Long = System::nanoTime) : LevelSink {
    @Volatile private var l = 0f
    @Volatile private var r = 0f
    @Volatile private var stamp = 0L

    fun publish(left: Float, right: Float) {
        l = left
        r = right
        stamp = nanoClock()
    }

    override fun onLevels(peakLeft: Float, peakRight: Float, rmsLeft: Float, rmsRight: Float) {
        // VU meters respond to average level; blend in some peak so speech transients read naturally.
        publish(rmsLeft * 1.25f + peakLeft * 0.15f, rmsRight * 1.25f + peakRight * 0.15f)
    }

    private fun fresh(): Boolean = nanoClock() - stamp < STALE_NANOS

    val left: Float get() = if (fresh()) l.coerceIn(0f, 1f) else 0f
    val right: Float get() = if (fresh()) r.coerceIn(0f, 1f) else 0f

    fun clear() = publish(0f, 0f)

    companion object {
        private const val STALE_NANOS = 250_000_000L
    }
}

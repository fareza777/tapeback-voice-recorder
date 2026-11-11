package com.f7developer.tapeback.ui.components

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.max

/**
 * Stored waveform with played/unplayed coloring and tap/drag seeking.
 * [progress] is read in the draw phase so the playhead moves without recomposition.
 */
@Composable
fun WaveformScrubber(
    waveform: List<Float>,
    progress: () -> Float,
    onSeek: (Float) -> Unit,
    playedColor: Color,
    idleColor: Color,
    modifier: Modifier = Modifier,
    height: Dp = 56.dp,
    enabled: Boolean = true,
    animate: Boolean = false,
    contentDescription: String,
) {
    val seek by rememberUpdatedState(onSeek)
    // Redraw every frame while the tape moves; progress() itself is not snapshot state.
    var frame by remember { mutableLongStateOf(0L) }
    if (animate) {
        LaunchedEffect(Unit) { while (true) withFrameNanos { frame = it } }
    }
    var dragFraction by remember { mutableStateOf<Float?>(null) }
    val bars = remember(waveform) { if (waveform.isEmpty()) List(64) { 0.08f } else waveform }
    Spacer(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .semantics {
                this.contentDescription = contentDescription
                progressBarRangeInfo = ProgressBarRangeInfo(progress().coerceIn(0f, 1f), 0f..1f)
                if (enabled) {
                    setProgress { value ->
                        seek(value.coerceIn(0f, 1f))
                        true
                    }
                }
            }
            .then(
                if (!enabled) Modifier else Modifier
                    .pointerInput(Unit) {
                        detectTapGestures { pos -> seek((pos.x / size.width).coerceIn(0f, 1f)) }
                    }
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragStart = { pos -> dragFraction = (pos.x / size.width).coerceIn(0f, 1f) },
                            onDragEnd = {
                                dragFraction?.let(seek)
                                dragFraction = null
                            },
                            onDragCancel = { dragFraction = null },
                            onHorizontalDrag = { change, _ ->
                                dragFraction = (change.position.x / size.width).coerceIn(0f, 1f)
                            },
                        )
                    },
            )
            .drawBehind {
                @Suppress("UNUSED_EXPRESSION") frame
                val p = (dragFraction ?: progress()).coerceIn(0f, 1f)
                val n = bars.size
                val gap = 2.dp.toPx()
                val barW = max(1.5f, (size.width - gap * (n - 1)) / n)
                val step = barW + gap
                val mid = size.height / 2f
                for (i in 0 until n) {
                    val x = i * step
                    val h = max(3.dp.toPx(), bars[i] * size.height * 0.92f)
                    val played = (x + barW / 2f) / size.width <= p
                    drawRoundRect(
                        color = if (played) playedColor else idleColor,
                        topLeft = Offset(x, mid - h / 2f),
                        size = Size(barW, h),
                        cornerRadius = CornerRadius(barW / 2f),
                    )
                }
                val head = p * size.width
                drawLine(Color.White.copy(alpha = 0.9f), Offset(head, 0f), Offset(head, size.height), 2.dp.toPx())
                drawCircle(playedColor, 5.dp.toPx(), Offset(head, 4.dp.toPx()))
            },
    )
}

/** Scrolling live input waveform while recording. [tick] changes whenever a new sample arrives. */
@Composable
fun LiveWaveform(
    levels: FloatArray,
    head: () -> Int,
    tick: Long,
    color: Color,
    modifier: Modifier = Modifier,
    height: Dp = 56.dp,
) {
    Spacer(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .drawBehind {
                @Suppress("UNUSED_EXPRESSION") tick
                val n = levels.size
                val gap = 2.dp.toPx()
                val barW = max(1.5f, (size.width - gap * (n - 1)) / n)
                val step = barW + gap
                val mid = size.height / 2f
                val h0 = head()
                for (i in 0 until n) {
                    // Oldest sample on the left, newest on the right.
                    val idx = (h0 + 1 + i) % n
                    val v = levels[idx]
                    val h = max(3.dp.toPx(), v * size.height * 0.92f)
                    val alpha = 0.35f + 0.65f * (i.toFloat() / n)
                    drawRoundRect(
                        color = color.copy(alpha = alpha),
                        topLeft = Offset(i * step, mid - h / 2f),
                        size = Size(barW, h),
                        cornerRadius = CornerRadius(barW / 2f),
                    )
                }
            },
    )
}

/** Small static waveform for list rows. */
@Composable
fun MiniWaveform(waveform: List<Float>, color: Color, modifier: Modifier = Modifier, bars: Int = 40) {
    val sampled = remember(waveform, bars) {
        if (waveform.isEmpty()) emptyList()
        else List(bars) { i ->
            val start = i * waveform.size / bars
            val end = max(start + 1, (i + 1) * waveform.size / bars).coerceAtMost(waveform.size)
            var peak = 0f
            for (j in start until end) peak = max(peak, waveform[j])
            peak
        }
    }
    Spacer(
        modifier = modifier.drawBehind {
            if (sampled.isEmpty()) return@drawBehind
            val gap = 1.5.dp.toPx()
            val barW = max(1f, (size.width - gap * (sampled.size - 1)) / sampled.size)
            val mid = size.height / 2f
            sampled.forEachIndexed { i, v ->
                val h = max(2.dp.toPx(), v * size.height)
                drawRoundRect(color, Offset(i * (barW + gap), mid - h / 2f), Size(barW, h), CornerRadius(barW / 2f))
            }
        },
    )
}

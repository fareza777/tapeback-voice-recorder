package com.f7developer.tapeback.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import kotlinx.coroutines.flow.first
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import com.f7developer.tapeback.domain.policy.VuBallistics
import com.f7developer.tapeback.ui.theme.BungeeFamily
import com.f7developer.tapeback.ui.theme.GroteskFamily
import com.f7developer.tapeback.ui.theme.Retro
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private const val SWEEP = 76f // degrees of needle travel
private val SCALE_MARKS = listOf(-20f, -10f, -7f, -5f, -3f, -1f, 0f, 1f, 2f, 3f)

/**
 * Analog VU meter with spring-damper needle ballistics and a peak LED.
 * @param level linear amplitude 0..1, polled every frame.
 */
@Composable
fun VuMeter(
    channelLabel: String,
    level: () -> Float,
    active: Boolean,
    modifier: Modifier = Modifier,
    contentDescription: String,
) {
    val palette = Retro.palette
    val reduceMotion = Retro.reduceMotion
    val measurer = rememberTextMeasurer(cacheSize = 16)
    var needle by remember { mutableFloatStateOf(0f) }
    var peakUntil by remember { mutableLongStateOf(0L) }
    var coarseVu by remember { mutableIntStateOf(VuBallistics.MIN_VU.roundToInt()) }
    val currentLevel by rememberUpdatedState(level)
    val isActive by rememberUpdatedState(active)

    LaunchedEffect(reduceMotion) {
        // Critically damped when motion is reduced: no overshoot, same information.
        val ballistics = if (reduceMotion) VuBallistics(naturalFrequencyHz = 2.4f, damping = 1f) else VuBallistics()
        var last = 0L
        var lastCoarse = 0L
        while (true) {
            if (!isActive && needle < 0.001f) {
                needle = 0f
                ballistics.reset()
                snapshotFlow { isActive }.first { it }
                last = 0L
            }
            withFrameNanos { now ->
                val dt = if (last == 0L) 0f else (now - last) / 1_000_000_000f
                last = now
                val amp = if (isActive) currentLevel() else 0f
                val target = VuBallistics.amplitudeToDeflection(amp)
                needle = ballistics.step(target, dt)
                val nowMs = now / 1_000_000L
                if (VuBallistics.amplitudeToVu(amp) >= -1f) peakUntil = nowMs + 1_000L
                if (nowMs - lastCoarse > 500L) {
                    lastCoarse = nowMs
                    coarseVu = VuBallistics.amplitudeToVu(amp).roundToInt()
                }
            }
        }
    }

    Spacer(
        modifier = modifier
            .aspectRatio(1.45f)
            .graphicsLayer()
            .semantics {
                this.contentDescription = contentDescription
                stateDescription = if (!active) "Idle" else "$coarseVu VU"
            }
            .drawWithCache {
                val w = size.width
                val h = size.height
                val pivot = Offset(w / 2f, h * 1.12f)
                val radius = h * 0.82f
                val face = Brush.verticalGradient(listOf(lerp(palette.vuFace, Color.White, 0.25f), palette.vuFace, lerp(palette.vuFace, Color.Black, 0.12f)))
                val glass = Brush.linearGradient(
                    listOf(Color.White.copy(alpha = 0.22f), Color.Transparent),
                    start = Offset.Zero, end = Offset(w * 0.7f, h * 0.6f),
                )
                val ink = palette.vuInk
                val red = Color(0xFFD7261E)
                val markStyle = TextStyle(fontFamily = GroteskFamily, fontWeight = FontWeight.Bold, fontSize = (h * 0.085f).toSp())
                val marks = SCALE_MARKS.map { vu ->
                    val label = when {
                        vu > 0 -> "+${vu.toInt()}"
                        else -> vu.toInt().toString()
                    }
                    Triple(vu, measurer.measure(label, markStyle.copy(color = if (vu > 0) red else ink)), label)
                }
                val vuLayout = measurer.measure("VU", TextStyle(fontFamily = BungeeFamily, fontSize = (h * 0.16f).toSp(), color = ink))
                val chLayout = measurer.measure(channelLabel, TextStyle(fontFamily = GroteskFamily, fontWeight = FontWeight.Bold, fontSize = (h * 0.09f).toSp(), color = ink.copy(alpha = 0.7f)))
                fun angleFor(vu: Float) = -SWEEP / 2f + VuBallistics.vuToDeflection(vu) * SWEEP
                fun point(angleDeg: Float, r: Float): Offset {
                    val rad = Math.toRadians(angleDeg.toDouble())
                    return Offset(pivot.x + (sin(rad) * r).toFloat(), pivot.y - (cos(rad) * r).toFloat())
                }
                val redArc = Path().apply {
                    val a0 = angleFor(0f)
                    val a1 = angleFor(3f)
                    val steps = 16
                    for (i in 0..steps) {
                        val a = a0 + (a1 - a0) * i / steps
                        val pt = point(a, radius)
                        if (i == 0) moveTo(pt.x, pt.y) else lineTo(pt.x, pt.y)
                    }
                }
                val baseArc = Path().apply {
                    val a0 = angleFor(-20f)
                    val a1 = angleFor(0f)
                    val steps = 32
                    for (i in 0..steps) {
                        val a = a0 + (a1 - a0) * i / steps
                        val pt = point(a, radius)
                        if (i == 0) moveTo(pt.x, pt.y) else lineTo(pt.x, pt.y)
                    }
                }
                val corner = CornerRadius(h * 0.08f)

                onDrawBehind {
                    drawRoundRect(Color.Black.copy(alpha = 0.55f), cornerRadius = corner)
                    val inset = h * 0.04f
                    drawRoundRect(face, Offset(inset, inset), Size(w - inset * 2, h - inset * 2), corner)
                    drawPath(baseArc, ink, style = Stroke(h * 0.012f))
                    drawPath(redArc, red, style = Stroke(h * 0.05f))
                    marks.forEach { (vu, layout, _) ->
                        val a = angleFor(vu)
                        val major = vu == -20f || vu == -10f || vu == -5f || vu == 0f || vu == 3f
                        drawLine(
                            if (vu > 0) red else ink,
                            point(a, radius),
                            point(a, radius + h * if (major) 0.09f else 0.06f),
                            strokeWidth = h * 0.012f,
                        )
                        val lp = point(a, radius + h * 0.17f)
                        drawText(layout, topLeft = Offset(lp.x - layout.size.width / 2f, lp.y - layout.size.height / 2f))
                    }
                    drawText(vuLayout, topLeft = Offset(w / 2f - vuLayout.size.width / 2f, h * 0.56f))
                    drawText(chLayout, topLeft = Offset(w * 0.1f, h * 0.7f))

                    // Peak LED
                    val ledOn = isActive && (System.nanoTime() / 1_000_000L) < peakUntil
                    val ledCenter = Offset(w * 0.88f, h * 0.76f)
                    drawCircle(Color.Black.copy(alpha = 0.4f), h * 0.055f, ledCenter)
                    drawCircle(if (ledOn) red else lerp(red, Color.Black, 0.65f), h * 0.04f, ledCenter)
                    if (ledOn) drawCircle(red.copy(alpha = 0.35f), h * 0.09f, ledCenter)

                    // Needle
                    val angle = -SWEEP / 2f + needle * SWEEP
                    rotate(angle, pivot = pivot) {
                        drawLine(
                            Color.Black.copy(alpha = 0.18f),
                            Offset(pivot.x + h * 0.02f, pivot.y - h * 0.2f),
                            Offset(pivot.x + h * 0.02f, pivot.y - radius - h * 0.04f),
                            strokeWidth = h * 0.02f,
                        )
                        drawLine(
                            palette.vuNeedle,
                            Offset(pivot.x, pivot.y - h * 0.2f),
                            Offset(pivot.x, pivot.y - radius - h * 0.05f),
                            strokeWidth = h * 0.016f,
                            cap = StrokeCap.Round,
                        )
                    }
                    // Needle pivot cover
                    drawRect(
                        Brush.verticalGradient(listOf(lerp(palette.panelTop, Color.Black, 0.2f), palette.panelBottom)),
                        Offset(0f, h * 0.86f), Size(w, h * 0.14f),
                    )
                    drawRoundRect(glass, Offset(inset, inset), Size(w - inset * 2, h - inset * 2), corner)
                }
            },
    )
}

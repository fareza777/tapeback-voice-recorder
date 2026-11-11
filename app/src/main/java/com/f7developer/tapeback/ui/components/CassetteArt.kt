package com.f7developer.tapeback.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import com.f7developer.tapeback.domain.model.CassetteSkin
import com.f7developer.tapeback.domain.policy.ReelPhysics
import com.f7developer.tapeback.ui.theme.BungeeFamily
import com.f7developer.tapeback.ui.theme.GroteskFamily
import com.f7developer.tapeback.ui.theme.MarkerFamily

/** Physical cassette proportions (100.4 mm × 63.8 mm). */
const val CASSETTE_ASPECT = 1000f / 636f

private const val W = 1000f
private const val H = 636f
private val LEFT_REEL = Offset(330f, 250f)
private val RIGHT_REEL = Offset(670f, 250f)
private const val FULL_PACK = 118f
private const val HUB = 44f

private val TapeDark = Color(0xFF2A170C)
private val TapeLight = Color(0xFF5B3A22)
private val HubWhite = Color(0xFFF2F0EB)
private val HubShade = Color(0xFFB9B4AA)
private val StripeA = Color(0xFFE85D3A)
private val StripeB = Color(0xFFF2A541)

/**
 * A fully vector-drawn compact cassette. Reel angles and tape progress are lambdas read only in
 * the draw phase, so spinning the reels redraws without recomposition.
 */
@Composable
fun CassetteArt(
    skin: CassetteSkin,
    labelColor: Color,
    title: String,
    sideLetter: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    sealedText: String? = null,
    progress: () -> Float = { 0f },
    leftAngle: () -> Float = { 0f },
    rightAngle: () -> Float = { 0f },
    contentDescription: String? = null,
) {
    val style = CassetteSkins.style(skin)
    val measurer = rememberTextMeasurer(cacheSize = 8)
    val ink = if (labelColor.luminance() > 0.45f) Color(0xFF1E1A16) else Color(0xFFF7F3EA)
    val semanticsModifier = if (contentDescription != null) Modifier.semantics { this.contentDescription = contentDescription } else Modifier

    Spacer(
        modifier = modifier
            .aspectRatio(CASSETTE_ASPECT)
            .then(semanticsModifier)
            .drawWithCache {
                val s = size.width / W
                val shell = Path().apply {
                    addRoundRect(RoundRect(0f, 0f, W, H, CornerRadius(36f)))
                }
                val windowRect = RoundRect(205f, 168f, 795f, 334f, CornerRadius(26f))
                val window = Path().apply { addRoundRect(windowRect) }
                val windowFrame = RoundRect(190f, 154f, 810f, 348f, CornerRadius(34f))
                val label = RoundRect(55f, 45f, 945f, 400f, CornerRadius(18f))
                val head = Path().apply {
                    moveTo(212f, H)
                    lineTo(262f, 452f)
                    lineTo(738f, 452f)
                    lineTo(788f, H)
                    close()
                }
                val shellBrush = Brush.verticalGradient(listOf(style.shellTop, style.shellBottom), 0f, H)
                val headColor = lerp(style.shellBottom, Color.Black, 0.18f)
                val gloss = Brush.linearGradient(
                    listOf(Color.White.copy(alpha = 0.16f), Color.Transparent),
                    start = Offset(0f, 0f), end = Offset(520f, 460f),
                )
                val windowGloss = Brush.linearGradient(
                    listOf(Color.White.copy(alpha = 0.18f), Color.Transparent, Color.White.copy(alpha = 0.06f)),
                    start = Offset(205f, 168f), end = Offset(795f, 334f),
                )
                val packBrushL = Brush.radialGradient(listOf(TapeLight, TapeDark), LEFT_REEL, FULL_PACK)
                val packBrushR = Brush.radialGradient(listOf(TapeLight, TapeDark), RIGHT_REEL, FULL_PACK)
                val screwBrush = Brush.linearGradient(listOf(Color.White, style.screw, lerp(style.screw, Color.Black, 0.45f)))

                fun px(v: Float) = (v * s).toSp()
                val titleLayout = measurer.measure(
                    text = title.ifBlank { " " },
                    style = TextStyle(fontFamily = MarkerFamily, fontSize = px(if (subtitle.isNullOrBlank()) 54f else 46f), color = ink),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    constraints = Constraints(maxWidth = (640f * s).toInt().coerceAtLeast(1)),
                )
                val sideLayout = measurer.measure(
                    text = sideLetter,
                    style = TextStyle(fontFamily = BungeeFamily, fontSize = px(64f), color = ink),
                )
                val subLayout = subtitle?.takeIf { it.isNotBlank() }?.let {
                    measurer.measure(
                        text = it.uppercase(),
                        style = TextStyle(fontFamily = GroteskFamily, fontWeight = FontWeight.Bold, fontSize = px(21f), color = ink.copy(alpha = 0.7f)),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        constraints = Constraints(maxWidth = (600f * s).toInt().coerceAtLeast(1)),
                    )
                }
                val sealLayout = sealedText?.let {
                    measurer.measure(
                        text = it.uppercase(),
                        style = TextStyle(fontFamily = BungeeFamily, fontSize = px(46f), color = Color(0xFFFFF4E0)),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        constraints = Constraints(maxWidth = (820f * s).toInt().coerceAtLeast(1)),
                    )
                }

                onDrawBehind {
                    val p = progress()
                    val packs = ReelPhysics.packs(p)
                    val aL = leftAngle()
                    val aR = rightAngle()

                    withTransform({ scale(s, s, pivot = Offset.Zero) }) {
                        style.glow?.let { glow ->
                            for (i in 1..4) {
                                val g = i * 7f
                                drawRoundRect(
                                    color = glow.copy(alpha = 0.07f),
                                    topLeft = Offset(-g, -g),
                                    size = Size(W + 2 * g, H + 2 * g),
                                    cornerRadius = CornerRadius(36f + g),
                                )
                            }
                        }
                        // Clear shells reveal the full tape packs through the plastic.
                        if (style.translucent) {
                            drawCircle(packBrushL, FULL_PACK * packs.supply, LEFT_REEL)
                            drawCircle(packBrushR, FULL_PACK * packs.takeUp, RIGHT_REEL)
                            drawLine(TapeDark, Offset(LEFT_REEL.x - FULL_PACK * packs.supply * 0.3f, 360f), Offset(248f, 600f), 5f)
                            drawLine(TapeDark, Offset(RIGHT_REEL.x + FULL_PACK * packs.takeUp * 0.3f, 360f), Offset(752f, 600f), 5f)
                        }
                        drawPath(shell, shellBrush, alpha = style.shellAlpha)
                        drawPath(shell, Color.White.copy(alpha = 0.16f), style = Stroke(3f))
                        drawRoundRect(
                            color = Color.Black.copy(alpha = 0.22f),
                            topLeft = Offset(10f, 10f),
                            size = Size(W - 20f, H - 20f),
                            cornerRadius = CornerRadius(30f),
                            style = Stroke(2f),
                        )

                        // Label sticker
                        drawRoundRect(
                            color = labelColor,
                            topLeft = Offset(label.left, label.top),
                            size = Size(label.width, label.height),
                            cornerRadius = CornerRadius(18f),
                        )
                        drawRect(StripeA, Offset(55f, 356f), Size(890f, 16f))
                        drawRect(StripeB, Offset(55f, 376f), Size(890f, 9f))
                        if (subLayout == null) drawLine(ink.copy(alpha = 0.18f), Offset(160f, 146f), Offset(910f, 146f), 2.5f)
                        drawRoundRect(
                            color = ink.copy(alpha = 0.85f),
                            topLeft = Offset(76f, 62f),
                            size = Size(70f, 78f),
                            cornerRadius = CornerRadius(10f),
                            style = Stroke(4f),
                        )

                        // Window frame + window with tape packs and hubs
                        drawRoundRect(
                            color = lerp(style.shellBottom, Color.Black, 0.35f),
                            topLeft = Offset(windowFrame.left, windowFrame.top),
                            size = Size(windowFrame.width, windowFrame.height),
                            cornerRadius = CornerRadius(34f),
                        )
                        clipPath(window) {
                            drawRect(style.window, Offset(205f, 168f), Size(590f, 166f))
                            drawCircle(packBrushL, FULL_PACK * packs.supply, LEFT_REEL)
                            drawCircle(packBrushR, FULL_PACK * packs.takeUp, RIGHT_REEL)
                            for (k in 1..3) {
                                val f = k / 4f
                                drawCircle(Color.Black.copy(alpha = 0.18f), FULL_PACK * packs.supply * (0.55f + 0.45f * f), LEFT_REEL, style = Stroke(1.5f))
                                drawCircle(Color.Black.copy(alpha = 0.18f), FULL_PACK * packs.takeUp * (0.55f + 0.45f * f), RIGHT_REEL, style = Stroke(1.5f))
                            }
                            // Tape-remaining scale between the hubs
                            for (i in 0..8) {
                                val x = 440f + i * 15f
                                val tall = i % 4 == 0
                                drawLine(
                                    Color.White.copy(alpha = 0.55f),
                                    Offset(x, if (tall) 212f else 222f),
                                    Offset(x, 240f),
                                    2.5f,
                                )
                            }
                            drawHub(LEFT_REEL, aL)
                            drawHub(RIGHT_REEL, aR)
                            drawRect(windowGloss, Offset(205f, 168f), Size(590f, 166f))
                        }
                        drawPath(window, Color.White.copy(alpha = 0.25f), style = Stroke(2.5f))

                        // Head area
                        drawPath(head, headColor, alpha = if (style.translucent) 0.92f else 1f)
                        drawPath(head, Color.White.copy(alpha = 0.12f), style = Stroke(2.5f))
                        val hole = Color(0xFF050505)
                        drawCircle(hole, 15f, Offset(300f, 560f))
                        drawCircle(hole, 15f, Offset(700f, 560f))
                        drawCircle(hole, 9f, Offset(272f, 604f))
                        drawCircle(hole, 9f, Offset(728f, 604f))
                        drawRect(hole, Offset(372f, 592f), Size(30f, 30f))
                        drawRect(hole, Offset(598f, 592f), Size(30f, 30f))
                        drawRect(hole, Offset(462f, 588f), Size(76f, 48f))
                        drawLine(TapeLight, Offset(236f, 624f), Offset(764f, 624f), 7f, cap = StrokeCap.Round)
                        drawRect(Color(0xFFBFA77A), Offset(480f, 608f), Size(40f, 14f)) // pressure pad felt

                        // Screws
                        listOf(
                            Offset(36f, 36f), Offset(964f, 36f), Offset(36f, 600f),
                            Offset(964f, 600f), Offset(500f, 512f),
                        ).forEach { drawScrew(it, screwBrush) }

                        // Seal strap for locked time capsules
                        if (sealLayout != null) {
                            drawRect(
                                Brush.verticalGradient(listOf(Color(0xFFC0392B), Color(0xFF7B1E14)), 205f, 300f),
                                Offset(0f, 210f), Size(W, 86f),
                            )
                            drawLine(Color(0xFFFFD27A), Offset(0f, 216f), Offset(W, 216f), 3f)
                            drawLine(Color(0xFFFFD27A), Offset(0f, 290f), Offset(W, 290f), 3f)
                        }

                        drawPath(shell, gloss)
                    }

                    // Text is laid out in device pixels for crisp rendering.
                    drawText(sideLayout, topLeft = Offset((111f * s) - sideLayout.size.width / 2f, (101f * s) - sideLayout.size.height / 2f))
                    if (subLayout != null) {
                        drawText(titleLayout, topLeft = Offset(170f * s, 52f * s))
                        drawText(subLayout, topLeft = Offset(172f * s, 150f * s - subLayout.size.height))
                    } else {
                        drawText(titleLayout, topLeft = Offset(170f * s, 132f * s - titleLayout.size.height))
                    }
                    sealLayout?.let {
                        drawText(it, topLeft = Offset((W * s - it.size.width) / 2f, 253f * s - it.size.height / 2f))
                    }
                }
            },
    )
}

private fun DrawScope.drawHub(center: Offset, angleDegrees: Float) {
    drawCircle(Brush.radialGradient(listOf(HubWhite, HubShade), center, HUB), HUB, center)
    drawCircle(Color.Black.copy(alpha = 0.25f), HUB, center, style = Stroke(2f))
    drawCircle(Color(0xFF151515), HUB * 0.6f, center)
    rotate(angleDegrees, pivot = center) {
        for (i in 0 until 6) {
            rotate(i * 60f, pivot = center) {
                drawRect(
                    color = HubWhite,
                    topLeft = Offset(center.x - 4.5f, center.y - HUB * 0.6f - 1f),
                    size = Size(9f, 12f),
                )
            }
        }
        // A molded marker on the hub flange makes rotation readable at a glance.
        drawCircle(Color(0xFF8E887E), 4.5f, Offset(center.x, center.y + HUB * 0.82f))
    }
}

private fun DrawScope.drawScrew(center: Offset, brush: Brush) {
    drawCircle(Color.Black.copy(alpha = 0.35f), 15f, center + Offset(0f, 2f))
    drawCircle(brush, 13f, center)
    val d = 7f
    val c = Color.Black.copy(alpha = 0.55f)
    drawLine(c, center + Offset(-d, -d), center + Offset(d, d), 3f)
    drawLine(c, center + Offset(-d, d), center + Offset(d, -d), 3f)
}

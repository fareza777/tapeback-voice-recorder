package com.f7developer.tapeback.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.snap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.f7developer.tapeback.ui.theme.Retro
import com.f7developer.tapeback.ui.theme.SilkscreenStyle

enum class KeySymbol { REC, PLAY, REW, FF, STOP, PAUSE }

data class TransportKey(
    val symbol: KeySymbol,
    val label: String,
    val contentDescription: String,
    val latched: Boolean,
    val enabled: Boolean,
    val onPress: () -> Unit,
)

/** A row of chunky piano keys. Latched keys stay down like a mechanical deck. */
@Composable
fun TransportKeys(keys: List<TransportKey>, modifier: Modifier = Modifier) {
    val palette = Retro.palette
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.55f), Color.Black.copy(alpha = 0.3f))))
            .padding(horizontal = 6.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        keys.forEach { key ->
            PianoKey(key, palette.rec, Modifier.weight(1f))
        }
    }
}

@Composable
private fun PianoKey(key: TransportKey, recColor: Color, modifier: Modifier) {
    val palette = Retro.palette
    val reduceMotion = Retro.reduceMotion
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val down = pressed || key.latched
    val travel by animateFloatAsState(
        targetValue = when {
            pressed -> 1f
            key.latched -> 0.72f
            else -> 0f
        },
        animationSpec = if (reduceMotion) snap() else spring(dampingRatio = 0.55f, stiffness = 1400f),
        label = "keyTravel",
    )
    val lip = 9.dp
    val stateText = if (key.latched) "Engaged" else "Released"
    Column(
        modifier = modifier
            .heightIn(min = 86.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = key.contentDescription
                selected = key.latched
                stateDescription = stateText
            }
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = key.enabled,
                role = Role.Button,
                onClick = key.onPress,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .drawBehind {
                    val lipPx = lip.toPx() * (1f - travel * 0.8f)
                    val keyH = size.height - lip.toPx()
                    val top = lip.toPx() - lipPx
                    val r = CornerRadius(8.dp.toPx())
                    // Shadow + front lip of the key
                    drawRoundRect(
                        color = Color.Black.copy(alpha = 0.5f),
                        topLeft = Offset(0f, top + 3f),
                        size = Size(size.width, keyH + lipPx),
                        cornerRadius = r,
                    )
                    drawRoundRect(
                        color = lerp(palette.keyBottom, Color.Black, 0.35f),
                        topLeft = Offset(0f, top),
                        size = Size(size.width, keyH + lipPx),
                        cornerRadius = r,
                    )
                    val faceTop = if (down) lerp(palette.keyTop, Color.Black, 0.08f) else palette.keyTop
                    drawRoundRect(
                        brush = Brush.verticalGradient(listOf(faceTop, palette.keyBottom), top, top + keyH),
                        topLeft = Offset(0f, top),
                        size = Size(size.width, keyH),
                        cornerRadius = r,
                    )
                    drawRoundRect(
                        color = Color.White.copy(alpha = if (down) 0.08f else 0.28f),
                        topLeft = Offset(3f, top + 2f),
                        size = Size(size.width - 6f, keyH * 0.18f),
                        cornerRadius = r,
                    )
                },
        ) {
            Canvas(
                modifier = Modifier
                    .size(26.dp)
                    .align(Alignment.Center)
                    .offset { IntOffset(0, ((-4).dp + lip * travel * 0.8f).roundToPx()) },
            ) {
                val lit = key.enabled || key.latched
                val ink = if (lit) palette.keyText else palette.keyText.copy(alpha = 0.35f)
                drawSymbol(key.symbol, if (key.symbol == KeySymbol.REC) recColor.copy(alpha = if (lit) 1f else 0.4f) else ink)
            }
            if (key.latched && (key.symbol == KeySymbol.REC || key.symbol == KeySymbol.PLAY)) {
                // Tally light on engaged REC / PLAY keys
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 6.dp + lip * travel * 0.8f, end = 6.dp)
                        .size(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (key.symbol == KeySymbol.REC) recColor else palette.play),
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = key.label,
            style = SilkscreenStyle,
            color = palette.panelText.copy(alpha = if (key.enabled || key.latched) 0.9f else 0.4f),
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private fun DrawScope.drawSymbol(symbol: KeySymbol, color: Color) {
    val w = size.width
    val h = size.height
    when (symbol) {
        KeySymbol.REC -> drawCircle(color, radius = w * 0.36f)
        KeySymbol.PLAY -> drawPath(triangle(w * 0.18f, 0f, w * 0.9f, h, pointRight = true), color)
        KeySymbol.FF -> {
            drawPath(triangle(0f, h * 0.12f, w * 0.5f, h * 0.88f, pointRight = true), color)
            drawPath(triangle(w * 0.5f, h * 0.12f, w, h * 0.88f, pointRight = true), color)
        }
        KeySymbol.REW -> {
            drawPath(triangle(0f, h * 0.12f, w * 0.5f, h * 0.88f, pointRight = false), color)
            drawPath(triangle(w * 0.5f, h * 0.12f, w, h * 0.88f, pointRight = false), color)
        }
        KeySymbol.STOP -> drawRoundRect(color, Offset(w * 0.14f, h * 0.14f), Size(w * 0.72f, h * 0.72f), CornerRadius(w * 0.08f))
        KeySymbol.PAUSE -> {
            drawRoundRect(color, Offset(w * 0.16f, h * 0.1f), Size(w * 0.24f, h * 0.8f), CornerRadius(w * 0.05f))
            drawRoundRect(color, Offset(w * 0.6f, h * 0.1f), Size(w * 0.24f, h * 0.8f), CornerRadius(w * 0.05f))
        }
    }
}

private fun triangle(left: Float, top: Float, right: Float, bottom: Float, pointRight: Boolean) = Path().apply {
    val midY = (top + bottom) / 2f
    if (pointRight) {
        moveTo(left, top); lineTo(right, midY); lineTo(left, bottom)
    } else {
        moveTo(right, top); lineTo(left, midY); lineTo(right, bottom)
    }
    close()
}

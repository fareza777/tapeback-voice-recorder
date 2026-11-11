package com.f7developer.tapeback.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.animation.core.InfiniteRepeatableSpec
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.f7developer.tapeback.ui.theme.GroteskFamily
import com.f7developer.tapeback.ui.theme.LcdFamily
import com.f7developer.tapeback.ui.theme.Retro
import com.f7developer.tapeback.ui.theme.SilkscreenStyle

/** Three rolling mechanical digits, like the counter on a real deck. */
@Composable
fun MechanicalCounter(value: Int, modifier: Modifier = Modifier) {
    val digits = value.coerceIn(0, 999).toString().padStart(3, '0')
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF0D0D0D))
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
            .padding(3.dp)
            .clearAndSetSemantics { contentDescription = "Tape counter $value" },
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        digits.forEach { d ->
            Box(
                modifier = Modifier
                    .width(18.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Brush.verticalGradient(listOf(Color(0xFFBDBDBD), Color(0xFFF7F7F7), Color(0xFFBDBDBD)))),
                contentAlignment = Alignment.Center,
            ) {
                AnimatedContent(
                    targetState = d,
                    transitionSpec = {
                        (slideInVertically(tween(110)) { it } togetherWith slideOutVertically(tween(110)) { -it })
                    },
                    label = "counterDigit",
                ) { digit ->
                    Text(
                        text = digit.toString(),
                        style = TextStyle(fontFamily = GroteskFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF151515)),
                    )
                }
            }
        }
    }
}

/** Glowing LCD readout with ghost segments behind the live text. */
@Composable
fun LcdReadout(text: String, modifier: Modifier = Modifier, ghost: String = "88:88", fontSizeSp: Int = 30, caption: String? = null) {
    val palette = Retro.palette
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(palette.lcdBg)
            .border(1.dp, Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
            .drawBehind {
                drawRect(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.07f), Color.Transparent), 0f, size.height * 0.5f))
            }
            .padding(horizontal = 10.dp, vertical = 2.dp)
            .semantics(mergeDescendants = true) {},
        contentAlignment = Alignment.CenterEnd,
    ) {
        Text(
            text = ghost,
            style = TextStyle(fontFamily = LcdFamily, fontSize = fontSizeSp.sp, color = palette.lcdText.copy(alpha = 0.09f)),
            textAlign = TextAlign.End,
            modifier = Modifier.clearAndSetSemantics { },
        )
        Text(
            text = text,
            style = TextStyle(
                fontFamily = LcdFamily,
                fontSize = fontSizeSp.sp,
                color = palette.lcdText,
                shadow = androidx.compose.ui.graphics.Shadow(palette.lcdText.copy(alpha = 0.6f), blurRadius = 12f),
            ),
            textAlign = TextAlign.End,
        )
        if (caption != null) {
            Text(
                text = caption,
                style = SilkscreenStyle.copy(color = palette.lcdText.copy(alpha = 0.7f), fontSize = 7.sp),
                modifier = Modifier.align(Alignment.TopStart).padding(top = 3.dp),
            )
        }
    }
}

/** Status lamp (REC / PLAY). Blinks when [blink] and motion is allowed. */
@Composable
fun StatusLamp(label: String, color: Color, on: Boolean, blink: Boolean = false, size: Dp = 9.dp) {
    val palette = Retro.palette
    val reduceMotion = Retro.reduceMotion
    val alpha = if (on && blink && !reduceMotion) {
        val t = rememberInfiniteTransition(label = "lamp")
        val a by t.animateFloat(
            initialValue = 1f,
            targetValue = 0.25f,
            animationSpec = InfiniteRepeatableSpec(tween(520), RepeatMode.Reverse),
            label = "lampAlpha",
        )
        a
    } else 1f
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(size)
                .alpha(if (on) alpha else 1f)
                .clip(CircleShape)
                .background(if (on) color else color.copy(alpha = 0.18f))
                .border(1.dp, Color.Black.copy(alpha = 0.4f), CircleShape),
        )
        Text(
            text = label,
            style = SilkscreenStyle,
            color = palette.panelText.copy(alpha = if (on) 1f else 0.55f),
            modifier = Modifier.padding(start = 5.dp),
        )
    }
}

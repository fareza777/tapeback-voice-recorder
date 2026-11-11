package com.f7developer.tapeback.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * Film-grain and scanline overlay. Pure decoration: pointer-transparent, invisible to TalkBack,
 * and frozen when motion is reduced.
 */
@Composable
fun AnalogOverlay(grain: Boolean, scanlines: Boolean, reduceMotion: Boolean, dark: Boolean, modifier: Modifier = Modifier) {
    if (!grain && !scanlines) return
    val noise = remember { noiseTile(96) }
    val brush = remember(noise) { ShaderBrush(ImageShader(noise, TileMode.Repeated, TileMode.Repeated)) }
    var jitter by remember { mutableStateOf(Offset.Zero) }
    if (grain && !reduceMotion) {
        LaunchedEffect(Unit) {
            val rnd = Random(7)
            while (true) {
                jitter = Offset(rnd.nextInt(0, 96).toFloat(), rnd.nextInt(0, 96).toFloat())
                delay(90)
            }
        }
    }
    Canvas(modifier = modifier.fillMaxSize().clearAndSetSemantics { }) {
        if (grain) {
            translate(-jitter.x, -jitter.y) {
                drawRect(
                    brush = brush,
                    topLeft = Offset.Zero,
                    size = androidx.compose.ui.geometry.Size(size.width + 96f, size.height + 96f),
                    alpha = if (dark) 0.07f else 0.05f,
                )
            }
        }
        if (scanlines) {
            val step = 3.dp.toPx()
            var y = 0f
            val color = androidx.compose.ui.graphics.Color.Black.copy(alpha = if (dark) 0.16f else 0.06f)
            while (y < size.height) {
                drawLine(color, Offset(0f, y), Offset(size.width, y), 1f)
                y += step
            }
        }
    }
}

private fun noiseTile(sizePx: Int): ImageBitmap {
    val bitmap = android.graphics.Bitmap.createBitmap(sizePx, sizePx, android.graphics.Bitmap.Config.ARGB_8888)
    val rnd = Random(42)
    val pixels = IntArray(sizePx * sizePx) {
        val v = rnd.nextInt(0, 256)
        val a = rnd.nextInt(60, 256)
        (a shl 24) or (v shl 16) or (v shl 8) or v
    }
    bitmap.setPixels(pixels, 0, sizePx, 0, 0, sizePx, sizePx)
    return bitmap.asImageBitmap()
}

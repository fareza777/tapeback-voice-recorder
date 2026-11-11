package com.f7developer.tapeback.ui.components

import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.f7developer.tapeback.ui.theme.Retro
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max

private object CoverCache {
    val cache = LruCache<String, ImageBitmap>(24)
}

/** Decodes a private cover image off the main thread with down-sampling and an LRU cache. */
@Composable
fun rememberCoverBitmap(path: String?, maxPx: Int = 512): ImageBitmap? {
    val key = path?.let { "$it@$maxPx" }
    val bitmap by produceState(initialValue = key?.let { CoverCache.cache.get(it) }, key) {
        if (key == null || path == null) {
            value = null
            return@produceState
        }
        CoverCache.cache.get(key)?.let { value = it; return@produceState }
        value = withContext(Dispatchers.IO) {
            runCatching {
                val file = File(path)
                if (!file.exists()) return@runCatching null
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(path, bounds)
                var sample = 1
                while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxPx) sample *= 2
                BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })?.asImageBitmap()
            }.getOrNull()
        }?.also { CoverCache.cache.put(key, it) }
    }
    return bitmap
}

/** Screen background: palette base with a warm radial glow, like light falling on a desk. */
fun Modifier.retroBackground(top: Color, glow: Color): Modifier = this
    .background(top)
    .drawBehind {
        drawRect(
            Brush.radialGradient(
                listOf(glow.copy(alpha = 0.55f), Color.Transparent),
                center = Offset(size.width * 0.15f, -size.height * 0.05f),
                radius = size.maxDimension * 0.75f,
            ),
        )
        drawRect(
            Brush.radialGradient(
                listOf(glow.copy(alpha = 0.22f), Color.Transparent),
                center = Offset(size.width * 1.05f, size.height * 0.65f),
                radius = size.maxDimension * 0.55f,
            ),
        )
    }

/** Brushed-metal hardware panel. */
@Composable
fun DeckPanel(modifier: Modifier = Modifier, contentPadding: Dp = 14.dp, content: @Composable ColumnScope.() -> Unit) {
    val palette = Retro.palette
    Column(
        modifier = modifier
            .shadow(14.dp, RoundedCornerShape(26.dp), ambientColor = Color.Black, spotColor = Color.Black)
            .clip(RoundedCornerShape(26.dp))
            .background(Brush.verticalGradient(listOf(palette.panelTop, palette.panelBottom)))
            .drawBehind {
                // Fine horizontal brushing
                var y = 0f
                val step = 2.dp.toPx()
                var i = 0
                while (y < size.height) {
                    val a = if (i % 3 == 0) 0.05f else 0.025f
                    drawLine(Color.White.copy(alpha = a), Offset(0f, y), Offset(size.width, y), 1f)
                    y += step
                    i++
                }
                drawRect(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.14f), Color.Transparent), 0f, size.height * 0.25f))
            }
            .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(26.dp))
            .padding(contentPadding),
        content = content,
    )
}

/** Recessed dark well (the cassette door). */
@Composable
fun CassetteWell(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val palette = Retro.palette
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.verticalGradient(listOf(Color.Black, palette.well, lerpGrey(palette.well))))
            .border(2.dp, Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.8f), Color.White.copy(alpha = 0.18f))), RoundedCornerShape(20.dp))
            .padding(12.dp),
        contentAlignment = Alignment.Center,
        content = content,
    )
}

private fun lerpGrey(c: Color) = androidx.compose.ui.graphics.lerp(c, Color.White, 0.06f)

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier, trailing: @Composable (() -> Unit)? = null) {
    Row(
        modifier = modifier.fillMaxWidth().padding(top = 18.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = Retro.palette.accent,
            modifier = Modifier.weight(1f),
        )
        trailing?.invoke()
    }
}

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    action: @Composable (() -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Retro.palette.accent.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = Retro.palette.accent, modifier = Modifier.size(36.dp))
        }
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 360.dp),
        )
        if (action != null) {
            Spacer(Modifier.height(4.dp))
            action()
        }
    }
}

/** Screen header with the Bungee display face. */
@Composable
fun ScreenHeader(title: String, subtitle: String?, modifier: Modifier = Modifier, actions: @Composable (() -> Unit)? = null) {
    Row(modifier = modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onBackground)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        actions?.invoke()
    }
}

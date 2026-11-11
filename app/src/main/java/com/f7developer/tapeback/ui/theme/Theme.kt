package com.f7developer.tapeback.ui.theme

import android.provider.Settings
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.f7developer.tapeback.domain.model.AppSettings
import com.f7developer.tapeback.domain.model.DarkMode

val LocalRetroPalette = staticCompositionLocalOf { RetroPalettes.of(com.f7developer.tapeback.domain.model.RetroThemeId.AMBER_WALKMAN, true) }

/** True when decorative motion should be minimized (user setting or system animations off). */
val LocalReduceMotion = staticCompositionLocalOf { false }

/** Read the palette inside composables: `Retro.palette.accent`. */
object Retro {
    val palette: RetroPalette
        @Composable get() = LocalRetroPalette.current
    val reduceMotion: Boolean
        @Composable get() = LocalReduceMotion.current
}

private val TapeShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(30.dp),
)

fun RetroPalette.toColorScheme(): ColorScheme {
    val base = if (isDark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = accent,
        onPrimary = onAccent,
        primaryContainer = accent.copy(alpha = 0.22f).compositeOver(surface),
        onPrimaryContainer = text,
        secondary = accent2,
        onSecondary = if (isDark) Color(0xFF111111) else Color.White,
        secondaryContainer = accent2.copy(alpha = 0.2f).compositeOver(surface),
        onSecondaryContainer = text,
        tertiary = play,
        background = background,
        onBackground = text,
        surface = surface,
        onSurface = text,
        surfaceVariant = surfaceRaised,
        onSurfaceVariant = textMuted,
        surfaceContainerLowest = background,
        surfaceContainerLow = surface,
        surfaceContainer = surface,
        surfaceContainerHigh = surfaceRaised,
        surfaceContainerHighest = surfaceRaised,
        surfaceBright = surfaceRaised,
        surfaceDim = background,
        outline = outline,
        outlineVariant = outline.copy(alpha = 0.6f),
        error = rec,
        onError = Color.White,
        inverseSurface = text,
        inverseOnSurface = background,
        inversePrimary = accent2,
    )
}

@Composable
fun TapeBackTheme(settings: AppSettings, content: @Composable () -> Unit) {
    val systemDark = isSystemInDarkTheme()
    val dark = when (settings.darkMode) {
        DarkMode.SYSTEM -> systemDark
        DarkMode.LIGHT -> false
        DarkMode.DARK -> true
    }
    val palette = remember(settings.theme, dark) { RetroPalettes.of(settings.theme, dark) }
    val context = LocalContext.current
    val systemAnimationsOff = remember {
        runCatching {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
        }.getOrDefault(false)
    }
    CompositionLocalProvider(
        LocalRetroPalette provides palette,
        LocalReduceMotion provides (settings.reduceMotion || systemAnimationsOff),
    ) {
        MaterialTheme(
            colorScheme = remember(palette) { palette.toColorScheme() },
            typography = TapeTypography,
            shapes = TapeShapes,
            content = content,
        )
    }
}

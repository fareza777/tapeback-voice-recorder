package com.f7developer.tapeback.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.f7developer.tapeback.R

/** Chunky retro display face (OFL). */
val BungeeFamily = FontFamily(Font(R.font.bungee))

/** Handwritten cassette label face (Apache 2.0). */
val MarkerFamily = FontFamily(Font(R.font.permanent_marker))

/** LCD / counter readout face (OFL). */
val LcdFamily = FontFamily(Font(R.font.vt323))

private fun grotesk(weight: Int) = Font(
    R.font.space_grotesk,
    FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

/** UI text face, variable weight (OFL). */
val GroteskFamily = FontFamily(grotesk(400), grotesk(500), grotesk(600), grotesk(700))

val TapeTypography: Typography = Typography().let { base ->
    Typography(
        displayLarge = TextStyle(fontFamily = BungeeFamily, fontSize = 48.sp, lineHeight = 52.sp),
        displayMedium = TextStyle(fontFamily = BungeeFamily, fontSize = 38.sp, lineHeight = 42.sp),
        displaySmall = TextStyle(fontFamily = BungeeFamily, fontSize = 30.sp, lineHeight = 34.sp),
        headlineLarge = TextStyle(fontFamily = BungeeFamily, fontSize = 28.sp, lineHeight = 32.sp),
        headlineMedium = TextStyle(fontFamily = BungeeFamily, fontSize = 24.sp, lineHeight = 28.sp),
        headlineSmall = TextStyle(fontFamily = GroteskFamily, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 28.sp),
        titleLarge = TextStyle(fontFamily = GroteskFamily, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 26.sp),
        titleMedium = TextStyle(fontFamily = GroteskFamily, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
        titleSmall = TextStyle(fontFamily = GroteskFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
        bodyLarge = TextStyle(fontFamily = GroteskFamily, fontSize = 16.sp, lineHeight = 24.sp),
        bodyMedium = TextStyle(fontFamily = GroteskFamily, fontSize = 14.sp, lineHeight = 20.sp),
        bodySmall = TextStyle(fontFamily = GroteskFamily, fontSize = 12.sp, lineHeight = 16.sp),
        labelLarge = TextStyle(fontFamily = GroteskFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
        labelMedium = TextStyle(fontFamily = GroteskFamily, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.04.em),
        labelSmall = base.labelSmall.copy(fontFamily = GroteskFamily, fontWeight = FontWeight.Bold, letterSpacing = 0.08.em),
    )
}

val LcdStyle = TextStyle(fontFamily = LcdFamily, fontSize = 30.sp, letterSpacing = 0.06.em)
val SilkscreenStyle = TextStyle(fontFamily = GroteskFamily, fontWeight = FontWeight.Bold, fontSize = 9.sp, letterSpacing = 0.18.em)

package com.f7developer.tapeback.ui.components

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.f7developer.tapeback.R
import com.f7developer.tapeback.domain.model.CassetteSkin

@Immutable
data class SkinStyle(
    val shellTop: Color,
    val shellBottom: Color,
    /** Below 1 the tape packs show through the shell, like a clear cassette. */
    val shellAlpha: Float,
    val window: Color,
    val screw: Color,
    val trim: Color,
    val glow: Color? = null,
) {
    val translucent: Boolean get() = shellAlpha < 1f
}

object CassetteSkins {
    fun style(skin: CassetteSkin): SkinStyle = when (skin) {
        CassetteSkin.CLASSIC_BLACK -> SkinStyle(
            shellTop = Color(0xFF2E2E32), shellBottom = Color(0xFF111113), shellAlpha = 1f,
            window = Color(0xD90E0E10), screw = Color(0xFFC9CDD2), trim = Color(0xFF45454B),
        )
        CassetteSkin.SMOKED_CLEAR -> SkinStyle(
            shellTop = Color(0xFF66707A), shellBottom = Color(0xFF2A2F35), shellAlpha = 0.74f,
            window = Color(0x8C15181C), screw = Color(0xFFD5D9DE), trim = Color(0xFF7D8791),
        )
        CassetteSkin.CHROME_METAL -> SkinStyle(
            shellTop = Color(0xFFF4F6F8), shellBottom = Color(0xFF858F98), shellAlpha = 1f,
            window = Color(0xD91B2027), screw = Color(0xFF5F6870), trim = Color(0xFFB9C1C8),
        )
        CassetteSkin.CREAM_BEIGE -> SkinStyle(
            shellTop = Color(0xFFF3E7CE), shellBottom = Color(0xFFC9B48C), shellAlpha = 1f,
            window = Color(0xD92A231A), screw = Color(0xFF8A7A60), trim = Color(0xFFDCCBA8),
        )
        CassetteSkin.HOT_PINK -> SkinStyle(
            shellTop = Color(0xFFFF7CC0), shellBottom = Color(0xFFB8155A), shellAlpha = 1f,
            window = Color(0xCC3A0A22), screw = Color(0xFFFFE0F0), trim = Color(0xFFFF9BD0),
        )
        CassetteSkin.OCEAN_BLUE -> SkinStyle(
            shellTop = Color(0xFF5BB0FF), shellBottom = Color(0xFF1B4699), shellAlpha = 0.8f,
            window = Color(0x990A1C33), screw = Color(0xFFE3F1FF), trim = Color(0xFF8CC8FF),
        )
        CassetteSkin.GHOST_GLOW -> SkinStyle(
            shellTop = Color(0xFFB4FFDA), shellBottom = Color(0xFF2FBF79), shellAlpha = 0.6f,
            window = Color(0x8C06261A), screw = Color(0xFFEFFFF6), trim = Color(0xFFCFFFE6),
            glow = Color(0xFF7CFFB2),
        )
    }

    @StringRes
    fun nameRes(skin: CassetteSkin): Int = when (skin) {
        CassetteSkin.CLASSIC_BLACK -> R.string.skin_classic_black
        CassetteSkin.SMOKED_CLEAR -> R.string.skin_smoked_clear
        CassetteSkin.CHROME_METAL -> R.string.skin_chrome_metal
        CassetteSkin.CREAM_BEIGE -> R.string.skin_cream_beige
        CassetteSkin.HOT_PINK -> R.string.skin_hot_pink
        CassetteSkin.OCEAN_BLUE -> R.string.skin_ocean_blue
        CassetteSkin.GHOST_GLOW -> R.string.skin_ghost_glow
    }

    /** Label paper colors available in the cassette editor. */
    val labelColors: List<Int> = listOf(
        0xFFF3E6C8.toInt(), // cream
        0xFFFFFFFF.toInt(), // white
        0xFFF7C948.toInt(), // mustard
        0xFFFF8A5B.toInt(), // tangerine
        0xFFEF476F.toInt(), // watermelon
        0xFFB794F6.toInt(), // lavender
        0xFF7AD3C2.toInt(), // mint
        0xFF7FB7FF.toInt(), // sky
        0xFFB5E48C.toInt(), // lime
        0xFF2B2B2B.toInt(), // midnight
    )

    val categories: List<String> = listOf("Diary", "Ideas", "Music", "Meetings", "Family", "Dreams", "Travel", "Memories")
}

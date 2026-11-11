package com.f7developer.tapeback.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.f7developer.tapeback.domain.model.RetroThemeId

/**
 * Hardware-inspired palette. Material 3 colors are derived from it so stock components match
 * the deck. Every theme has a light and a dark variant.
 */
@Immutable
data class RetroPalette(
    val isDark: Boolean,
    val background: Color,
    val backgroundGlow: Color,
    val surface: Color,
    val surfaceRaised: Color,
    val outline: Color,
    val text: Color,
    val textMuted: Color,
    val accent: Color,
    val onAccent: Color,
    val accent2: Color,
    val rec: Color,
    val play: Color,
    val panelTop: Color,
    val panelBottom: Color,
    val panelText: Color,
    val well: Color,
    val keyTop: Color,
    val keyBottom: Color,
    val keyText: Color,
    val lcdBg: Color,
    val lcdText: Color,
    val vuFace: Color,
    val vuInk: Color,
    val vuNeedle: Color,
)

object RetroPalettes {

    fun of(id: RetroThemeId, dark: Boolean): RetroPalette = when (id) {
        RetroThemeId.AMBER_WALKMAN -> if (dark) amberDark else amberLight
        RetroThemeId.NEON_MIXTAPE -> if (dark) neonDark else neonLight
        RetroThemeId.CHROME_HIFI -> if (dark) chromeDark else chromeLight
        RetroThemeId.KIROWEEN -> if (dark) kiroweenDark else kiroweenLight
        RetroThemeId.BEIGE_CLASSIC -> if (dark) beigeDark else beigeLight
    }

    private val amberDark = RetroPalette(
        isDark = true,
        background = Color(0xFF120E0B), backgroundGlow = Color(0xFF3A2312),
        surface = Color(0xFF1D1712), surfaceRaised = Color(0xFF2A211A), outline = Color(0xFF4A3B2E),
        text = Color(0xFFF7EBDD), textMuted = Color(0xFFC2AE98),
        accent = Color(0xFFFF9F1C), onAccent = Color(0xFF231300), accent2 = Color(0xFFFFD166),
        rec = Color(0xFFFF4B3E), play = Color(0xFF6BD968),
        panelTop = Color(0xFF3B342E), panelBottom = Color(0xFF1E1A17), panelText = Color(0xFFE8D9C6),
        well = Color(0xFF0B0907),
        keyTop = Color(0xFFDCD3C8), keyBottom = Color(0xFF9C9186), keyText = Color(0xFF2A221C),
        lcdBg = Color(0xFF1A0F05), lcdText = Color(0xFFFFB347),
        vuFace = Color(0xFFF6E7C1), vuInk = Color(0xFF2B2018), vuNeedle = Color(0xFFD7261E),
    )

    private val amberLight = RetroPalette(
        isDark = false,
        background = Color(0xFFF5ECDF), backgroundGlow = Color(0xFFFFD9A8),
        surface = Color(0xFFFFF8EF), surfaceRaised = Color(0xFFF0E3D1), outline = Color(0xFFD4C1A8),
        text = Color(0xFF2A1D12), textMuted = Color(0xFF6E5A47),
        accent = Color(0xFFC2410C), onAccent = Color(0xFFFFFFFF), accent2 = Color(0xFFB7791F),
        rec = Color(0xFFD62828), play = Color(0xFF2F8F3A),
        panelTop = Color(0xFFE9E1D6), panelBottom = Color(0xFFBDB2A4), panelText = Color(0xFF3A2C20),
        well = Color(0xFF2A221C),
        keyTop = Color(0xFFFDFBF7), keyBottom = Color(0xFFC9BFB2), keyText = Color(0xFF2A221C),
        lcdBg = Color(0xFF241608), lcdText = Color(0xFFFFB347),
        vuFace = Color(0xFFFBEFCF), vuInk = Color(0xFF2B2018), vuNeedle = Color(0xFFC81E1E),
    )

    private val neonDark = RetroPalette(
        isDark = true,
        background = Color(0xFF0C0920), backgroundGlow = Color(0xFF3B0F5C),
        surface = Color(0xFF15112E), surfaceRaised = Color(0xFF221B45), outline = Color(0xFF3D3470),
        text = Color(0xFFF4EEFF), textMuted = Color(0xFFB7AEDB),
        accent = Color(0xFFFF3EA5), onAccent = Color(0xFF2A0016), accent2 = Color(0xFF2DE2E6),
        rec = Color(0xFFFF2E63), play = Color(0xFF2DE2E6),
        panelTop = Color(0xFF2B2450), panelBottom = Color(0xFF130F2B), panelText = Color(0xFFE4DCFF),
        well = Color(0xFF07051A),
        keyTop = Color(0xFF3A3170), keyBottom = Color(0xFF1E1945), keyText = Color(0xFFF4EEFF),
        lcdBg = Color(0xFF041A1F), lcdText = Color(0xFF2DE2E6),
        vuFace = Color(0xFF1A1440), vuInk = Color(0xFF2DE2E6), vuNeedle = Color(0xFFFF3EA5),
    )

    private val neonLight = RetroPalette(
        isDark = false,
        background = Color(0xFFF8F2FF), backgroundGlow = Color(0xFFFFC6E6),
        surface = Color(0xFFFFFFFF), surfaceRaised = Color(0xFFF0E6FF), outline = Color(0xFFD7C8F2),
        text = Color(0xFF1D1340), textMuted = Color(0xFF5E5486),
        accent = Color(0xFFC2187A), onAccent = Color(0xFFFFFFFF), accent2 = Color(0xFF00838F),
        rec = Color(0xFFD81B60), play = Color(0xFF00838F),
        panelTop = Color(0xFFECE4FF), panelBottom = Color(0xFFC9BCEB), panelText = Color(0xFF2A1F55),
        well = Color(0xFF1B1538),
        keyTop = Color(0xFFFFFFFF), keyBottom = Color(0xFFD3C6F0), keyText = Color(0xFF2A1F55),
        lcdBg = Color(0xFF051C21), lcdText = Color(0xFF2DE2E6),
        vuFace = Color(0xFFFFF6FB), vuInk = Color(0xFF2A1F55), vuNeedle = Color(0xFFC2187A),
    )

    private val chromeDark = RetroPalette(
        isDark = true,
        background = Color(0xFF0E1114), backgroundGlow = Color(0xFF16324A),
        surface = Color(0xFF171C21), surfaceRaised = Color(0xFF222932), outline = Color(0xFF39434F),
        text = Color(0xFFEEF3F7), textMuted = Color(0xFFA9B6C2),
        accent = Color(0xFF5CC8FF), onAccent = Color(0xFF001E2E), accent2 = Color(0xFFB0BEC5),
        rec = Color(0xFFFF5252), play = Color(0xFF69F0AE),
        panelTop = Color(0xFF5E6770), panelBottom = Color(0xFF2B3138), panelText = Color(0xFFE6EDF2),
        well = Color(0xFF080A0C),
        keyTop = Color(0xFFE3E8EC), keyBottom = Color(0xFF8E99A3), keyText = Color(0xFF1B2228),
        lcdBg = Color(0xFF031420), lcdText = Color(0xFF7FDBFF),
        vuFace = Color(0xFFEAF6FF), vuInk = Color(0xFF13202B), vuNeedle = Color(0xFFE53935),
    )

    private val chromeLight = RetroPalette(
        isDark = false,
        background = Color(0xFFEEF1F4), backgroundGlow = Color(0xFFCFE9FA),
        surface = Color(0xFFFFFFFF), surfaceRaised = Color(0xFFE3E8ED), outline = Color(0xFFC3CCD5),
        text = Color(0xFF141A20), textMuted = Color(0xFF55616D),
        accent = Color(0xFF0277BD), onAccent = Color(0xFFFFFFFF), accent2 = Color(0xFF546E7A),
        rec = Color(0xFFD32F2F), play = Color(0xFF2E7D32),
        panelTop = Color(0xFFF4F6F8), panelBottom = Color(0xFFB8C2CB), panelText = Color(0xFF1F2A33),
        well = Color(0xFF1A2027),
        keyTop = Color(0xFFFFFFFF), keyBottom = Color(0xFFBCC6CF), keyText = Color(0xFF1B2228),
        lcdBg = Color(0xFF031420), lcdText = Color(0xFF7FDBFF),
        vuFace = Color(0xFFF2FAFF), vuInk = Color(0xFF13202B), vuNeedle = Color(0xFFD32F2F),
    )

    private val kiroweenDark = RetroPalette(
        isDark = true,
        background = Color(0xFF0B0711), backgroundGlow = Color(0xFF3D1A5C),
        surface = Color(0xFF160F21), surfaceRaised = Color(0xFF231832), outline = Color(0xFF412C5A),
        text = Color(0xFFF6EEFF), textMuted = Color(0xFFBBA8D6),
        accent = Color(0xFFFF7A1A), onAccent = Color(0xFF2A1000), accent2 = Color(0xFF9D4EDD),
        rec = Color(0xFFFF3D3D), play = Color(0xFF7CFFB2),
        panelTop = Color(0xFF2D2140), panelBottom = Color(0xFF130C1D), panelText = Color(0xFFEADCFF),
        well = Color(0xFF05030A),
        keyTop = Color(0xFF3B2C52), keyBottom = Color(0xFF1C1328), keyText = Color(0xFFFFB27A),
        lcdBg = Color(0xFF04140B), lcdText = Color(0xFF7CFFB2),
        vuFace = Color(0xFF1B1226), vuInk = Color(0xFFFFB27A), vuNeedle = Color(0xFF7CFFB2),
    )

    private val kiroweenLight = RetroPalette(
        isDark = false,
        background = Color(0xFFFFF4E8), backgroundGlow = Color(0xFFE9D5FF),
        surface = Color(0xFFFFFBF6), surfaceRaised = Color(0xFFF6E7FF), outline = Color(0xFFE0C8F0),
        text = Color(0xFF241433), textMuted = Color(0xFF66527A),
        accent = Color(0xFFD9480F), onAccent = Color(0xFFFFFFFF), accent2 = Color(0xFF7B2CBF),
        rec = Color(0xFFD62828), play = Color(0xFF138A4F),
        panelTop = Color(0xFFF1E4FF), panelBottom = Color(0xFFCDB4E8), panelText = Color(0xFF2E1A45),
        well = Color(0xFF1C1228),
        keyTop = Color(0xFFFFFFFF), keyBottom = Color(0xFFD9C3EE), keyText = Color(0xFF2E1A45),
        lcdBg = Color(0xFF04140B), lcdText = Color(0xFF7CFFB2),
        vuFace = Color(0xFFFFF7EC), vuInk = Color(0xFF2E1A45), vuNeedle = Color(0xFFD9480F),
    )

    private val beigeDark = RetroPalette(
        isDark = true,
        background = Color(0xFF17140F), backgroundGlow = Color(0xFF3B3122),
        surface = Color(0xFF221D16), surfaceRaised = Color(0xFF2E2820), outline = Color(0xFF4C4234),
        text = Color(0xFFF3EADB), textMuted = Color(0xFFBFB29D),
        accent = Color(0xFFE0B072), onAccent = Color(0xFF2B1C08), accent2 = Color(0xFF9CC5A1),
        rec = Color(0xFFE5533D), play = Color(0xFF9CC5A1),
        panelTop = Color(0xFFCFC2A8), panelBottom = Color(0xFF9F927A), panelText = Color(0xFF2A2116),
        well = Color(0xFF14110C),
        keyTop = Color(0xFF4A4134), keyBottom = Color(0xFF2B251D), keyText = Color(0xFFF3EADB),
        lcdBg = Color(0xFF1B1E0E), lcdText = Color(0xFFC9F27B),
        vuFace = Color(0xFFF4E9C8), vuInk = Color(0xFF2A2116), vuNeedle = Color(0xFF1B1B1B),
    )

    private val beigeLight = RetroPalette(
        isDark = false,
        background = Color(0xFFEFE6D2), backgroundGlow = Color(0xFFF7DDB4),
        surface = Color(0xFFFBF6EA), surfaceRaised = Color(0xFFE6DAC1), outline = Color(0xFFCDBD9C),
        text = Color(0xFF2A2116), textMuted = Color(0xFF6B5D47),
        accent = Color(0xFF8B5E34), onAccent = Color(0xFFFFFFFF), accent2 = Color(0xFF4F7A55),
        rec = Color(0xFFC62828), play = Color(0xFF3E7B45),
        panelTop = Color(0xFFEDE3CD), panelBottom = Color(0xFFC6B796), panelText = Color(0xFF2A2116),
        well = Color(0xFF2A241A),
        keyTop = Color(0xFF5A5043), keyBottom = Color(0xFF332C23), keyText = Color(0xFFF3EADB),
        lcdBg = Color(0xFF1B1E0E), lcdText = Color(0xFFC9F27B),
        vuFace = Color(0xFFFBF1D6), vuInk = Color(0xFF2A2116), vuNeedle = Color(0xFF1B1B1B),
    )
}

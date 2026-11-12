package com.f7developer.tapeback.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.f7developer.tapeback.R
import com.f7developer.tapeback.domain.model.DarkMode
import com.f7developer.tapeback.domain.model.RetroThemeId
import com.f7developer.tapeback.ui.theme.Retro
import com.f7developer.tapeback.ui.theme.RetroPalettes

fun themeName(id: RetroThemeId): Int = when (id) {
    RetroThemeId.AMBER_WALKMAN -> R.string.theme_amber
    RetroThemeId.NEON_MIXTAPE -> R.string.theme_neon
    RetroThemeId.CHROME_HIFI -> R.string.theme_chrome
    RetroThemeId.KIROWEEN -> R.string.theme_kiroween
    RetroThemeId.BEIGE_CLASSIC -> R.string.theme_beige
}

/** Horizontal row of deck finishes, each previewed in its own palette. */
@Composable
fun ThemePickerRow(
    selected: RetroThemeId,
    onSelect: (RetroThemeId) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    val dark = Retro.palette.isDark
    LazyRow(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(10.dp), contentPadding = contentPadding) {
        items(RetroThemeId.entries) { id ->
            ThemeCard(id, selected = selected == id, dark = dark, onClick = { onSelect(id) })
        }
    }
}

@Composable
fun DarkModeSelector(mode: DarkMode, onSelect: (DarkMode) -> Unit, modifier: Modifier = Modifier) {
    SingleChoiceSegmentedButtonRow(modifier.fillMaxWidth()) {
        DarkMode.entries.forEachIndexed { i, m ->
            SegmentedButton(
                selected = mode == m,
                onClick = { onSelect(m) },
                shape = SegmentedButtonDefaults.itemShape(i, DarkMode.entries.size),
            ) {
                Text(
                    stringResource(
                        when (m) {
                            DarkMode.SYSTEM -> R.string.mode_system
                            DarkMode.LIGHT -> R.string.mode_light
                            DarkMode.DARK -> R.string.mode_dark
                        },
                    ),
                )
            }
        }
    }
}

@Composable
private fun ThemeCard(id: RetroThemeId, selected: Boolean, dark: Boolean, onClick: () -> Unit) {
    val p = RetroPalettes.of(id, dark)
    val palette = Retro.palette
    Column(
        modifier = Modifier
            .width(128.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(p.background)
            .border(if (selected) 3.dp else 1.dp, if (selected) palette.accent else palette.outline, RoundedCornerShape(18.dp))
            .semantics { this.selected = selected }
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(54.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Brush.verticalGradient(listOf(p.panelTop, p.panelBottom))),
            contentAlignment = Alignment.Center,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                listOf(p.rec, p.accent, p.accent2, p.lcdText).forEach {
                    Box(Modifier.size(14.dp).clip(CircleShape).background(it))
                }
            }
        }
        Text(stringResource(themeName(id)), style = MaterialTheme.typography.labelLarge, color = p.text, maxLines = 1)
    }
}

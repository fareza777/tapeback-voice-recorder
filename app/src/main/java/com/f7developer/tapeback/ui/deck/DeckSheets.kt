package com.f7developer.tapeback.ui.deck

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.f7developer.tapeback.R
import com.f7developer.tapeback.domain.model.CassetteSummary
import com.f7developer.tapeback.domain.model.EffectPreset
import com.f7developer.tapeback.domain.model.PlaybackFx
import com.f7developer.tapeback.domain.model.RecordTarget
import com.f7developer.tapeback.domain.model.TapeSide
import com.f7developer.tapeback.domain.policy.TimeFormat
import com.f7developer.tapeback.ui.components.CassetteArt
import com.f7developer.tapeback.ui.theme.Retro
import kotlin.math.roundToInt

@Composable
fun TargetPickerSheet(
    cassettes: List<CassetteSummary>,
    current: RecordTarget?,
    onPick: (Long, TapeSide) -> Unit,
    onManageLibrary: () -> Unit,
    onDismiss: () -> Unit,
) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var side by remember { mutableStateOf(current?.side ?: TapeSide.A) }
    val palette = Retro.palette
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = state, containerColor = palette.surface) {
        Column(Modifier.padding(horizontal = 20.dp).navigationBarsPadding()) {
            Text(stringResource(R.string.target_title), style = MaterialTheme.typography.titleLarge)
            Text(
                stringResource(R.string.target_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.padding(6.dp))
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                TapeSide.entries.forEachIndexed { i, s ->
                    SegmentedButton(
                        selected = side == s,
                        onClick = { side = s },
                        shape = SegmentedButtonDefaults.itemShape(i, TapeSide.entries.size),
                    ) { Text(stringResource(R.string.side_format, s.letter)) }
                }
            }
            Spacer(Modifier.padding(6.dp))
            LazyColumn(
                modifier = Modifier.heightIn(max = 460.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(cassettes, key = { it.cassette.id }) { summary ->
                    val c = summary.cassette
                    val selected = current?.cassetteId == c.id && current.side == side
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (selected) palette.accent.copy(alpha = 0.14f) else palette.surfaceRaised, RoundedCornerShape(18.dp))
                            .border(1.dp, if (selected) palette.accent else Color.Transparent, RoundedCornerShape(18.dp))
                            .semantics { this.selected = selected }
                            .clickable(role = Role.RadioButton) { onPick(c.id, side) }
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CassetteArt(
                            skin = c.skin,
                            labelColor = Color(c.labelColor),
                            title = c.title,
                            sideLetter = side.letter,
                            modifier = Modifier.width(92.dp),
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(c.title, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                            Text(
                                stringResource(
                                    R.string.cassette_stats,
                                    summary.sideACount,
                                    summary.sideBCount,
                                    TimeFormat.humanTotal(summary.totalDurationMs),
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (selected) Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = palette.accent)
                    }
                }
            }
            TextButton(onClick = onManageLibrary, modifier = Modifier.padding(vertical = 8.dp)) {
                Text(stringResource(R.string.target_manage))
            }
        }
    }
}

@Composable
fun EffectsSheet(fx: PlaybackFx, onChange: (PlaybackFx) -> Unit, onDismiss: () -> Unit) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val palette = Retro.palette
    var hiss by remember { mutableFloatStateOf(fx.hiss) }
    var speed by remember { mutableFloatStateOf(fx.speed) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = state, containerColor = palette.surface) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(stringResource(R.string.fx_title), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.fx_body), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            EffectPreset.entries.forEach { preset ->
                val selected = fx.preset == preset
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (selected) palette.accent.copy(alpha = 0.16f) else palette.surfaceRaised, RoundedCornerShape(18.dp))
                        .border(1.dp, if (selected) palette.accent else Color.Transparent, RoundedCornerShape(18.dp))
                        .semantics { this.selected = selected }
                        .clickable(role = Role.RadioButton) { onChange(fx.copy(preset = preset)) }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(presetName(preset)), style = MaterialTheme.typography.titleSmall)
                        Text(
                            stringResource(presetDescription(preset)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (selected) Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = palette.accent)
                }
            }

            Text(stringResource(R.string.fx_hiss, (hiss * 100).roundToInt()), style = MaterialTheme.typography.titleSmall)
            Slider(
                value = hiss,
                onValueChange = { hiss = it },
                onValueChangeFinished = { onChange(fx.copy(hiss = hiss)) },
                valueRange = 0f..1f,
            )

            Text(stringResource(R.string.fx_speed, speedLabel(speed)), style = MaterialTheme.typography.titleSmall)
            Slider(
                value = speed,
                onValueChange = { speed = (it * 20).roundToInt() / 20f },
                onValueChangeFinished = { onChange(fx.copy(speed = speed)) },
                valueRange = 0.5f..2f,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0.75f, 1f, 1.25f, 1.5f, 2f).forEach { s ->
                    FilterChip(
                        selected = kotlin.math.abs(speed - s) < 0.01f,
                        onClick = {
                            speed = s
                            onChange(fx.copy(speed = s))
                        },
                        label = { Text(speedLabel(s)) },
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.fx_link_pitch), style = MaterialTheme.typography.titleSmall)
                    Text(
                        stringResource(R.string.fx_link_pitch_body),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = fx.linkPitch, onCheckedChange = { onChange(fx.copy(linkPitch = it)) })
            }
        }
    }
}

/** 1×, 0.75×, 1.25× - no rounding surprises. */
fun speedLabel(speed: Float): String =
    java.math.BigDecimal(((speed * 100).roundToInt() / 100.0).toString()).stripTrailingZeros().toPlainString() + "×"

fun presetName(p: EffectPreset): Int = when (p) {
    EffectPreset.CLEAN -> R.string.fx_clean
    EffectPreset.VINTAGE -> R.string.fx_vintage
    EffectPreset.LOFI -> R.string.fx_lofi
    EffectPreset.RADIO -> R.string.fx_radio
}

private fun presetDescription(p: EffectPreset): Int = when (p) {
    EffectPreset.CLEAN -> R.string.fx_clean_desc
    EffectPreset.VINTAGE -> R.string.fx_vintage_desc
    EffectPreset.LOFI -> R.string.fx_lofi_desc
    EffectPreset.RADIO -> R.string.fx_radio_desc
}

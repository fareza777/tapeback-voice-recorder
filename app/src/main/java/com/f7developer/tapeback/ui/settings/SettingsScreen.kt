package com.f7developer.tapeback.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.f7developer.tapeback.BuildConfig
import com.f7developer.tapeback.R
import com.f7developer.tapeback.data.RecordingRepository
import com.f7developer.tapeback.di.AppContainer
import com.f7developer.tapeback.domain.model.AppSettings
import com.f7developer.tapeback.domain.model.DarkMode
import com.f7developer.tapeback.domain.model.RecordingQuality
import com.f7developer.tapeback.domain.model.RetroThemeId
import com.f7developer.tapeback.domain.policy.TimeFormat
import com.f7developer.tapeback.ui.appContainer
import com.f7developer.tapeback.ui.components.SectionLabel
import com.f7developer.tapeback.ui.theme.BungeeFamily
import com.f7developer.tapeback.ui.theme.Retro
import com.f7developer.tapeback.ui.theme.RetroPalettes
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val container: AppContainer) : ViewModel() {
    private val repo = container.settingsRepository
    val settings: StateFlow<AppSettings> = repo.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())
    val storage: StateFlow<RecordingRepository.StorageStats> = container.recordingRepository.observeStorage()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RecordingRepository.StorageStats(0, 0))

    fun freeBytes(): Long = container.files.freeBytes()

    fun theme(t: RetroThemeId) = viewModelScope.launch { repo.setTheme(t) }
    fun darkMode(m: DarkMode) = viewModelScope.launch { repo.setDarkMode(m) }
    fun grain(on: Boolean) = viewModelScope.launch { repo.setGrain(on) }
    fun scanlines(on: Boolean) = viewModelScope.launch { repo.setScanlines(on) }
    fun reduceMotion(on: Boolean) = viewModelScope.launch { repo.setReduceMotion(on) }
    fun sounds(on: Boolean) = viewModelScope.launch { repo.setMechanicalSounds(on) }
    fun haptics(on: Boolean) = viewModelScope.launch { repo.setHaptics(on) }
    fun quality(q: RecordingQuality) = viewModelScope.launch { repo.setQuality(q) }
    fun keepScreenOn(on: Boolean) = viewModelScope.launch { repo.setKeepScreenOn(on) }
    fun linkPitch(on: Boolean) = viewModelScope.launch { repo.setFx(settings.value.fx.copy(linkPitch = on)) }
}

@Composable
fun SettingsScreen(onBack: () -> Unit, contentPadding: PaddingValues) {
    val container = appContainer()
    val vm: SettingsViewModel = viewModel { SettingsViewModel(container) }
    val s by vm.settings.collectAsStateWithLifecycle()
    val storage by vm.storage.collectAsStateWithLifecycle()
    val palette = Retro.palette

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = contentPadding.calculateBottomPadding() + 32.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth().statusBarsPadding().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.cd_back)) }
                Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.headlineMedium)
            }
        }

        item { SectionLabel(stringResource(R.string.settings_appearance)) }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(RetroThemeId.entries) { id ->
                    ThemeCard(id, selected = s.theme == id, dark = palette.isDark, onClick = { vm.theme(id) })
                }
            }
        }
        item {
            Spacer(Modifier.height(12.dp))
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                DarkMode.entries.forEachIndexed { i, mode ->
                    SegmentedButton(
                        selected = s.darkMode == mode,
                        onClick = { vm.darkMode(mode) },
                        shape = SegmentedButtonDefaults.itemShape(i, DarkMode.entries.size),
                    ) {
                        Text(
                            stringResource(
                                when (mode) {
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
        item { ToggleRow(stringResource(R.string.settings_grain), stringResource(R.string.settings_grain_body), s.grain, vm::grain) }
        item { ToggleRow(stringResource(R.string.settings_scanlines), stringResource(R.string.settings_scanlines_body), s.scanlines, vm::scanlines) }
        item { ToggleRow(stringResource(R.string.settings_reduce_motion), stringResource(R.string.settings_reduce_motion_body), s.reduceMotion, vm::reduceMotion) }

        item { SectionLabel(stringResource(R.string.settings_feel)) }
        item { ToggleRow(stringResource(R.string.settings_sounds), stringResource(R.string.settings_sounds_body), s.mechanicalSounds, vm::sounds) }
        item { ToggleRow(stringResource(R.string.settings_haptics), stringResource(R.string.settings_haptics_body), s.haptics, vm::haptics) }

        item { SectionLabel(stringResource(R.string.settings_recording)) }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                RecordingQuality.entries.forEach { q ->
                    val selected = s.quality == q
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (selected) palette.accent.copy(alpha = 0.15f) else palette.surfaceRaised)
                            .border(1.dp, if (selected) palette.accent else Color.Transparent, RoundedCornerShape(16.dp))
                            .semantics { this.selected = selected }
                            .clickable(role = Role.RadioButton) { vm.quality(q) }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                stringResource(
                                    when (q) {
                                        RecordingQuality.STANDARD -> R.string.quality_standard
                                        RecordingQuality.HIGH -> R.string.quality_high
                                        RecordingQuality.STUDIO -> R.string.quality_studio
                                    },
                                ),
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Text(
                                stringResource(R.string.quality_detail, q.sampleRate / 1000f, q.bitRate / 1000, megabytesPerHour(q)),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (selected) Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = palette.accent)
                    }
                }
            }
        }
        item { ToggleRow(stringResource(R.string.settings_keep_screen), stringResource(R.string.settings_keep_screen_body), s.keepScreenOn, vm::keepScreenOn) }

        item { SectionLabel(stringResource(R.string.settings_playback)) }
        item { ToggleRow(stringResource(R.string.fx_link_pitch), stringResource(R.string.fx_link_pitch_body), s.fx.linkPitch, vm::linkPitch) }

        item { SectionLabel(stringResource(R.string.settings_storage)) }
        item {
            InfoCard {
                Text(
                    stringResource(R.string.storage_used, storage.count, TimeFormat.fileSize(storage.bytes)),
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    stringResource(R.string.storage_free, TimeFormat.fileSize(vm.freeBytes())),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item { SectionLabel(stringResource(R.string.settings_privacy)) }
        item {
            InfoCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Lock, contentDescription = null, tint = palette.play)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.privacy_title), style = MaterialTheme.typography.titleSmall)
                }
                Text(stringResource(R.string.privacy_body), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        item { SectionLabel(stringResource(R.string.settings_about)) }
        item {
            InfoCard {
                Text(stringResource(R.string.brand_wordmark), style = MaterialTheme.typography.titleLarge.copy(fontFamily = BungeeFamily), color = palette.accent)
                Text(stringResource(R.string.about_version, BuildConfig.VERSION_NAME), style = MaterialTheme.typography.bodySmall)
                Text(stringResource(R.string.about_publisher), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                Text(stringResource(R.string.about_fonts), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(stringResource(R.string.about_sounds), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private fun megabytesPerHour(q: RecordingQuality): Int = (q.bitRate / 8 * 3600 / 1_000_000)

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
        Text(
            stringResource(
                when (id) {
                    RetroThemeId.AMBER_WALKMAN -> R.string.theme_amber
                    RetroThemeId.NEON_MIXTAPE -> R.string.theme_neon
                    RetroThemeId.CHROME_HIFI -> R.string.theme_chrome
                    RetroThemeId.KIROWEEN -> R.string.theme_kiroween
                    RetroThemeId.BEIGE_CLASSIC -> R.string.theme_beige
                },
            ),
            style = MaterialTheme.typography.labelLarge,
            color = p.text,
            maxLines = 1,
        )
    }
}

@Composable
private fun ToggleRow(title: String, body: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onChange)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun InfoCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Retro.palette.surfaceRaised)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) { content() }
}

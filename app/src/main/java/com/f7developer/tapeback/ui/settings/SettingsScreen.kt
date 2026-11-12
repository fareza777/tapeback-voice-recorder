package com.f7developer.tapeback.ui.settings

import android.Manifest
import android.net.Uri
import android.os.Build
import android.text.format.DateFormat
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Animation
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Grain
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Policy
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.SdStorage
import androidx.compose.material.icons.rounded.SettingsBackupRestore
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.TimerOff
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.Waves
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.f7developer.tapeback.BuildConfig
import com.f7developer.tapeback.R
import com.f7developer.tapeback.core.AppActions
import com.f7developer.tapeback.data.RecordingRepository
import com.f7developer.tapeback.data.backup.BackupManager
import com.f7developer.tapeback.di.AppContainer
import com.f7developer.tapeback.domain.model.AppSettings
import com.f7developer.tapeback.domain.model.DarkMode
import com.f7developer.tapeback.domain.model.DiaryReminder
import com.f7developer.tapeback.domain.model.PlaybackFx
import com.f7developer.tapeback.domain.model.RecordingOptions
import com.f7developer.tapeback.domain.model.RecordingQuality
import com.f7developer.tapeback.domain.model.RepeatMode
import com.f7developer.tapeback.domain.model.RetroThemeId
import com.f7developer.tapeback.domain.policy.TimeFormat
import com.f7developer.tapeback.ui.appContainer
import com.f7developer.tapeback.ui.components.DarkModeSelector
import com.f7developer.tapeback.ui.components.SectionLabel
import com.f7developer.tapeback.ui.components.ThemePickerRow
import com.f7developer.tapeback.ui.components.rememberPermissionState
import com.f7developer.tapeback.ui.deck.presetName
import com.f7developer.tapeback.ui.deck.speedLabel
import com.f7developer.tapeback.ui.lock.canUseDeviceLock
import com.f7developer.tapeback.ui.lock.rememberDeviceAuthenticator
import com.f7developer.tapeback.ui.theme.Retro
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

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
    fun detailsAfterRecording(on: Boolean) = viewModelScope.launch { repo.setDetailsAfterRecording(on) }
    fun linkPitch(on: Boolean) = viewModelScope.launch { repo.setFx(settings.value.fx.copy(linkPitch = on)) }
    fun resetFx() = viewModelScope.launch { repo.setFx(PlaybackFx(linkPitch = settings.value.fx.linkPitch)) }
    fun replayIntro() = viewModelScope.launch { repo.setOnboardingDone(false) }
    fun countdown(seconds: Int) = viewModelScope.launch { repo.setCountdown(seconds) }
    fun autoStop(minutes: Int) = viewModelScope.launch { repo.setAutoStop(minutes) }
    fun repeat(mode: RepeatMode) = viewModelScope.launch { repo.setRepeatMode(mode) }
    fun skipSilence(on: Boolean) = viewModelScope.launch { repo.setSkipSilence(on) }
    fun reminder(r: DiaryReminder) = viewModelScope.launch { repo.setReminder(r) }
    fun appLock(on: Boolean) = viewModelScope.launch { repo.setAppLock(on) }
    fun hideInRecents(on: Boolean) = viewModelScope.launch { repo.setHideInRecents(on) }

    val backup: StateFlow<BackupManager.State> get() = container.backupManager.state
    fun exportBackup(uri: Uri) = container.backupManager.export(uri)
    fun restoreBackup(uri: Uri) = container.backupManager.restore(uri)
    fun acknowledgeBackup() = container.backupManager.acknowledge()
    fun resetAll(onDone: () -> Unit) = viewModelScope.launch {
        repo.resetToDefaults()
        onDone()
    }
}

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenAbout: () -> Unit,
    onLockVerified: () -> Unit,
    snackbar: SnackbarHostState,
    contentPadding: PaddingValues,
) {
    val container = appContainer()
    val vm: SettingsViewModel = viewModel { SettingsViewModel(container) }
    val s by vm.settings.collectAsStateWithLifecycle()
    val storage by vm.storage.collectAsStateWithLifecycle()
    val backup by vm.backup.collectAsStateWithLifecycle()
    val palette = Retro.palette
    val context = LocalContext.current
    val activity = LocalActivity.current
    val scope = rememberCoroutineScope()
    val permissions = rememberPermissionState()
    var micAsked by rememberSaveable { mutableStateOf(false) }
    var showPolicy by rememberSaveable { mutableStateOf(false) }
    var confirmReset by rememberSaveable { mutableStateOf(false) }
    var pickTime by rememberSaveable { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(BackupManager.MIME)) { uri ->
        if (uri != null) vm.exportBackup(uri)
    }
    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.restoreBackup(uri)
    }
    val lockFailed = stringResource(R.string.settings_lock_failed)
    val authenticate = rememberDeviceAuthenticator { ok ->
        if (ok) {
            vm.appLock(true)
            onLockVerified()
        } else {
            scope.launch { snackbar.showSnackbar(lockFailed) }
        }
    }
    val lockTitle = stringResource(R.string.settings_lock_confirm_title)
    val lockSubtitle = stringResource(R.string.lock_prompt_subtitle)
    val deviceSecure = remember(permissions.value) { canUseDeviceLock(context) }

    // Report finished backups and restores once.
    val res = LocalContext.current.resources
    LaunchedEffect(backup) {
        val text = when (val b = backup) {
            is BackupManager.State.Exported -> res.getQuantityString(R.plurals.backup_done, b.recordings, b.recordings, TimeFormat.fileSize(b.bytes))
            is BackupManager.State.Restored -> res.getString(R.string.restore_done, b.recordings, b.cassettes, b.skipped)
            is BackupManager.State.Failed -> res.getString(
                when {
                    !b.restoring -> R.string.backup_failed
                    b.notABackup -> R.string.restore_not_backup
                    else -> R.string.restore_failed
                },
            )
            else -> null
        }
        if (text != null) {
            // Show from the screen scope: acknowledging changes `backup`, which restarts this effect.
            scope.launch { snackbar.showSnackbar(text) }
            vm.acknowledgeBackup()
        }
    }

    val micLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        micAsked = true
        permissions.refresh()
    }
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        permissions.refresh()
        if (!it) AppActions.openNotificationSettings(context)
    }
    val noStore = stringResource(R.string.msg_no_store)
    val resetDone = stringResource(R.string.settings_reset_done)

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

        // ---- Appearance ----
        item { SectionLabel(stringResource(R.string.settings_appearance)) }
        item { ThemePickerRow(selected = s.theme, onSelect = { vm.theme(it) }) }
        item {
            Spacer(Modifier.height(12.dp))
            DarkModeSelector(s.darkMode, { vm.darkMode(it) })
            Spacer(Modifier.height(12.dp))
        }
        item {
            Group {
                ToggleRow(Icons.Rounded.Grain, R.string.settings_grain, R.string.settings_grain_body, s.grain, vm::grain)
                Divider()
                ToggleRow(Icons.Rounded.Tv, R.string.settings_scanlines, R.string.settings_scanlines_body, s.scanlines, vm::scanlines)
                Divider()
                ToggleRow(Icons.Rounded.Animation, R.string.settings_reduce_motion, R.string.settings_reduce_motion_body, s.reduceMotion, vm::reduceMotion)
            }
        }

        // ---- Sound & feel ----
        item { SectionLabel(stringResource(R.string.settings_feel)) }
        item {
            Group {
                ToggleRow(Icons.AutoMirrored.Rounded.VolumeUp, R.string.settings_sounds, R.string.settings_sounds_body, s.mechanicalSounds, vm::sounds)
                Divider()
                ToggleRow(Icons.Rounded.Vibration, R.string.settings_haptics, R.string.settings_haptics_body, s.haptics, vm::haptics)
            }
        }

        // ---- Recording ----
        item { SectionLabel(stringResource(R.string.settings_recording)) }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                RecordingQuality.entries.forEach { q -> QualityOption(q, selected = s.quality == q, onClick = { vm.quality(q) }) }
            }
            Spacer(Modifier.height(10.dp))
        }
        item {
            Group {
                ToggleRow(Icons.Rounded.EditNote, R.string.settings_details_after, R.string.settings_details_after_body, s.detailsAfterRecording, vm::detailsAfterRecording)
                Divider()
                ToggleRow(Icons.Rounded.LightMode, R.string.settings_keep_screen, R.string.settings_keep_screen_body, s.keepScreenOn, vm::keepScreenOn)
                Divider()
                ChoiceRow(
                    icon = Icons.Rounded.Timer,
                    title = R.string.settings_countdown,
                    body = R.string.settings_countdown_body,
                    options = RecordingOptions.COUNTDOWN_CHOICES,
                    selected = s.countdownSeconds,
                    label = { if (it == 0) stringResource(R.string.option_off) else stringResource(R.string.option_seconds, it) },
                    onSelect = { vm.countdown(it) },
                )
                Divider()
                ChoiceRow(
                    icon = Icons.Rounded.TimerOff,
                    title = R.string.settings_auto_stop,
                    body = R.string.settings_auto_stop_body,
                    options = RecordingOptions.AUTO_STOP_CHOICES,
                    selected = s.autoStopMinutes,
                    label = { if (it == 0) stringResource(R.string.option_off) else it.toString() },
                    onSelect = { vm.autoStop(it) },
                )
            }
        }

        // ---- Playback ----
        item { SectionLabel(stringResource(R.string.settings_playback)) }
        item {
            val fx = s.fx
            val isDefault = fx.preset == PlaybackFx().preset && fx.speed == 1f && fx.hiss == 0f
            Group {
                ToggleRow(Icons.Rounded.Waves, R.string.fx_link_pitch, R.string.fx_link_pitch_body, fx.linkPitch, vm::linkPitch)
                Divider()
                ToggleRow(Icons.Rounded.FastForward, R.string.skip_silence, R.string.settings_skip_silence_body, s.skipSilence, vm::skipSilence)
                Divider()
                ChoiceRow(
                    icon = Icons.Rounded.Repeat,
                    title = R.string.settings_repeat,
                    body = R.string.settings_repeat_body,
                    options = listOf(RepeatMode.OFF, RepeatMode.SIDE, RepeatMode.ONE),
                    selected = s.repeatMode,
                    label = {
                        stringResource(
                            when (it) {
                                RepeatMode.OFF -> R.string.option_off
                                RepeatMode.SIDE -> R.string.option_repeat_side
                                RepeatMode.ONE -> R.string.option_repeat_one
                            },
                        )
                    },
                    onSelect = { vm.repeat(it) },
                )
                Divider()
                NavRow(
                    icon = Icons.Rounded.Tune,
                    title = stringResource(R.string.settings_fx_title),
                    body = stringResource(R.string.settings_fx_summary, stringResource(presetName(fx.preset)), speedLabel(fx.speed), (fx.hiss * 100).roundToInt()),
                    onClick = null,
                    trailing = {
                        TextButton(onClick = { vm.resetFx() }, enabled = !isDefault) { Text(stringResource(R.string.settings_fx_reset)) }
                    },
                )
            }
        }

        // ---- Diary reminder ----
        item { SectionLabel(stringResource(R.string.settings_diary)) }
        item {
            val r = s.reminder
            Group {
                ToggleRow(
                    Icons.Rounded.NotificationsActive,
                    R.string.settings_reminder,
                    if (permissions.value.notifications) R.string.settings_reminder_body else R.string.settings_reminder_body_blocked,
                    r.enabled,
                    onChange = { on ->
                        vm.reminder(r.copy(enabled = on))
                        if (on && !permissions.value.notifications) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            else AppActions.openNotificationSettings(context)
                        }
                    },
                )
                Divider()
                NavRow(
                    icon = Icons.Rounded.Schedule,
                    title = stringResource(R.string.settings_reminder_time),
                    body = stringResource(R.string.settings_reminder_time_body, formatClock(context, r.hour, r.minute)),
                    onClick = { pickTime = true },
                )
            }
        }

        // ---- Permissions & notifications ----
        item { SectionLabel(stringResource(R.string.settings_permissions)) }
        item {
            val p = permissions.value
            Group {
                NavRow(
                    icon = Icons.Rounded.Mic,
                    title = stringResource(R.string.settings_mic),
                    body = stringResource(if (p.microphone) R.string.settings_mic_body_on else R.string.settings_mic_body_off),
                    onClick = {
                        val canAsk = !micAsked || (activity != null && ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.RECORD_AUDIO))
                        if (!p.microphone && canAsk) micLauncher.launch(Manifest.permission.RECORD_AUDIO) else AppActions.openAppSettings(context)
                    },
                    trailing = { StatusPill(p.microphone, R.string.status_allowed, R.string.status_not_allowed) },
                )
                Divider()
                NavRow(
                    icon = Icons.Rounded.Notifications,
                    title = stringResource(R.string.settings_notifications),
                    body = stringResource(if (p.notifications) R.string.settings_notifications_body_on else R.string.settings_notifications_body_off),
                    onClick = {
                        if (!p.notifications && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            AppActions.openNotificationSettings(context)
                        }
                    },
                    trailing = { StatusPill(p.notifications, R.string.status_on, R.string.status_off) },
                )
            }
        }

        // ---- Storage ----
        item { SectionLabel(stringResource(R.string.settings_storage)) }
        item {
            val free = remember(storage) { vm.freeBytes() }
            val used = storage.bytes
            Group {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    LeadingIcon(Icons.Rounded.SdStorage)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            stringResource(R.string.storage_used, pluralStringResource(R.plurals.recordings_count, storage.count, storage.count), TimeFormat.fileSize(used)),
                            style = MaterialTheme.typography.titleSmall,
                        )
                        LinearProgressIndicator(
                            progress = { if (used > 0 && used + free > 0) (used.toFloat() / (used + free)).coerceIn(0.01f, 1f) else 0f },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                            color = palette.accent,
                            trackColor = palette.outline.copy(alpha = 0.4f),
                            drawStopIndicator = {},
                        )
                        Text(
                            stringResource(R.string.storage_free, TimeFormat.fileSize(free)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        // ---- Backup & restore ----
        item { SectionLabel(stringResource(R.string.settings_backup)) }
        item {
            val running = backup as? BackupManager.State.Running
            Group {
                NavRow(
                    Icons.Rounded.Backup,
                    stringResource(R.string.settings_backup_export),
                    stringResource(R.string.settings_backup_export_body),
                    onClick = if (running == null) ({ exportLauncher.launch(backupFileName()) }) else null,
                )
                Divider()
                NavRow(
                    Icons.Rounded.SettingsBackupRestore,
                    stringResource(R.string.settings_backup_restore),
                    stringResource(R.string.settings_backup_restore_body),
                    onClick = if (running == null) ({ restoreLauncher.launch(arrayOf(BackupManager.MIME, "application/x-zip-compressed", "application/octet-stream")) }) else null,
                )
                if (running != null) {
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            if (running.total > 0) {
                                stringResource(
                                    if (running.restoring) R.string.restore_progress else R.string.backup_progress,
                                    running.done,
                                    running.total,
                                )
                            } else {
                                stringResource(if (running.restoring) R.string.restore_preparing else R.string.backup_preparing)
                            },
                            style = MaterialTheme.typography.bodySmall,
                        )
                        if (running.total > 0) {
                            LinearProgressIndicator(
                                progress = { running.done / running.total.toFloat() },
                                modifier = Modifier.fillMaxWidth(),
                                drawStopIndicator = {},
                            )
                        } else {
                            LinearProgressIndicator(Modifier.fillMaxWidth())
                        }
                    }
                }
            }
            Text(
                stringResource(R.string.settings_backup_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, top = 8.dp),
            )
        }

        // ---- Privacy ----
        item { SectionLabel(stringResource(R.string.settings_privacy)) }
        item {
            Group {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                    LeadingIcon(Icons.Rounded.Lock, tint = palette.play)
                    Spacer(Modifier.width(14.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(stringResource(R.string.privacy_title), style = MaterialTheme.typography.titleSmall)
                        Text(stringResource(R.string.privacy_body), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Divider()
                ToggleRow(
                    Icons.Rounded.Fingerprint,
                    R.string.settings_app_lock,
                    if (deviceSecure) R.string.settings_app_lock_body else R.string.settings_app_lock_unavailable,
                    s.appLock,
                    enabled = deviceSecure || s.appLock,
                    onChange = { on -> if (on) authenticate(lockTitle, lockSubtitle) else vm.appLock(false) },
                )
                Divider()
                ToggleRow(Icons.Rounded.VisibilityOff, R.string.settings_hide_recents, R.string.settings_hide_recents_body, s.hideInRecents, vm::hideInRecents)
                Divider()
                NavRow(Icons.Rounded.Policy, stringResource(R.string.settings_privacy_policy), stringResource(R.string.settings_privacy_policy_body), onClick = { showPolicy = true })
            }
        }

        // ---- Help ----
        item { SectionLabel(stringResource(R.string.settings_help)) }
        item {
            Group {
                NavRow(Icons.Rounded.Replay, stringResource(R.string.settings_replay_intro), stringResource(R.string.settings_replay_intro_body), onClick = { vm.replayIntro() })
            }
        }

        // ---- Support ----
        item { SectionLabel(stringResource(R.string.settings_support)) }
        item {
            Group {
                NavRow(
                    Icons.Rounded.Star,
                    stringResource(R.string.settings_rate),
                    stringResource(R.string.settings_rate_body),
                    onClick = { if (!AppActions.rate(context)) scope.launch { snackbar.showSnackbar(noStore) } },
                    tint = palette.accent2,
                )
                Divider()
                NavRow(Icons.Rounded.Share, stringResource(R.string.settings_share), stringResource(R.string.settings_share_body), onClick = { AppActions.share(context) })
            }
        }

        // ---- About ----
        item { SectionLabel(stringResource(R.string.settings_about)) }
        item {
            Group {
                NavRow(
                    Icons.Rounded.Info,
                    stringResource(R.string.settings_about_row),
                    stringResource(R.string.settings_about_row_body, BuildConfig.VERSION_NAME),
                    onClick = onOpenAbout,
                )
                Divider()
                NavRow(
                    Icons.Rounded.RestartAlt,
                    stringResource(R.string.settings_reset),
                    stringResource(R.string.settings_reset_body),
                    onClick = { confirmReset = true },
                    tint = palette.rec,
                )
            }
        }
    }

    if (showPolicy) {
        TextDialog(title = stringResource(R.string.settings_privacy_policy), text = stringResource(R.string.privacy_policy_body), onDismiss = { showPolicy = false })
    }
    if (pickTime) {
        ReminderTimeDialog(
            hour = s.reminder.hour,
            minute = s.reminder.minute,
            onDismiss = { pickTime = false },
            onConfirm = { h, m ->
                pickTime = false
                vm.reminder(s.reminder.copy(minuteOfDay = h * 60 + m))
            },
        )
    }
    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            icon = { Icon(Icons.Rounded.RestartAlt, contentDescription = null) },
            title = { Text(stringResource(R.string.settings_reset_title)) },
            text = { Text(stringResource(R.string.settings_reset_confirm_body)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmReset = false
                    vm.resetAll { scope.launch { snackbar.showSnackbar(resetDone) } }
                }) { Text(stringResource(R.string.action_reset)) }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

private fun megabytesPerHour(q: RecordingQuality): Int = (q.bitRate / 8 * 3600 / 1_000_000)

@Composable
private fun QualityOption(q: RecordingQuality, selected: Boolean, onClick: () -> Unit) {
    val palette = Retro.palette
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) palette.accent.copy(alpha = 0.15f) else palette.surfaceRaised)
            .border(1.dp, if (selected) palette.accent else Color.Transparent, RoundedCornerShape(16.dp))
            .semantics { this.selected = selected }
            .clickable(role = Role.RadioButton, onClick = onClick)
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
                stringResource(R.string.quality_detail, String.format(Locale.US, "%.1f", q.sampleRate / 1000f), q.bitRate / 1000, megabytesPerHour(q)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (selected) Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = palette.accent)
    }
}

/** Rounded card that holds a run of related rows. */
@Composable
internal fun Group(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Retro.palette.surfaceRaised),
        content = content,
    )
}

@Composable
private fun Divider() {
    HorizontalDivider(Modifier.padding(start = 66.dp), color = Retro.palette.outline.copy(alpha = 0.5f))
}

@Composable
internal fun LeadingIcon(icon: ImageVector, tint: Color = Retro.palette.accent) {
    Box(
        Modifier.size(36.dp).clip(RoundedCornerShape(12.dp)).background(tint.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun ToggleRow(
    icon: ImageVector,
    title: Int,
    body: Int,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onChange)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LeadingIcon(icon)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(stringResource(title), style = MaterialTheme.typography.titleSmall)
            Text(stringResource(body), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = null, enabled = enabled)
    }
}

/** A setting with a few discrete values, shown as a segmented row under its title. */
@Composable
private fun <T> ChoiceRow(
    icon: ImageVector,
    title: Int,
    body: Int,
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            LeadingIcon(icon)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(title), style = MaterialTheme.typography.titleSmall)
                Text(stringResource(body), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(10.dp))
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(start = 50.dp)) {
            options.forEachIndexed { i, option ->
                SegmentedButton(
                    selected = option == selected,
                    onClick = { onSelect(option) },
                    shape = SegmentedButtonDefaults.itemShape(i, options.size),
                    icon = {},
                ) { Text(label(option), maxLines = 1, style = MaterialTheme.typography.labelMedium) }
            }
        }
    }
}

/** "8:00 PM" or "20:00", following the phone's 12/24-hour setting, always with English AM/PM. */
private fun formatClock(context: android.content.Context, hour: Int, minute: Int): String {
    val pattern = if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm a"
    return LocalTime.of(hour, minute).format(DateTimeFormatter.ofPattern(pattern, Locale.US))
}

private fun backupFileName(): String = "TapeBack-backup-${LocalDate.now()}.zip"

@Composable
private fun ReminderTimeDialog(hour: Int, minute: Int, onDismiss: () -> Unit, onConfirm: (Int, Int) -> Unit) {
    val context = LocalContext.current
    val state = rememberTimePickerState(initialHour = hour, initialMinute = minute, is24Hour = DateFormat.is24HourFormat(context))
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_reminder_time)) },
        text = { TimePicker(state = state) },
        confirmButton = { TextButton(onClick = { onConfirm(state.hour, state.minute) }) { Text(stringResource(R.string.action_save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

@Composable
internal fun NavRow(
    icon: ImageVector,
    title: String,
    body: String?,
    onClick: (() -> Unit)?,
    tint: Color = Retro.palette.accent,
    trailing: (@Composable () -> Unit)? = null,
) {
    val clickable = if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .then(clickable)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LeadingIcon(icon, tint)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            if (body != null) {
                Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.width(8.dp))
        when {
            trailing != null -> trailing()
            onClick != null -> Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun StatusPill(on: Boolean, onText: Int, offText: Int) {
    val palette = Retro.palette
    val color = if (on) palette.play else palette.rec
    Text(
        stringResource(if (on) onText else offText),
        style = MaterialTheme.typography.labelMedium,
        color = color,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

@Composable
internal fun TextDialog(title: String, text: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(text, style = MaterialTheme.typography.bodyMedium)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) } },
    )
}

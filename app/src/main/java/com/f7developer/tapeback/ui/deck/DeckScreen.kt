package com.f7developer.tapeback.ui.deck

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.view.HapticFeedbackConstants
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.f7developer.tapeback.R
import com.f7developer.tapeback.domain.policy.ReelPhysics
import com.f7developer.tapeback.domain.policy.TimeFormat
import com.f7developer.tapeback.ui.appContainer
import com.f7developer.tapeback.ui.components.CassetteArt
import com.f7developer.tapeback.ui.components.CassetteWell
import com.f7developer.tapeback.ui.components.DeckPanel
import com.f7developer.tapeback.ui.components.KeySymbol
import com.f7developer.tapeback.ui.components.LcdReadout
import com.f7developer.tapeback.ui.components.LiveWaveform
import com.f7developer.tapeback.ui.components.MechanicalCounter
import com.f7developer.tapeback.ui.components.StatusLamp
import com.f7developer.tapeback.ui.components.TransportKey
import com.f7developer.tapeback.ui.components.TransportKeys
import com.f7developer.tapeback.ui.components.VuMeter
import com.f7developer.tapeback.ui.components.WaveformScrubber
import com.f7developer.tapeback.ui.components.rememberReelMotion
import com.f7developer.tapeback.ui.recording.RecordingSheet
import com.f7developer.tapeback.ui.theme.BungeeFamily
import com.f7developer.tapeback.ui.theme.Retro
import com.f7developer.tapeback.ui.theme.SilkscreenStyle
import kotlinx.coroutines.delay

@Composable
fun DeckScreen(
    snackbar: SnackbarHostState,
    contentPadding: PaddingValues,
    onOpenSettings: () -> Unit,
    onOpenLibrary: () -> Unit,
) {
    val container = appContainer()
    val vm: DeckViewModel = viewModel { DeckViewModel(container) }
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val view = LocalView.current
    val palette = Retro.palette
    val reduceMotion = Retro.reduceMotion

    var sheetFor by remember { mutableStateOf<Pair<Long, Boolean>?>(null) }
    var showTargets by remember { mutableStateOf(false) }
    var showFx by remember { mutableStateOf(false) }
    var showMicSettings by remember { mutableStateOf(false) }

    val haptics by rememberUpdatedState(state.settings.haptics)
    fun tap() {
        if (haptics) view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    }

    val micLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            vm.onRec()
        } else {
            val activity = context as? Activity
            val canAskAgain = activity != null && ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.RECORD_AUDIO)
            if (!canAskAgain) showMicSettings = true
        }
    }

    LaunchedEffect(vm) {
        vm.events.collect { event ->
            when (event) {
                DeckEvent.RequestMicPermission -> micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                is DeckEvent.Message -> snackbar.showSnackbar(
                    if (event.arg != null) context.getString(event.text, event.arg) else context.getString(event.text),
                )
                is DeckEvent.RecordingSaved -> sheetFor = event.recordingId to event.sealNow
            }
        }
    }

    // Launcher shortcut "Record now" and other cross-screen record requests.
    LaunchedEffect(Unit) {
        container.recordRequest.collect { requested ->
            if (requested) {
                container.recordRequest.value = false
                vm.onRec()
            }
        }
    }

    val recording = state.isRecordingMode
    DisposableEffect(recording, state.settings.keepScreenOn) {
        view.keepScreenOn = recording && state.settings.keepScreenOn
        onDispose { view.keepScreenOn = false }
    }

    // 10 Hz readout ticker for counter and LCD while the tape moves.
    var positionMs by remember { mutableLongStateOf(0L) }
    var counter by remember { mutableIntStateOf(0) }
    val moving = state.mode != DeckMode.STOPPED
    LaunchedEffect(moving, state.loaded?.recording?.id, state.mode) {
        while (true) {
            positionMs = vm.livePositionMs()
            counter = ReelPhysics.counter(vm.progress())
            if (!moving) break
            delay(100)
        }
    }

    val reels = rememberReelMotion(state.reelSpeed, vm::progress, reduceMotion)

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= 720.dp
        val scroll = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scroll)
                .padding(top = contentPadding.calculateTopPadding(), bottom = contentPadding.calculateBottomPadding())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            DeckHeader(onOpenSettings)
            if (state.capsuleMode) CapsuleModeBanner(onCancel = vm::cancelCapsuleMode)

            val deckPanel: @Composable (Modifier) -> Unit = { m ->
                DeckPanel(m) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            stringResource(R.string.deck_silkscreen),
                            style = SilkscreenStyle,
                            color = palette.panelText.copy(alpha = 0.75f),
                            modifier = Modifier.weight(1f),
                        )
                        StatusLamp(stringResource(R.string.lamp_rec), palette.rec, on = recording, blink = state.mode == DeckMode.REC_PAUSED)
                        Spacer(Modifier.width(12.dp))
                        StatusLamp(stringResource(R.string.lamp_play), palette.play, on = state.mode == DeckMode.PLAYING, blink = state.mode == DeckMode.PAUSED)
                    }
                    Spacer(Modifier.height(10.dp))
                    CassetteWell(Modifier.fillMaxWidth()) {
                        val cassette = state.cassette
                        InsertableCassette(visible = state.inserted && cassette != null, reduceMotion = reduceMotion) {
                            if (cassette != null) {
                                val title = state.loaded?.recording?.title ?: cassette.title
                                CassetteArt(
                                    skin = cassette.skin,
                                    labelColor = Color(cassette.labelColor),
                                    title = title,
                                    sideLetter = state.side.letter,
                                    subtitle = if (state.loaded != null) cassette.title else cassette.category,
                                    progress = vm::progress,
                                    leftAngle = { reels.left },
                                    rightAngle = { reels.right },
                                    contentDescription = stringResource(R.string.cd_cassette_in_deck, cassette.title, state.side.letter),
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                        if (!state.inserted || cassette == null) {
                            EmptyWell(onInsert = {
                                tap()
                                if (cassette == null) showTargets = true else vm.insert()
                            }, onChoose = { showTargets = true })
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            MechanicalCounter(counter)
                            Text(stringResource(R.string.counter), style = SilkscreenStyle, color = palette.panelText.copy(alpha = 0.7f))
                        }
                        Spacer(Modifier.width(10.dp))
                        val shownMs = if (recording || state.mode == DeckMode.SAVING) state.recorder.elapsedMs else positionMs
                        LcdReadout(
                            text = TimeFormat.lcd(shownMs),
                            ghost = if (shownMs >= 3_600_000L) "8:88:88" else "88:88",
                            caption = when {
                                recording -> stringResource(R.string.lcd_rec)
                                state.isWinding -> stringResource(if (state.mode == DeckMode.WIND_FF) R.string.lcd_ff else R.string.lcd_rew)
                                state.durationMs > 0 -> "/ " + TimeFormat.lcd(state.durationMs)
                                else -> null
                            },
                            modifier = Modifier.weight(1f).height(48.dp),
                        )
                        Spacer(Modifier.width(10.dp))
                        FlipButton(
                            sideLetter = state.side.flipped().letter,
                            enabled = !state.recorder.isBusy && state.cassette != null,
                            onClick = {
                                tap()
                                vm.flipSide()
                            },
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        val levels = if (recording) container.recordLevels else container.playbackLevels
                        val meterActive = state.mode == DeckMode.RECORDING || state.mode == DeckMode.PLAYING
                        VuMeter(
                            channelLabel = "L",
                            level = { levels.left },
                            active = meterActive,
                            contentDescription = stringResource(R.string.cd_vu_left),
                            modifier = Modifier.weight(1f),
                        )
                        VuMeter(
                            channelLabel = "R",
                            level = { levels.right },
                            active = meterActive,
                            contentDescription = stringResource(R.string.cd_vu_right),
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            val controls: @Composable (Modifier) -> Unit = { m ->
                Column(m, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    TransportKeys(
                        keys = transportKeys(state, onKey = { tap() }, vm = vm),
                    )
                    TrackStrip(
                        state = state,
                        onPrev = { tap(); vm.previousTrack() },
                        onNext = { tap(); vm.nextTrack() },
                        onSeek = vm::seekTo,
                        livePosition = vm::livePositionMs,
                        liveLevels = container.recordingController.liveLevels,
                        liveHead = { container.recordingController.liveHead },
                        onDetails = { id -> sheetFor = id to false },
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        AssistChip(
                            onClick = { showTargets = true },
                            label = {
                                Text(
                                    stringResource(
                                        R.string.deck_target_chip,
                                        state.targetCassette?.title ?: "—",
                                        state.target?.side?.letter ?: "A",
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                            leadingIcon = { Icon(Icons.AutoMirrored.Rounded.QueueMusic, contentDescription = null, modifier = Modifier.size(18.dp)) },
                            colors = AssistChipDefaults.assistChipColors(containerColor = palette.surfaceRaised),
                            modifier = Modifier.weight(1f),
                        )
                        AssistChip(
                            onClick = { showFx = true },
                            label = {
                                Text(
                                    stringResource(presetName(state.fx.preset)) + " · " +
                                        speedLabel(state.fx.speed),
                                    maxLines = 1,
                                )
                            },
                            leadingIcon = { Icon(Icons.Rounded.GraphicEq, contentDescription = null, modifier = Modifier.size(18.dp)) },
                            colors = AssistChipDefaults.assistChipColors(containerColor = palette.surfaceRaised),
                        )
                    }
                }
            }

            if (wide) {
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    deckPanel(Modifier.weight(1.1f))
                    controls(Modifier.weight(1f))
                }
            } else {
                deckPanel(Modifier.fillMaxWidth().widthIn(max = 640.dp).align(Alignment.CenterHorizontally))
                controls(Modifier.fillMaxWidth().widthIn(max = 640.dp).align(Alignment.CenterHorizontally))
            }
        }
    }

    if (showTargets) {
        TargetPickerSheet(
            cassettes = state.cassettes,
            current = state.target,
            onPick = { id, side ->
                vm.selectTarget(id, side)
                showTargets = false
            },
            onManageLibrary = {
                showTargets = false
                onOpenLibrary()
            },
            onDismiss = { showTargets = false },
        )
    }
    if (showFx) {
        EffectsSheet(fx = state.fx, onChange = vm::setFx, onDismiss = { showFx = false })
    }
    sheetFor?.let { (id, seal) ->
        RecordingSheet(recordingId = id, onDismiss = { sheetFor = null }, onPlayInDeck = {}, openSealOnStart = seal)
    }
    if (showMicSettings) {
        AlertDialog(
            onDismissRequest = { showMicSettings = false },
            title = { Text(stringResource(R.string.mic_denied_title)) },
            text = { Text(stringResource(R.string.mic_denied_body)) },
            confirmButton = {
                TextButton(onClick = {
                    showMicSettings = false
                    context.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                }) { Text(stringResource(R.string.open_settings)) }
            },
            dismissButton = { TextButton(onClick = { showMicSettings = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

@Composable
private fun transportKeys(state: DeckUiState, onKey: () -> Unit, vm: DeckViewModel): List<TransportKey> {
    val m = state.mode
    val busyRec = state.recorder.isBusy
    fun press(block: () -> Unit): () -> Unit = {
        onKey()
        block()
    }
    return listOf(
        TransportKey(
            KeySymbol.REC, stringResource(R.string.key_rec), stringResource(R.string.cd_key_rec),
            latched = m == DeckMode.RECORDING || m == DeckMode.REC_PAUSED || m == DeckMode.STARTING,
            enabled = m != DeckMode.SAVING,
            onPress = press(vm::onRec),
        ),
        TransportKey(
            KeySymbol.PLAY, stringResource(R.string.key_play), stringResource(R.string.cd_key_play),
            // On a real deck PLAY goes down together with REC.
            latched = m == DeckMode.PLAYING || m == DeckMode.PAUSED || m == DeckMode.RECORDING || m == DeckMode.REC_PAUSED,
            enabled = !busyRec,
            onPress = press(vm::onPlay),
        ),
        TransportKey(
            KeySymbol.REW, stringResource(R.string.key_rew), stringResource(R.string.cd_key_rew),
            latched = m == DeckMode.WIND_REW,
            enabled = !busyRec,
            onPress = press { vm.onWind(forward = false) },
        ),
        TransportKey(
            KeySymbol.FF, stringResource(R.string.key_ff), stringResource(R.string.cd_key_ff),
            latched = m == DeckMode.WIND_FF,
            enabled = !busyRec,
            onPress = press { vm.onWind(forward = true) },
        ),
        TransportKey(
            KeySymbol.STOP, stringResource(R.string.key_stop), stringResource(R.string.cd_key_stop),
            latched = false,
            enabled = true,
            onPress = press(vm::onStop),
        ),
        TransportKey(
            KeySymbol.PAUSE, stringResource(R.string.key_pause), stringResource(R.string.cd_key_pause),
            latched = m == DeckMode.PAUSED || m == DeckMode.REC_PAUSED,
            enabled = m == DeckMode.PLAYING || m == DeckMode.PAUSED || m == DeckMode.RECORDING || m == DeckMode.REC_PAUSED,
            onPress = press(vm::onPause),
        ),
    )
}

/** The cassette slides out of the well on eject and drops back in on insert. */
@Composable
private fun InsertableCassette(visible: Boolean, reduceMotion: Boolean, content: @Composable () -> Unit) {
    AnimatedVisibility(
        visible = visible,
        enter = if (reduceMotion) fadeIn(tween(1)) else slideInVertically(tween(420)) { -it / 2 } + fadeIn(tween(300)),
        exit = if (reduceMotion) fadeOut(tween(1)) else slideOutVertically(tween(380)) { -it } + fadeOut(tween(300)),
    ) { content() }
}

@Composable
private fun DeckHeader(onOpenSettings: () -> Unit) {
    val palette = Retro.palette
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.brand_wordmark),
                style = MaterialTheme.typography.headlineMedium.copy(
                    brush = Brush.horizontalGradient(listOf(palette.accent, palette.accent2)),
                ),
                modifier = Modifier.semantics { heading() },
            )
            Text(stringResource(R.string.tagline), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onClick = onOpenSettings, modifier = Modifier.size(48.dp)) {
            Icon(Icons.Rounded.Settings, contentDescription = stringResource(R.string.cd_settings))
        }
    }
}

@Composable
private fun CapsuleModeBanner(onCancel: () -> Unit) {
    val palette = Retro.palette
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.horizontalGradient(listOf(palette.accent2.copy(alpha = 0.35f), palette.accent.copy(alpha = 0.3f))))
            .padding(start = 14.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.HourglassTop, contentDescription = null, tint = palette.accent)
        Spacer(Modifier.width(10.dp))
        Text(stringResource(R.string.capsule_mode_banner), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        IconButton(onClick = onCancel) { Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.cd_cancel_capsule_mode)) }
    }
}

@Composable
private fun EmptyWell(onInsert: () -> Unit, onChoose: () -> Unit) {
    val palette = Retro.palette
    Column(
        modifier = Modifier.fillMaxWidth().aspectRatio(1.75f),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(stringResource(R.string.well_empty), style = MaterialTheme.typography.titleMedium.copy(fontFamily = BungeeFamily), color = palette.panelText)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FilledTonalButton(onClick = onInsert) { Text(stringResource(R.string.well_insert)) }
            TextButton(onClick = onChoose) { Text(stringResource(R.string.well_choose)) }
        }
    }
}

@Composable
private fun FlipButton(sideLetter: String, enabled: Boolean, onClick: () -> Unit) {
    val palette = Retro.palette
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Brush.verticalGradient(listOf(palette.keyTop, palette.keyBottom)))
                .border(1.dp, Color.Black.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                .clickable(enabled = enabled, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.SwapVert,
                contentDescription = stringResource(R.string.cd_flip_side, sideLetter),
                tint = palette.keyText.copy(alpha = if (enabled) 1f else 0.4f),
            )
        }
        Text(stringResource(R.string.flip_to, sideLetter), style = SilkscreenStyle, color = palette.panelText.copy(alpha = 0.7f))
    }
}

@Composable
private fun TrackStrip(
    state: DeckUiState,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Float) -> Unit,
    livePosition: () -> Long,
    liveLevels: FloatArray,
    liveHead: () -> Int,
    onDetails: (Long) -> Unit,
) {
    val palette = Retro.palette
    val recording = state.isRecordingMode
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(palette.surface)
            .border(1.dp, palette.outline.copy(alpha = 0.6f), RoundedCornerShape(22.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(enabled = state.loaded != null && !recording) { state.loaded?.let { onDetails(it.recording.id) } }
                    .padding(vertical = 2.dp),
            ) {
                val title = when {
                    recording -> stringResource(R.string.recording_now)
                    state.mode == DeckMode.SAVING -> stringResource(R.string.saving_tape)
                    state.loaded != null -> state.loaded.recording.title
                    else -> stringResource(R.string.no_tape_loaded)
                }
                Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val sub = when {
                    recording -> stringResource(R.string.recording_to, state.targetCassette?.title ?: "", state.side.letter)
                    state.loaded != null -> stringResource(
                        R.string.track_position,
                        state.player.currentIndex + 1,
                        state.player.queue.size.coerceAtLeast(1),
                        state.side.letter,
                    )
                    else -> stringResource(R.string.no_tape_hint)
                }
                Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
            val canSkip = state.loaded != null && !recording
            IconButton(onClick = onPrev, enabled = canSkip) {
                Icon(Icons.Rounded.SkipPrevious, contentDescription = stringResource(R.string.cd_previous))
            }
            IconButton(onClick = onNext, enabled = canSkip && state.player.currentIndex < state.player.queue.size - 1) {
                Icon(Icons.Rounded.SkipNext, contentDescription = stringResource(R.string.cd_next))
            }
        }
        if (recording) {
            LiveWaveform(
                levels = liveLevels,
                head = liveHead,
                tick = state.recorder.tick,
                color = palette.rec,
            )
        } else {
            val duration = state.durationMs
            WaveformScrubber(
                waveform = state.loaded?.recording?.waveform.orEmpty(),
                progress = { if (duration > 0) livePosition().toFloat() / duration else 0f },
                onSeek = onSeek,
                playedColor = palette.accent,
                idleColor = palette.outline,
                enabled = state.loaded != null && duration > 0,
                animate = state.mode == DeckMode.PLAYING || state.isWinding,
                contentDescription = stringResource(R.string.cd_scrubber),
            )
        }
    }
}

package com.f7developer.tapeback.ui.deck

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.f7developer.tapeback.R
import com.f7developer.tapeback.audio.play.PlayerEvent
import com.f7developer.tapeback.audio.play.PlayerSnapshot
import com.f7developer.tapeback.audio.play.SleepTimer
import com.f7developer.tapeback.audio.record.RecorderEvent
import com.f7developer.tapeback.audio.record.RecorderState
import com.f7developer.tapeback.audio.record.RecorderStatus
import com.f7developer.tapeback.core.sound.Sfx
import com.f7developer.tapeback.di.AppContainer
import com.f7developer.tapeback.domain.model.AppSettings
import com.f7developer.tapeback.domain.model.Cassette
import com.f7developer.tapeback.domain.model.CassetteSummary
import com.f7developer.tapeback.domain.model.PlaybackFx
import com.f7developer.tapeback.domain.model.RecordTarget
import com.f7developer.tapeback.domain.model.RecordingItem
import com.f7developer.tapeback.domain.model.RepeatMode
import com.f7developer.tapeback.domain.model.TapeSide
import com.f7developer.tapeback.domain.policy.ReelPhysics
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

enum class DeckMode { STOPPED, PLAYING, PAUSED, COUNTDOWN, STARTING, RECORDING, REC_PAUSED, SAVING, WIND_FF, WIND_REW }

data class DeckUiState(
    val mode: DeckMode = DeckMode.STOPPED,
    val inserted: Boolean = true,
    /** The cassette physically in the deck: the loaded recording's cassette, or the record target. */
    val cassette: Cassette? = null,
    val side: TapeSide = TapeSide.A,
    val target: RecordTarget? = null,
    val targetCassette: Cassette? = null,
    val loaded: RecordingItem? = null,
    val player: PlayerSnapshot = PlayerSnapshot(),
    val recorder: RecorderState = RecorderState(),
    val cassettes: List<CassetteSummary> = emptyList(),
    val settings: AppSettings = AppSettings(),
    val capsuleMode: Boolean = false,
    /** Seconds left on the pre-roll countdown, 0 when none is running. */
    val countdown: Int = 0,
    val sleepTimer: SleepTimer? = null,
) {
    val fx: PlaybackFx get() = settings.fx
    val isRecordingMode: Boolean get() = mode == DeckMode.RECORDING || mode == DeckMode.REC_PAUSED || mode == DeckMode.STARTING
    val isWinding: Boolean get() = mode == DeckMode.WIND_FF || mode == DeckMode.WIND_REW
    val durationMs: Long get() = player.durationMs.takeIf { it > 0 } ?: loaded?.recording?.durationMs ?: 0L

    /** Signed transport speed in multiples of normal play, for the reel animation. */
    val reelSpeed: Float
        get() = when (mode) {
            DeckMode.PLAYING -> fx.speed
            DeckMode.RECORDING -> 1f
            DeckMode.WIND_FF -> ReelPhysics.WIND_MULTIPLIER
            DeckMode.WIND_REW -> -ReelPhysics.WIND_MULTIPLIER
            else -> 0f
        }
}

sealed interface DeckEvent {
    data object RequestMicPermission : DeckEvent
    data class Message(@StringRes val text: Int, val arg: String? = null) : DeckEvent
    data class RecordingSaved(val recordingId: Long, val sealNow: Boolean) : DeckEvent
    /** Saved without opening the details sheet (the user turned it off in Settings). */
    data class RecordingSavedQuietly(val recordingId: Long, val cassetteTitle: String, val side: String) : DeckEvent
}

private data class LocalDeck(
    val inserted: Boolean = true,
    val pausedByKey: Boolean = false,
    val wind: Int = 0, // -1 rewind, 1 fast-forward
    val countdown: Int = 0,
)

class DeckViewModel(private val container: AppContainer) : ViewModel() {

    private val recorder = container.recordingController
    private val player = container.playerConnection
    private val sounds = container.sounds
    private val settingsRepo = container.settingsRepository

    private val local = MutableStateFlow(LocalDeck())
    private var windJob: Job? = null
    private var countdownJob: Job? = null
    @Volatile private var windPositionMs: Long = 0L

    private val _events = MutableSharedFlow<DeckEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<DeckEvent> = _events.asSharedFlow()

    private val loadedItem: Flow<RecordingItem?> = player.snapshot
        .map { it.currentId }
        .distinctUntilChanged()
        .flatMapLatest { id -> if (id == null) flowOf(null) else container.recordingRepository.observeItem(id) }

    private val base = combine(
        settingsRepo.settings,
        recorder.state,
        player.snapshot,
        container.cassetteRepository.observeSummaries(),
        loadedItem,
    ) { settings, rec, snap, cassettes, loaded -> Base(settings, rec, snap, cassettes, loaded) }

    private data class Base(
        val settings: AppSettings,
        val rec: RecorderState,
        val snap: PlayerSnapshot,
        val cassettes: List<CassetteSummary>,
        val loaded: RecordingItem?,
    )

    val state: StateFlow<DeckUiState> = combine(base, local, container.capsuleModeRequest, player.sleepTimer) { b, l, capsule, sleep ->
        val target = b.settings.recordTarget
            ?.takeIf { t -> b.cassettes.any { it.cassette.id == t.cassetteId } }
            ?: b.cassettes.firstOrNull()?.let { RecordTarget(it.cassette.id, TapeSide.A) }
        val targetCassette = target?.let { t -> b.cassettes.firstOrNull { it.cassette.id == t.cassetteId }?.cassette }
        val loadedCassette = b.loaded?.let { item -> b.cassettes.firstOrNull { it.cassette.id == item.recording.cassetteId }?.cassette }
        val inDeck = if (b.rec.isBusy) targetCassette else loadedCassette ?: targetCassette
        val side = if (b.rec.isBusy) target?.side ?: TapeSide.A else b.loaded?.recording?.side ?: target?.side ?: TapeSide.A
        DeckUiState(
            mode = deriveMode(b.rec, b.snap, l),
            inserted = l.inserted || b.rec.isBusy || b.snap.hasTape,
            cassette = inDeck,
            side = side,
            target = target,
            targetCassette = targetCassette,
            loaded = if (b.rec.isBusy) null else b.loaded,
            player = b.snap,
            recorder = b.rec,
            cassettes = b.cassettes,
            settings = b.settings,
            capsuleMode = capsule,
            countdown = l.countdown,
            sleepTimer = sleep,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DeckUiState())

    init {
        viewModelScope.launch {
            recorder.events.collect { event ->
                when (event) {
                    is RecorderEvent.Saved -> onSaved(event.recordingId, event.autoStopped)
                    RecorderEvent.TooShort -> message(R.string.msg_too_short)
                    is RecorderEvent.Failed -> message(
                        when (event.reason) {
                            RecorderEvent.Reason.PERMISSION -> R.string.msg_mic_permission
                            RecorderEvent.Reason.STORAGE -> R.string.msg_low_storage
                            RecorderEvent.Reason.MIC_BUSY -> R.string.msg_mic_busy
                            RecorderEvent.Reason.ENGINE -> R.string.msg_recorder_failed
                        },
                    )
                }
            }
        }
        viewModelScope.launch {
            player.events.collect { event ->
                when (event) {
                    PlayerEvent.TapeDamaged -> message(R.string.msg_tape_damaged)
                    PlayerEvent.CapsuleLocked -> message(R.string.msg_capsule_locked)
                    PlayerEvent.SleepTimerFired -> {
                        local.update { it.copy(pausedByKey = false) }
                        message(R.string.msg_sleep_fired)
                    }
                }
            }
        }
        viewModelScope.launch {
            // Playback reaching the end releases the PLAY key like an auto-stop deck.
            player.snapshot.map { it.ended }.distinctUntilChanged().collect { ended ->
                if (ended) local.update { it.copy(pausedByKey = false) }
            }
        }
    }

    private fun deriveMode(rec: RecorderState, snap: PlayerSnapshot, l: LocalDeck): DeckMode = when (rec.status) {
        RecorderStatus.STARTING -> DeckMode.STARTING
        RecorderStatus.RECORDING -> DeckMode.RECORDING
        RecorderStatus.PAUSED -> DeckMode.REC_PAUSED
        RecorderStatus.SAVING -> DeckMode.SAVING
        RecorderStatus.IDLE -> when {
            l.countdown > 0 -> DeckMode.COUNTDOWN
            l.wind > 0 -> DeckMode.WIND_FF
            l.wind < 0 -> DeckMode.WIND_REW
            snap.isPlaying || (snap.playWhenReady && snap.buffering) -> DeckMode.PLAYING
            snap.hasTape && l.pausedByKey -> DeckMode.PAUSED
            else -> DeckMode.STOPPED
        }
    }

    /** Position for 60 fps animation (winding position while winding). */
    fun livePositionMs(): Long = if (local.value.wind != 0) windPositionMs else player.livePositionMs()

    /** Tape progress 0..1 for reel pack sizes. */
    fun progress(): Float {
        val s = state.value
        return when {
            s.isRecordingMode || s.mode == DeckMode.SAVING ->
                (s.recorder.elapsedMs.toFloat() / SIDE_CAPACITY_MS).coerceIn(0f, 0.98f)
            s.durationMs > 0 -> (livePositionMs().toFloat() / s.durationMs).coerceIn(0f, 1f)
            else -> 0f
        }
    }

    // ---- Transport keys -------------------------------------------------------------------

    fun onRec() {
        val s = state.value
        when (s.recorder.status) {
            RecorderStatus.PAUSED -> {
                viewModelScope.launch {
                    sounds.play(Sfx.KEY)
                    delay(CLICK_GUARD_MS) // keep the click out of the recording
                    recorder.resume()
                }
                return
            }
            RecorderStatus.IDLE -> Unit
            else -> return
        }
        if (local.value.countdown > 0) {
            // REC again during the countdown skips straight to recording.
            countdownJob?.cancel()
            countdownJob = null
            local.update { it.copy(countdown = 0) }
            viewModelScope.launch { beginRecording() }
            return
        }
        if (!recorder.hasPermission()) {
            _events.tryEmit(DeckEvent.RequestMicPermission)
            return
        }
        val seconds = s.settings.countdownSeconds
        if (seconds <= 0) {
            viewModelScope.launch { beginRecording() }
            return
        }
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            stopWinding(seek = true)
            if (s.player.isPlaying || s.player.playWhenReady) player.pause()
            for (n in seconds downTo 1) {
                local.update { it.copy(countdown = n, inserted = true, pausedByKey = false) }
                sounds.play(Sfx.KEY)
                delay(1_000)
            }
            local.update { it.copy(countdown = 0) }
            countdownJob = null
            beginRecording()
        }
    }

    private suspend fun beginRecording() {
        val s = state.value
        stopWinding(seek = true)
        if (s.player.isPlaying || s.player.playWhenReady) player.pause()
        local.update { it.copy(inserted = true, pausedByKey = false) }
        val target = s.target ?: container.cassetteRepository.ensureDefault().let { RecordTarget(it.id, TapeSide.A) }
        sounds.play(Sfx.LATCH)
        delay(CLICK_GUARD_MS + 100) // keep the latch click out of the recording
        recorder.start(target, s.settings.quality, s.settings.autoStopMinutes * 60_000L)
    }

    /** Cancels a running pre-roll countdown. Returns true if one was running. */
    private fun cancelCountdown(): Boolean {
        if (local.value.countdown == 0 && countdownJob == null) return false
        countdownJob?.cancel()
        countdownJob = null
        local.update { it.copy(countdown = 0) }
        return true
    }

    fun onPlay() {
        cancelCountdown()
        val s = state.value
        if (s.recorder.isBusy) {
            message(R.string.msg_stop_recording_first)
            return
        }
        sounds.play(Sfx.LATCH)
        if (s.isWinding) {
            stopWinding(seek = true)
            player.play()
            return
        }
        local.update { it.copy(inserted = true, pausedByKey = false) }
        if (s.player.hasTape) {
            player.play()
        } else {
            viewModelScope.launch { loadTargetSide(playWhenReady = true) }
        }
    }

    fun onPause() {
        val s = state.value
        when (s.recorder.status) {
            RecorderStatus.RECORDING -> {
                recorder.pause()
                sounds.play(Sfx.KEY) // after pausing: never captured
                return
            }
            RecorderStatus.PAUSED -> {
                onRec()
                return
            }
            RecorderStatus.IDLE -> Unit
            else -> return
        }
        if (s.isWinding) return
        sounds.play(Sfx.KEY)
        when (s.mode) {
            DeckMode.PLAYING -> {
                player.pause()
                local.update { it.copy(pausedByKey = true) }
            }
            DeckMode.PAUSED -> {
                local.update { it.copy(pausedByKey = false) }
                player.play()
            }
            else -> Unit
        }
    }

    fun onStop() {
        if (cancelCountdown()) {
            sounds.play(Sfx.STOP)
            return
        }
        val s = state.value
        when {
            s.recorder.isCapturing || s.recorder.status == RecorderStatus.STARTING -> {
                recorder.stop()
                sounds.play(Sfx.STOP)
            }
            s.isWinding -> {
                stopWinding(seek = true)
                sounds.play(Sfx.STOP)
            }
            s.mode == DeckMode.PLAYING || s.mode == DeckMode.PAUSED -> {
                player.pause()
                local.update { it.copy(pausedByKey = false) }
                sounds.play(Sfx.STOP)
            }
            s.player.hasTape -> {
                // Pressing STOP while stopped ejects, like the STOP/EJECT key on a real deck.
                player.eject()
                local.update { it.copy(inserted = false) }
                sounds.play(Sfx.EJECT)
            }
            s.inserted -> {
                local.update { it.copy(inserted = false) }
                sounds.play(Sfx.EJECT)
            }
        }
    }

    fun onWind(forward: Boolean) {
        cancelCountdown()
        val s = state.value
        if (s.recorder.isBusy) {
            message(R.string.msg_stop_recording_first)
            return
        }
        val dir = if (forward) 1 else -1
        if (local.value.wind == dir) {
            stopWinding(seek = true)
            sounds.play(Sfx.STOP)
            return
        }
        sounds.play(Sfx.KEY)
        viewModelScope.launch {
            if (!state.value.player.hasTape) {
                if (!loadTargetSide(playWhenReady = false)) return@launch
                withTimeoutOrNull(1_500) { player.snapshot.first { it.hasTape && it.durationMs > 0 } }
            }
            val wasWinding = local.value.wind != 0
            if (!wasWinding) windPositionMs = player.livePositionMs()
            player.pause()
            local.update { it.copy(wind = dir, pausedByKey = false, inserted = true) }
            sounds.startWind(if (forward) 1.15f else 1f)
            windJob?.cancel()
            windJob = viewModelScope.launch {
                var last = System.nanoTime()
                while (isActive) {
                    delay(16)
                    val now = System.nanoTime()
                    val dtMs = (now - last) / 1_000_000f
                    last = now
                    val duration = state.value.durationMs
                    val next = windPositionMs + (dir * ReelPhysics.WIND_MULTIPLIER * dtMs).toLong()
                    if (next <= 0L || (duration > 0 && next >= duration)) {
                        windPositionMs = next.coerceIn(0L, duration.coerceAtLeast(0L))
                        stopWinding(seek = true, cancelJob = false)
                        sounds.play(Sfx.STOP)
                        break
                    }
                    windPositionMs = next
                }
            }
        }
    }

    private fun stopWinding(seek: Boolean, cancelJob: Boolean = true) {
        if (local.value.wind == 0) return
        if (cancelJob) windJob?.cancel()
        windJob = null
        sounds.stopWind()
        if (seek) player.seekTo(windPositionMs)
        local.update { it.copy(wind = 0) }
    }

    fun seekTo(fraction: Float) {
        val s = state.value
        if (s.recorder.isBusy || s.durationMs <= 0) return
        val pos = (fraction.coerceIn(0f, 1f) * s.durationMs).toLong()
        if (s.isWinding) windPositionMs = pos else player.seekTo(pos)
    }

    fun nextTrack() {
        if (state.value.recorder.isBusy) return
        sounds.play(Sfx.KEY)
        player.next()
    }

    fun previousTrack() {
        if (state.value.recorder.isBusy) return
        sounds.play(Sfx.KEY)
        player.previous()
    }

    // ---- Cassette / side selection --------------------------------------------------------

    fun selectTarget(cassetteId: Long, side: TapeSide) {
        val s = state.value
        if (s.recorder.isBusy) {
            message(R.string.msg_stop_recording_first)
            return
        }
        viewModelScope.launch {
            val target = RecordTarget(cassetteId, side)
            settingsRepo.setRecordTarget(target)
            val loaded = s.loaded?.recording
            if (loaded != null && (loaded.cassetteId != cassetteId || loaded.side != side)) {
                stopWinding(seek = false)
                player.eject()
            }
            local.update { it.copy(inserted = true, pausedByKey = false) }
            sounds.play(Sfx.INSERT)
        }
    }

    /** Turn the cassette over, like flipping a real tape to its other side. */
    fun flipSide() {
        val s = state.value
        val cassette = s.cassette ?: return
        selectTarget(cassette.id, s.side.flipped())
    }

    fun insert() {
        local.update { it.copy(inserted = true) }
        sounds.play(Sfx.INSERT)
    }

    fun setFx(fx: PlaybackFx) {
        viewModelScope.launch { settingsRepo.setFx(fx) }
    }

    fun cancelCapsuleMode() {
        container.capsuleModeRequest.value = false
    }

    // ---- Repeat, skip silence, sleep timer --------------------------------------------------

    fun cycleRepeat() {
        val next = when (state.value.settings.repeatMode) {
            RepeatMode.OFF -> RepeatMode.SIDE
            RepeatMode.SIDE -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        sounds.play(Sfx.KEY)
        viewModelScope.launch { settingsRepo.setRepeatMode(next) }
    }

    fun setSkipSilence(on: Boolean) {
        viewModelScope.launch { settingsRepo.setSkipSilence(on) }
    }

    /** minutes > 0 arms a timer, 0 cancels, [SLEEP_END_OF_TRACK] stops after the current recording. */
    fun setSleepTimer(minutes: Int) {
        when {
            minutes == SLEEP_END_OF_TRACK -> player.setSleepAtEndOfTrack()
            minutes > 0 -> player.setSleepTimer(minutes)
            else -> player.cancelSleepTimer()
        }
    }

    // ---- Helpers --------------------------------------------------------------------------

    /** Loads the current target side. Returns false (with a message) if the side is blank. */
    private suspend fun loadTargetSide(playWhenReady: Boolean): Boolean {
        val target = state.value.target ?: return false
        val items = container.recordingRepository.bySide(target.cassetteId, target.side)
        if (items.isEmpty()) {
            message(R.string.msg_side_blank, target.side.letter)
            return false
        }
        val now = System.currentTimeMillis()
        val firstPlayable = items.indexOfFirst { it.recording.capsuleUnlockAt == null || it.recording.capsuleUnlockAt <= now }
        if (firstPlayable < 0) {
            message(R.string.msg_capsule_locked)
            return false
        }
        player.load(items, firstPlayable, playWhenReady)
        return true
    }

    private suspend fun onSaved(recordingId: Long, autoStopped: Boolean = false) {
        if (autoStopped) message(R.string.msg_auto_stopped, state.value.settings.autoStopMinutes.toString())
        val item = container.recordingRepository.get(recordingId) ?: return
        val sideItems = container.recordingRepository.bySide(item.recording.cassetteId, item.recording.side)
        val index = sideItems.indexOfFirst { it.recording.id == recordingId }.coerceAtLeast(0)
        val sealNow = container.capsuleModeRequest.value
        container.capsuleModeRequest.value = false
        if (!sealNow) player.load(sideItems, index, playWhenReady = false)
        local.update { it.copy(pausedByKey = false) }
        if (!sealNow && !state.value.settings.detailsAfterRecording) {
            _events.emit(DeckEvent.RecordingSavedQuietly(recordingId, item.cassetteTitle, item.recording.side.letter))
        } else {
            _events.emit(DeckEvent.RecordingSaved(recordingId, sealNow))
        }
    }

    private fun message(@StringRes res: Int, arg: String? = null) {
        _events.tryEmit(DeckEvent.Message(res, arg))
    }

    override fun onCleared() {
        stopWinding(seek = true)
        super.onCleared()
    }

    companion object {
        /** One side of a C90: the reels fill over 45 minutes while recording. */
        const val SIDE_CAPACITY_MS = 45 * 60 * 1000L
        const val CLICK_GUARD_MS = 140L
        const val SLEEP_END_OF_TRACK = -1
        val SLEEP_CHOICES = listOf(15, 30, 45, 60, 90)
    }
}

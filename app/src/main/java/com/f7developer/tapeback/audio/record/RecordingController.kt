package com.f7developer.tapeback.audio.record

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaMetadataRetriever
import android.os.SystemClock
import android.util.Log
import androidx.core.content.ContextCompat
import com.f7developer.tapeback.audio.fx.LevelBus
import com.f7developer.tapeback.core.AppFiles
import com.f7developer.tapeback.data.CassetteRepository
import com.f7developer.tapeback.data.RecordingRepository
import com.f7developer.tapeback.domain.model.RecordTarget
import com.f7developer.tapeback.domain.model.RecordingQuality
import com.f7developer.tapeback.domain.policy.TimeFormat
import com.f7developer.tapeback.domain.policy.WaveformCodec
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.ZoneId

enum class RecorderStatus { IDLE, STARTING, RECORDING, PAUSED, SAVING }

data class RecorderState(
    val status: RecorderStatus = RecorderStatus.IDLE,
    val target: RecordTarget? = null,
    val cassetteTitle: String = "",
    val elapsedMs: Long = 0L,
    /** Increments on every meter tick so the live waveform can redraw. */
    val tick: Long = 0L,
) {
    val isBusy: Boolean get() = status != RecorderStatus.IDLE
    val isCapturing: Boolean get() = status == RecorderStatus.RECORDING || status == RecorderStatus.PAUSED
}

sealed interface RecorderEvent {
    data class Saved(val recordingId: Long, val durationMs: Long) : RecorderEvent
    data object TooShort : RecorderEvent
    data class Failed(val reason: Reason) : RecorderEvent

    enum class Reason { PERMISSION, STORAGE, MIC_BUSY, ENGINE }
}

/**
 * App-scoped owner of the recording session. Every stop path funnels through [stop], which
 * finalizes the file and stores the Room row, so a recording is never silently lost.
 */
class RecordingController(
    private val context: Context,
    private val scope: CoroutineScope,
    private val files: AppFiles,
    private val recordings: RecordingRepository,
    private val cassettes: CassetteRepository,
    val levels: LevelBus,
) {
    private val engine = RecorderEngine(context)

    private val _state = MutableStateFlow(RecorderState())
    val state: StateFlow<RecorderState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<RecorderEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<RecorderEvent> = _events.asSharedFlow()

    /** Ring buffer of the most recent perceptual levels for the live waveform. */
    val liveLevels = FloatArray(LIVE_BINS)
    @Volatile var liveHead: Int = 0
        private set

    private var session: Session? = null
    private var ticker: Job? = null

    private class Session(
        val file: File,
        val target: RecordTarget,
        val quality: RecordingQuality,
        val createdAt: Long,
        var startedRealtime: Long = 0L,
        var pausedSince: Long = 0L,
        var pausedTotal: Long = 0L,
        val levels: ArrayList<Float> = ArrayList(4096),
    ) {
        fun elapsed(now: Long): Long {
            if (startedRealtime == 0L) return 0L
            val pausedNow = if (pausedSince > 0L) now - pausedSince else 0L
            return (now - startedRealtime - pausedTotal - pausedNow).coerceAtLeast(0L)
        }
    }

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    /** Called from the UI (which must be visible). Starts the foreground service which begins capture. */
    fun start(target: RecordTarget, quality: RecordingQuality) {
        if (_state.value.isBusy) return
        if (!hasPermission()) {
            _events.tryEmit(RecorderEvent.Failed(RecorderEvent.Reason.PERMISSION))
            return
        }
        if (files.freeBytes() < AppFiles.MIN_FREE_BYTES) {
            _events.tryEmit(RecorderEvent.Failed(RecorderEvent.Reason.STORAGE))
            return
        }
        session = Session(files.newRecordingFile(), target, quality, System.currentTimeMillis())
        liveLevels.fill(0f)
        _state.value = RecorderState(status = RecorderStatus.STARTING, target = target)
        scope.launch {
            val title = cassettes.get(target.cassetteId)?.title.orEmpty()
            _state.update { if (it.target == target) it.copy(cassetteTitle = title) else it }
        }
        val intent = Intent(context, RecordingService::class.java).setAction(RecordingService.ACTION_START)
        try {
            ContextCompat.startForegroundService(context, intent)
        } catch (e: Exception) {
            Log.e(TAG, "Could not start recording service", e)
            failStart(RecorderEvent.Reason.ENGINE)
        }
    }

    /** Invoked by [RecordingService] after it entered the foreground. */
    internal fun beginCapture() {
        val s = session ?: return
        if (_state.value.status != RecorderStatus.STARTING) return
        try {
            engine.start(s.file, s.quality) { _ -> scope.launch(Dispatchers.Main) { stop(fromError = true) } }
        } catch (e: Exception) {
            Log.e(TAG, "MediaRecorder failed to start", e)
            failStart(RecorderEvent.Reason.MIC_BUSY)
            return
        }
        s.startedRealtime = SystemClock.elapsedRealtime()
        _state.update { it.copy(status = RecorderStatus.RECORDING) }
        startTicker()
    }

    internal fun failStart(reason: RecorderEvent.Reason) {
        engine.release()
        session?.file?.delete()
        session = null
        ticker?.cancel()
        levels.clear()
        _state.value = RecorderState()
        _events.tryEmit(RecorderEvent.Failed(reason))
    }

    fun pause() {
        val s = session ?: return
        if (_state.value.status != RecorderStatus.RECORDING) return
        engine.pause()
        s.pausedSince = SystemClock.elapsedRealtime()
        levels.clear()
        _state.update { it.copy(status = RecorderStatus.PAUSED, elapsedMs = s.elapsed(SystemClock.elapsedRealtime())) }
    }

    fun resume() {
        val s = session ?: return
        if (_state.value.status != RecorderStatus.PAUSED) return
        engine.resume()
        if (s.pausedSince > 0L) s.pausedTotal += SystemClock.elapsedRealtime() - s.pausedSince
        s.pausedSince = 0L
        _state.update { it.copy(status = RecorderStatus.RECORDING) }
    }

    fun togglePause() {
        when (_state.value.status) {
            RecorderStatus.RECORDING -> pause()
            RecorderStatus.PAUSED -> resume()
            else -> Unit
        }
    }

    /** Single exit path for every way a recording can end. */
    fun stop(fromError: Boolean = false) {
        val s = session ?: return
        val status = _state.value.status
        if (status == RecorderStatus.SAVING || status == RecorderStatus.IDLE) return
        if (status == RecorderStatus.STARTING) {
            failStart(RecorderEvent.Reason.ENGINE)
            return
        }
        ticker?.cancel()
        val now = SystemClock.elapsedRealtime()
        val elapsed = s.elapsed(now)
        _state.update { it.copy(status = RecorderStatus.SAVING, elapsedMs = elapsed) }
        val finalized = engine.stop()
        levels.clear()
        session = null
        scope.launch {
            val event = withContext(Dispatchers.IO) { persist(s, finalized, elapsed) }
            _state.value = RecorderState()
            _events.emit(event)
            if (fromError && event is RecorderEvent.Saved) Log.w(TAG, "Recording ended by engine error; saved what was captured")
        }
    }

    private suspend fun persist(s: Session, finalized: Boolean, elapsed: Long): RecorderEvent {
        val file = s.file
        if (!finalized || !file.exists() || file.length() == 0L || elapsed < MIN_DURATION_MS) {
            file.delete()
            return if (finalized || elapsed < MIN_DURATION_MS) RecorderEvent.TooShort
            else RecorderEvent.Failed(RecorderEvent.Reason.ENGINE)
        }
        val measured = runCatching {
            MediaMetadataRetriever().run {
                try {
                    setDataSource(file.absolutePath)
                    extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
                } finally {
                    release()
                }
            }
        }.getOrNull()
        val duration = measured?.takeIf { it > 0 } ?: elapsed
        val target = s.target
        val cassetteId = cassettes.get(target.cassetteId)?.id ?: cassettes.ensureDefault().id
        val id = recordings.insert(
            cassetteId = cassetteId,
            side = target.side,
            title = TimeFormat.defaultEntryTitle(s.createdAt, ZoneId.systemDefault()),
            filePath = file.absolutePath,
            durationMs = duration,
            sizeBytes = file.length(),
            createdAt = s.createdAt,
            waveform = s.levels,
        )
        return RecorderEvent.Saved(id, duration)
    }

    private fun startTicker() {
        ticker?.cancel()
        ticker = scope.launch(Dispatchers.Main) {
            while (isActive) {
                val s = session ?: break
                val status = _state.value.status
                if (status == RecorderStatus.RECORDING) {
                    // getMaxAmplitude() resets on read: this ticker is its only reader.
                    val amp = engine.maxAmplitude() / 32767f
                    levels.publish(amp, amp)
                    val perceptual = WaveformCodec.perceptual(amp)
                    s.levels += perceptual
                    val head = (liveHead + 1) % LIVE_BINS
                    liveLevels[head] = perceptual
                    liveHead = head
                }
                _state.update { it.copy(elapsedMs = s.elapsed(SystemClock.elapsedRealtime()), tick = it.tick + 1) }
                delay(TICK_MS)
            }
        }
    }

    companion object {
        private const val TAG = "RecordingController"
        const val TICK_MS = 50L
        const val LIVE_BINS = 120
        const val MIN_DURATION_MS = 1_000L
    }
}

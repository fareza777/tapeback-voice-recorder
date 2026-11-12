package com.f7developer.tapeback.audio.play

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.SystemClock
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.f7developer.tapeback.audio.fx.FxChainParams
import com.f7developer.tapeback.audio.fx.LevelBus
import com.f7developer.tapeback.audio.fx.TapeEffectChain
import com.f7developer.tapeback.domain.model.PlaybackFx
import com.f7developer.tapeback.domain.model.RecordingItem
import com.f7developer.tapeback.domain.model.RepeatMode
import com.f7developer.tapeback.domain.policy.CapsulePolicy
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
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

/** What the deck needs to know about the transport, updated ~10× per second while playing. */
data class PlayerSnapshot(
    val queue: List<Long> = emptyList(),
    val currentId: Long? = null,
    val currentIndex: Int = 0,
    val isPlaying: Boolean = false,
    val playWhenReady: Boolean = false,
    val buffering: Boolean = false,
    val ended: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val speed: Float = 1f,
    /** Elapsed realtime when [positionMs] was sampled, for smooth extrapolation. */
    val sampledAt: Long = 0L,
) {
    val hasTape: Boolean get() = currentId != null
}

sealed interface PlayerEvent {
    data object TapeDamaged : PlayerEvent
    data object CapsuleLocked : PlayerEvent
    data object SleepTimerFired : PlayerEvent
}

/** An armed sleep timer: either a wall-clock deadline or "stop when this recording ends". */
data class SleepTimer(
    /** [SystemClock.elapsedRealtime] deadline, or null for [endOfTrack]. */
    val endsAtElapsed: Long?,
    val endOfTrack: Boolean,
) {
    fun remainingMs(now: Long = SystemClock.elapsedRealtime()): Long = endsAtElapsed?.let { (it - now).coerceAtLeast(0L) } ?: 0L
}

/**
 * App-scoped bridge from UI to [PlaybackService] through a [MediaController].
 * All controller calls run on the main thread, as Media3 requires.
 */
class PlayerConnection(
    private val context: Context,
    private val scope: CoroutineScope,
    private val effectChain: TapeEffectChain,
    val levels: LevelBus,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private var controller: MediaController? = null
    private val connectLock = Mutex()
    private var ticker: Job? = null
    private var fx = PlaybackFx()
    private var repeat = RepeatMode.OFF
    private var sleepJob: Job? = null

    private val _sleepTimer = MutableStateFlow<SleepTimer?>(null)
    val sleepTimer: StateFlow<SleepTimer?> = _sleepTimer.asStateFlow()

    private val _snapshot = MutableStateFlow(PlayerSnapshot())
    val snapshot: StateFlow<PlayerSnapshot> = _snapshot.asStateFlow()

    private val _events = MutableSharedFlow<PlayerEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<PlayerEvent> = _events.asSharedFlow()

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            publish(player)
            if (player.isPlaying) startTicker() else ticker?.cancel()
        }

        override fun onPlayerError(error: PlaybackException) {
            Log.w(TAG, "Playback error", error)
            _events.tryEmit(PlayerEvent.TapeDamaged)
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val auto = reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO || reason == Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT
            if (auto && _sleepTimer.value?.endOfTrack == true) fireSleepTimer(fade = false)
        }

        // A single recording looping on "repeat" reports no item transition through the controller,
        // but every automatic jump to the next or same item is an AUTO_TRANSITION discontinuity.
        override fun onPositionDiscontinuity(oldPosition: Player.PositionInfo, newPosition: Player.PositionInfo, reason: Int) {
            if (reason == Player.DISCONTINUITY_REASON_AUTO_TRANSITION && _sleepTimer.value?.endOfTrack == true) {
                fireSleepTimer(fade = false)
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED && _sleepTimer.value?.endOfTrack == true) {
                _sleepTimer.value = null
                _events.tryEmit(PlayerEvent.SleepTimerFired)
            }
        }
    }

    private suspend fun connect(): MediaController = withContext(Dispatchers.Main) {
        connectLock.withLock {
            controller?.let { return@withLock it }
            val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
            val c = MediaController.Builder(context, token).buildAsync().await()
            c.addListener(listener)
            controller = c
            applyFxTo(c)
            c.repeatMode = repeat.toPlayer()
            publish(c)
            c
        }
    }

    private fun withController(block: (MediaController) -> Unit) {
        scope.launch(Dispatchers.Main) {
            runCatching { block(connect()) }.onFailure { Log.w(TAG, "Controller call failed", it) }
        }
    }

    /** Load a queue of recordings. Locked capsules and missing files are refused. */
    fun load(items: List<RecordingItem>, startIndex: Int = 0, playWhenReady: Boolean = true, startPositionMs: Long = 0L) {
        val now = clock()
        val requested = items.getOrNull(startIndex)
        if (requested != null && CapsulePolicy.isLocked(requested.recording, now)) {
            _events.tryEmit(PlayerEvent.CapsuleLocked)
            return
        }
        val playable = items.filter { !CapsulePolicy.isLocked(it.recording, now) && File(it.recording.filePath).exists() }
        if (playable.isEmpty()) {
            _events.tryEmit(PlayerEvent.TapeDamaged)
            return
        }
        val index = requested?.let { r -> playable.indexOfFirst { it.recording.id == r.recording.id } }?.takeIf { it >= 0 } ?: 0
        val mediaItems = playable.map { it.toMediaItem() }
        withController { c ->
            c.setMediaItems(mediaItems, index, startPositionMs)
            c.prepare()
            c.playWhenReady = playWhenReady
        }
    }

    fun play() = withController { c ->
        if (c.playbackState == Player.STATE_ENDED) c.seekTo(0L)
        if (c.playbackState == Player.STATE_IDLE) c.prepare()
        c.play()
    }

    fun pause() = withController { it.pause() }

    fun seekTo(positionMs: Long) = withController { it.seekTo(positionMs.coerceAtLeast(0L)) }

    fun seekToItem(index: Int) = withController { c -> if (index in 0 until c.mediaItemCount) c.seekTo(index, 0L) }

    fun next() = withController { if (it.hasNextMediaItem()) it.seekToNextMediaItem() }

    fun previous() = withController { it.seekToPreviousMediaItem() }

    fun eject() = withController {
        it.stop()
        it.clearMediaItems()
    }

    /** Remove a deleted recording from the queue so the deck never points at a missing file. */
    fun onRecordingDeleted(id: Long) = withController { c ->
        for (i in c.mediaItemCount - 1 downTo 0) {
            if (c.getMediaItemAt(i).mediaId == id.toString()) c.removeMediaItem(i)
        }
        if (c.mediaItemCount == 0) c.stop()
    }

    fun applyFx(newFx: PlaybackFx) {
        fx = newFx
        effectChain.params = FxChainParams(newFx.preset, newFx.hiss)
        controller?.let { c -> scope.launch(Dispatchers.Main) { applyFxTo(c) } }
    }

    private fun applyFxTo(c: MediaController) {
        val pitch = if (fx.linkPitch) fx.speed else 1f
        c.playbackParameters = PlaybackParameters(fx.speed, pitch)
    }

    fun applyRepeat(mode: RepeatMode) {
        repeat = mode
        controller?.let { c -> scope.launch(Dispatchers.Main) { c.repeatMode = mode.toPlayer() } }
    }

    /** Pause playback after [minutes], fading the last few seconds out like a deck winding down. */
    fun setSleepTimer(minutes: Int) {
        sleepJob?.cancel()
        val endsAt = SystemClock.elapsedRealtime() + minutes * 60_000L
        _sleepTimer.value = SleepTimer(endsAtElapsed = endsAt, endOfTrack = false)
        sleepJob = scope.launch(Dispatchers.Main) {
            delay((endsAt - SystemClock.elapsedRealtime() - FADE_MS).coerceAtLeast(0L))
            fireSleepTimer(fade = true)
        }
    }

    /** Pause when the current recording finishes. */
    fun setSleepAtEndOfTrack() {
        sleepJob?.cancel()
        sleepJob = null
        _sleepTimer.value = SleepTimer(endsAtElapsed = null, endOfTrack = true)
    }

    fun cancelSleepTimer() {
        sleepJob?.cancel()
        sleepJob = null
        _sleepTimer.value = null
    }

    private fun fireSleepTimer(fade: Boolean) {
        val c = controller
        _sleepTimer.value = null
        if (c == null) return
        scope.launch(Dispatchers.Main) {
            if (fade && c.isPlaying) {
                val steps = 20
                for (i in steps downTo 1) {
                    runCatching { c.volume = i / steps.toFloat() }
                    delay(FADE_MS / steps)
                }
            }
            c.pause()
            runCatching { c.volume = 1f }
            _events.tryEmit(PlayerEvent.SleepTimerFired)
        }
    }

    private fun RepeatMode.toPlayer(): Int = when (this) {
        RepeatMode.OFF -> Player.REPEAT_MODE_OFF
        RepeatMode.ONE -> Player.REPEAT_MODE_ONE
        RepeatMode.SIDE -> Player.REPEAT_MODE_ALL
    }

    /** Extrapolated position for 60 fps animation between snapshots. */
    fun livePositionMs(): Long {
        val s = _snapshot.value
        if (!s.isPlaying) return s.positionMs
        val delta = ((SystemClock.elapsedRealtime() - s.sampledAt) * s.speed).toLong()
        val pos = s.positionMs + delta
        return if (s.durationMs > 0) pos.coerceAtMost(s.durationMs) else pos
    }

    private fun publish(player: Player) {
        val ids = (0 until player.mediaItemCount).mapNotNull { player.getMediaItemAt(it).mediaId.toLongOrNull() }
        val duration = player.duration.takeIf { it > 0 } ?: 0L
        _snapshot.value = PlayerSnapshot(
            queue = ids,
            currentId = player.currentMediaItem?.mediaId?.toLongOrNull(),
            currentIndex = player.currentMediaItemIndex,
            isPlaying = player.isPlaying,
            playWhenReady = player.playWhenReady,
            buffering = player.playbackState == Player.STATE_BUFFERING,
            ended = player.playbackState == Player.STATE_ENDED,
            positionMs = player.currentPosition.coerceAtLeast(0L),
            durationMs = duration,
            speed = player.playbackParameters.speed,
            sampledAt = SystemClock.elapsedRealtime(),
        )
    }

    private fun startTicker() {
        if (ticker?.isActive == true) return
        ticker = scope.launch(Dispatchers.Main) {
            while (isActive) {
                controller?.let(::publish)
                delay(100)
            }
        }
    }

    private fun RecordingItem.toMediaItem(): MediaItem {
        val r = recording
        val metadata = MediaMetadata.Builder()
            .setTitle(r.title)
            .setArtist(cassetteTitle)
            .setAlbumTitle("Side ${r.side.letter}")
            .setDisplayTitle(r.title)
            .build()
        return MediaItem.Builder()
            .setMediaId(r.id.toString())
            .setUri(Uri.fromFile(File(r.filePath)))
            .setMimeType("audio/mp4")
            .setMediaMetadata(metadata)
            .build()
    }

    private companion object {
        const val TAG = "PlayerConnection"
        const val FADE_MS = 5_000L
    }
}

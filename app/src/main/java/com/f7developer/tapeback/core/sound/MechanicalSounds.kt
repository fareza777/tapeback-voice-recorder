package com.f7developer.tapeback.core.sound

import android.media.AudioAttributes
import android.media.SoundPool
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.ConcurrentHashMap

enum class Sfx { KEY, LATCH, STOP, EJECT, INSERT, WIND }

/**
 * Plays the synthesized mechanical sounds through a [SoundPool]. Sounds are rendered once to WAV
 * files in the cache directory (they are regenerated if the cache is cleared).
 */
class MechanicalSounds(
    private val dir: File,
    private val scope: CoroutineScope,
) {
    @Volatile var enabled: Boolean = true

    private val pool: SoundPool = SoundPool.Builder()
        .setMaxStreams(4)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()

    private val ids = ConcurrentHashMap<Sfx, Int>()
    private val loaded = ConcurrentHashMap.newKeySet<Int>()
    @Volatile private var windStream = 0
    @Volatile private var prepared = false

    init {
        pool.setOnLoadCompleteListener { _, sampleId, status -> if (status == 0) loaded += sampleId }
    }

    fun prepare() {
        if (prepared) return
        prepared = true
        scope.launch(Dispatchers.IO) {
            runCatching {
                dir.mkdirs()
                Sfx.entries.forEach { sfx ->
                    val file = File(dir, "v$VERSION-${sfx.name.lowercase()}.wav")
                    if (!file.exists() || file.length() < 44) {
                        val pcm = when (sfx) {
                            Sfx.KEY -> ToneSynth.keyClick()
                            Sfx.LATCH -> ToneSynth.keyLatch()
                            Sfx.STOP -> ToneSynth.stopClunk()
                            Sfx.EJECT -> ToneSynth.eject()
                            Sfx.INSERT -> ToneSynth.insert()
                            Sfx.WIND -> ToneSynth.windLoop()
                        }
                        file.writeBytes(WavWriter.encode(pcm))
                    }
                    ids[sfx] = pool.load(file.absolutePath, 1)
                }
            }.onFailure {
                prepared = false
                Log.w(TAG, "Could not prepare mechanical sounds", it)
            }
        }
    }

    fun play(sfx: Sfx, volume: Float = 0.9f) {
        if (!enabled) return
        val id = ids[sfx] ?: return
        if (id !in loaded) return
        pool.play(id, volume, volume, 1, 0, 1f)
    }

    fun startWind(rate: Float = 1f) {
        if (!enabled) return
        val id = ids[Sfx.WIND] ?: return
        if (id !in loaded) return
        stopWind()
        windStream = pool.play(id, 0.45f, 0.45f, 0, -1, rate.coerceIn(0.5f, 2f))
    }

    fun stopWind() {
        val s = windStream
        if (s != 0) pool.stop(s)
        windStream = 0
    }

    private companion object {
        const val TAG = "MechanicalSounds"
        const val VERSION = 1
    }
}

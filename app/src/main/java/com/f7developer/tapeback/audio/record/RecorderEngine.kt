package com.f7developer.tapeback.audio.record

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import com.f7developer.tapeback.domain.model.RecordingQuality
import java.io.File

/** Thin, main-thread-only wrapper around [MediaRecorder] producing mono AAC in MPEG-4. */
class RecorderEngine(private val context: Context) {

    private var recorder: MediaRecorder? = null

    val isActive: Boolean get() = recorder != null

    /** @throws Exception if the recorder cannot be prepared or started (e.g. mic busy). */
    fun start(file: File, quality: RecordingQuality, onError: (String) -> Unit) {
        release()
        val r = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context) else legacyRecorder()
        try {
            r.setAudioSource(MediaRecorder.AudioSource.MIC)
            r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            r.setAudioChannels(1)
            r.setAudioSamplingRate(quality.sampleRate)
            r.setAudioEncodingBitRate(quality.bitRate)
            r.setOutputFile(file.absolutePath)
            r.setOnErrorListener { _, what, extra -> onError("Recorder error $what/$extra") }
            r.prepare()
            r.start()
            recorder = r
        } catch (e: Exception) {
            runCatching { r.reset() }
            runCatching { r.release() }
            throw e
        }
    }

    @Suppress("DEPRECATION")
    private fun legacyRecorder() = MediaRecorder()

    fun pause() {
        runCatching { recorder?.pause() }.onFailure { Log.w(TAG, "pause failed", it) }
    }

    fun resume() {
        runCatching { recorder?.resume() }.onFailure { Log.w(TAG, "resume failed", it) }
    }

    /** Peak amplitude since the previous call, 0..32767. */
    fun maxAmplitude(): Int = runCatching { recorder?.maxAmplitude ?: 0 }.getOrDefault(0)

    /** Stops and releases. Returns true if the file was finalized correctly. */
    fun stop(): Boolean {
        val r = recorder ?: return false
        recorder = null
        val ok = runCatching { r.stop() }.onFailure { Log.w(TAG, "stop failed (likely too short)", it) }.isSuccess
        runCatching { r.reset() }
        runCatching { r.release() }
        return ok
    }

    fun release() {
        val r = recorder ?: return
        recorder = null
        runCatching { r.reset() }
        runCatching { r.release() }
    }

    private companion object {
        const val TAG = "RecorderEngine"
    }
}

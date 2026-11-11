package com.f7developer.tapeback.audio.fx

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.common.audio.AudioProcessor.UnhandledAudioFormatException
import androidx.media3.common.audio.BaseAudioProcessor
import java.nio.ByteBuffer

/**
 * Media3 adapter that runs [TapeEffectChain] on 16-bit PCM inside ExoPlayer's audio sink.
 * It stays active for every PCM stream so presets can be switched live without re-preparing.
 */
@OptIn(UnstableApi::class)
class TapeAudioProcessor(private val chain: TapeEffectChain) : BaseAudioProcessor() {

    private var scratch = FloatArray(4096)
    private var channelCount = 2

    override fun onConfigure(inputAudioFormat: AudioFormat): AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) {
            throw UnhandledAudioFormatException(inputAudioFormat)
        }
        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val bytes = inputBuffer.remaining()
        if (bytes == 0) return
        val samples = bytes / 2
        if (scratch.size < samples) scratch = FloatArray(samples)
        val buf = scratch
        for (i in 0 until samples) buf[i] = inputBuffer.short / 32768f
        chain.process(buf, samples / channelCount)
        val out = replaceOutputBuffer(bytes)
        for (i in 0 until samples) out.putShort((buf[i] * 32767f).toInt().toShort())
        out.flip()
    }

    override fun onFlush() {
        channelCount = inputAudioFormat.channelCount.coerceAtLeast(1)
        chain.configure(inputAudioFormat.sampleRate, channelCount)
    }

    override fun onReset() {
        chain.reset()
    }
}

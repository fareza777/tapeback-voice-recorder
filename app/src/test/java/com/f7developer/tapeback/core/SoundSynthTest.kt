package com.f7developer.tapeback.core

import com.f7developer.tapeback.core.sound.ToneSynth
import com.f7developer.tapeback.core.sound.WavWriter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs

class SoundSynthTest {

    @Test
    fun `all mechanical sounds are non-silent and within full scale`() {
        listOf(ToneSynth.keyClick(), ToneSynth.keyLatch(), ToneSynth.stopClunk(), ToneSynth.eject(), ToneSynth.insert(), ToneSynth.windLoop())
            .forEach { pcm ->
                assertTrue(pcm.isNotEmpty())
                val peak = pcm.maxOf { abs(it) }
                assertTrue("peak $peak", peak in 0.2f..1f)
            }
    }

    @Test
    fun `click sounds are short`() {
        assertTrue(ToneSynth.keyClick().size < ToneSynth.SAMPLE_RATE / 5)
    }

    @Test
    fun `wind loop is exactly one second and seamless at the seam`() {
        val loop = ToneSynth.windLoop()
        assertEquals(ToneSynth.SAMPLE_RATE, loop.size)
        // The harmonic part is periodic; only filtered noise differs across the seam.
        assertTrue(abs(loop.last() - loop.first()) < 0.2f)
    }

    @Test
    fun `wav writer produces a valid 16-bit mono header`() {
        val pcm = floatArrayOf(0f, 0.5f, -0.5f, 1f)
        val wav = WavWriter.encode(pcm, 44_100)
        assertEquals(44 + 8, wav.size)
        val b = ByteBuffer.wrap(wav).order(ByteOrder.LITTLE_ENDIAN)
        assertEquals("RIFF", String(wav, 0, 4, Charsets.US_ASCII))
        assertEquals(36 + 8, b.getInt(4))
        assertEquals("WAVE", String(wav, 8, 4, Charsets.US_ASCII))
        assertEquals(1, b.getShort(20).toInt()) // PCM
        assertEquals(1, b.getShort(22).toInt()) // mono
        assertEquals(44_100, b.getInt(24))
        assertEquals(16, b.getShort(34).toInt())
        assertEquals(8, b.getInt(40))
        assertEquals(32767, b.getShort(50).toInt())
    }
}

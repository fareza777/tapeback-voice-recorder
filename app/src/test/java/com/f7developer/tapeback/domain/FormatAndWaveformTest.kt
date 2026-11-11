package com.f7developer.tapeback.domain

import com.f7developer.tapeback.domain.policy.TimeFormat
import com.f7developer.tapeback.domain.policy.WaveformCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class FormatAndWaveformTest {

    @Test
    fun `durations format like a tape deck`() {
        assertEquals("0:07", TimeFormat.duration(7_400))
        assertEquals("3:45", TimeFormat.duration(225_000))
        assertEquals("1:02:03", TimeFormat.duration(3_723_000))
        assertEquals("00:07", TimeFormat.lcd(7_000))
        assertEquals("1:00:00", TimeFormat.lcd(3_600_000))
        assertEquals("0:00", TimeFormat.duration(-5))
    }

    @Test
    fun `human totals are compact`() {
        assertEquals("45s", TimeFormat.humanTotal(45_000))
        assertEquals("14m", TimeFormat.humanTotal(14 * 60_000L))
        assertEquals("2h 14m", TimeFormat.humanTotal((2 * 60 + 14) * 60_000L))
    }

    @Test
    fun `file sizes use binary units`() {
        assertEquals("500 KB", TimeFormat.fileSize(512_000))
        assertEquals("1.5 MB", TimeFormat.fileSize(1_572_864))
        assertEquals("2.00 GB", TimeFormat.fileSize(2L * 1024 * 1024 * 1024))
    }

    @Test
    fun `default entry title uses English month names`() {
        val zone = ZoneId.of("UTC")
        val t = ZonedDateTime.of(2026, 11, 11, 21, 4, 0, 0, zone).toInstant().toEpochMilli()
        assertEquals("Entry · Nov 11, 9:04 PM", TimeFormat.defaultEntryTitle(t, zone))
        assertEquals("Wednesday, Nov 11, 2026", TimeFormat.dayHeader(t, zone))
    }

    @Test
    fun `waveform encodes into at most 200 bytes and round-trips`() {
        val levels = List(5_000) { i -> (i % 100) / 100f }
        val bytes = WaveformCodec.encode(levels)
        assertEquals(WaveformCodec.MAX_BINS, bytes.size)
        val decoded = WaveformCodec.decode(bytes)
        decoded.forEach { assertTrue(it in 0f..1f) }
        // Peak-picking keeps loud moments visible.
        assertTrue(decoded.max() >= 0.98f)
    }

    @Test
    fun `short waveforms are kept as-is`() {
        val levels = listOf(0f, 0.5f, 1f)
        val decoded = WaveformCodec.decode(WaveformCodec.encode(levels))
        assertEquals(3, decoded.size)
        assertEquals(0.5f, decoded[1], 1f / 255f)
        assertEquals(emptyList<Float>(), WaveformCodec.decode(null))
    }

    @Test
    fun `perceptual level maps dBFS to 0 to 1`() {
        assertEquals(1f, WaveformCodec.perceptual(1f), 1e-5f)
        assertEquals(0f, WaveformCodec.perceptual(0f), 0f)
        assertEquals(0.5f, WaveformCodec.perceptual(Math.pow(10.0, -24.0 / 20).toFloat()), 1e-3f)
    }
}

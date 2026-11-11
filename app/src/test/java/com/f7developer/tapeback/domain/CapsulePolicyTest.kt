package com.f7developer.tapeback.domain

import com.f7developer.tapeback.domain.policy.CapsulePolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class CapsulePolicyTest {
    private val zone = ZoneId.of("Asia/Jakarta")
    private val now = ZonedDateTime.of(2026, 11, 11, 21, 0, 0, 0, zone).toInstant().toEpochMilli()

    @Test
    fun `locked capsule cannot be played before unlock`() {
        val r = TestData.recording(capsuleUnlockAt = now + 1_000)
        assertTrue(CapsulePolicy.isLocked(r, now))
        assertFalse(CapsulePolicy.canAccess(r, now))
    }

    @Test
    fun `capsule unlocks exactly at its unlock time`() {
        val r = TestData.recording(capsuleUnlockAt = now)
        assertFalse(CapsulePolicy.isLocked(r, now))
        assertTrue(CapsulePolicy.canAccess(r, now))
    }

    @Test
    fun `ordinary recordings are never locked`() {
        assertFalse(CapsulePolicy.isLocked(TestData.recording(), now))
    }

    @Test
    fun `unlock happens at nine in the morning local time`() {
        val unlock = CapsulePolicy.unlockInstant(LocalDate.of(2027, 11, 11), zone)
        val z = java.time.Instant.ofEpochMilli(unlock).atZone(zone)
        assertEquals(9, z.hour)
        assertEquals(0, z.minute)
        assertEquals(LocalDate.of(2027, 11, 11), z.toLocalDate())
    }

    @Test
    fun `quick picks add calendar months and the earliest date is tomorrow`() {
        assertEquals(LocalDate.of(2027, 11, 11), CapsulePolicy.quickPickDate(CapsulePolicy.QuickPick.ONE_YEAR, now, zone))
        assertEquals(LocalDate.of(2031, 11, 11), CapsulePolicy.quickPickDate(CapsulePolicy.QuickPick.FIVE_YEARS, now, zone))
        assertEquals(LocalDate.of(2026, 12, 11), CapsulePolicy.quickPickDate(CapsulePolicy.QuickPick.ONE_MONTH, now, zone))
        assertEquals(LocalDate.of(2026, 11, 12), CapsulePolicy.earliestDate(now, zone))
    }

    @Test
    fun `countdown splits remaining time into days hours minutes seconds`() {
        val unlock = now + ((2L * 86_400 + 3 * 3_600 + 4 * 60 + 5) * 1000)
        val c = CapsulePolicy.countdown(unlock, now)
        assertEquals(CapsulePolicy.Countdown(2, 3, 4, 5), c)
        assertTrue(CapsulePolicy.countdown(now - 10, now).isZero)
    }

    @Test
    fun `seal progress runs from zero to one`() {
        assertEquals(0f, CapsulePolicy.sealProgress(now, now + 1000, now), 0f)
        assertEquals(0.5f, CapsulePolicy.sealProgress(now, now + 1000, now + 500), 1e-4f)
        assertEquals(1f, CapsulePolicy.sealProgress(now, now + 1000, now + 5000), 0f)
    }

    @Test
    fun `unlock time must be in the future`() {
        assertTrue(CapsulePolicy.isValidUnlock(now + 1, now))
        assertFalse(CapsulePolicy.isValidUnlock(now, now))
    }
}

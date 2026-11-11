package com.f7developer.tapeback.domain.policy

import com.f7developer.tapeback.domain.model.Recording
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/** Rules for time capsules: when they unlock and what may be done with them. */
object CapsulePolicy {
    /** Capsules unlock at 09:00 local time on the chosen date – a pleasant morning surprise. */
    val UNLOCK_TIME: LocalTime = LocalTime.of(9, 0)

    data class Countdown(val days: Long, val hours: Int, val minutes: Int, val seconds: Int) {
        val isZero: Boolean get() = days == 0L && hours == 0 && minutes == 0 && seconds == 0
    }

    enum class QuickPick(val months: Long) {
        ONE_MONTH(1),
        SIX_MONTHS(6),
        ONE_YEAR(12),
        FIVE_YEARS(60),
    }

    fun isLocked(unlockAt: Long?, now: Long): Boolean = unlockAt != null && now < unlockAt

    fun isLocked(recording: Recording, now: Long): Boolean = isLocked(recording.capsuleUnlockAt, now)

    /** A recording may be played, shared or exported only when it is not a locked capsule. */
    fun canAccess(recording: Recording, now: Long): Boolean = !isLocked(recording, now)

    fun unlockInstant(date: LocalDate, zone: ZoneId): Long =
        date.atTime(UNLOCK_TIME).atZone(zone).toInstant().toEpochMilli()

    fun quickPickDate(pick: QuickPick, now: Long, zone: ZoneId): LocalDate =
        Instant.ofEpochMilli(now).atZone(zone).toLocalDate().plusMonths(pick.months)

    /** The earliest date a capsule may be sealed until (tomorrow). */
    fun earliestDate(now: Long, zone: ZoneId): LocalDate =
        Instant.ofEpochMilli(now).atZone(zone).toLocalDate().plusDays(1)

    fun isValidUnlock(unlockAt: Long, now: Long): Boolean = unlockAt > now

    fun countdown(unlockAt: Long, now: Long): Countdown {
        val remaining = (unlockAt - now).coerceAtLeast(0L) / 1000L
        val days = remaining / 86_400L
        val hours = ((remaining % 86_400L) / 3_600L).toInt()
        val minutes = ((remaining % 3_600L) / 60L).toInt()
        val seconds = (remaining % 60L).toInt()
        return Countdown(days, hours, minutes, seconds)
    }

    /** Fraction of the sealed period that has elapsed, for the progress ring. */
    fun sealProgress(sealedAt: Long, unlockAt: Long, now: Long): Float {
        val total = (unlockAt - sealedAt).coerceAtLeast(1L)
        return ((now - sealedAt).toFloat() / total).coerceIn(0f, 1f)
    }
}

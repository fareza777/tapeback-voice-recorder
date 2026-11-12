package com.f7developer.tapeback.domain.policy

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/** When the daily diary reminder fires, and which day counts as "today". */
object ReminderPolicy {

    /** The next moment strictly after [nowMs] that matches [minuteOfDay] in [zone]. */
    fun nextTrigger(nowMs: Long, minuteOfDay: Int, zone: ZoneId): Long {
        val now = Instant.ofEpochMilli(nowMs).atZone(zone)
        val time = LocalTime.of((minuteOfDay / 60).coerceIn(0, 23), (minuteOfDay % 60).coerceIn(0, 59))
        var candidate: ZonedDateTime = now.toLocalDate().atTime(time).atZone(zone)
        if (!candidate.isAfter(now)) candidate = now.toLocalDate().plusDays(1).atTime(time).atZone(zone)
        return candidate.toInstant().toEpochMilli()
    }

    /** Start (inclusive) and end (exclusive) of the local day that contains [nowMs]. */
    fun dayBounds(nowMs: Long, zone: ZoneId): Pair<Long, Long> {
        val day: LocalDate = Instant.ofEpochMilli(nowMs).atZone(zone).toLocalDate()
        val start = day.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        return start to end
    }
}

package com.f7developer.tapeback.domain.policy

import com.f7developer.tapeback.domain.model.RecordingItem
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

/** Pure filtering, grouping and statistics for the audio diary. */
object DiaryLogic {

    data class Filter(
        val query: String = "",
        val favoritesOnly: Boolean = false,
        val tag: String? = null,
    )

    data class DayGroup(val date: LocalDate, val items: List<RecordingItem>)

    data class Stats(val entries: Int, val totalDurationMs: Long, val streakDays: Int)

    /** Visible diary entries: locked capsules are hidden, filters applied, newest first. */
    fun apply(items: List<RecordingItem>, filter: Filter, now: Long): List<RecordingItem> {
        val q = filter.query.trim().lowercase(Locale.ENGLISH)
        return items.asSequence()
            .filter { !CapsulePolicy.isLocked(it.recording, now) }
            .filter { !filter.favoritesOnly || it.recording.favorite }
            .filter { tag -> filter.tag == null || tag.recording.tags.any { it.equals(filter.tag, ignoreCase = true) } }
            .filter { q.isEmpty() || matches(it, q) }
            .sortedByDescending { it.recording.createdAt }
            .toList()
    }

    private fun matches(item: RecordingItem, q: String): Boolean =
        item.recording.title.lowercase(Locale.ENGLISH).contains(q) ||
            item.cassetteTitle.lowercase(Locale.ENGLISH).contains(q) ||
            item.recording.tags.any { it.lowercase(Locale.ENGLISH).contains(q) } ||
            (item.recording.capsuleNote?.lowercase(Locale.ENGLISH)?.contains(q) ?: false)

    fun groupByDay(items: List<RecordingItem>, zone: ZoneId): List<DayGroup> =
        items.groupBy { localDate(it.recording.createdAt, zone) }
            .map { (date, list) -> DayGroup(date, list.sortedByDescending { it.recording.createdAt }) }
            .sortedByDescending { it.date }

    fun stats(items: List<RecordingItem>, now: Long, zone: ZoneId): Stats {
        val visible = items.filter { !CapsulePolicy.isLocked(it.recording, now) }
        return Stats(
            entries = visible.size,
            totalDurationMs = visible.sumOf { it.recording.durationMs },
            streakDays = streak(visible.map { it.recording.createdAt }, now, zone),
        )
    }

    /**
     * Consecutive days with at least one entry, ending today. If there is no entry today yet,
     * a streak that ended yesterday still counts (you have until midnight to keep it alive).
     */
    fun streak(timestamps: List<Long>, now: Long, zone: ZoneId): Int {
        if (timestamps.isEmpty()) return 0
        val days = timestamps.map { localDate(it, zone) }.toHashSet()
        val today = localDate(now, zone)
        var cursor = if (today in days) today else today.minusDays(1)
        var count = 0
        while (cursor in days) {
            count++
            cursor = cursor.minusDays(1)
        }
        return count
    }

    /** Entries recorded on the same month/day in previous years. */
    fun onThisDay(items: List<RecordingItem>, now: Long, zone: ZoneId): List<RecordingItem> {
        val today = localDate(now, zone)
        return items.filter {
            val d = localDate(it.recording.createdAt, zone)
            d.year < today.year && d.month == today.month && d.dayOfMonth == today.dayOfMonth &&
                !CapsulePolicy.isLocked(it.recording, now)
        }.sortedByDescending { it.recording.createdAt }
    }

    fun allTags(items: List<RecordingItem>): List<String> =
        items.flatMap { it.recording.tags }
            .groupingBy { it.lowercase(Locale.ENGLISH) }
            .eachCount()
            .entries
            .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
            .map { it.key }

    /** Normalize user tag input: trimmed, lowercase, no leading '#', no blanks, unique, ≤ 24 chars. */
    fun normalizeTags(raw: List<String>): List<String> =
        raw.map { it.trim().removePrefix("#").trim().lowercase(Locale.ENGLISH).take(24) }
            .filter { it.isNotEmpty() }
            .distinct()

    fun localDate(epochMs: Long, zone: ZoneId): LocalDate = Instant.ofEpochMilli(epochMs).atZone(zone).toLocalDate()
}

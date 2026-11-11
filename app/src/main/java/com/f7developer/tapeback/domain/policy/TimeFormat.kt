package com.f7developer.tapeback.domain.policy

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** English-only formatting helpers (TapeBack ships as a single global English build). */
object TimeFormat {
    private val dayHeader = DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy", Locale.ENGLISH)
    private val shortDate = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH)
    private val clock = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)
    private val entry = DateTimeFormatter.ofPattern("MMM d, h:mm a", Locale.ENGLISH)

    /** 0:07, 3:45, 1:02:03 */
    fun duration(ms: Long): String {
        val totalSeconds = (ms.coerceAtLeast(0L) / 1000L)
        val h = totalSeconds / 3600
        val m = (totalSeconds % 3600) / 60
        val s = totalSeconds % 60
        return if (h > 0) String.format(Locale.ENGLISH, "%d:%02d:%02d", h, m, s)
        else String.format(Locale.ENGLISH, "%d:%02d", m, s)
    }

    /** Fixed-width LCD readout: 00:07 or 1:02:03. */
    fun lcd(ms: Long): String {
        val totalSeconds = (ms.coerceAtLeast(0L) / 1000L)
        val h = totalSeconds / 3600
        val m = (totalSeconds % 3600) / 60
        val s = totalSeconds % 60
        return if (h > 0) String.format(Locale.ENGLISH, "%d:%02d:%02d", h, m, s)
        else String.format(Locale.ENGLISH, "%02d:%02d", m, s)
    }

    /** "2h 14m", "14m", "45s" */
    fun humanTotal(ms: Long): String {
        val totalSeconds = ms.coerceAtLeast(0L) / 1000L
        val h = totalSeconds / 3600
        val m = (totalSeconds % 3600) / 60
        val s = totalSeconds % 60
        return when {
            h > 0 -> "${h}h ${m}m"
            m > 0 -> "${m}m"
            else -> "${s}s"
        }
    }

    fun dayHeader(epochMs: Long, zone: ZoneId): String = dayHeader.format(Instant.ofEpochMilli(epochMs).atZone(zone))

    fun shortDate(epochMs: Long, zone: ZoneId): String = shortDate.format(Instant.ofEpochMilli(epochMs).atZone(zone))

    fun clock(epochMs: Long, zone: ZoneId): String = clock.format(Instant.ofEpochMilli(epochMs).atZone(zone))

    /** Default title for a fresh recording: "Entry · Nov 11, 9:04 PM". */
    fun defaultEntryTitle(epochMs: Long, zone: ZoneId): String =
        "Entry · " + entry.format(Instant.ofEpochMilli(epochMs).atZone(zone))

    fun fileSize(bytes: Long): String {
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        return when {
            gb >= 1 -> String.format(Locale.ENGLISH, "%.2f GB", gb)
            mb >= 1 -> String.format(Locale.ENGLISH, "%.1f MB", mb)
            else -> String.format(Locale.ENGLISH, "%.0f KB", kb)
        }
    }
}

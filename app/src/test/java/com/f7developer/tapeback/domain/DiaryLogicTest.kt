package com.f7developer.tapeback.domain

import com.f7developer.tapeback.domain.policy.DiaryLogic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class DiaryLogicTest {
    private val zone = ZoneId.of("Europe/Berlin")
    private fun at(y: Int, m: Int, d: Int, h: Int = 12) = ZonedDateTime.of(y, m, d, h, 0, 0, 0, zone).toInstant().toEpochMilli()
    private val now = at(2026, 11, 11, 20)

    @Test
    fun `locked capsules are hidden from the diary`() {
        val items = listOf(
            TestData.item(TestData.recording(title = "visible", createdAt = at(2026, 11, 10))),
            TestData.item(TestData.recording(title = "sealed", createdAt = at(2026, 11, 10), capsuleUnlockAt = now + 1)),
            TestData.item(TestData.recording(title = "opened", createdAt = at(2026, 1, 1), capsuleUnlockAt = now - 1)),
        )
        val titles = DiaryLogic.apply(items, DiaryLogic.Filter(), now).map { it.recording.title }
        assertEquals(listOf("visible", "opened"), titles)
    }

    @Test
    fun `search matches titles tags cassette names and notes case-insensitively`() {
        val items = listOf(
            TestData.item(TestData.recording(title = "Morning walk", createdAt = 3)),
            TestData.item(TestData.recording(title = "x", tags = listOf("Dreams"), createdAt = 2)),
            TestData.item(TestData.recording(title = "y", createdAt = 1), cassetteTitle = "Road Trip"),
        )
        assertEquals(1, DiaryLogic.apply(items, DiaryLogic.Filter(query = "WALK"), now).size)
        assertEquals(1, DiaryLogic.apply(items, DiaryLogic.Filter(query = "dream"), now).size)
        assertEquals(1, DiaryLogic.apply(items, DiaryLogic.Filter(query = "road"), now).size)
        assertEquals(3, DiaryLogic.apply(items, DiaryLogic.Filter(query = "  "), now).size)
    }

    @Test
    fun `favorites and tag filters combine`() {
        val items = listOf(
            TestData.item(TestData.recording(favorite = true, tags = listOf("ideas"))),
            TestData.item(TestData.recording(favorite = true)),
            TestData.item(TestData.recording(tags = listOf("ideas"))),
        )
        assertEquals(2, DiaryLogic.apply(items, DiaryLogic.Filter(favoritesOnly = true), now).size)
        assertEquals(1, DiaryLogic.apply(items, DiaryLogic.Filter(favoritesOnly = true, tag = "IDEAS"), now).size)
    }

    @Test
    fun `entries are grouped by local day newest first`() {
        val items = listOf(
            TestData.item(TestData.recording(createdAt = at(2026, 11, 10, 9))),
            TestData.item(TestData.recording(createdAt = at(2026, 11, 11, 8))),
            TestData.item(TestData.recording(createdAt = at(2026, 11, 10, 22))),
        )
        val groups = DiaryLogic.groupByDay(items, zone)
        assertEquals(listOf(LocalDate.of(2026, 11, 11), LocalDate.of(2026, 11, 10)), groups.map { it.date })
        assertEquals(2, groups[1].items.size)
        assertTrue(groups[1].items[0].recording.createdAt > groups[1].items[1].recording.createdAt)
    }

    @Test
    fun `streak counts consecutive days ending today`() {
        val ts = listOf(at(2026, 11, 11), at(2026, 11, 10), at(2026, 11, 9), at(2026, 11, 7))
        assertEquals(3, DiaryLogic.streak(ts, now, zone))
    }

    @Test
    fun `streak survives until midnight when today has no entry yet`() {
        val ts = listOf(at(2026, 11, 10), at(2026, 11, 9))
        assertEquals(2, DiaryLogic.streak(ts, now, zone))
        assertEquals(0, DiaryLogic.streak(listOf(at(2026, 11, 8)), now, zone))
        assertEquals(0, DiaryLogic.streak(emptyList(), now, zone))
    }

    @Test
    fun `on this day finds the same date in previous years`() {
        val items = listOf(
            TestData.item(TestData.recording(title = "a", createdAt = at(2024, 11, 11))),
            TestData.item(TestData.recording(title = "b", createdAt = at(2025, 11, 12))),
            TestData.item(TestData.recording(title = "c", createdAt = at(2026, 11, 11))),
        )
        assertEquals(listOf("a"), DiaryLogic.onThisDay(items, now, zone).map { it.recording.title })
    }

    @Test
    fun `stats exclude locked capsules`() {
        val items = listOf(
            TestData.item(TestData.recording(createdAt = at(2026, 11, 11), durationMs = 1_000)),
            TestData.item(TestData.recording(createdAt = at(2026, 11, 11), durationMs = 9_000, capsuleUnlockAt = now + 5)),
        )
        val stats = DiaryLogic.stats(items, now, zone)
        assertEquals(1, stats.entries)
        assertEquals(1_000L, stats.totalDurationMs)
        assertEquals(1, stats.streakDays)
    }

    @Test
    fun `tags are normalized`() {
        assertEquals(listOf("ideas", "family"), DiaryLogic.normalizeTags(listOf(" #Ideas ", "ideas", "", "FAMILY")))
        assertEquals(24, DiaryLogic.normalizeTags(listOf("x".repeat(40))).first().length)
    }

    @Test
    fun `all tags are ordered by frequency then name`() {
        val items = listOf(
            TestData.item(TestData.recording(tags = listOf("b", "a"))),
            TestData.item(TestData.recording(tags = listOf("b"))),
        )
        assertEquals(listOf("b", "a"), DiaryLogic.allTags(items))
    }
}

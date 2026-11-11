package com.f7developer.tapeback

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.f7developer.tapeback.data.db.CassetteEntity
import com.f7developer.tapeback.data.db.RecordingEntity
import com.f7developer.tapeback.data.db.TapeDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TapeDatabaseTest {
    private lateinit var db: TapeDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, TapeDatabase::class.java).build()
        db.openHelper.writableDatabase.execSQL("PRAGMA foreign_keys = ON")
    }

    @After
    fun tearDown() = db.close()

    private suspend fun cassette(title: String = "Tape") = db.cassetteDao().insert(
        CassetteEntity(title = title, category = "Diary", labelColor = 0, skin = "CLASSIC_BLACK", coverPath = null, createdAt = 1, updatedAt = 1),
    )

    private suspend fun recording(cassetteId: Long, side: String, duration: Long, createdAt: Long = 10) = db.recordingDao().insert(
        RecordingEntity(
            cassetteId = cassetteId, side = side, title = "Rec $createdAt", filePath = "/x/$createdAt.m4a",
            durationMs = duration, sizeBytes = 100, createdAt = createdAt, waveform = byteArrayOf(1, 2, 3),
        ),
    )

    @Test
    fun statsProjectionCountsSidesAndDuration() = runTest {
        val id = cassette()
        recording(id, "A", 1_000, 1)
        recording(id, "A", 2_000, 2)
        recording(id, "B", 3_000, 3)
        val stats = db.cassetteDao().observeWithStats().first().single()
        assertEquals(2, stats.sideACount)
        assertEquals(1, stats.sideBCount)
        assertEquals(6_000L, stats.totalDurationMs)
    }

    @Test
    fun deletingACassetteCascadesToRecordingsAndTags() = runTest {
        val id = cassette()
        val rec = recording(id, "A", 1_000)
        db.recordingDao().replaceTags(rec, listOf("ideas", "family"))
        db.cassetteDao().delete(id)
        assertNull(db.recordingDao().get(rec))
        assertTrue(db.recordingDao().observeAll().first().isEmpty())
    }

    @Test
    fun tagsAreReplacedAndReturnedWithRecording() = runTest {
        val id = cassette()
        val rec = recording(id, "A", 1_000)
        db.recordingDao().replaceTags(rec, listOf("a", "b"))
        db.recordingDao().replaceTags(rec, listOf("b", "c"))
        val row = db.recordingDao().get(rec)
        assertEquals(setOf("b", "c"), row?.tags?.toSet())
        assertEquals("Tape", row?.cassette?.title)
        assertTrue(row?.recording?.waveform?.contentEquals(byteArrayOf(1, 2, 3)) == true)
    }

    @Test
    fun sealAndBreakSealUpdateCapsuleColumns() = runTest {
        val id = cassette()
        val rec = recording(id, "B", 1_000)
        db.recordingDao().seal(rec, unlockAt = 5_000, sealedAt = 100, note = "hi")
        assertEquals(1, db.recordingDao().pendingCapsules().size)
        db.recordingDao().markCapsuleNotified(rec)
        assertTrue(db.recordingDao().pendingCapsules().isEmpty())
        db.recordingDao().breakSeal(rec)
        assertNull(db.recordingDao().get(rec)?.recording?.capsuleUnlockAt)
    }

    @Test
    fun bySideReturnsChronologicalOrder() = runTest {
        val id = cassette()
        recording(id, "A", 1_000, 30)
        recording(id, "A", 1_000, 10)
        recording(id, "B", 1_000, 20)
        val side = db.recordingDao().bySide(id, "A").map { it.recording.createdAt }
        assertEquals(listOf(10L, 30L), side)
    }
}

package com.f7developer.tapeback.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CassetteDao {
    @Query(
        """
        SELECT c.*,
            (SELECT COUNT(*) FROM recordings r WHERE r.cassetteId = c.id AND r.side = 'A') AS sideACount,
            (SELECT COUNT(*) FROM recordings r WHERE r.cassetteId = c.id AND r.side = 'B') AS sideBCount,
            (SELECT COALESCE(SUM(r.durationMs), 0) FROM recordings r WHERE r.cassetteId = c.id) AS totalDurationMs
        FROM cassettes c
        ORDER BY c.updatedAt DESC
        """,
    )
    fun observeWithStats(): Flow<List<CassetteWithStats>>

    @Query("SELECT * FROM cassettes WHERE id = :id")
    fun observe(id: Long): Flow<CassetteEntity?>

    @Query("SELECT * FROM cassettes WHERE id = :id")
    suspend fun get(id: Long): CassetteEntity?

    @Query("SELECT * FROM cassettes ORDER BY updatedAt DESC LIMIT 1")
    suspend fun mostRecent(): CassetteEntity?

    @Query("SELECT COUNT(*) FROM cassettes")
    suspend fun count(): Int

    @Insert
    suspend fun insert(entity: CassetteEntity): Long

    @Update
    suspend fun update(entity: CassetteEntity)

    @Query("UPDATE cassettes SET updatedAt = :now WHERE id = :id")
    suspend fun touch(id: Long, now: Long)

    @Query("DELETE FROM cassettes WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM cassettes ORDER BY createdAt ASC")
    suspend fun all(): List<CassetteEntity>

    @Query("SELECT * FROM cassettes WHERE title = :title AND createdAt = :createdAt LIMIT 1")
    suspend fun findByTitleAndCreated(title: String, createdAt: Long): CassetteEntity?

    @Query(
        "SELECT * FROM cassettes c WHERE c.title = :title " +
            "AND NOT EXISTS (SELECT 1 FROM recordings r WHERE r.cassetteId = c.id) LIMIT 1",
    )
    suspend fun findEmptyByTitle(title: String): CassetteEntity?
}

@Dao
interface RecordingDao {
    @Transaction
    @Query("SELECT * FROM recordings ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<RecordingRow>>

    @Transaction
    @Query("SELECT * FROM recordings WHERE cassetteId = :cassetteId ORDER BY createdAt ASC")
    fun observeByCassette(cassetteId: Long): Flow<List<RecordingRow>>

    @Transaction
    @Query("SELECT * FROM recordings WHERE id = :id")
    fun observe(id: Long): Flow<RecordingRow?>

    @Transaction
    @Query("SELECT * FROM recordings WHERE id = :id")
    suspend fun get(id: Long): RecordingRow?

    @Transaction
    @Query("SELECT * FROM recordings WHERE cassetteId = :cassetteId AND side = :side ORDER BY createdAt ASC")
    suspend fun bySide(cassetteId: Long, side: String): List<RecordingRow>

    @Query("SELECT filePath FROM recordings WHERE cassetteId = :cassetteId")
    suspend fun filePathsForCassette(cassetteId: Long): List<String>

    @Query("SELECT * FROM recordings WHERE capsuleUnlockAt IS NOT NULL AND capsuleNotified = 0")
    suspend fun pendingCapsules(): List<RecordingEntity>

    @Query("SELECT COUNT(*) FROM recordings")
    fun observeCount(): Flow<Int>

    @Query("SELECT COALESCE(SUM(sizeBytes), 0) FROM recordings")
    fun observeTotalBytes(): Flow<Long>

    @Query("SELECT COUNT(*) FROM recordings WHERE createdAt >= :start AND createdAt < :end")
    suspend fun countCreatedBetween(start: Long, end: Long): Int

    @Transaction
    @Query("SELECT * FROM recordings ORDER BY createdAt ASC")
    suspend fun allWithTags(): List<RecordingWithTags>

    @Query("SELECT COUNT(*) FROM recordings WHERE createdAt = :createdAt AND durationMs = :durationMs")
    suspend fun countMatching(createdAt: Long, durationMs: Long): Int

    @Insert
    suspend fun insert(entity: RecordingEntity): Long

    @Query("UPDATE recordings SET title = :title WHERE id = :id")
    suspend fun rename(id: Long, title: String)

    @Query("UPDATE recordings SET favorite = :favorite WHERE id = :id")
    suspend fun setFavorite(id: Long, favorite: Boolean)

    @Query("UPDATE recordings SET cassetteId = :cassetteId, side = :side WHERE id = :id")
    suspend fun move(id: Long, cassetteId: Long, side: String)

    @Query(
        "UPDATE recordings SET capsuleUnlockAt = :unlockAt, capsuleSealedAt = :sealedAt, " +
            "capsuleNote = :note, capsuleNotified = 0 WHERE id = :id",
    )
    suspend fun seal(id: Long, unlockAt: Long, sealedAt: Long, note: String?)

    @Query("UPDATE recordings SET capsuleUnlockAt = NULL, capsuleSealedAt = NULL, capsuleNotified = 0 WHERE id = :id")
    suspend fun breakSeal(id: Long)

    @Query("UPDATE recordings SET capsuleNotified = 1 WHERE id = :id")
    suspend fun markCapsuleNotified(id: Long)

    @Query("DELETE FROM recordings WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM recordings WHERE cassetteId = :cassetteId")
    suspend fun deleteByCassette(cassetteId: Long)

    @Query("DELETE FROM recording_tags WHERE recordingId = :recordingId")
    suspend fun clearTags(recordingId: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTags(tags: List<RecordingTagEntity>)

    @Transaction
    suspend fun replaceTags(recordingId: Long, tags: List<String>) {
        clearTags(recordingId)
        if (tags.isNotEmpty()) insertTags(tags.map { RecordingTagEntity(recordingId, it) })
    }
}

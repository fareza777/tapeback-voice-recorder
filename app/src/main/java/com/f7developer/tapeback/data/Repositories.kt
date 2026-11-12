package com.f7developer.tapeback.data

import androidx.room.withTransaction
import com.f7developer.tapeback.core.AppFiles
import com.f7developer.tapeback.data.db.CassetteEntity
import com.f7developer.tapeback.data.db.RecordingEntity
import com.f7developer.tapeback.data.db.TapeDatabase
import com.f7developer.tapeback.domain.model.Cassette
import com.f7developer.tapeback.domain.model.CassetteSkin
import com.f7developer.tapeback.domain.model.CassetteSummary
import com.f7developer.tapeback.domain.model.Recording
import com.f7developer.tapeback.domain.model.RecordingItem
import com.f7developer.tapeback.domain.model.TapeSide
import com.f7developer.tapeback.domain.policy.DiaryLogic
import com.f7developer.tapeback.domain.policy.WaveformCodec
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class CassetteRepository(
    private val db: TapeDatabase,
    private val files: AppFiles,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val dao = db.cassetteDao()
    private val recordingDao = db.recordingDao()

    fun observeSummaries(): Flow<List<CassetteSummary>> = dao.observeWithStats().map { list -> list.map { it.toDomain() } }

    fun observe(id: Long): Flow<Cassette?> = dao.observe(id).map { it?.toDomain() }

    suspend fun get(id: Long): Cassette? = dao.get(id)?.toDomain()

    suspend fun mostRecent(): Cassette? = dao.mostRecent()?.toDomain()

    suspend fun create(
        title: String,
        category: String,
        labelColor: Int,
        skin: CassetteSkin,
        coverPath: String?,
    ): Long {
        val now = clock()
        return dao.insert(
            CassetteEntity(
                title = title.trim().ifEmpty { "Untitled Tape" }.take(MAX_TITLE),
                category = category.trim().take(MAX_CATEGORY),
                labelColor = labelColor,
                skin = skin.name,
                coverPath = coverPath,
                createdAt = now,
                updatedAt = now,
            ),
        )
    }

    suspend fun update(cassette: Cassette, replacedCoverPath: String? = null) {
        dao.update(
            cassette.copy(
                title = cassette.title.trim().ifEmpty { "Untitled Tape" }.take(MAX_TITLE),
                category = cassette.category.trim().take(MAX_CATEGORY),
                updatedAt = clock(),
            ).toEntity(),
        )
        if (replacedCoverPath != null && replacedCoverPath != cassette.coverPath) files.deleteQuietly(replacedCoverPath)
    }

    suspend fun touch(id: Long) = dao.touch(id, clock())

    /** Deletes the cassette, every recording on it and all their files. Returns the deleted recording paths. */
    suspend fun delete(id: Long) {
        val cassette = dao.get(id) ?: return
        val paths = recordingDao.filePathsForCassette(id)
        db.withTransaction {
            recordingDao.deleteByCassette(id)
            dao.delete(id)
        }
        paths.forEach(files::deleteQuietly)
        files.deleteQuietly(cassette.coverPath)
    }

    /** Guarantees at least one cassette exists and returns a usable one. */
    suspend fun ensureDefault(): Cassette {
        dao.mostRecent()?.let { return it.toDomain() }
        val id = create(
            title = DEFAULT_TITLE,
            category = "Diary",
            labelColor = DEFAULT_LABEL_COLOR,
            skin = CassetteSkin.SMOKED_CLEAR,
            coverPath = null,
        )
        return requireNotNull(dao.get(id)).toDomain()
    }

    companion object {
        const val DEFAULT_TITLE = "My First Mixtape"
        const val MAX_TITLE = 48
        const val MAX_CATEGORY = 24
    }
}

class RecordingRepository(
    private val db: TapeDatabase,
    private val files: AppFiles,
) {
    private val dao = db.recordingDao()
    private val cassetteDao = db.cassetteDao()

    fun observeItems(): Flow<List<RecordingItem>> = dao.observeAll().map { rows -> rows.map { it.toItem() } }

    fun observeByCassette(cassetteId: Long): Flow<List<Recording>> =
        dao.observeByCassette(cassetteId).map { rows -> rows.map { it.toItem().recording } }

    fun observeItem(id: Long): Flow<RecordingItem?> = dao.observe(id).map { it?.toItem() }

    suspend fun get(id: Long): RecordingItem? = dao.get(id)?.toItem()

    suspend fun bySide(cassetteId: Long, side: TapeSide): List<RecordingItem> =
        dao.bySide(cassetteId, side.name).map { it.toItem() }

    data class StorageStats(val count: Int, val bytes: Long)

    fun observeStorage(): Flow<StorageStats> =
        dao.observeCount().combine(dao.observeTotalBytes()) { count, bytes -> StorageStats(count, bytes) }

    suspend fun insert(
        cassetteId: Long,
        side: TapeSide,
        title: String,
        filePath: String,
        durationMs: Long,
        sizeBytes: Long,
        createdAt: Long,
        waveform: List<Float>,
    ): Long {
        val id = dao.insert(
            RecordingEntity(
                cassetteId = cassetteId,
                side = side.name,
                title = title.take(MAX_TITLE),
                filePath = filePath,
                durationMs = durationMs,
                sizeBytes = sizeBytes,
                createdAt = createdAt,
                waveform = WaveformCodec.encode(waveform),
            ),
        )
        cassetteDao.touch(cassetteId, createdAt)
        return id
    }

    suspend fun rename(id: Long, title: String) {
        val clean = title.trim().take(MAX_TITLE)
        if (clean.isNotEmpty()) dao.rename(id, clean)
    }

    suspend fun setFavorite(id: Long, favorite: Boolean) = dao.setFavorite(id, favorite)

    suspend fun setTags(id: Long, tags: List<String>) = dao.replaceTags(id, DiaryLogic.normalizeTags(tags))

    suspend fun move(id: Long, cassetteId: Long, side: TapeSide) = dao.move(id, cassetteId, side.name)

    suspend fun seal(id: Long, unlockAt: Long, sealedAt: Long, note: String?) =
        dao.seal(id, unlockAt, sealedAt, note?.trim()?.take(MAX_NOTE)?.ifEmpty { null })

    suspend fun breakSeal(id: Long) = dao.breakSeal(id)

    suspend fun markCapsuleNotified(id: Long) = dao.markCapsuleNotified(id)

    suspend fun pendingCapsules(): List<Recording> = dao.pendingCapsules().map { it.toDomain(emptyList()) }

    /** Number of entries recorded in [startMs, endMs). */
    suspend fun countCreatedBetween(startMs: Long, endMs: Long): Int = dao.countCreatedBetween(startMs, endMs)

    suspend fun delete(id: Long) {
        val row = dao.get(id) ?: return
        dao.delete(id)
        files.deleteQuietly(row.recording.filePath)
    }

    companion object {
        const val MAX_TITLE = 80
        const val MAX_NOTE = 500
    }
}

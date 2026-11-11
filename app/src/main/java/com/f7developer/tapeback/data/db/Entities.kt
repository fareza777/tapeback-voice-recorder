package com.f7developer.tapeback.data.db

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "cassettes")
data class CassetteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val category: String,
    val labelColor: Int,
    val skin: String,
    val coverPath: String?,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "recordings",
    foreignKeys = [
        ForeignKey(
            entity = CassetteEntity::class,
            parentColumns = ["id"],
            childColumns = ["cassetteId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("cassetteId"), Index("createdAt"), Index("capsuleUnlockAt")],
)
data class RecordingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cassetteId: Long,
    val side: String,
    val title: String,
    val filePath: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val createdAt: Long,
    val favorite: Boolean = false,
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB) val waveform: ByteArray? = null,
    val capsuleUnlockAt: Long? = null,
    val capsuleSealedAt: Long? = null,
    val capsuleNote: String? = null,
    val capsuleNotified: Boolean = false,
) {
    // ByteArray needs content-based equality for data class semantics.
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RecordingEntity) return false
        return id == other.id && cassetteId == other.cassetteId && side == other.side && title == other.title &&
            filePath == other.filePath && durationMs == other.durationMs && sizeBytes == other.sizeBytes &&
            createdAt == other.createdAt && favorite == other.favorite &&
            (waveform?.contentEquals(other.waveform) ?: (other.waveform == null)) &&
            capsuleUnlockAt == other.capsuleUnlockAt && capsuleSealedAt == other.capsuleSealedAt &&
            capsuleNote == other.capsuleNote && capsuleNotified == other.capsuleNotified
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + filePath.hashCode()
        result = 31 * result + title.hashCode()
        result = 31 * result + (waveform?.contentHashCode() ?: 0)
        return result
    }
}

@Entity(
    tableName = "recording_tags",
    primaryKeys = ["recordingId", "tag"],
    foreignKeys = [
        ForeignKey(
            entity = RecordingEntity::class,
            parentColumns = ["id"],
            childColumns = ["recordingId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("tag")],
)
data class RecordingTagEntity(
    val recordingId: Long,
    val tag: String,
)

data class CassetteWithStats(
    @Embedded val cassette: CassetteEntity,
    val sideACount: Int,
    val sideBCount: Int,
    val totalDurationMs: Long,
)

data class RecordingWithTags(
    @Embedded val recording: RecordingEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "recordingId",
        entity = RecordingTagEntity::class,
        projection = ["tag"],
    )
    val tags: List<String>,
)

data class RecordingRow(
    @Embedded val recording: RecordingEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "recordingId",
        entity = RecordingTagEntity::class,
        projection = ["tag"],
    )
    val tags: List<String>,
    @Relation(parentColumn = "cassetteId", entityColumn = "id")
    val cassette: CassetteEntity?,
)

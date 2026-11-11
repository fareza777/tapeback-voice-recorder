package com.f7developer.tapeback.data

import com.f7developer.tapeback.data.db.CassetteEntity
import com.f7developer.tapeback.data.db.CassetteWithStats
import com.f7developer.tapeback.data.db.RecordingEntity
import com.f7developer.tapeback.data.db.RecordingRow
import com.f7developer.tapeback.domain.model.Cassette
import com.f7developer.tapeback.domain.model.CassetteSkin
import com.f7developer.tapeback.domain.model.CassetteSummary
import com.f7developer.tapeback.domain.model.Recording
import com.f7developer.tapeback.domain.model.RecordingItem
import com.f7developer.tapeback.domain.model.TapeSide
import com.f7developer.tapeback.domain.policy.WaveformCodec

internal fun CassetteEntity.toDomain() = Cassette(
    id = id,
    title = title,
    category = category,
    labelColor = labelColor,
    skin = CassetteSkin.fromName(skin),
    coverPath = coverPath,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

internal fun Cassette.toEntity() = CassetteEntity(
    id = id,
    title = title,
    category = category,
    labelColor = labelColor,
    skin = skin.name,
    coverPath = coverPath,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

internal fun CassetteWithStats.toDomain() = CassetteSummary(
    cassette = cassette.toDomain(),
    sideACount = sideACount,
    sideBCount = sideBCount,
    totalDurationMs = totalDurationMs,
)

internal fun RecordingEntity.toDomain(tags: List<String>) = Recording(
    id = id,
    cassetteId = cassetteId,
    side = TapeSide.fromName(side),
    title = title,
    filePath = filePath,
    durationMs = durationMs,
    sizeBytes = sizeBytes,
    createdAt = createdAt,
    favorite = favorite,
    tags = tags.sorted(),
    waveform = WaveformCodec.decode(waveform),
    capsuleUnlockAt = capsuleUnlockAt,
    capsuleSealedAt = capsuleSealedAt,
    capsuleNote = capsuleNote,
    capsuleNotified = capsuleNotified,
)

internal fun RecordingRow.toItem() = RecordingItem(
    recording = recording.toDomain(tags),
    cassetteTitle = cassette?.title.orEmpty(),
    cassetteSkin = CassetteSkin.fromName(cassette?.skin),
    labelColor = cassette?.labelColor ?: DEFAULT_LABEL_COLOR,
)

/** Cream label paper. */
const val DEFAULT_LABEL_COLOR: Int = 0xFFF3E6C8.toInt()

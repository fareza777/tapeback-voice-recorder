package com.f7developer.tapeback.domain

import com.f7developer.tapeback.domain.model.CassetteSkin
import com.f7developer.tapeback.domain.model.Recording
import com.f7developer.tapeback.domain.model.RecordingItem
import com.f7developer.tapeback.domain.model.TapeSide

object TestData {
    private var nextId = 1L

    fun recording(
        title: String = "Entry",
        createdAt: Long = 0L,
        favorite: Boolean = false,
        tags: List<String> = emptyList(),
        durationMs: Long = 60_000L,
        capsuleUnlockAt: Long? = null,
        capsuleNote: String? = null,
    ) = Recording(
        id = nextId++,
        cassetteId = 1L,
        side = TapeSide.A,
        title = title,
        filePath = "/tmp/x.m4a",
        durationMs = durationMs,
        sizeBytes = 1_000L,
        createdAt = createdAt,
        favorite = favorite,
        tags = tags,
        waveform = emptyList(),
        capsuleUnlockAt = capsuleUnlockAt,
        capsuleSealedAt = capsuleUnlockAt?.let { createdAt },
        capsuleNote = capsuleNote,
        capsuleNotified = false,
    )

    fun item(recording: Recording, cassetteTitle: String = "My First Mixtape") =
        RecordingItem(recording, cassetteTitle, CassetteSkin.CLASSIC_BLACK, 0)
}

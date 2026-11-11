package com.f7developer.tapeback.domain.model

/** Side of a cassette. A real cassette has two; so do ours. */
enum class TapeSide(val letter: String) {
    A("A"),
    B("B");

    fun flipped(): TapeSide = if (this == A) B else A

    companion object {
        fun fromName(name: String?): TapeSide = entries.firstOrNull { it.name == name } ?: A
    }
}

/** Visual shell style of a cassette. Persisted by [name]. */
enum class CassetteSkin {
    CLASSIC_BLACK,
    SMOKED_CLEAR,
    CHROME_METAL,
    CREAM_BEIGE,
    HOT_PINK,
    OCEAN_BLUE,
    GHOST_GLOW;

    companion object {
        fun fromName(name: String?): CassetteSkin = entries.firstOrNull { it.name == name } ?: CLASSIC_BLACK
    }
}

data class Cassette(
    val id: Long,
    val title: String,
    val category: String,
    val labelColor: Int,
    val skin: CassetteSkin,
    val coverPath: String?,
    val createdAt: Long,
    val updatedAt: Long,
)

data class CassetteSummary(
    val cassette: Cassette,
    val sideACount: Int,
    val sideBCount: Int,
    val totalDurationMs: Long,
) {
    val totalCount: Int get() = sideACount + sideBCount
}

data class Recording(
    val id: Long,
    val cassetteId: Long,
    val side: TapeSide,
    val title: String,
    val filePath: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val createdAt: Long,
    val favorite: Boolean,
    val tags: List<String>,
    /** Compact waveform, values 0..1, at most [com.f7developer.tapeback.domain.policy.WaveformCodec.MAX_BINS]. */
    val waveform: List<Float>,
    val capsuleUnlockAt: Long?,
    val capsuleSealedAt: Long?,
    val capsuleNote: String?,
    val capsuleNotified: Boolean,
) {
    val isCapsule: Boolean get() = capsuleUnlockAt != null
}

/** A recording joined with the cassette it lives on (for diary/capsule lists). */
data class RecordingItem(
    val recording: Recording,
    val cassetteTitle: String,
    val cassetteSkin: CassetteSkin,
    val labelColor: Int,
)

/** Where the deck records to. */
data class RecordTarget(val cassetteId: Long, val side: TapeSide)

enum class RecordingQuality(val sampleRate: Int, val bitRate: Int) {
    STANDARD(44_100, 96_000),
    HIGH(44_100, 128_000),
    STUDIO(48_000, 192_000);

    companion object {
        fun fromName(name: String?): RecordingQuality = entries.firstOrNull { it.name == name } ?: HIGH
    }
}

enum class EffectPreset {
    CLEAN,
    VINTAGE,
    LOFI,
    RADIO;

    companion object {
        fun fromName(name: String?): EffectPreset = entries.firstOrNull { it.name == name } ?: CLEAN
    }
}

enum class RetroThemeId {
    AMBER_WALKMAN,
    NEON_MIXTAPE,
    CHROME_HIFI,
    KIROWEEN,
    BEIGE_CLASSIC;

    companion object {
        fun fromName(name: String?): RetroThemeId = entries.firstOrNull { it.name == name } ?: AMBER_WALKMAN
    }
}

enum class DarkMode {
    SYSTEM,
    LIGHT,
    DARK;

    companion object {
        fun fromName(name: String?): DarkMode = entries.firstOrNull { it.name == name } ?: SYSTEM
    }
}

data class PlaybackFx(
    val preset: EffectPreset = EffectPreset.CLEAN,
    /** 0..1 */
    val hiss: Float = 0f,
    /** 0.5..2.0 */
    val speed: Float = 1f,
    val linkPitch: Boolean = true,
)

data class AppSettings(
    val onboardingDone: Boolean = false,
    val theme: RetroThemeId = RetroThemeId.AMBER_WALKMAN,
    val darkMode: DarkMode = DarkMode.SYSTEM,
    val grain: Boolean = true,
    val scanlines: Boolean = false,
    val reduceMotion: Boolean = false,
    val mechanicalSounds: Boolean = true,
    val haptics: Boolean = true,
    val quality: RecordingQuality = RecordingQuality.HIGH,
    val keepScreenOn: Boolean = true,
    val fx: PlaybackFx = PlaybackFx(),
    val recordTarget: RecordTarget? = null,
)

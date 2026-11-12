package com.f7developer.tapeback.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.f7developer.tapeback.domain.model.AppSettings
import com.f7developer.tapeback.domain.model.DarkMode
import com.f7developer.tapeback.domain.model.DiaryReminder
import com.f7developer.tapeback.domain.model.EffectPreset
import com.f7developer.tapeback.domain.model.PlaybackFx
import com.f7developer.tapeback.domain.model.RecordTarget
import com.f7developer.tapeback.domain.model.RecordingOptions
import com.f7developer.tapeback.domain.model.RecordingQuality
import com.f7developer.tapeback.domain.model.RepeatMode
import com.f7developer.tapeback.domain.model.RetroThemeId
import com.f7developer.tapeback.domain.model.TapeSide
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

private val Context.settingsStore: DataStore<Preferences> by preferencesDataStore(name = "tapeback_settings")

class SettingsRepository(context: Context) {
    private val store = context.applicationContext.settingsStore

    private object Keys {
        val onboarding = booleanPreferencesKey("onboarding_done")
        val theme = stringPreferencesKey("theme")
        val darkMode = stringPreferencesKey("dark_mode")
        val grain = booleanPreferencesKey("grain")
        val scanlines = booleanPreferencesKey("scanlines")
        val reduceMotion = booleanPreferencesKey("reduce_motion")
        val sounds = booleanPreferencesKey("mechanical_sounds")
        val haptics = booleanPreferencesKey("haptics")
        val quality = stringPreferencesKey("quality")
        val keepScreenOn = booleanPreferencesKey("keep_screen_on")
        val detailsAfterRecording = booleanPreferencesKey("details_after_recording")
        val countdown = intPreferencesKey("countdown_seconds")
        val autoStop = intPreferencesKey("auto_stop_minutes")
        val repeat = stringPreferencesKey("repeat_mode")
        val skipSilence = booleanPreferencesKey("skip_silence")
        val reminderOn = booleanPreferencesKey("reminder_enabled")
        val reminderMinute = intPreferencesKey("reminder_minute")
        val appLock = booleanPreferencesKey("app_lock")
        val hideInRecents = booleanPreferencesKey("hide_in_recents")
        val fxPreset = stringPreferencesKey("fx_preset")
        val fxHiss = floatPreferencesKey("fx_hiss")
        val fxSpeed = floatPreferencesKey("fx_speed")
        val fxLinkPitch = booleanPreferencesKey("fx_link_pitch")
        val targetCassette = longPreferencesKey("target_cassette")
        val targetSide = stringPreferencesKey("target_side")
    }

    val settings: Flow<AppSettings> = store.data.map { p ->
        val defaults = AppSettings()
        AppSettings(
            onboardingDone = p[Keys.onboarding] ?: false,
            theme = RetroThemeId.fromName(p[Keys.theme]),
            darkMode = DarkMode.fromName(p[Keys.darkMode]),
            grain = p[Keys.grain] ?: defaults.grain,
            scanlines = p[Keys.scanlines] ?: defaults.scanlines,
            reduceMotion = p[Keys.reduceMotion] ?: defaults.reduceMotion,
            mechanicalSounds = p[Keys.sounds] ?: defaults.mechanicalSounds,
            haptics = p[Keys.haptics] ?: defaults.haptics,
            quality = RecordingQuality.fromName(p[Keys.quality]),
            keepScreenOn = p[Keys.keepScreenOn] ?: defaults.keepScreenOn,
            detailsAfterRecording = p[Keys.detailsAfterRecording] ?: defaults.detailsAfterRecording,
            countdownSeconds = (p[Keys.countdown] ?: 0).takeIf { it in RecordingOptions.COUNTDOWN_CHOICES } ?: 0,
            autoStopMinutes = (p[Keys.autoStop] ?: 0).takeIf { it in RecordingOptions.AUTO_STOP_CHOICES } ?: 0,
            repeatMode = RepeatMode.fromName(p[Keys.repeat]),
            skipSilence = p[Keys.skipSilence] ?: defaults.skipSilence,
            reminder = DiaryReminder(
                enabled = p[Keys.reminderOn] ?: false,
                minuteOfDay = (p[Keys.reminderMinute] ?: defaults.reminder.minuteOfDay).coerceIn(0, 24 * 60 - 1),
            ),
            appLock = p[Keys.appLock] ?: defaults.appLock,
            hideInRecents = p[Keys.hideInRecents] ?: defaults.hideInRecents,
            fx = PlaybackFx(
                preset = EffectPreset.fromName(p[Keys.fxPreset]),
                hiss = (p[Keys.fxHiss] ?: 0f).coerceIn(0f, 1f),
                speed = (p[Keys.fxSpeed] ?: 1f).coerceIn(MIN_SPEED, MAX_SPEED),
                linkPitch = p[Keys.fxLinkPitch] ?: true,
            ),
            recordTarget = p[Keys.targetCassette]?.let { RecordTarget(it, TapeSide.fromName(p[Keys.targetSide])) },
        )
    }.distinctUntilChanged()

    suspend fun setOnboardingDone(done: Boolean = true) = store.edit { it[Keys.onboarding] = done }
    suspend fun setTheme(theme: RetroThemeId) = store.edit { it[Keys.theme] = theme.name }
    suspend fun setDarkMode(mode: DarkMode) = store.edit { it[Keys.darkMode] = mode.name }
    suspend fun setGrain(on: Boolean) = store.edit { it[Keys.grain] = on }
    suspend fun setScanlines(on: Boolean) = store.edit { it[Keys.scanlines] = on }
    suspend fun setReduceMotion(on: Boolean) = store.edit { it[Keys.reduceMotion] = on }
    suspend fun setMechanicalSounds(on: Boolean) = store.edit { it[Keys.sounds] = on }
    suspend fun setHaptics(on: Boolean) = store.edit { it[Keys.haptics] = on }
    suspend fun setQuality(quality: RecordingQuality) = store.edit { it[Keys.quality] = quality.name }
    suspend fun setKeepScreenOn(on: Boolean) = store.edit { it[Keys.keepScreenOn] = on }
    suspend fun setDetailsAfterRecording(on: Boolean) = store.edit { it[Keys.detailsAfterRecording] = on }
    suspend fun setCountdown(seconds: Int) = store.edit { it[Keys.countdown] = seconds }
    suspend fun setAutoStop(minutes: Int) = store.edit { it[Keys.autoStop] = minutes }
    suspend fun setRepeatMode(mode: RepeatMode) = store.edit { it[Keys.repeat] = mode.name }
    suspend fun setSkipSilence(on: Boolean) = store.edit { it[Keys.skipSilence] = on }
    suspend fun setReminder(reminder: DiaryReminder) = store.edit {
        it[Keys.reminderOn] = reminder.enabled
        it[Keys.reminderMinute] = reminder.minuteOfDay
    }
    suspend fun setAppLock(on: Boolean) = store.edit { it[Keys.appLock] = on }
    suspend fun setHideInRecents(on: Boolean) = store.edit { it[Keys.hideInRecents] = on }

    /**
     * Restores every preference to its default. Onboarding state and the deck's recording target are
     * kept, so the user is not sent through the intro again and the deck keeps its cassette.
     */
    suspend fun resetToDefaults() = store.edit { p ->
        val onboarding = p[Keys.onboarding]
        val cassette = p[Keys.targetCassette]
        val side = p[Keys.targetSide]
        p.clear()
        onboarding?.let { p[Keys.onboarding] = it }
        cassette?.let { p[Keys.targetCassette] = it }
        side?.let { p[Keys.targetSide] = it }
    }

    suspend fun setFx(fx: PlaybackFx) = store.edit {
        it[Keys.fxPreset] = fx.preset.name
        it[Keys.fxHiss] = fx.hiss.coerceIn(0f, 1f)
        it[Keys.fxSpeed] = fx.speed.coerceIn(MIN_SPEED, MAX_SPEED)
        it[Keys.fxLinkPitch] = fx.linkPitch
    }

    suspend fun setRecordTarget(target: RecordTarget) = store.edit {
        it[Keys.targetCassette] = target.cassetteId
        it[Keys.targetSide] = target.side.name
    }

    companion object {
        const val MIN_SPEED = 0.5f
        const val MAX_SPEED = 2.0f
    }
}

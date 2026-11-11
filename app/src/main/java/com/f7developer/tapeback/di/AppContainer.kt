package com.f7developer.tapeback.di

import android.content.Context
import com.f7developer.tapeback.audio.export.AudioExporter
import com.f7developer.tapeback.audio.fx.LevelBus
import com.f7developer.tapeback.audio.fx.TapeEffectChain
import com.f7developer.tapeback.audio.play.PlayerConnection
import com.f7developer.tapeback.audio.record.RecordingController
import com.f7developer.tapeback.capsule.CapsuleScheduler
import com.f7developer.tapeback.core.AppFiles
import com.f7developer.tapeback.core.sound.MechanicalSounds
import com.f7developer.tapeback.data.CassetteRepository
import com.f7developer.tapeback.data.RecordingRepository
import com.f7developer.tapeback.data.SettingsRepository
import com.f7developer.tapeback.data.db.TapeDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** Manual dependency graph. One instance lives in [com.f7developer.tapeback.TapeBackApp]. */
class AppContainer(context: Context) {
    private val app = context.applicationContext

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val files = AppFiles(app)
    val database: TapeDatabase by lazy { TapeDatabase.create(app) }
    val settingsRepository = SettingsRepository(app)
    val cassetteRepository: CassetteRepository by lazy { CassetteRepository(database, files) }
    val recordingRepository: RecordingRepository by lazy { RecordingRepository(database, files) }

    val playbackLevels = LevelBus()
    val recordLevels = LevelBus()
    val effectChain = TapeEffectChain(playbackLevels)

    val playerConnection by lazy { PlayerConnection(app, appScope, effectChain, playbackLevels) }
    val recordingController by lazy {
        RecordingController(app, appScope, files, recordingRepository, cassetteRepository, recordLevels)
    }
    val capsuleScheduler by lazy { CapsuleScheduler(app, recordingRepository) }
    val sounds by lazy { MechanicalSounds(files.soundsDir, appScope) }
    val exporter by lazy { AudioExporter(app) }

    /** Cross-screen requests to the deck (capsule recording, launcher "Record" shortcut). */
    val capsuleModeRequest = MutableStateFlow(false)
    val recordRequest = MutableStateFlow(false)

    fun start() {
        appScope.launch {
            cassetteRepository.ensureDefault()
            capsuleScheduler.resync()
        }
        appScope.launch {
            settingsRepository.settings.map { it.fx }.distinctUntilChanged().collect { playerConnection.applyFx(it) }
        }
        appScope.launch {
            settingsRepository.settings.map { it.mechanicalSounds }.distinctUntilChanged().collect { sounds.enabled = it }
        }
        sounds.prepare()
    }
}

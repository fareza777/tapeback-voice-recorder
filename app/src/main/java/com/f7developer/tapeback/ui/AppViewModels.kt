package com.f7developer.tapeback.ui

import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.f7developer.tapeback.TapeBackApp
import com.f7developer.tapeback.di.AppContainer
import com.f7developer.tapeback.domain.model.AppSettings
import com.f7developer.tapeback.domain.model.DarkMode
import com.f7developer.tapeback.domain.model.RetroThemeId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Access the manual DI graph from composition. */
@Composable
fun appContainer(): AppContainer = (LocalContext.current.applicationContext as TapeBackApp).container

/** Root state: settings (theme, overlays) and whether onboarding is complete. */
class RootViewModel(private val container: AppContainer) : ViewModel() {

    /** null until DataStore has been read, so the splash screen can stay up until then. */
    val settings: StateFlow<AppSettings?> = container.settingsRepository.settings
        .map<AppSettings, AppSettings?> { it }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun completeOnboarding() {
        viewModelScope.launch { container.settingsRepository.setOnboardingDone() }
    }

    fun setTheme(theme: RetroThemeId) {
        viewModelScope.launch { container.settingsRepository.setTheme(theme) }
    }

    fun setDarkMode(mode: DarkMode) {
        viewModelScope.launch { container.settingsRepository.setDarkMode(mode) }
    }

    // ---- App lock -------------------------------------------------------------------------

    private val _unlocked = MutableStateFlow(false)
    /** False until the user verifies this session; reset after [LOCK_GRACE_MS] in the background. */
    val unlocked: StateFlow<Boolean> = _unlocked.asStateFlow()
    private var stoppedAt = 0L

    fun markUnlocked() {
        _unlocked.value = true
    }

    fun onAppStopped(changingConfigurations: Boolean) {
        if (!changingConfigurations) stoppedAt = SystemClock.elapsedRealtime()
    }

    fun onAppStarted() {
        if (stoppedAt > 0L && SystemClock.elapsedRealtime() - stoppedAt > LOCK_GRACE_MS) _unlocked.value = false
        stoppedAt = 0L
    }

    companion object {
        /** Short trips to the share sheet, photo picker or permission dialog don't re-lock. */
        const val LOCK_GRACE_MS = 60_000L
    }
}

package com.f7developer.tapeback.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.f7developer.tapeback.TapeBackApp
import com.f7developer.tapeback.di.AppContainer
import com.f7developer.tapeback.domain.model.AppSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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
}

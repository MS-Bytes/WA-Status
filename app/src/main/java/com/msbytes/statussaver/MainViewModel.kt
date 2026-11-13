package com.msbytes.statussaver

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.msbytes.statussaver.domain.model.AppSettings
import com.msbytes.statussaver.domain.repository.SettingsRepository
import com.msbytes.statussaver.security.AppLockManager
import com.msbytes.statussaver.work.WorkScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    appLockManager: AppLockManager,
    workScheduler: WorkScheduler,
) : ViewModel() {

    /** Null until DataStore has been read (the splash screen stays up until then). */
    val settings: StateFlow<AppSettings?> = settingsRepository.settings
        .onEach {
            appLockManager.onSettingsLoaded(it.appLockEnabled)
            workScheduler.sync(it.backgroundWorkNeeded)
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val locked: StateFlow<Boolean> = appLockManager.locked

    fun completeOnboarding() {
        viewModelScope.launch { settingsRepository.setOnboardingDone() }
    }
}

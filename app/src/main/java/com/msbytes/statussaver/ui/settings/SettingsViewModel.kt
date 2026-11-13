package com.msbytes.statussaver.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.msbytes.statussaver.ads.AdManager
import com.msbytes.statussaver.ads.ConsentManager
import com.msbytes.statussaver.domain.model.AppSettings
import com.msbytes.statussaver.domain.model.MediaType
import com.msbytes.statussaver.domain.model.StatusSource
import com.msbytes.statussaver.domain.model.ThemeMode
import com.msbytes.statussaver.domain.repository.SavedRepository
import com.msbytes.statussaver.domain.repository.SettingsRepository
import com.msbytes.statussaver.domain.repository.StatusRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StorageSummary(val bytes: Long = 0, val images: Int = 0, val videos: Int = 0)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val statusRepository: StatusRepository,
    savedRepository: SavedRepository,
    val adManager: AdManager,
    val consentManager: ConsentManager,
) : ViewModel() {

    val settings: StateFlow<AppSettings> =
        settingsRepository.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    val storage: StateFlow<StorageSummary> = savedRepository.saved.map { list ->
        StorageSummary(list.sumOf { it.size }, list.count { it.type == MediaType.IMAGE }, list.count { it.type == MediaType.VIDEO })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StorageSummary())

    private val _access = MutableStateFlow(emptyMap<StatusSource, Boolean>())
    val access: StateFlow<Map<StatusSource, Boolean>> = _access.asStateFlow()

    init {
        viewModelScope.launch { savedRepository.refresh() }
        refreshAccess()
    }

    fun refreshAccess() {
        _access.value = StatusSource.entries.associateWith { statusRepository.hasAccess(it) }
    }

    fun initialFolderUri(source: StatusSource): Uri = statusRepository.initialFolderUri(source)

    /** Returns false when the wrong folder was picked. */
    fun grantAccess(source: StatusSource, uri: Uri): Boolean =
        statusRepository.grantAccess(source, uri).also { refreshAccess() }

    fun revokeAccess(source: StatusSource) {
        statusRepository.revokeAccess(source)
        refreshAccess()
    }

    fun setTheme(mode: ThemeMode) = launch { settingsRepository.setThemeMode(mode) }
    fun setDynamicColor(enabled: Boolean) = launch { settingsRepository.setDynamicColor(enabled) }
    fun setAutoSave(source: StatusSource, enabled: Boolean) = launch { settingsRepository.setAutoSave(source, enabled) }
    fun setNotifications(enabled: Boolean) = launch { settingsRepository.setNotifyNewStatuses(enabled) }
    fun setPin(pin: String?) = launch { settingsRepository.setPin(pin) }
    fun setBiometric(enabled: Boolean) = launch { settingsRepository.setBiometricEnabled(enabled) }

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }
}

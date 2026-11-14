package com.msbytes.statussaver.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.msbytes.statussaver.ads.ConsentManager
import com.msbytes.statussaver.domain.model.AppSettings
import com.msbytes.statussaver.domain.model.MediaType
import com.msbytes.statussaver.domain.model.ThemeMode
import com.msbytes.statussaver.domain.repository.SavedRepository
import com.msbytes.statussaver.domain.repository.SettingsRepository
import com.msbytes.statussaver.domain.repository.StatusRepository
import com.msbytes.statussaver.play.PlayStoreHelper
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
    val consentManager: ConsentManager,
    val playStoreHelper: PlayStoreHelper,
) : ViewModel() {

    val settings: StateFlow<AppSettings> =
        settingsRepository.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    val storage: StateFlow<StorageSummary> = savedRepository.saved.map { list ->
        StorageSummary(list.sumOf { it.size }, list.count { it.type == MediaType.IMAGE }, list.count { it.type == MediaType.VIDEO })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StorageSummary())

    private val _hasAccess = MutableStateFlow(statusRepository.hasAccess())
    val hasAccess: StateFlow<Boolean> = _hasAccess.asStateFlow()

    init {
        viewModelScope.launch { savedRepository.refresh() }
    }

    fun initialFolderUri(): Uri = statusRepository.initialFolderUri()

    /** Returns false when an unrelated folder was picked. */
    fun grantAccess(uri: Uri): Boolean =
        statusRepository.grantAccess(uri).also { _hasAccess.value = statusRepository.hasAccess() }

    fun revokeAccess() {
        statusRepository.revokeAccess()
        _hasAccess.value = statusRepository.hasAccess()
    }

    fun setTheme(mode: ThemeMode) = launch { settingsRepository.setThemeMode(mode) }
    fun setDynamicColor(enabled: Boolean) = launch { settingsRepository.setDynamicColor(enabled) }
    fun setAutoSave(enabled: Boolean) = launch { settingsRepository.setAutoSave(enabled) }
    fun setNotifications(enabled: Boolean) = launch { settingsRepository.setNotifyNewStatuses(enabled) }
    fun setLiveDetection(enabled: Boolean) = launch { settingsRepository.setLiveDetection(enabled) }
    fun setPin(pin: String?) = launch { settingsRepository.setPin(pin) }
    fun setBiometric(enabled: Boolean) = launch { settingsRepository.setBiometricEnabled(enabled) }

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }
}

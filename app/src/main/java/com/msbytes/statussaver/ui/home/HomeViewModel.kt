package com.msbytes.statussaver.ui.home

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.msbytes.statussaver.R
import com.msbytes.statussaver.core.UiEvent
import com.msbytes.statussaver.core.UiState
import com.msbytes.statussaver.data.share.ShareHelper
import com.msbytes.statussaver.domain.model.MediaType
import com.msbytes.statussaver.domain.model.SaveResult
import com.msbytes.statussaver.domain.model.SortOrder
import com.msbytes.statussaver.domain.model.StatusAccessException
import com.msbytes.statussaver.domain.model.StatusItem
import com.msbytes.statussaver.domain.model.StatusSource
import com.msbytes.statussaver.domain.repository.SavedRepository
import com.msbytes.statussaver.domain.repository.SettingsRepository
import com.msbytes.statussaver.domain.repository.StatusRepository
import com.msbytes.statussaver.ui.common.SaveActions
import com.msbytes.statussaver.widget.WidgetUpdater
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val statusRepository: StatusRepository,
    private val savedRepository: SavedRepository,
    private val settingsRepository: SettingsRepository,
    private val saveActions: SaveActions,
    private val shareHelper: ShareHelper,
    private val widgetUpdater: WidgetUpdater,
) : ViewModel() {

    private val local = MutableStateFlow(HomeUiState())
    private val loadFailed = MutableStateFlow(false)
    private val baselines = mutableMapOf<StatusSource, Long>()

    private val _events = Channel<UiEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    val uiState: StateFlow<HomeUiState> = combine(
        local,
        loadFailed,
        statusRepository.statuses,
        savedRepository.saved,
        settingsRepository.settings,
    ) { state, failed, statuses, saved, settings ->
        val items = statuses[state.source]
        val content = when {
            failed -> UiState.Error(R.string.error_load_statuses)
            items == null -> UiState.Loading
            items.isEmpty() -> UiState.Empty
            else -> UiState.Success(settings.sortOrder.apply(items))
        }
        state.copy(content = content, savedNames = saved.mapTo(HashSet()) { it.name }, sortOrder = settings.sortOrder)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    init {
        viewModelScope.launch { savedRepository.refresh() }
    }

    fun selectSource(source: StatusSource) {
        if (source == local.value.source) return
        local.update { it.copy(source = source, selection = emptySet(), access = AccessState.CHECKING) }
        loadFailed.value = false
        viewModelScope.launch { loadBaseline(source) }
        refresh()
    }

    /** Called when the screen comes to the foreground. */
    fun onStart() {
        viewModelScope.launch {
            baselines.clear()
            loadBaseline(local.value.source)
        }
        refresh()
        viewModelScope.launch { savedRepository.refresh() }
    }

    /** Called when the screen goes to the background: everything visible is now "seen". */
    fun onStop() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            statusRepository.statuses.value.keys.forEach { settingsRepository.setLastSeen(it, now) }
            widgetUpdater.update()
        }
    }

    fun refresh(silent: Boolean = false) {
        val source = local.value.source
        viewModelScope.launch {
            val installed = statusRepository.isAppInstalled(source)
            val hasAccess = statusRepository.hasAccess(source)
            local.update {
                it.copy(
                    installed = installed,
                    access = when {
                        hasAccess -> AccessState.GRANTED
                        it.access == AccessState.WRONG_FOLDER || it.access == AccessState.REVOKED -> it.access
                        else -> AccessState.NEEDED
                    },
                )
            }
            if (!hasAccess) return@launch
            if (!silent) local.update { it.copy(isRefreshing = true) }
            statusRepository.refresh(source)
                .onSuccess { items ->
                    loadFailed.value = false
                    autoSaveIfEnabled(source, items)
                    widgetUpdater.update()
                }
                .onFailure { error ->
                    when (error) {
                        is StatusAccessException.NoAccess -> local.update { it.copy(access = AccessState.REVOKED) }
                        is StatusAccessException.FolderMissing ->
                            local.update { it.copy(access = AccessState.FOLDER_MISSING) }
                        else -> loadFailed.value = true
                    }
                }
            local.update { it.copy(isRefreshing = false) }
        }
    }

    fun initialFolderUri(): Uri = statusRepository.initialFolderUri(local.value.source)

    fun onFolderPicked(uri: Uri?) {
        if (uri == null) return
        val source = local.value.source
        if (statusRepository.grantAccess(source, uri)) {
            local.update { it.copy(access = AccessState.GRANTED) }
            _events.trySend(UiEvent.Message(R.string.permission_granted))
            refresh()
        } else {
            local.update { it.copy(access = AccessState.WRONG_FOLDER) }
        }
    }

    fun save(item: StatusItem) {
        if (item.id in local.value.savingIds) return
        local.update { it.copy(savingIds = it.savingIds + item.id) }
        viewModelScope.launch {
            val event = saveActions.save(item)
            local.update { it.copy(savingIds = it.savingIds - item.id) }
            _events.send(event)
        }
    }

    fun setSortOrder(order: SortOrder) {
        viewModelScope.launch { settingsRepository.setSortOrder(order) }
    }

    // ------------------------------------------------------------ Multi-select

    fun toggleSelection(item: StatusItem) = local.update {
        it.copy(selection = if (item.id in it.selection) it.selection - item.id else it.selection + item.id)
    }

    fun selectAll(type: MediaType) = local.update { state ->
        state.copy(selection = currentItems(type).mapTo(HashSet()) { it.id })
    }

    fun clearSelection() = local.update { it.copy(selection = emptySet()) }

    fun saveSelected(type: MediaType) {
        val items = selectedItems(type)
        clearSelection()
        if (items.isEmpty()) return
        viewModelScope.launch { _events.send(saveActions.saveMany(items)) }
    }

    fun shareSelected(type: MediaType) {
        val items = selectedItems(type)
        clearSelection()
        viewModelScope.launch {
            shareHelper.buildShareIntent(items)?.let { _events.send(UiEvent.LaunchIntent(it, R.string.share_chooser_title)) }
        }
    }

    private fun currentItems(type: MediaType): List<StatusItem> =
        statusRepository.statuses.value[local.value.source].orEmpty().filter { it.type == type }

    private fun selectedItems(type: MediaType): List<StatusItem> {
        val ids = local.value.selection
        return currentItems(type).filter { it.id in ids }
    }

    // ------------------------------------------------------------ Helpers

    private suspend fun loadBaseline(source: StatusSource) {
        val baseline = baselines.getOrPut(source) {
            settingsRepository.lastSeen(source).let { if (it == 0L) Long.MAX_VALUE else it }
        }
        if (local.value.source == source) local.update { it.copy(newSince = baseline) }
    }

    /** Auto-save while the app is open (the background worker covers the rest). */
    private suspend fun autoSaveIfEnabled(source: StatusSource, items: List<StatusItem>) {
        if (!settingsRepository.settings.first().autoSave(source)) return
        val savedNames = savedRepository.saved.value.mapTo(HashSet()) { it.name }
        var count = 0
        items.filter { it.name !in savedNames }.forEach {
            if (savedRepository.save(it) is SaveResult.Saved) count++
        }
        if (count > 0) _events.send(UiEvent.Plural(R.plurals.notif_auto_saved, count))
    }
}

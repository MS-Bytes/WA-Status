package com.msbytes.statussaver.ui.saved

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.msbytes.statussaver.R
import com.msbytes.statussaver.core.UiEvent
import com.msbytes.statussaver.core.UiState
import com.msbytes.statussaver.data.share.ShareHelper
import com.msbytes.statussaver.domain.model.MediaType
import com.msbytes.statussaver.domain.model.StatusItem
import com.msbytes.statussaver.domain.repository.FavoritesRepository
import com.msbytes.statussaver.domain.repository.SavedRepository
import com.msbytes.statussaver.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SavedFilter { ALL, IMAGES, VIDEOS, FAVORITES }

data class SavedUiState(
    val content: UiState<List<StatusItem>> = UiState.Loading,
    val filter: SavedFilter = SavedFilter.ALL,
    val favoriteIds: Set<String> = emptySet(),
    val selection: Set<String> = emptySet(),
    val totalBytes: Long = 0,
    val imageCount: Int = 0,
    val videoCount: Int = 0,
)

@HiltViewModel
class SavedViewModel @Inject constructor(
    private val savedRepository: SavedRepository,
    private val favoritesRepository: FavoritesRepository,
    private val shareHelper: ShareHelper,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    private val filter = MutableStateFlow(SavedFilter.ALL)
    private val selection = MutableStateFlow<Set<String>>(emptySet())
    private val loaded = MutableStateFlow(false)

    private val _events = Channel<UiEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    val uiState: StateFlow<SavedUiState> = combine(
        combine(savedRepository.saved, loaded, ::Pair),
        favoritesRepository.favoriteIds,
        filter,
        selection,
        settingsRepository.settings,
    ) { (saved, isLoaded), favorites, filter, selection, settings ->
        val filtered = when (filter) {
            SavedFilter.ALL -> saved
            SavedFilter.IMAGES -> saved.filter { it.type == MediaType.IMAGE }
            SavedFilter.VIDEOS -> saved.filter { it.type == MediaType.VIDEO }
            SavedFilter.FAVORITES -> saved.filter { it.id in favorites }
        }
        SavedUiState(
            content = when {
                !isLoaded -> UiState.Loading
                filtered.isEmpty() -> UiState.Empty
                else -> UiState.Success(settings.sortOrder.apply(filtered))
            },
            filter = filter,
            favoriteIds = favorites,
            selection = selection,
            totalBytes = saved.sumOf { it.size },
            imageCount = saved.count { it.type == MediaType.IMAGE },
            videoCount = saved.count { it.type == MediaType.VIDEO },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SavedUiState())

    fun refresh() {
        viewModelScope.launch {
            savedRepository.refresh()
            loaded.value = true
        }
    }

    fun setFilter(value: SavedFilter) {
        filter.value = value
        selection.value = emptySet()
    }

    fun toggleSelection(item: StatusItem) = selection.update {
        if (item.id in it) it - item.id else it + item.id
    }

    fun selectAll() {
        selection.value = (uiState.value.content as? UiState.Success)?.data.orEmpty().mapTo(HashSet()) { it.id }
    }

    fun clearSelection() {
        selection.value = emptySet()
    }

    fun shareSelected() {
        val items = selectedItems()
        clearSelection()
        viewModelScope.launch {
            shareHelper.buildShareIntent(items)?.let { _events.send(UiEvent.LaunchIntent(it, R.string.share_chooser_title)) }
        }
    }

    fun deleteSelected() {
        val items = selectedItems()
        clearSelection()
        if (items.isEmpty()) return
        viewModelScope.launch {
            val outcome = savedRepository.delete(items)
            if (outcome.deletedCount > 0) _events.send(UiEvent.Plural(R.plurals.deleted_count, outcome.deletedCount))
            outcome.confirmationRequest?.let { _events.send(UiEvent.ConfirmDelete(it)) }
        }
    }

    fun selectedCount(): Int = selection.value.size

    private fun selectedItems(): List<StatusItem> {
        val ids = selection.value
        return savedRepository.saved.value.filter { it.id in ids }
    }
}

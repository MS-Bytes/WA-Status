package com.msbytes.statussaver.ui.viewer

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.msbytes.statussaver.R
import com.msbytes.statussaver.core.UiEvent
import com.msbytes.statussaver.data.share.ShareHelper
import com.msbytes.statussaver.domain.model.StatusItem
import com.msbytes.statussaver.domain.model.MediaType
import com.msbytes.statussaver.domain.repository.FavoritesRepository
import com.msbytes.statussaver.domain.repository.SavedRepository
import com.msbytes.statussaver.domain.repository.StatusRepository
import com.msbytes.statussaver.ui.common.SaveActions
import com.msbytes.statussaver.ui.navigation.ViewerCollection
import com.msbytes.statussaver.ui.navigation.ViewerRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ViewerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val statusRepository: StatusRepository,
    private val savedRepository: SavedRepository,
    private val favoritesRepository: FavoritesRepository,
    private val saveActions: SaveActions,
    private val shareHelper: ShareHelper,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<ViewerRoute>()
    val startId: String = route.startId

    /**
     * Status lists are snapshotted when the viewer opens so a background rescan never
     * shifts the page under the user's finger. Saved items stay live (for deletes).
     */
    val items: StateFlow<List<StatusItem>> = if (route.collection == ViewerCollection.SAVED) {
        savedRepository.saved
    } else {
        val type = if (route.collection == ViewerCollection.VIDEOS) MediaType.VIDEO else MediaType.IMAGE
        MutableStateFlow(statusRepository.statuses.value.orEmpty().filter { it.type == type }).asStateFlow()
    }

    val favoriteIds: StateFlow<Set<String>> =
        favoritesRepository.favoriteIds.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    val savedNames: StateFlow<Set<String>> = savedRepository.saved
        .map { list -> list.mapTo(HashSet()) { it.name } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    private val _saving = MutableStateFlow<Set<String>>(emptySet())
    val saving: StateFlow<Set<String>> = _saving.asStateFlow()

    private val _events = Channel<UiEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch { savedRepository.refresh() }
    }

    fun save(item: StatusItem) {
        if (item.id in _saving.value) return
        _saving.update { it + item.id }
        viewModelScope.launch {
            val event = saveActions.save(item)
            _saving.update { it - item.id }
            _events.send(event)
        }
    }

    fun share(item: StatusItem) = launchShare(item, null)

    /** Repost to WhatsApp / WhatsApp Business (whichever is installed). */
    fun repost(item: StatusItem, chooserTitle: String) {
        viewModelScope.launch {
            val intent = shareHelper.buildRepostIntent(item, statusRepository.installedApps(), chooserTitle)
            _events.send(intent?.let { UiEvent.LaunchIntent(it) } ?: UiEvent.Message(R.string.not_installed_title))
        }
    }

    fun toggleFavorite(item: StatusItem) {
        viewModelScope.launch { favoritesRepository.toggle(item) }
    }

    fun delete(item: StatusItem) {
        viewModelScope.launch {
            val outcome = savedRepository.delete(listOf(item))
            if (outcome.deletedCount > 0) _events.send(UiEvent.Plural(R.plurals.deleted_count, outcome.deletedCount))
            outcome.confirmationRequest?.let { _events.send(UiEvent.ConfirmDelete(it)) }
        }
    }

    fun refreshSaved() {
        viewModelScope.launch { savedRepository.refresh() }
    }

    private fun launchShare(item: StatusItem, packageName: String?) {
        viewModelScope.launch {
            val intent = shareHelper.buildShareIntent(listOf(item), packageName)
            if (intent == null) {
                _events.send(UiEvent.Message(R.string.error_file_missing))
            } else {
                _events.send(UiEvent.LaunchIntent(intent, if (packageName == null) R.string.share_chooser_title else null))
            }
        }
    }
}

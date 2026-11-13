package com.msbytes.statussaver.ui.editor

import android.content.Context
import androidx.core.net.toUri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.msbytes.statussaver.core.MediaPreviewCache
import com.msbytes.statussaver.domain.model.MediaType
import com.msbytes.statussaver.domain.model.SaveResult
import com.msbytes.statussaver.domain.repository.SavedRepository
import com.msbytes.statussaver.ui.common.SaveActions
import com.msbytes.statussaver.ui.navigation.EditorRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class VideoEditorState(
    val durationMs: Long = 0,
    val startMs: Long = 0,
    val endMs: Long = 0,
    val removeAudio: Boolean = false,
    /** 0..100 while exporting, null otherwise. */
    val progress: Int? = null,
    val savedOk: Boolean? = null,
    val error: Boolean = false,
) {
    val hasChanges: Boolean get() = removeAudio || startMs > 0 || (durationMs > 0 && endMs < durationMs)
}

@HiltViewModel
class VideoEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context,
    private val exporter: VideoExporter,
    private val savedRepository: SavedRepository,
    private val saveActions: SaveActions,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<EditorRoute>()
    val uri = route.uri.toUri()

    private val _state = MutableStateFlow(VideoEditorState())
    val state: StateFlow<VideoEditorState> = _state.asStateFlow()
    private var exportJob: Job? = null

    init {
        viewModelScope.launch {
            val duration = MediaPreviewCache.videoDuration(context, uri)
            if (duration == null || duration <= 0) {
                _state.update { it.copy(error = true) }
            } else {
                _state.update { it.copy(durationMs = duration, endMs = duration) }
            }
        }
    }

    fun setRange(start: Long, end: Long) = _state.update {
        it.copy(startMs = start.coerceIn(0, it.durationMs), endMs = end.coerceIn(start + MIN_LENGTH_MS, it.durationMs))
    }

    fun setRemoveAudio(remove: Boolean) = _state.update { it.copy(removeAudio = remove) }

    fun export() {
        if (exportJob?.isActive == true) return
        val s = _state.value
        _state.update { it.copy(progress = 0) }
        exportJob = viewModelScope.launch {
            val ok = try {
                val file = exporter.export(uri, s.startMs, s.endMs, s.removeAudio) { p ->
                    _state.update { it.copy(progress = p) }
                }
                val result = savedRepository.saveFile(file, editedFileName("mp4"), MediaType.VIDEO)
                file.delete()
                result is SaveResult.Saved
            } catch (e: CancellationException) {
                _state.update { it.copy(progress = null) }
                throw e
            } catch (_: Exception) {
                false
            }
            if (ok) saveActions.onUserSaved()
            _state.update { it.copy(progress = null, savedOk = ok) }
        }
    }

    fun cancelExport() {
        exportJob?.cancel()
        _state.update { it.copy(progress = null) }
    }

    fun consumeSaveResult() = _state.update { it.copy(savedOk = null) }

    private companion object {
        const val MIN_LENGTH_MS = 1_000L
    }
}

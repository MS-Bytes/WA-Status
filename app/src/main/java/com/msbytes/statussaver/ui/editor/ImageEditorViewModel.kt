package com.msbytes.statussaver.ui.editor

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.net.toUri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.msbytes.statussaver.domain.model.MediaType
import com.msbytes.statussaver.domain.model.SaveResult
import com.msbytes.statussaver.domain.repository.SavedRepository
import com.msbytes.statussaver.ui.common.SaveActions
import com.msbytes.statussaver.ui.navigation.EditorRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

enum class EditorTool { CROP, ADJUST, FILTER, TEXT, DRAW }

data class ImageEditorState(
    val loading: Boolean = true,
    val error: Boolean = false,
    val image: ImageBitmap? = null,
    val thumbnail: ImageBitmap? = null,
    val edits: ImageEdits = ImageEdits(),
    val tool: EditorTool = EditorTool.CROP,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val saving: Boolean = false,
    /** Set once the edited copy is saved; the screen then closes. */
    val savedOk: Boolean? = null,
) {
    val hasChanges: Boolean get() = edits != ImageEdits()
}

@HiltViewModel
class ImageEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context,
    private val savedRepository: SavedRepository,
    private val saveActions: SaveActions,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<EditorRoute>()
    private val _state = MutableStateFlow(ImageEditorState())
    val state: StateFlow<ImageEditorState> = _state.asStateFlow()

    private var original: Bitmap? = null
    private var transformed: Bitmap? = null
    private var transformKey = Triple(0, false, false)

    private val undoStack = ArrayDeque<ImageEdits>()
    private val redoStack = ArrayDeque<ImageEdits>()
    private var pendingSnapshot: ImageEdits? = null

    init {
        viewModelScope.launch {
            val bitmap = withContext(Dispatchers.IO) {
                runCatching { ImageRenderer.decode(context, route.uri.toUri()) }.getOrNull()
            }
            if (bitmap == null) {
                _state.update { it.copy(loading = false, error = true) }
            } else {
                original = bitmap
                transformed = bitmap
                val thumb = withContext(Dispatchers.Default) { ImageRenderer.thumbnail(bitmap) }
                _state.update {
                    it.copy(loading = false, image = bitmap.asImageBitmap(), thumbnail = thumb.asImageBitmap())
                }
            }
        }
    }

    fun selectTool(tool: EditorTool) = _state.update { it.copy(tool = tool) }

    /** Live change while a slider / drag is in progress (no history entry yet). */
    fun preview(change: (ImageEdits) -> ImageEdits) {
        val current = _state.value.edits
        if (pendingSnapshot == null) pendingSnapshot = current
        setEdits(change(current))
    }

    /** Ends a live change and records it in the undo history. */
    fun commit() {
        val before = pendingSnapshot ?: return
        pendingSnapshot = null
        if (before != _state.value.edits) {
            undoStack.addLast(before)
            redoStack.clear()
            if (undoStack.size > MAX_HISTORY) undoStack.removeFirst()
        }
        updateHistoryFlags()
    }

    /** A one-shot change (button press). */
    fun apply(change: (ImageEdits) -> ImageEdits) {
        preview(change)
        commit()
    }

    fun undo() {
        val previous = undoStack.removeLastOrNull() ?: return
        redoStack.addLast(_state.value.edits)
        setEdits(previous)
        updateHistoryFlags()
    }

    fun redo() {
        val next = redoStack.removeLastOrNull() ?: return
        undoStack.addLast(_state.value.edits)
        setEdits(next)
        updateHistoryFlags()
    }

    fun rotate() = apply { it.copy(rotation = (it.rotation + 90) % 360, crop = ImageEdits.FULL, aspect = CropAspect.FREE) }
    fun flipHorizontal() = apply { it.copy(flipHorizontal = !it.flipHorizontal, crop = ImageEdits.FULL, aspect = CropAspect.FREE) }
    fun flipVertical() = apply { it.copy(flipVertical = !it.flipVertical, crop = ImageEdits.FULL, aspect = CropAspect.FREE) }

    fun setAspect(aspect: CropAspect) {
        val bmp = transformed ?: return
        val ratio = aspect.ratio
        val crop = if (ratio == null) {
            ImageEdits.FULL
        } else {
            val w = bmp.width.toFloat()
            val h = bmp.height.toFloat()
            val (cw, ch) = if (w / h > ratio) h * ratio to h else w to w / ratio
            val nw = cw / w
            val nh = ch / h
            Rect((1 - nw) / 2, (1 - nh) / 2, (1 + nw) / 2, (1 + nh) / 2)
        }
        apply { it.copy(aspect = aspect, crop = crop) }
    }

    fun addText(text: String, color: Int, sizeFraction: Float) = apply {
        it.copy(texts = it.texts + TextLayer(System.nanoTime(), text, color, sizeFraction, 0.5f, 0.5f))
    }

    fun updateText(id: Long, text: String, color: Int, sizeFraction: Float) = apply { edits ->
        edits.copy(texts = edits.texts.map { if (it.id == id) it.copy(text = text, color = color, sizeFraction = sizeFraction) else it })
    }

    fun removeText(id: Long) = apply { edits -> edits.copy(texts = edits.texts.filterNot { it.id == id }) }

    fun moveText(id: Long, dx: Float, dy: Float) = preview { edits ->
        edits.copy(
            texts = edits.texts.map {
                if (it.id == id) it.copy(x = (it.x + dx).coerceIn(0f, 1f), y = (it.y + dy).coerceIn(0f, 1f)) else it
            },
        )
    }

    fun addStroke(stroke: DrawStroke) = apply { it.copy(strokes = it.strokes + stroke) }

    fun save() {
        val base = transformed ?: return
        if (_state.value.saving) return
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            val edits = _state.value.edits
            val result = withContext(Dispatchers.Default) {
                runCatching {
                    val rendered = ImageRenderer.render(base, edits)
                    val dir = File(context.cacheDir, "editor").apply { mkdirs() }
                    val file = File(dir, "edit_${System.currentTimeMillis()}.jpg")
                    file.outputStream().use { rendered.compress(Bitmap.CompressFormat.JPEG, 95, it) }
                    file
                }.getOrNull()
            }
            val saveResult = result?.let {
                savedRepository.saveFile(it, editedFileName("jpg"), MediaType.IMAGE).also { _ -> it.delete() }
            }
            val ok = saveResult is SaveResult.Saved
            if (ok) saveActions.onUserSaved()
            _state.update { it.copy(saving = false, savedOk = ok) }
        }
    }

    fun consumeSaveResult() = _state.update { it.copy(savedOk = null) }

    private fun setEdits(edits: ImageEdits) {
        _state.update { it.copy(edits = edits) }
        val key = Triple(edits.rotation, edits.flipHorizontal, edits.flipVertical)
        if (key != transformKey) {
            transformKey = key
            val source = original ?: return
            viewModelScope.launch {
                val bmp = withContext(Dispatchers.Default) {
                    ImageRenderer.transform(source, key.first, key.second, key.third)
                }
                if (transformKey == key) {
                    transformed = bmp
                    _state.update { it.copy(image = bmp.asImageBitmap()) }
                }
            }
        }
    }

    private fun updateHistoryFlags() =
        _state.update { it.copy(canUndo = undoStack.isNotEmpty(), canRedo = redoStack.isNotEmpty()) }

    private companion object {
        const val MAX_HISTORY = 50
    }
}

fun editedFileName(extension: String): String {
    val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    return "StatusSaver_Edit_$stamp.$extension"
}

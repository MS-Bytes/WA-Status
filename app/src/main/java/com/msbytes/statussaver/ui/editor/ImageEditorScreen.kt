package com.msbytes.statussaver.ui.editor

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Redo
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.BrokenImage
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Crop
import androidx.compose.material.icons.rounded.Flip
import androidx.compose.material.icons.automirrored.rounded.RotateRight
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.msbytes.statussaver.R
import com.msbytes.statussaver.ui.components.ConfirmDialog
import com.msbytes.statussaver.ui.components.MessageState

val EditorPalette = listOf(
    Color.White, Color.Black, Color(0xFFFF3B30), Color(0xFFFFCC00),
    Color(0xFF34C759), Color(0xFF007AFF), Color(0xFF8E5CF6), Color(0xFFFF2D92),
)

@Composable
fun ImageEditorScreen(onClose: () -> Unit, viewModel: ImageEditorViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var confirmDiscard by remember { mutableStateOf(false) }
    var textDialog by remember { mutableStateOf<TextLayer?>(null) }
    var brush by remember { mutableStateOf(BrushSettings(Color.White.toArgb(), 0.012f, eraser = false)) }

    BackHandler(enabled = state.hasChanges && !state.saving) { confirmDiscard = true }
    LaunchedEffect(state.savedOk) {
        when (state.savedOk) {
            true -> {
                Toast.makeText(context, R.string.editor_saved, Toast.LENGTH_SHORT).show()
                viewModel.consumeSaveResult()
                onClose()
            }
            false -> {
                Toast.makeText(context, R.string.editor_failed, Toast.LENGTH_SHORT).show()
                viewModel.consumeSaveResult()
            }
            null -> Unit
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.editor_title)) },
                navigationIcon = {
                    IconButton(onClick = { if (state.hasChanges) confirmDiscard = true else onClose() }) {
                        Icon(Icons.Rounded.Close, stringResource(R.string.action_close))
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::undo, enabled = state.canUndo) {
                        Icon(Icons.AutoMirrored.Rounded.Undo, stringResource(R.string.action_undo))
                    }
                    IconButton(onClick = viewModel::redo, enabled = state.canRedo) {
                        Icon(Icons.AutoMirrored.Rounded.Redo, stringResource(R.string.action_redo))
                    }
                    IconButton(onClick = viewModel::save, enabled = state.image != null && !state.saving) {
                        Icon(Icons.Rounded.Check, stringResource(R.string.action_save))
                    }
                },
            )
        },
        bottomBar = {
            if (state.image != null) {
                Column(Modifier.navigationBarsPadding()) {
                    ToolPanel(
                        state = state,
                        brush = brush,
                        onBrushChange = { brush = it },
                        viewModel = viewModel,
                        onAddText = { textDialog = TextLayer(0, "", Color.White.toArgb(), 0.08f, 0.5f, 0.5f) },
                    )
                    ToolTabs(state.tool, viewModel::selectTool)
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).padding(16.dp), contentAlignment = Alignment.Center) {
            val image = state.image
            when {
                state.loading -> CircularProgressIndicator()
                state.error || image == null -> MessageState(
                    Icons.Rounded.BrokenImage,
                    stringResource(R.string.error_title),
                    stringResource(R.string.error_file_corrupted),
                )
                else -> EditorCanvas(
                    image = image,
                    edits = state.edits,
                    tool = state.tool,
                    brush = brush,
                    onCropChange = { rect -> viewModel.preview { it.copy(crop = rect) } },
                    onCropEnd = viewModel::commit,
                    onTextMove = viewModel::moveText,
                    onTextMoveEnd = viewModel::commit,
                    onTextTap = { textDialog = it },
                    onStroke = viewModel::addStroke,
                )
            }
            if (state.saving) {
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }
    }

    textDialog?.let { layer ->
        TextLayerDialog(
            initial = layer,
            isNew = layer.id == 0L,
            onDismiss = { textDialog = null },
            onConfirm = { text, color, size ->
                if (layer.id == 0L) viewModel.addText(text, color, size) else viewModel.updateText(layer.id, text, color, size)
                textDialog = null
            },
            onDelete = { viewModel.removeText(layer.id); textDialog = null },
        )
    }
    if (confirmDiscard) {
        ConfirmDialog(
            title = stringResource(R.string.editor_discard_title),
            text = stringResource(R.string.editor_discard_body),
            confirmLabel = stringResource(R.string.action_discard),
            onConfirm = onClose,
            onDismiss = { confirmDiscard = false },
        )
    }
}

@Composable
private fun ToolTabs(selected: EditorTool, onSelect: (EditorTool) -> Unit) {
    NavigationBar(windowInsets = androidx.compose.foundation.layout.WindowInsets(0)) {
        EditorTool.entries.forEach { tool ->
            val (icon, label) = when (tool) {
                EditorTool.CROP -> Icons.Rounded.Crop to R.string.editor_crop
                EditorTool.ADJUST -> Icons.Rounded.Tune to R.string.editor_adjust
                EditorTool.FILTER -> Icons.Rounded.AutoFixHigh to R.string.editor_filters
                EditorTool.TEXT -> Icons.Rounded.TextFields to R.string.editor_text
                EditorTool.DRAW -> Icons.Rounded.Brush to R.string.editor_draw
            }
            NavigationBarItem(
                selected = selected == tool,
                onClick = { onSelect(tool) },
                icon = { Icon(icon, null) },
                label = { Text(stringResource(label)) },
            )
        }
    }
}

@Composable
private fun ToolPanel(
    state: ImageEditorState,
    brush: BrushSettings,
    onBrushChange: (BrushSettings) -> Unit,
    viewModel: ImageEditorViewModel,
    onAddText: () -> Unit,
) {
    val edits = state.edits
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        when (state.tool) {
            EditorTool.CROP -> {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(onClick = viewModel::rotate) {
                        Icon(Icons.AutoMirrored.Rounded.RotateRight, null)
                        Text(stringResource(R.string.editor_rotate), Modifier.padding(start = 6.dp))
                    }
                    IconButton(onClick = viewModel::flipHorizontal) {
                        Icon(Icons.Rounded.Flip, stringResource(R.string.editor_flip_horizontal))
                    }
                    IconButton(onClick = viewModel::flipVertical) {
                        Icon(Icons.Rounded.Flip, stringResource(R.string.editor_flip_vertical), Modifier.rotate(90f))
                    }
                }
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CropAspect.entries.forEach { aspect ->
                        FilterChip(
                            selected = edits.aspect == aspect,
                            onClick = { viewModel.setAspect(aspect) },
                            label = {
                                Text(
                                    stringResource(
                                        when (aspect) {
                                            CropAspect.FREE -> R.string.editor_aspect_free
                                            CropAspect.SQUARE -> R.string.editor_aspect_square
                                            CropAspect.PORTRAIT_4_5 -> R.string.editor_aspect_4_5
                                            CropAspect.STORY_9_16 -> R.string.editor_aspect_9_16
                                            CropAspect.WIDE_16_9 -> R.string.editor_aspect_16_9
                                        },
                                    ),
                                )
                            },
                        )
                    }
                }
            }

            EditorTool.ADJUST -> {
                AdjustSlider(R.string.editor_brightness, edits.brightness, { v -> viewModel.preview { it.copy(brightness = v) } }, viewModel::commit)
                AdjustSlider(R.string.editor_contrast, edits.contrast, { v -> viewModel.preview { it.copy(contrast = v) } }, viewModel::commit)
                AdjustSlider(R.string.editor_saturation, edits.saturation, { v -> viewModel.preview { it.copy(saturation = v) } }, viewModel::commit)
            }

            EditorTool.FILTER -> {
                val thumb = state.thumbnail
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(FilterPreset.entries) { preset ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable { viewModel.apply { it.copy(filter = preset) } },
                        ) {
                            if (thumb != null) {
                                Image(
                                    bitmap = thumb,
                                    contentDescription = stringResource(preset.label),
                                    contentScale = ContentScale.Crop,
                                    colorFilter = ColorFilter.colorMatrix(ColorMatrix(preset.matrix().array)),
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .then(
                                            if (edits.filter == preset) {
                                                Modifier.border(BorderStroke(3.dp, MaterialTheme.colorScheme.primary), RoundedCornerShape(12.dp))
                                            } else {
                                                Modifier
                                            },
                                        ),
                                )
                            }
                            Text(stringResource(preset.label), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            EditorTool.TEXT -> {
                Button(onClick = onAddText, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Rounded.TextFields, null)
                    Text(stringResource(R.string.editor_add_text), Modifier.padding(start = 8.dp))
                }
            }

            EditorTool.DRAW -> {
                ColorRow(selected = brush.color, enabled = !brush.eraser) { onBrushChange(brush.copy(color = it, eraser = false)) }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.editor_brush_size), style = MaterialTheme.typography.labelMedium)
                    Slider(
                        value = brush.widthFraction,
                        onValueChange = { onBrushChange(brush.copy(widthFraction = it)) },
                        valueRange = 0.004f..0.05f,
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                    )
                    FilterChip(
                        selected = brush.eraser,
                        onClick = { onBrushChange(brush.copy(eraser = !brush.eraser)) },
                        label = { Text(stringResource(R.string.editor_eraser)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun AdjustSlider(label: Int, value: Float, onChange: (Float) -> Unit, onDone: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(label), style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(90.dp))
        Slider(
            value = value,
            onValueChange = onChange,
            onValueChangeFinished = onDone,
            valueRange = -1f..1f,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
fun ColorRow(selected: Int, enabled: Boolean = true, onSelect: (Int) -> Unit) {
    Row(
        Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        EditorPalette.forEach { color ->
            val argb = color.toArgb()
            Box(
                Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(
                        width = if (enabled && argb == selected) 3.dp else 1.dp,
                        color = if (enabled && argb == selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        shape = CircleShape,
                    )
                    .clickable { onSelect(argb) },
            )
        }
    }
}

@Composable
private fun TextLayerDialog(
    initial: TextLayer,
    isNew: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String, Int, Float) -> Unit,
    onDelete: () -> Unit,
) {
    var text by rememberSaveable { mutableStateOf(initial.text) }
    var color by rememberSaveable { mutableIntStateOf(initial.color) }
    var size by rememberSaveable { mutableFloatStateOf(initial.sizeFraction) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.editor_add_text)) },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text(stringResource(R.string.editor_text_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.editor_color), style = MaterialTheme.typography.labelMedium)
                ColorRow(selected = color) { color = it }
                Text(stringResource(R.string.editor_size), style = MaterialTheme.typography.labelMedium)
                Slider(value = size, onValueChange = { size = it }, valueRange = 0.03f..0.2f)
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text.trim(), color, size) }, enabled = text.isNotBlank()) {
                Text(stringResource(R.string.action_done))
            }
        },
        dismissButton = {
            Row {
                if (!isNew) TextButton(onClick = onDelete) { Text(stringResource(R.string.action_delete)) }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
            }
        },
    )
}

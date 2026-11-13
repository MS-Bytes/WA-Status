package com.msbytes.statussaver.ui.editor

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.msbytes.statussaver.R
import com.msbytes.statussaver.core.Formatters
import com.msbytes.statussaver.ui.components.ConfirmDialog
import com.msbytes.statussaver.ui.components.MessageState
import kotlinx.coroutines.delay

@Composable
fun VideoEditorScreen(onClose: () -> Unit, viewModel: VideoEditorViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var confirmDiscard by remember { mutableStateOf(false) }

    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(viewModel.uri))
            prepare()
            playWhenReady = true
        }
    }
    DisposableEffect(Unit) { onDispose { player.release() } }
    LaunchedEffect(state.removeAudio) { player.volume = if (state.removeAudio) 0f else 1f }
    LaunchedEffect(state.startMs) { player.seekTo(state.startMs) }
    // Loop the preview inside the trimmed range.
    LaunchedEffect(state.startMs, state.endMs) {
        while (true) {
            if (state.endMs > 0 && player.currentPosition >= state.endMs) player.seekTo(state.startMs)
            delay(200)
        }
    }
    LaunchedEffect(state.progress) { if (state.progress != null) player.pause() }

    BackHandler(enabled = state.hasChanges && state.progress == null) { confirmDiscard = true }
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
                    IconButton(onClick = viewModel::export, enabled = state.durationMs > 0 && state.progress == null) {
                        Icon(Icons.Rounded.Check, stringResource(R.string.action_save))
                    }
                },
            )
        },
    ) { padding ->
        if (state.error) {
            MessageState(
                Icons.Rounded.ErrorOutline,
                stringResource(R.string.error_title),
                stringResource(R.string.error_file_corrupted),
                modifier = Modifier.padding(padding),
            )
            return@Scaffold
        }
        Column(Modifier.fillMaxSize().padding(padding).navigationBarsPadding()) {
            Box(Modifier.weight(1f).fillMaxWidth().background(Color.Black), contentAlignment = Alignment.Center) {
                AndroidView(factory = { PlayerView(it).apply { this.player = player } }, modifier = Modifier.fillMaxSize())
            }
            Column(Modifier.padding(16.dp)) {
                Text(stringResource(R.string.editor_trim), style = MaterialTheme.typography.titleMedium)
                if (state.durationMs > 0) {
                    RangeSlider(
                        value = state.startMs.toFloat()..state.endMs.toFloat(),
                        onValueChange = { viewModel.setRange(it.start.toLong(), it.endInclusive.toLong()) },
                        valueRange = 0f..state.durationMs.toFloat(),
                    )
                    Text(
                        stringResource(
                            R.string.editor_trim_range,
                            Formatters.duration(state.startMs),
                            Formatters.duration(state.endMs),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                    )
                } else {
                    CircularProgressIndicator()
                }
                Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.editor_mute_audio), Modifier.weight(1f))
                    Switch(checked = state.removeAudio, onCheckedChange = viewModel::setRemoveAudio)
                }
            }
        }
    }

    state.progress?.let { progress ->
        AlertDialog(
            onDismissRequest = {},
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
            title = { Text(stringResource(R.string.editor_saving_progress, progress)) },
            text = { LinearProgressIndicator(progress = { progress / 100f }, modifier = Modifier.fillMaxWidth()) },
            confirmButton = {},
            dismissButton = { TextButton(onClick = viewModel::cancelExport) { Text(stringResource(R.string.action_cancel)) } },
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

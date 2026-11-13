package com.msbytes.statussaver.ui.common

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import com.msbytes.statussaver.R
import com.msbytes.statussaver.ads.LocalAdManager
import com.msbytes.statussaver.core.IntentUtils
import com.msbytes.statussaver.core.UiEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.coroutineScope

/** Snackbar shown after a save, with Open and Share actions. */
class SavedSnackbarVisuals(
    override val message: String,
    val uri: Uri,
    val mimeType: String,
) : SnackbarVisuals {
    override val actionLabel: String? = null
    override val withDismissAction: Boolean = false
    override val duration: SnackbarDuration = SnackbarDuration.Short
}

@Composable
fun AppSnackbarHost(state: SnackbarHostState, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val adManager = LocalAdManager.current
    SnackbarHost(state, modifier) { data ->
        val visuals = data.visuals
        if (visuals is SavedSnackbarVisuals) {
            Snackbar(
                action = {
                    Row {
                        TextButton(onClick = {
                            data.dismiss()
                            adManager?.suppressNextAppOpen()
                            runCatching { context.startActivity(IntentUtils.viewMedia(visuals.uri, visuals.mimeType)) }
                        }) { Text(stringResource(R.string.action_open)) }
                        TextButton(onClick = {
                            data.dismiss()
                            adManager?.suppressNextAppOpen()
                            val send = Intent(Intent.ACTION_SEND)
                                .setType(visuals.mimeType.ifBlank { "*/*" })
                                .putExtra(Intent.EXTRA_STREAM, visuals.uri)
                                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            runCatching {
                                context.startActivity(Intent.createChooser(send, context.getString(R.string.share_chooser_title)))
                            }
                        }) { Text(stringResource(R.string.action_share)) }
                    }
                },
            ) { Text(visuals.message) }
        } else {
            Snackbar(data)
        }
    }
}

/** Collects one-off [UiEvent]s from a ViewModel and performs them. */
@Composable
fun HandleUiEvents(events: Flow<UiEvent>, snackbarHostState: SnackbarHostState, onDeleteConfirmed: () -> Unit = {}) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val adManager = LocalAdManager.current
    val deleteLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
        onDeleteConfirmed()
    }
    LaunchedEffect(events) {
        coroutineScope {
            events.collect { event ->
                when (event) {
                    is UiEvent.Message -> launch { snackbarHostState.showSnackbar(context.getString(event.text)) }
                    is UiEvent.Plural -> launch {
                        snackbarHostState.showSnackbar(
                            resources.getQuantityString(event.pluralRes, event.count, event.count),
                        )
                    }
                    is UiEvent.Saved -> launch {
                        snackbarHostState.currentSnackbarData?.dismiss()
                        snackbarHostState.showSnackbar(
                            SavedSnackbarVisuals(context.getString(R.string.save_success), event.uri, event.mimeType),
                        )
                    }
                    is UiEvent.LaunchIntent -> {
                        adManager?.suppressNextAppOpen()
                        val intent = event.chooserTitle?.let { Intent.createChooser(event.intent, context.getString(it)) }
                            ?: event.intent
                        try {
                            context.startActivity(intent)
                        } catch (_: ActivityNotFoundException) {
                            launch { snackbarHostState.showSnackbar(context.getString(R.string.error_no_app_to_open)) }
                        }
                    }
                    is UiEvent.ConfirmDelete -> deleteLauncher.launch(IntentSenderRequest.Builder(event.sender).build())
                }
            }
        }
    }
}

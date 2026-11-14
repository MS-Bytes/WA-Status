package com.msbytes.statussaver.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.msbytes.statussaver.R
import com.msbytes.statussaver.core.Formatters
import com.msbytes.statussaver.core.MediaPreviewCache
import com.msbytes.statussaver.data.media.MediaStoreSaver
import com.msbytes.statussaver.domain.model.StatusItem

@Composable
fun InfoSheet(item: StatusItem, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val resolution by produceState<String?>(null, item.uri) {
        value = MediaPreviewCache.resolution(context, item.uri, item.isVideo)
    }
    val duration by produceState<Long?>(null, item.uri) {
        if (item.isVideo) value = MediaPreviewCache.videoDuration(context, item.uri)
    }
    val location = item.source?.displayName ?: MediaStoreSaver.relativePath(item.type)

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 24.dp).navigationBarsPadding()) {
            Text(stringResource(R.string.info_title), style = MaterialTheme.typography.titleLarge)
            InfoRow(stringResource(R.string.info_name), item.name)
            InfoRow(stringResource(R.string.info_type), item.mimeType)
            InfoRow(stringResource(R.string.info_size), Formatters.size(item.size))
            InfoRow(stringResource(R.string.info_date), Formatters.dateTime(item.lastModified))
            resolution?.let { InfoRow(stringResource(R.string.info_resolution), it) }
            duration?.let { InfoRow(stringResource(R.string.info_duration), Formatters.duration(it)) }
            InfoRow(stringResource(R.string.info_location), location)
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(top = 14.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(110.dp),
        )
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

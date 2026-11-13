package com.msbytes.statussaver.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.msbytes.statussaver.R
import com.msbytes.statussaver.domain.model.StatusSource

/** Guided folder-permission screen shown in place of the grid (no ads here). */
@Composable
fun PermissionContent(
    source: StatusSource,
    access: AccessState,
    onGrant: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showWhy by rememberSaveable { mutableStateOf(false) }
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(120.dp)
                .background(
                    Brush.linearGradient(
                        listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f)),
                    ),
                    CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.FolderOpen, stringResource(R.string.cd_illustration), Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.height(20.dp))
        Text(stringResource(R.string.permission_title), style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.permission_body, stringResource(R.string.app_short_name), source.displayName),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        val error = when (access) {
            AccessState.WRONG_FOLDER -> stringResource(R.string.permission_wrong_folder_title) to
                stringResource(R.string.permission_wrong_folder_body)
            AccessState.REVOKED -> stringResource(R.string.permission_revoked_title) to
                stringResource(R.string.permission_revoked_body, source.displayName)
            AccessState.FOLDER_MISSING -> stringResource(R.string.error_title) to
                stringResource(R.string.permission_folder_missing, source.displayName)
            else -> null
        }
        if (error != null) {
            Spacer(Modifier.height(16.dp))
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Rounded.ErrorOutline, null, tint = MaterialTheme.colorScheme.onErrorContainer)
                    Column {
                        Text(error.first, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onErrorContainer)
                        Text(error.second, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        Card(Modifier.fillMaxWidth().widthIn(max = 480.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Step(1, stringResource(R.string.permission_step_1))
                Step(2, stringResource(R.string.permission_step_2))
                Step(3, stringResource(R.string.permission_step_3))
            }
        }
        Spacer(Modifier.height(24.dp))
        Button(onClick = onGrant, modifier = Modifier.fillMaxWidth().widthIn(max = 480.dp).height(52.dp)) {
            Text(stringResource(R.string.permission_grant))
        }
        TextButton(onClick = { showWhy = !showWhy }) { Text(stringResource(R.string.permission_why_title)) }
        AnimatedVisibility(showWhy) {
            Text(
                stringResource(R.string.permission_why_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun Step(number: Int, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(28.dp).background(MaterialTheme.colorScheme.primary, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text("$number", color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.labelLarge)
        }
        Spacer(Modifier.size(12.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

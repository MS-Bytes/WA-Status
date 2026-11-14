package com.msbytes.statussaver.ui.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.VolunteerActivism
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.msbytes.statussaver.R

/** Shows the USDT (TRC20) donation address with a copy button. */
@Composable
fun DonateDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val address = stringResource(R.string.donate_address)
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.VolunteerActivism, null) },
        title = { Text(stringResource(R.string.donate_title)) },
        text = {
            Column {
                Text(stringResource(R.string.donate_subtitle), style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.donate_network), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(12.dp))
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    SelectionContainer {
                        Text(
                            address,
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(12.dp),
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Button(onClick = { copy(context, address) }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Rounded.ContentCopy, null)
                    Text(stringResource(R.string.action_copy), Modifier.padding(start = 8.dp))
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    stringResource(R.string.donate_warning),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) } },
    )
}

private fun copy(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    clipboard.setPrimaryClip(ClipData.newPlainText("USDT TRC20", text))
    // Android 13+ shows its own clipboard confirmation.
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        Toast.makeText(context, R.string.donate_copied, Toast.LENGTH_SHORT).show()
    }
}

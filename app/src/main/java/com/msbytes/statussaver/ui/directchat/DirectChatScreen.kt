package com.msbytes.statussaver.ui.directchat

import android.content.Context
import android.telephony.TelephonyManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.msbytes.statussaver.R
import com.msbytes.statussaver.ads.LocalAdManager
import com.msbytes.statussaver.core.IntentUtils
import com.msbytes.statussaver.domain.model.StatusSource
import java.util.Locale

@Composable
fun DirectChatScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val adManager = LocalAdManager.current
    var countryIso by rememberSaveable { mutableStateOf(defaultCountryIso(context)) }
    val country = Countries.firstOrNull { it.iso == countryIso } ?: Countries.first { it.iso == "US" }
    var phone by rememberSaveable { mutableStateOf("") }
    var message by rememberSaveable { mutableStateOf("") }
    var showPicker by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf(false) }

    fun open(source: StatusSource) {
        val digits = phone.filter { it.isDigit() }.trimStart('0')
        val full = country.dialCode + digits
        if (digits.length < 5 || full.length > 15) {
            error = true
            return
        }
        error = false
        val installed = runCatching { context.packageManager.getPackageInfo(source.packageName, 0) }.isSuccess
        adManager?.suppressNextAppOpen()
        runCatching {
            context.startActivity(IntentUtils.directChat(full, message, if (installed) source.packageName else null))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.direct_chat_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.cd_back)) }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .navigationBarsPadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.direct_chat_desc), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { showPicker = true }, modifier = Modifier.width(120.dp).height(56.dp)) {
                    Text("${country.flag} +${country.dialCode}")
                }
                OutlinedTextField(
                    value = phone,
                    onValueChange = { value -> phone = value.filter { it.isDigit() || it == ' ' }.take(18); error = false },
                    label = { Text(stringResource(R.string.direct_chat_phone)) },
                    isError = error,
                    supportingText = if (error) ({ Text(stringResource(R.string.direct_chat_invalid)) }) else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }
            OutlinedTextField(
                value = message,
                onValueChange = { message = it.take(1000) },
                label = { Text(stringResource(R.string.direct_chat_message)) },
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(stringResource(R.string.direct_chat_open_in), style = MaterialTheme.typography.labelLarge)
            Button(onClick = { open(StatusSource.WHATSAPP) }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Icon(Icons.AutoMirrored.Rounded.Send, null)
                Text(stringResource(R.string.source_whatsapp), Modifier.padding(start = 8.dp))
            }
            OutlinedButton(onClick = { open(StatusSource.BUSINESS) }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Icon(Icons.AutoMirrored.Rounded.Send, null)
                Text(stringResource(R.string.source_business), Modifier.padding(start = 8.dp))
            }
        }
    }

    if (showPicker) {
        CountryPickerDialog(onDismiss = { showPicker = false }) {
            countryIso = it.iso
            showPicker = false
        }
    }
}

@Composable
private fun CountryPickerDialog(onDismiss: () -> Unit, onPick: (Country) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    val filtered = remember(query) {
        Countries.filter { it.name.contains(query, ignoreCase = true) || it.dialCode.startsWith(query.trimStart('+')) }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.direct_chat_country_code)) },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text(stringResource(R.string.direct_chat_search_country)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                LazyColumn(Modifier.height(360.dp)) {
                    items(filtered, key = { it.iso }) { country ->
                        Text(
                            "${country.flag}  ${country.name}  (+${country.dialCode})",
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPick(country) }
                                .padding(vertical = 12.dp),
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) } },
    )
}

private fun defaultCountryIso(context: Context): String {
    val sim = runCatching {
        (context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager)?.simCountryIso
    }.getOrNull()
    val iso = (sim?.takeIf { it.isNotBlank() } ?: Locale.getDefault().country).uppercase(Locale.US)
    return if (Countries.any { it.iso == iso }) iso else "US"
}

package com.msbytes.statussaver.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.msbytes.statussaver.R

/** Two-step "create PIN" + "confirm PIN" dialog. */
@Composable
fun PinSetupDialog(onDismiss: () -> Unit, onPinSet: (String) -> Unit) {
    var first by rememberSaveable { mutableStateOf<String?>(null) }
    var input by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (first == null) R.string.lock_set_pin else R.string.lock_confirm_pin)) },
        text = {
            Column {
                OutlinedTextField(
                    value = input,
                    onValueChange = { value -> if (value.length <= 4 && value.all { it.isDigit() }) input = value },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    isError = error,
                )
                if (error) Text(stringResource(R.string.lock_pin_mismatch), color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = {
            TextButton(enabled = input.length == 4, onClick = {
                val firstPin = first
                when {
                    firstPin == null -> { first = input; input = ""; error = false }
                    firstPin == input -> onPinSet(input)
                    else -> { first = null; input = ""; error = true }
                }
            }) { Text(stringResource(R.string.action_next)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

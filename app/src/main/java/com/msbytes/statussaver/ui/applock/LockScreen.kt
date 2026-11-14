package com.msbytes.statussaver.ui.applock

import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.msbytes.statussaver.R
import com.msbytes.statussaver.ui.components.AppLogo
import kotlinx.coroutines.delay

/** Full-screen PIN / biometric lock (never shows ads). */
@Composable
fun LockScreen(onExit: () -> Unit, viewModel: LockViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val biometricEnabled by viewModel.biometricEnabled.collectAsStateWithLifecycle()
    val activity = LocalActivity.current as? FragmentActivity
    BackHandler(onBack = onExit)

    val canUseBiometric = remember(activity) {
        activity != null && BiometricManager.from(activity)
            .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS
    }
    val title = stringResource(R.string.lock_biometric_title)
    val negative = stringResource(R.string.lock_enter_pin)
    fun showBiometric() {
        val host = activity ?: return
        val prompt = BiometricPrompt(
            host,
            ContextCompat.getMainExecutor(host),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    viewModel.onBiometricSuccess()
                }
            },
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setNegativeButtonText(negative)
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
            .build()
        prompt.authenticate(info)
    }
    LaunchedEffect(biometricEnabled) { if (biometricEnabled && canUseBiometric) showBiometric() }

    var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    LaunchedEffect(state.lockedUntil) {
        while (SystemClock.elapsedRealtime() < state.lockedUntil) {
            now = SystemClock.elapsedRealtime()
            delay(500)
        }
        now = SystemClock.elapsedRealtime()
    }
    val secondsLeft = ((state.lockedUntil - now) / 1000).toInt()

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            AppLogo(72.dp)
            Spacer(Modifier.height(20.dp))
            Text(stringResource(R.string.lock_enter_pin), style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                repeat(LockViewModel.PIN_LENGTH) { i ->
                    val filled = i < state.pin.length
                    Box(
                        Modifier
                            .size(16.dp)
                            .then(
                                if (filled) {
                                    Modifier.background(MaterialTheme.colorScheme.primary, CircleShape)
                                } else {
                                    Modifier.border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                },
                            ),
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = when {
                    secondsLeft > 0 -> stringResource(R.string.lock_too_many_attempts, secondsLeft)
                    state.wrong -> stringResource(R.string.lock_wrong_pin)
                    else -> ""
                },
                color = MaterialTheme.colorScheme.error,
            )
            Spacer(Modifier.height(24.dp))
            val rows = listOf("123", "456", "789")
            rows.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    row.forEach { digit -> Key(digit.toString()) { viewModel.onDigit(digit) } }
                }
                Spacer(Modifier.height(16.dp))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(72.dp), contentAlignment = Alignment.Center) {
                    if (biometricEnabled && canUseBiometric) {
                        IconButton(onClick = { showBiometric() }) {
                            Icon(Icons.Rounded.Fingerprint, stringResource(R.string.lock_use_biometric), Modifier.size(36.dp))
                        }
                    }
                }
                Key("0") { viewModel.onDigit('0') }
                Box(Modifier.size(72.dp), contentAlignment = Alignment.Center) {
                    IconButton(onClick = viewModel::onBackspace) {
                        Icon(Icons.AutoMirrored.Rounded.Backspace, stringResource(R.string.action_back))
                    }
                }
            }
        }
    }
}

@Composable
private fun Key(label: String, onClick: () -> Unit) {
    FilledTonalIconButton(onClick = onClick, modifier = Modifier.size(72.dp)) {
        Text(label, style = MaterialTheme.typography.headlineSmall)
    }
}

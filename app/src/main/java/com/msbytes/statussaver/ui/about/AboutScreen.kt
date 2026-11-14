package com.msbytes.statussaver.ui.about

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.msbytes.statussaver.BuildConfig
import com.msbytes.statussaver.R
import com.msbytes.statussaver.ads.LocalAdManager
import com.msbytes.statussaver.core.IntentUtils
import com.msbytes.statussaver.ui.components.AppLogo

/** Replace with the real support address before publishing. */
private const val SUPPORT_EMAIL = "support@example.com"

@Composable
fun AboutScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val adManager = LocalAdManager.current
    fun open(intent: android.content.Intent) {
        adManager?.suppressNextAppOpen()
        runCatching { context.startActivity(intent) }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_about)) },
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
                .navigationBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AppLogo(96.dp)
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text(stringResource(R.string.about_version, BuildConfig.VERSION_NAME), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(stringResource(R.string.about_developer, stringResource(R.string.developer_name)), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(20.dp))
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.disclaimer_title), style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(4.dp))
                    Text(stringResource(R.string.disclaimer), style = MaterialTheme.typography.bodySmall)
                }
            }
            Spacer(Modifier.height(20.dp))
            OutlinedButton(onClick = { open(IntentUtils.openUrl(BuildConfig.PRIVACY_POLICY_URL)) }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.settings_privacy_policy))
            }
            OutlinedButton(
                onClick = { open(IntentUtils.email(SUPPORT_EMAIL, context.getString(R.string.app_name))) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.about_contact)) }
        }
    }
}

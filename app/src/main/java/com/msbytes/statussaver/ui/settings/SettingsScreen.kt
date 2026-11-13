package com.msbytes.statussaver.ui.settings

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.AutoMode
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Password
import androidx.compose.material.icons.rounded.Policy
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.SmartDisplay
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.msbytes.statussaver.BuildConfig
import com.msbytes.statussaver.R
import com.msbytes.statussaver.core.Formatters
import com.msbytes.statussaver.core.IntentUtils
import com.msbytes.statussaver.domain.model.StatusSource
import com.msbytes.statussaver.domain.model.ThemeMode
import com.msbytes.statussaver.play.PlayStoreHelper
import com.msbytes.statussaver.ui.common.AppSnackbarHost
import com.msbytes.statussaver.ui.components.ConfirmDialog
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.launch

@EntryPoint
@InstallIn(SingletonComponent::class)
interface PlayStoreEntryPoint {
    fun playStoreHelper(): PlayStoreHelper
}

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenFaq: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val storage by viewModel.storage.collectAsStateWithLifecycle()
    val access by viewModel.access.collectAsStateWithLifecycle()
    val adsEnabled by viewModel.adManager.adsEnabled.collectAsStateWithLifecycle()
    val privacyOptionsRequired by viewModel.consentManager.privacyOptionsRequired.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val activity = LocalActivity.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val playStore = remember { EntryPointAccessors.fromApplication(context.applicationContext, PlayStoreEntryPoint::class.java).playStoreHelper() }

    var themeDialog by remember { mutableStateOf(false) }
    var pinDialog by remember { mutableStateOf(false) }
    var notifRationale by remember { mutableStateOf(false) }
    var notifDenied by remember { mutableStateOf(false) }
    var pickingSource by remember { mutableStateOf<StatusSource?>(null) }
    var revokeSource by remember { mutableStateOf<StatusSource?>(null) }

    fun message(res: Int) = scope.launch { snackbar.showSnackbar(context.getString(res)) }

    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            viewModel.setNotifications(true)
        } else if (activity != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.POST_NOTIFICATIONS)
        ) {
            notifDenied = true // "Don't ask again" → offer app settings
        }
    }
    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        val source = pickingSource ?: return@rememberLauncherForActivityResult
        pickingSource = null
        if (uri != null) {
            message(if (viewModel.grantAccess(source, uri)) R.string.permission_granted else R.string.permission_wrong_folder_body)
        }
    }

    fun enableNotifications() {
        val needsPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        if (needsPermission) notifRationale = true else viewModel.setNotifications(true)
    }

    val biometricAvailable = remember {
        BiometricManager.from(context).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) ==
            BiometricManager.BIOMETRIC_SUCCESS
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.cd_back)) }
                },
            )
        },
        snackbarHost = { AppSnackbarHost(snackbar) },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding(),
        ) {
            Section(R.string.settings_appearance)
            Item(Icons.Rounded.DarkMode, stringResource(R.string.settings_theme), themeLabel(settings.themeMode)) { themeDialog = true }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                SwitchItem(
                    Icons.Rounded.Palette,
                    stringResource(R.string.settings_dynamic_color),
                    stringResource(R.string.settings_dynamic_color_desc),
                    settings.dynamicColor,
                    viewModel::setDynamicColor,
                )
            }

            Section(R.string.settings_folder_access)
            StatusSource.entries.forEach { source ->
                val granted = access[source] == true
                Item(
                    Icons.Rounded.Folder,
                    source.displayName,
                    stringResource(if (granted) R.string.folder_access_granted else R.string.folder_access_not_granted),
                ) {
                    if (granted) {
                        revokeSource = source
                    } else {
                        pickingSource = source
                        folderPicker.launch(viewModel.initialFolderUri(source))
                    }
                }
            }

            Section(R.string.settings_auto_save)
            SwitchItem(
                Icons.Rounded.AutoMode,
                stringResource(R.string.settings_auto_save_whatsapp),
                stringResource(R.string.settings_auto_save_desc),
                settings.autoSaveWhatsApp,
            ) { viewModel.setAutoSave(StatusSource.WHATSAPP, it) }
            SwitchItem(
                Icons.Rounded.AutoMode,
                stringResource(R.string.settings_auto_save_business),
                stringResource(R.string.settings_auto_save_desc),
                settings.autoSaveBusiness,
            ) { viewModel.setAutoSave(StatusSource.BUSINESS, it) }

            Section(R.string.settings_notifications)
            SwitchItem(
                Icons.Rounded.Notifications,
                stringResource(R.string.settings_notify_new),
                stringResource(R.string.settings_notify_new_desc),
                settings.notifyNewStatuses,
            ) { if (it) enableNotifications() else viewModel.setNotifications(false) }

            Section(R.string.settings_privacy_security)
            SwitchItem(
                Icons.Rounded.Lock,
                stringResource(R.string.settings_app_lock),
                stringResource(R.string.settings_app_lock_desc),
                settings.appLockEnabled,
            ) { enable ->
                if (enable) {
                    pinDialog = true
                } else {
                    viewModel.setPin(null)
                    message(R.string.lock_disabled)
                }
            }
            if (settings.appLockEnabled) {
                Item(Icons.Rounded.Password, stringResource(R.string.settings_change_pin), null) { pinDialog = true }
                if (biometricAvailable) {
                    SwitchItem(Icons.Rounded.Fingerprint, stringResource(R.string.settings_biometric), null, settings.biometricEnabled, viewModel::setBiometric)
                }
            }

            Section(R.string.settings_storage)
            Item(
                Icons.Rounded.Storage,
                stringResource(R.string.settings_storage_usage),
                stringResource(R.string.settings_storage_summary, Formatters.size(storage.bytes), storage.images, storage.videos),
                onClick = null,
            )

            if (adsEnabled || privacyOptionsRequired) {
                Section(R.string.settings_ads)
                if (adsEnabled && activity != null) {
                    val now = System.currentTimeMillis()
                    Item(
                        Icons.Rounded.SmartDisplay,
                        stringResource(R.string.settings_remove_interstitials),
                        if (settings.adFreeUntil > now) {
                            stringResource(R.string.settings_ad_free_active, Formatters.time(settings.adFreeUntil))
                        } else {
                            null
                        },
                    ) {
                        viewModel.adManager.showRewarded(activity) { rewarded ->
                            message(if (rewarded) R.string.ad_rewarded_granted else R.string.ad_rewarded_unavailable)
                        }
                    }
                }
                if (privacyOptionsRequired && activity != null) {
                    Item(
                        Icons.Rounded.PrivacyTip,
                        stringResource(R.string.settings_privacy_options),
                        stringResource(R.string.settings_privacy_options_desc),
                    ) { viewModel.consentManager.showPrivacyOptionsForm(activity) }
                }
            }

            Section(R.string.settings_support)
            Item(Icons.Rounded.Policy, stringResource(R.string.settings_privacy_policy), null) {
                viewModel.adManager.suppressNextAppOpen()
                runCatching { context.startActivity(IntentUtils.openUrl(BuildConfig.PRIVACY_POLICY_URL)) }
            }
            Item(Icons.AutoMirrored.Rounded.HelpOutline, stringResource(R.string.settings_how_to_use), null, onClick = onOpenFaq)
            Item(Icons.Rounded.Star, stringResource(R.string.settings_rate), null) {
                viewModel.adManager.suppressNextAppOpen()
                activity?.let { playStore.openStorePage(it) }
            }
            Item(Icons.Rounded.Share, stringResource(R.string.settings_share), null) {
                viewModel.adManager.suppressNextAppOpen()
                val send = Intent(Intent.ACTION_SEND)
                    .setType("text/plain")
                    .putExtra(Intent.EXTRA_TEXT, context.getString(R.string.share_app_text, playStore.storeUrl))
                runCatching { context.startActivity(Intent.createChooser(send, context.getString(R.string.share_chooser_title))) }
            }
            Item(Icons.Rounded.SystemUpdate, stringResource(R.string.settings_check_updates), null) {
                if (activity != null) {
                    scope.launch { if (!playStore.checkForUpdate(activity)) message(R.string.update_none) }
                }
            }
            Item(Icons.Rounded.Info, stringResource(R.string.settings_about), null, onClick = onOpenAbout)
        }
    }

    if (themeDialog) {
        AlertDialog(
            onDismissRequest = { themeDialog = false },
            title = { Text(stringResource(R.string.settings_theme)) },
            text = {
                Column {
                    ThemeMode.entries.forEach { mode ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .selectable(selected = settings.themeMode == mode) {
                                    viewModel.setTheme(mode)
                                    themeDialog = false
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = settings.themeMode == mode, onClick = null)
                            Text(themeLabel(mode), Modifier.padding(start = 12.dp))
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { themeDialog = false }) { Text(stringResource(R.string.action_close)) } },
        )
    }
    if (pinDialog) {
        PinSetupDialog(onDismiss = { pinDialog = false }) { pin ->
            viewModel.setPin(pin)
            pinDialog = false
            message(R.string.lock_pin_set)
        }
    }
    if (notifRationale) {
        ConfirmDialog(
            title = stringResource(R.string.permission_notifications_title),
            text = stringResource(R.string.permission_notifications_body),
            confirmLabel = stringResource(R.string.action_allow),
            onConfirm = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            },
            onDismiss = { notifRationale = false },
        )
    }
    if (notifDenied) {
        ConfirmDialog(
            title = stringResource(R.string.permission_notifications_title),
            text = stringResource(R.string.permission_denied_permanently),
            confirmLabel = stringResource(R.string.open_app_settings),
            onConfirm = { runCatching { context.startActivity(IntentUtils.appSettings(context)) } },
            onDismiss = { notifDenied = false },
        )
    }
    revokeSource?.let { source ->
        ConfirmDialog(
            title = stringResource(R.string.folder_access_revoke),
            text = stringResource(R.string.folder_access_revoke_body, source.displayName),
            confirmLabel = stringResource(R.string.folder_access_revoke),
            onConfirm = { viewModel.revokeAccess(source) },
            onDismiss = { revokeSource = null },
        )
    }
}

@Composable
private fun themeLabel(mode: ThemeMode): String = stringResource(
    when (mode) {
        ThemeMode.SYSTEM -> R.string.theme_system
        ThemeMode.LIGHT -> R.string.theme_light
        ThemeMode.DARK -> R.string.theme_dark
    },
)

@Composable
private fun Section(title: Int) {
    Text(
        stringResource(title),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 4.dp),
    )
}

@Composable
private fun Item(icon: ImageVector, title: String, subtitle: String?, onClick: (() -> Unit)?) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = subtitle?.let { { Text(it) } },
        leadingContent = { Icon(icon, null) },
        modifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier,
    )
}

@Composable
private fun SwitchItem(icon: ImageVector, title: String, subtitle: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = subtitle?.let { { Text(it) } },
        leadingContent = { Icon(icon, null) },
        trailingContent = { Switch(checked = checked, onCheckedChange = null) },
        modifier = Modifier.clickable { onChange(!checked) },
    )
}

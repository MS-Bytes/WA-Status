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
import androidx.compose.material.icons.rounded.Bolt
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
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.VolunteerActivism
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
import com.msbytes.statussaver.ads.LocalAdManager
import com.msbytes.statussaver.core.Formatters
import com.msbytes.statussaver.core.IntentUtils
import com.msbytes.statussaver.domain.model.ThemeMode
import com.msbytes.statussaver.ui.common.AppSnackbarHost
import com.msbytes.statussaver.ui.components.ConfirmDialog
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenFaq: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val storage by viewModel.storage.collectAsStateWithLifecycle()
    val hasAccess by viewModel.hasAccess.collectAsStateWithLifecycle()
    val privacyOptionsRequired by viewModel.consentManager.privacyOptionsRequired.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val activity = LocalActivity.current
    val adManager = LocalAdManager.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val playStore = viewModel.playStoreHelper

    var themeDialog by remember { mutableStateOf(false) }
    var pinDialog by remember { mutableStateOf(false) }
    var donateDialog by remember { mutableStateOf(false) }
    var notifRationale by remember { mutableStateOf(false) }
    var notifDenied by remember { mutableStateOf(false) }
    var revokeDialog by remember { mutableStateOf(false) }

    fun message(res: Int) = scope.launch { snackbar.showSnackbar(context.getString(res)) }
    fun openExternal(intent: Intent) {
        adManager?.suppressNextAppOpen()
        runCatching { context.startActivity(intent) }
    }

    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            viewModel.setNotifications(true)
        } else if (activity != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.POST_NOTIFICATIONS)
        ) {
            notifDenied = true // "Don't ask again" → offer app settings
        }
    }
    val batteryExempt = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {}
    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            message(if (viewModel.grantAccess(uri)) R.string.permission_granted else R.string.permission_wrong_folder_body)
        }
    }

    fun enableNotifications() {
        val needsPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        if (needsPermission) notifRationale = true else viewModel.setNotifications(true)
    }

    fun enableLiveDetection() {
        viewModel.setLiveDetection(true)
        enableNotifications()
        if (!IntentUtils.isIgnoringBatteryOptimizations(context)) {
            runCatching { batteryExempt.launch(IntentUtils.requestIgnoreBatteryOptimizations(context)) }
        }
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
                SwitchItem(Icons.Rounded.Palette, stringResource(R.string.settings_dynamic_color), settings.dynamicColor, onChange = viewModel::setDynamicColor)
            }

            Section(R.string.settings_status_access)
            Item(
                Icons.Rounded.Folder,
                stringResource(R.string.settings_folder_access),
                stringResource(if (hasAccess) R.string.folder_access_granted else R.string.folder_access_not_granted),
            ) {
                if (hasAccess) revokeDialog = true else folderPicker.launch(viewModel.initialFolderUri())
            }

            Section(R.string.settings_automation)
            SwitchItem(Icons.Rounded.AutoMode, stringResource(R.string.settings_auto_save), settings.autoSave, onChange = viewModel::setAutoSave)
            SwitchItem(
                Icons.Rounded.Notifications,
                stringResource(R.string.settings_notify_new),
                settings.notifyNewStatuses,
                subtitle = stringResource(R.string.settings_notify_new_desc),
            ) { if (it) enableNotifications() else viewModel.setNotifications(false) }
            SwitchItem(
                Icons.Rounded.Bolt,
                stringResource(R.string.settings_live_detection),
                settings.liveDetection,
                subtitle = stringResource(R.string.settings_live_detection_desc),
            ) { if (it) enableLiveDetection() else viewModel.setLiveDetection(false) }

            Section(R.string.settings_privacy_security)
            SwitchItem(Icons.Rounded.Lock, stringResource(R.string.settings_app_lock), settings.appLockEnabled) { enable ->
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
                    SwitchItem(Icons.Rounded.Fingerprint, stringResource(R.string.settings_biometric), settings.biometricEnabled, onChange = viewModel::setBiometric)
                }
            }
            // Required by Google's consent rules in regions where it applies (EEA, UK, ...).
            if (privacyOptionsRequired && activity != null) {
                Item(Icons.Rounded.PrivacyTip, stringResource(R.string.settings_privacy_choices), null) {
                    viewModel.consentManager.showPrivacyOptionsForm(activity)
                }
            }
            Item(Icons.Rounded.Policy, stringResource(R.string.settings_privacy_policy), null) {
                openExternal(IntentUtils.openUrl(BuildConfig.PRIVACY_POLICY_URL))
            }

            Section(R.string.settings_storage)
            Item(
                Icons.Rounded.Storage,
                stringResource(R.string.settings_storage_usage),
                stringResource(R.string.settings_storage_summary, Formatters.size(storage.bytes), storage.images, storage.videos),
                onClick = null,
            )

            Section(R.string.settings_support)
            if (BuildConfig.DONATE_ENABLED) {
                Item(Icons.Rounded.VolunteerActivism, stringResource(R.string.donate_title), stringResource(R.string.donate_subtitle)) {
                    donateDialog = true
                }
            }
            Item(Icons.AutoMirrored.Rounded.HelpOutline, stringResource(R.string.settings_faq), null, onClick = onOpenFaq)
            Item(Icons.Rounded.Star, stringResource(R.string.settings_rate), null) {
                adManager?.suppressNextAppOpen()
                activity?.let { playStore.openStorePage(it) }
            }
            Item(Icons.Rounded.Share, stringResource(R.string.settings_share), null) {
                val send = Intent(Intent.ACTION_SEND)
                    .setType("text/plain")
                    .putExtra(Intent.EXTRA_TEXT, context.getString(R.string.share_app_text, playStore.storeUrl))
                openExternal(Intent.createChooser(send, context.getString(R.string.share_chooser_title)))
            }
            Item(Icons.Rounded.SystemUpdate, stringResource(R.string.settings_check_updates), null) {
                if (activity != null) scope.launch { if (!playStore.checkForUpdate(activity)) message(R.string.update_none) }
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
    if (donateDialog) DonateDialog(onDismiss = { donateDialog = false })
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
    if (revokeDialog) {
        ConfirmDialog(
            title = stringResource(R.string.folder_access_revoke),
            text = stringResource(R.string.folder_access_revoke_body),
            confirmLabel = stringResource(R.string.folder_access_revoke),
            onConfirm = viewModel::revokeAccess,
            onDismiss = { revokeDialog = false },
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
private fun SwitchItem(
    icon: ImageVector,
    title: String,
    checked: Boolean,
    subtitle: String? = null,
    onChange: (Boolean) -> Unit,
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = subtitle?.let { { Text(it) } },
        leadingContent = { Icon(icon, null) },
        trailingContent = { Switch(checked = checked, onCheckedChange = null) },
        modifier = Modifier.clickable { onChange(!checked) },
    )
}

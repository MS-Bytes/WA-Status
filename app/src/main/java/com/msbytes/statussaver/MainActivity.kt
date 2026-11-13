package com.msbytes.statussaver

import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.activity.compose.setContent
import com.msbytes.statussaver.ads.AdManager
import com.msbytes.statussaver.ads.ConsentManager
import com.msbytes.statussaver.ads.LocalAdManager
import com.msbytes.statussaver.play.PlayStoreHelper
import com.msbytes.statussaver.ui.applock.LockScreen
import com.msbytes.statussaver.ui.navigation.AppNavHost
import com.msbytes.statussaver.ui.theme.StatusSaverTheme
import com.msbytes.statussaver.ui.theme.isDarkTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Single activity. Extends FragmentActivity because BiometricPrompt (App Lock) needs it.
 */
@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject lateinit var adManager: AdManager
    @Inject lateinit var consentManager: ConsentManager
    @Inject lateinit var playStoreHelper: PlayStoreHelper

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        splash.setKeepOnScreenCondition { viewModel.settings.value == null }

        // UMP consent is refreshed on every launch, before any ad is requested.
        consentManager.gatherConsent(this) { adManager.initializeIfAllowed() }

        if (savedInstanceState == null) {
            lifecycleScope.launch { playStoreHelper.checkForUpdate(this@MainActivity) }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                playStoreHelper.reviewRequests.collect { playStoreHelper.launchReview(this@MainActivity) }
            }
        }

        setContent {
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            val current = settings ?: return@setContent
            val locked by viewModel.locked.collectAsStateWithLifecycle()
            val dark = isDarkTheme(current.themeMode)
            val showOnboarding = remember { !current.onboardingDone }

            LaunchedEffect(dark) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark },
                    navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark },
                )
            }

            StatusSaverTheme(darkTheme = dark, dynamicColor = current.dynamicColor) {
                CompositionLocalProvider(LocalAdManager provides adManager) {
                    Box {
                        AppNavHost(
                            showOnboarding = showOnboarding,
                            locked = locked,
                            onOnboardingDone = viewModel::completeOnboarding,
                        )
                        if (locked) LockScreen(onExit = { finish() })
                    }
                    UpdateDownloadedDialog()
                }
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun UpdateDownloadedDialog() {
        val downloaded by playStoreHelper.updateDownloaded.collectAsStateWithLifecycle()
        var dismissed by remember { mutableStateOf(false) }
        if (downloaded && !dismissed) {
            AlertDialog(
                onDismissRequest = { dismissed = true },
                text = { Text(stringResource(R.string.update_downloaded)) },
                confirmButton = {
                    TextButton(onClick = { playStoreHelper.completeUpdate() }) { Text(stringResource(R.string.action_restart)) }
                },
                dismissButton = {
                    TextButton(onClick = { dismissed = true }) { Text(stringResource(R.string.action_cancel)) }
                },
            )
        }
    }
}

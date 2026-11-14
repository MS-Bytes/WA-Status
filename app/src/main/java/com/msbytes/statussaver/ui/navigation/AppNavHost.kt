package com.msbytes.statussaver.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.msbytes.statussaver.BuildConfig
import com.msbytes.statussaver.ads.LocalAdManager
import com.msbytes.statussaver.core.IntentUtils
import com.msbytes.statussaver.ui.about.AboutScreen
import com.msbytes.statussaver.ui.editor.ImageEditorScreen
import com.msbytes.statussaver.ui.editor.VideoEditorScreen
import com.msbytes.statussaver.ui.faq.FaqScreen
import com.msbytes.statussaver.ui.home.HomeScreen
import com.msbytes.statussaver.ui.onboarding.OnboardingScreen
import com.msbytes.statussaver.ui.settings.SettingsScreen
import com.msbytes.statussaver.ui.viewer.ViewerScreen

@Composable
fun AppNavHost(showOnboarding: Boolean, locked: Boolean, onOnboardingDone: () -> Unit) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val adManager = LocalAdManager.current

    // Full-screen ads are only allowed on content screens (never onboarding, editor or lock).
    val entry by navController.currentBackStackEntryAsState()
    val destination = entry?.destination
    val adsAllowedHere = destination != null && !locked &&
        !destination.hasRoute<OnboardingRoute>() && !destination.hasRoute<EditorRoute>()
    LaunchedEffect(adsAllowedHere) { adManager?.fullScreenAllowed = adsAllowedHere }

    NavHost(
        navController = navController,
        startDestination = if (showOnboarding) OnboardingRoute else HomeRoute,
        enterTransition = { fadeIn(tween(220)) + slideInHorizontally(tween(220)) { it / 12 } },
        exitTransition = { fadeOut(tween(180)) },
        popEnterTransition = { fadeIn(tween(220)) },
        popExitTransition = { fadeOut(tween(180)) + slideOutHorizontally(tween(220)) { it / 12 } },
    ) {
        composable<OnboardingRoute> {
            OnboardingScreen(
                onFinish = {
                    onOnboardingDone()
                    navController.navigate(HomeRoute) { popUpTo<OnboardingRoute> { inclusive = true } }
                },
                onOpenPrivacyPolicy = {
                    adManager?.suppressNextAppOpen()
                    runCatching { context.startActivity(IntentUtils.openUrl(BuildConfig.PRIVACY_POLICY_URL)) }
                },
            )
        }
        composable<HomeRoute> {
            HomeScreen(
                onOpenViewer = { collection, id -> navController.navigate(ViewerRoute(collection, id)) },
                onOpenSettings = { navController.navigate(SettingsRoute) },
                onOpenFaq = { navController.navigate(FaqRoute) },
            )
        }
        composable<ViewerRoute> {
            ViewerScreen(
                onBack = { navController.popBackStack() },
                onEdit = { item -> navController.navigate(EditorRoute(item.uri.toString(), item.isVideo, item.name)) },
            )
        }
        composable<EditorRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<EditorRoute>()
            if (route.isVideo) {
                VideoEditorScreen(onClose = { navController.popBackStack() })
            } else {
                ImageEditorScreen(onClose = { navController.popBackStack() })
            }
        }
        composable<SettingsRoute> {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onOpenAbout = { navController.navigate(AboutRoute) },
                onOpenFaq = { navController.navigate(FaqRoute) },
            )
        }
        composable<AboutRoute> { AboutScreen(onBack = { navController.popBackStack() }) }
        composable<FaqRoute> { FaqScreen(onBack = { navController.popBackStack() }) }
    }
}

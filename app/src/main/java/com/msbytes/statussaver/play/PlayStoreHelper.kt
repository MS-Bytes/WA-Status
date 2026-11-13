package com.msbytes.statussaver.play

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.review.ReviewManagerFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/** Google Play In-App Review, In-App Updates (flexible) and store links. */
@Singleton
class PlayStoreHelper @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val updateManager by lazy { AppUpdateManagerFactory.create(context) }

    private val _updateDownloaded = MutableStateFlow(false)
    val updateDownloaded: StateFlow<Boolean> = _updateDownloaded.asStateFlow()

    private val _reviewRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val reviewRequests: SharedFlow<Unit> = _reviewRequests.asSharedFlow()

    private val installListener = InstallStateUpdatedListener { state ->
        if (state.installStatus() == InstallStatus.DOWNLOADED) _updateDownloaded.value = true
    }

    val storeUrl: String get() = "https://play.google.com/store/apps/details?id=${context.packageName}"

    /** Asks the activity to show the review flow (called after the 5th save). */
    fun requestReview() {
        _reviewRequests.tryEmit(Unit)
    }

    suspend fun launchReview(activity: Activity) {
        runCatching {
            val manager = ReviewManagerFactory.create(activity)
            val info = manager.requestReviewFlow().await()
            manager.launchReviewFlow(activity, info).await()
        }
    }

    /** Starts a flexible update when one is available. Returns false if none. */
    suspend fun checkForUpdate(activity: Activity): Boolean = runCatching {
        val info = updateManager.appUpdateInfo.await()
        if (info.installStatus() == InstallStatus.DOWNLOADED) {
            _updateDownloaded.value = true
            return@runCatching true
        }
        val available = info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
            info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
        if (available) {
            updateManager.registerListener(installListener)
            updateManager.startUpdateFlow(info, activity, AppUpdateOptions.defaultOptions(AppUpdateType.FLEXIBLE))
        }
        available
    }.getOrDefault(false)

    fun completeUpdate() {
        runCatching { updateManager.completeUpdate() }
    }

    fun openStorePage(activity: Activity) {
        val market = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${context.packageName}"))
        try {
            activity.startActivity(market)
        } catch (_: ActivityNotFoundException) {
            runCatching { activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(storeUrl))) }
        }
    }
}

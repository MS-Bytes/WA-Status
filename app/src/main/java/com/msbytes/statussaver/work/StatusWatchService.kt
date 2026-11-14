package com.msbytes.statussaver.work

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import com.msbytes.statussaver.notifications.Notifier
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Keeps WAVAULT watching the status folders while the app is closed, so a status
 * WhatsApp downloads in the background shows up (and notifies) within seconds —
 * without the user opening WhatsApp. Started only when "Instant Detection" is on.
 */
@AndroidEntryPoint
class StatusWatchService : Service() {

    @Inject lateinit var runner: StatusSyncRunner
    @Inject lateinit var notifier: Notifier

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var loop: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        } else {
            0
        }
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notifier.buildWatchNotification(), type)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (loop?.isActive != true) {
            loop = scope.launch {
                while (isActive) {
                    runCatching { runner.sync() }
                    delay(POLL_INTERVAL_MS)
                }
            }
        }
        return START_STICKY // restart if the system kills it
    }

    // Android 14+ limits data-sync services; stop cleanly, the 15-min worker still covers.
    override fun onTimeout(startId: Int) = stopSelf()
    override fun onTimeout(startId: Int, fgsType: Int) = stopSelf()

    override fun onDestroy() {
        loop?.cancel()
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val NOTIFICATION_ID = 2001
        private const val POLL_INTERVAL_MS = 7_000L
    }
}

package com.msbytes.statussaver.work

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Starts / stops [StatusWatchService]. Must be called from a foreground context or on boot. */
@Singleton
class StatusWatchController @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun sync(enabled: Boolean) {
        val intent = Intent(context, StatusWatchService::class.java)
        if (enabled) {
            runCatching { ContextCompat.startForegroundService(context, intent) }
        } else {
            runCatching { context.stopService(intent) }
        }
    }
}

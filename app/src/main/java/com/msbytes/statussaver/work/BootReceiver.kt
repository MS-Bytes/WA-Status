package com.msbytes.statussaver.work

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.msbytes.statussaver.domain.repository.SettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Restarts the live watch service after a reboot if the user enabled it. */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var controller: StatusWatchController

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_LOCKED_BOOT_COMPLETED
        ) {
            return
        }
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (settingsRepository.settings.first().liveDetection) controller.sync(true)
            } finally {
                pending.finish()
            }
        }
    }
}

package com.msbytes.statussaver.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.msbytes.statussaver.domain.model.SaveResult
import com.msbytes.statussaver.domain.repository.SavedRepository
import com.msbytes.statussaver.domain.repository.SettingsRepository
import com.msbytes.statussaver.domain.repository.StatusRepository
import com.msbytes.statussaver.notifications.Notifier
import com.msbytes.statussaver.widget.WidgetUpdater
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

/**
 * Runs every ~15 minutes: detects status files WhatsApp has downloaded in the background,
 * notifies the user and optionally auto-saves. Nothing leaves the device.
 */
@HiltWorker
class StatusCheckWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val statusRepository: StatusRepository,
    private val savedRepository: SavedRepository,
    private val settingsRepository: SettingsRepository,
    private val notifier: Notifier,
    private val widgetUpdater: WidgetUpdater,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (!statusRepository.hasAccess()) return Result.success()
        val settings = settingsRepository.settings.first()
        val items = statusRepository.refresh().getOrNull() ?: return Result.success()

        var autoSaved = 0
        if (settings.autoSave) {
            items.forEach { if (savedRepository.save(it) is SaveResult.Saved) autoSaved++ }
        }

        val lastNotified = settingsRepository.lastNotified()
        val fresh = items.filter { it.lastModified > lastNotified }
        if (fresh.isNotEmpty()) {
            settingsRepository.setLastNotified(fresh.maxOf { it.lastModified })
            // First run only sets the baseline, so the user isn't spammed with old statuses.
            if (settings.notifyNewStatuses && lastNotified > 0) notifier.showNewStatuses(fresh.size)
        }
        if (autoSaved > 0) notifier.showAutoSaved(autoSaved)
        widgetUpdater.update()
        return Result.success()
    }
}

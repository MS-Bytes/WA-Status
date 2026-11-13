package com.msbytes.statussaver.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.msbytes.statussaver.domain.model.SaveResult
import com.msbytes.statussaver.domain.model.StatusSource
import com.msbytes.statussaver.domain.repository.SavedRepository
import com.msbytes.statussaver.domain.repository.SettingsRepository
import com.msbytes.statussaver.domain.repository.StatusRepository
import com.msbytes.statussaver.notifications.Notifier
import com.msbytes.statussaver.widget.WidgetUpdater
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

/**
 * Runs every ~15 minutes when auto-save or new-status alerts are enabled.
 * Only reads the folders the user already granted; nothing leaves the device.
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
        val settings = settingsRepository.settings.first()
        var newCount = 0
        var autoSaved = 0

        for (source in StatusSource.entries) {
            if (!statusRepository.hasAccess(source)) continue
            val items = statusRepository.refresh(source).getOrNull() ?: continue

            if (settings.autoSave(source)) {
                items.forEach { if (savedRepository.save(it) is SaveResult.Saved) autoSaved++ }
            }

            val lastNotified = settingsRepository.lastNotified(source)
            val fresh = items.filter { it.lastModified > lastNotified }
            if (fresh.isNotEmpty()) {
                newCount += fresh.size
                settingsRepository.setLastNotified(source, fresh.maxOf { it.lastModified })
            }
        }

        if (settings.notifyNewStatuses && newCount > 0) notifier.showNewStatuses(newCount)
        if (autoSaved > 0) notifier.showAutoSaved(autoSaved)
        widgetUpdater.update()
        return Result.success()
    }
}

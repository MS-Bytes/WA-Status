package com.msbytes.statussaver.work

import com.msbytes.statussaver.domain.model.SaveResult
import com.msbytes.statussaver.domain.repository.SavedRepository
import com.msbytes.statussaver.domain.repository.SettingsRepository
import com.msbytes.statussaver.domain.repository.StatusRepository
import com.msbytes.statussaver.notifications.Notifier
import com.msbytes.statussaver.widget.WidgetUpdater
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * One scan cycle shared by the periodic [StatusCheckWorker] and the live
 * [StatusWatchService]: refreshes the status list (which updates the in-app UI too),
 * optionally auto-saves, and notifies about statuses WhatsApp has newly downloaded.
 * Nothing leaves the device.
 */
@Singleton
class StatusSyncRunner @Inject constructor(
    private val statusRepository: StatusRepository,
    private val savedRepository: SavedRepository,
    private val settingsRepository: SettingsRepository,
    private val notifier: Notifier,
    private val widgetUpdater: WidgetUpdater,
) {
    suspend fun sync() {
        if (!statusRepository.hasAccess()) return
        val settings = settingsRepository.settings.first()
        val items = statusRepository.refresh().getOrNull() ?: return

        var autoSaved = 0
        if (settings.autoSave) {
            items.forEach { if (savedRepository.save(it) is SaveResult.Saved) autoSaved++ }
        }

        val lastNotified = settingsRepository.lastNotified()
        val fresh = items.filter { it.lastModified > lastNotified }
        if (fresh.isNotEmpty()) {
            settingsRepository.setLastNotified(fresh.maxOf { it.lastModified })
            // The very first scan only sets the baseline, so old statuses aren't announced.
            if (settings.notifyNewStatuses && lastNotified > 0) notifier.showNewStatuses(fresh.size)
        }
        if (autoSaved > 0) notifier.showAutoSaved(autoSaved)
        widgetUpdater.update()
    }
}

package com.msbytes.statussaver.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import com.msbytes.statussaver.domain.repository.SettingsRepository
import com.msbytes.statussaver.domain.repository.StatusRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Recomputes "new since last visit" and refreshes the widget. */
@Singleton
class WidgetUpdater @Inject constructor(
    @ApplicationContext private val context: Context,
    private val statusRepository: StatusRepository,
    private val settings: SettingsRepository,
) {
    suspend fun update() {
        val lastSeen = settings.lastSeen()
        val count = statusRepository.statuses.value.orEmpty().count { it.lastModified > lastSeen }
        settings.setWidgetNewCount(count)
        runCatching { StatusWidget().updateAll(context) }
    }
}

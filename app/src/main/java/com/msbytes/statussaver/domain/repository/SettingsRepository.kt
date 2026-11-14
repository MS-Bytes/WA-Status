package com.msbytes.statussaver.domain.repository

import com.msbytes.statussaver.domain.model.AppSettings
import com.msbytes.statussaver.domain.model.SortOrder
import com.msbytes.statussaver.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val settings: Flow<AppSettings>
    val widgetNewCount: Flow<Int>

    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setDynamicColor(enabled: Boolean)
    suspend fun setOnboardingDone()
    suspend fun setAutoSave(enabled: Boolean)
    suspend fun setNotifyNewStatuses(enabled: Boolean)
    suspend fun setLiveDetection(enabled: Boolean)
    suspend fun setNotificationPromptShown()
    suspend fun setSortOrder(order: SortOrder)
    suspend fun setAdFreeUntil(epochMillis: Long)
    suspend fun setBiometricEnabled(enabled: Boolean)
    suspend fun setWidgetNewCount(count: Int)

    /** Time the user last looked at statuses (drives the NEW badge). */
    suspend fun lastSeen(): Long
    suspend fun setLastSeen(epochMillis: Long)

    /** Newest status time already reported by the background checker. */
    suspend fun lastNotified(): Long
    suspend fun setLastNotified(epochMillis: Long)

    /** Returns the new total number of successful saves (used for the review prompt). */
    suspend fun incrementSaveCount(): Int

    /** App Lock. A null pin disables the lock. */
    suspend fun setPin(pin: String?)
    suspend fun verifyPin(pin: String): Boolean
}

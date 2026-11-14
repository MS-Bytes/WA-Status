package com.msbytes.statussaver.domain.model

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val onboardingDone: Boolean = false,
    val autoSave: Boolean = false,
    /** On by default so new statuses are detected in the background. */
    val notifyNewStatuses: Boolean = true,
    /** Foreground service that detects new statuses instantly in the background. */
    val liveDetection: Boolean = false,
    val notificationPromptShown: Boolean = false,
    val appLockEnabled: Boolean = false,
    val biometricEnabled: Boolean = false,
    val sortOrder: SortOrder = SortOrder.NEWEST,
    /** Epoch millis until which full-screen ads are paused (rewarded ad). */
    val adFreeUntil: Long = 0L,
) {
    val backgroundWorkNeeded: Boolean get() = autoSave || notifyNewStatuses
}

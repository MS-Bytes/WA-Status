package com.msbytes.statussaver.domain.model

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val onboardingDone: Boolean = false,
    val autoSaveWhatsApp: Boolean = false,
    val autoSaveBusiness: Boolean = false,
    val notifyNewStatuses: Boolean = false,
    val appLockEnabled: Boolean = false,
    val biometricEnabled: Boolean = false,
    val sortOrder: SortOrder = SortOrder.NEWEST,
    /** Epoch millis until which full-screen ads are paused (rewarded ad). */
    val adFreeUntil: Long = 0L,
) {
    fun autoSave(source: StatusSource): Boolean = when (source) {
        StatusSource.WHATSAPP -> autoSaveWhatsApp
        StatusSource.BUSINESS -> autoSaveBusiness
    }

    val backgroundWorkNeeded: Boolean
        get() = autoSaveWhatsApp || autoSaveBusiness || notifyNewStatuses
}

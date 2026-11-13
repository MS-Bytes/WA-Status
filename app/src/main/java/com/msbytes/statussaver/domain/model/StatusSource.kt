package com.msbytes.statussaver.domain.model

/**
 * The two supported messenger apps and the location of their cached status folders
 * (relative to the root of primary shared storage).
 */
enum class StatusSource(
    val packageName: String,
    val displayName: String,
    val folderPaths: List<String>,
) {
    WHATSAPP(
        packageName = "com.whatsapp",
        displayName = "WhatsApp",
        folderPaths = listOf(
            "Android/media/com.whatsapp/WhatsApp/Media/.Statuses",
            "WhatsApp/Media/.Statuses", // legacy location (older WhatsApp builds on Android 10)
        ),
    ),
    BUSINESS(
        packageName = "com.whatsapp.w4b",
        displayName = "WhatsApp Business",
        folderPaths = listOf(
            "Android/media/com.whatsapp.w4b/WhatsApp Business/Media/.Statuses",
            "WhatsApp Business/Media/.Statuses",
        ),
    ),
}

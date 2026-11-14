package com.msbytes.statussaver.domain.model

/**
 * Supported messenger apps and their cached status folders (relative to the storage root).
 * WAVAULT scans all of them together — the user never has to pick an app.
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
            "WhatsApp/Media/.Statuses", // legacy location (old WhatsApp builds)
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

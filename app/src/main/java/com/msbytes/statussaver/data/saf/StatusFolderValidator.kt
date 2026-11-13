package com.msbytes.statussaver.data.saf

import com.msbytes.statussaver.domain.model.MediaType
import com.msbytes.statussaver.domain.model.StatusSource

/** Pure helpers (no Android dependencies) so they can be unit-tested on the JVM. */
object StatusFolderValidator {

    private val imageExtensions = setOf("jpg", "jpeg", "png", "webp", "gif")
    private val videoExtensions = setOf("mp4", "3gp")

    /**
     * @param treeDocumentId e.g. `primary:Android/media/com.whatsapp/WhatsApp/Media/.Statuses`
     */
    fun matches(treeDocumentId: String, source: StatusSource): Boolean {
        if (!treeDocumentId.contains(':')) return false
        val path = treeDocumentId.substringAfter(':').trim('/')
        return source.folderPaths.any { it.equals(path, ignoreCase = true) }
    }

    /** Media type for a supported status file, or null for .nomedia / unsupported files. */
    fun mediaTypeOf(fileName: String): MediaType? {
        if (fileName.startsWith(".")) return null
        val ext = fileName.substringAfterLast('.', "").lowercase()
        return when (ext) {
            in imageExtensions -> MediaType.IMAGE
            in videoExtensions -> MediaType.VIDEO
            else -> null
        }
    }

    fun mimeTypeOf(fileName: String): String =
        when (fileName.substringAfterLast('.', "").lowercase()) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "webp" -> "image/webp"
            "gif" -> "image/gif"
            "mp4" -> "video/mp4"
            "3gp" -> "video/3gpp"
            else -> "application/octet-stream"
        }
}

package com.msbytes.statussaver.data.saf

import com.msbytes.statussaver.domain.model.MediaType
import com.msbytes.statussaver.domain.model.StatusSource

/** Pure helpers (no Android dependencies) so they can be unit-tested on the JVM. */
object StatusFolderValidator {

    data class StatusFolder(val source: StatusSource, val path: String)

    /** Every known status folder of every supported app. */
    val allFolders: List<StatusFolder> =
        StatusSource.entries.flatMap { source -> source.folderPaths.map { StatusFolder(source, it) } }

    private val imageExtensions = setOf("jpg", "jpeg", "png", "webp", "gif")
    private val videoExtensions = setOf("mp4", "3gp")

    /**
     * Status folders reachable from a granted tree, e.g. `primary:Android/media` covers both
     * WhatsApp and WhatsApp Business; `primary:` (storage root, Android 10 and older) covers all.
     */
    fun coveredFolders(treeDocumentId: String): List<StatusFolder> {
        if (!treeDocumentId.contains(':')) return emptyList()
        val path = treeDocumentId.substringAfter(':').trim('/')
        return allFolders.filter { folder ->
            path.isEmpty() ||
                folder.path.equals(path, ignoreCase = true) ||
                folder.path.startsWith("$path/", ignoreCase = true)
        }
    }

    fun volumeOf(treeDocumentId: String): String = treeDocumentId.substringBefore(':')

    /** Media type for a supported status file, or null for .nomedia / unsupported files. */
    fun mediaTypeOf(fileName: String): MediaType? {
        if (fileName.startsWith(".")) return null
        return when (fileName.substringAfterLast('.', "").lowercase()) {
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

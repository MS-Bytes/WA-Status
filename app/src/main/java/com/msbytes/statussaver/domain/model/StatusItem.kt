package com.msbytes.statussaver.domain.model

import android.net.Uri

/**
 * A single image or video. Used both for cached statuses ([source] != null)
 * and for files already saved by this app ([source] == null).
 */
data class StatusItem(
    val uri: Uri,
    val name: String,
    val size: Long,
    val lastModified: Long,
    val type: MediaType,
    val mimeType: String,
    val source: StatusSource?,
) {
    /** Stable key for lazy lists, selection and favorites. */
    val id: String get() = uri.toString()
    val isVideo: Boolean get() = type == MediaType.VIDEO
    val isSaved: Boolean get() = source == null
}

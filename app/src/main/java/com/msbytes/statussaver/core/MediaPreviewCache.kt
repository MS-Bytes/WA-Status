package com.msbytes.statussaver.core

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

/** Lazily reads video durations / resolutions once and caches them in memory. */
object MediaPreviewCache {
    private val durations = LruCache<String, Long>(1000)

    /** At most 2 metadata reads at once so fast scrolling never floods the IO pool. */
    private val permits = Semaphore(2)

    suspend fun videoDuration(context: Context, uri: Uri): Long? {
        durations.get(uri.toString())?.let { return it }
        return permits.withPermit {
            withContext(Dispatchers.IO) {
                runCatching {
                MediaMetadataRetriever().useRetriever(context, uri) {
                    it.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
                }
                }.getOrNull()?.also { durations.put(uri.toString(), it) }
            }
        }
    }

    /** Width x height text, e.g. "1080 × 1920". */
    suspend fun resolution(context: Context, uri: Uri, isVideo: Boolean): String? = withContext(Dispatchers.IO) {
        runCatching {
            if (isVideo) {
                MediaMetadataRetriever().useRetriever(context, uri) {
                    val w = it.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                    val h = it.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                    if (w != null && h != null) "$w × $h" else null
                }
            } else {
                val options = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
                context.contentResolver.openInputStream(uri)?.use {
                    android.graphics.BitmapFactory.decodeStream(it, null, options)
                }
                if (options.outWidth > 0) "${options.outWidth} × ${options.outHeight}" else null
            }
        }.getOrNull()
    }

    private inline fun <T> MediaMetadataRetriever.useRetriever(
        context: Context,
        uri: Uri,
        block: (MediaMetadataRetriever) -> T,
    ): T = try {
        setDataSource(context, uri)
        block(this)
    } finally {
        release()
    }
}

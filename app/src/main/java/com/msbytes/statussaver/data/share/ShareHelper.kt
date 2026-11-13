package com.msbytes.statussaver.data.share

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.msbytes.statussaver.domain.model.StatusItem
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Builds share / repost intents. Status files live behind our SAF grant, so they are
 * first copied into the app cache and shared through FileProvider (works with every app).
 * Saved files are app-owned MediaStore items and are shared directly.
 */
@Singleton
class ShareHelper @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val shareDir get() = File(context.cacheDir, "shared")

    suspend fun buildShareIntent(items: List<StatusItem>, targetPackage: String? = null): Intent? =
        withContext(Dispatchers.IO) {
            if (items.isEmpty()) return@withContext null
            cleanOldCopies()
            val uris = items.mapNotNull { shareableUri(it) }
            if (uris.isEmpty()) return@withContext null

            val mime = when {
                items.all { it.isVideo } -> "video/*"
                items.none { it.isVideo } -> "image/*"
                else -> "*/*"
            }
            val intent = if (uris.size == 1) {
                Intent(Intent.ACTION_SEND).putExtra(Intent.EXTRA_STREAM, uris.first())
            } else {
                Intent(Intent.ACTION_SEND_MULTIPLE).putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
            }
            intent.type = mime
            intent.clipData = ClipData.newRawUri(null, uris.first()).apply {
                uris.drop(1).forEach { addItem(ClipData.Item(it)) }
            }
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            if (targetPackage != null) intent.setPackage(targetPackage)
            intent
        }

    private fun shareableUri(item: StatusItem): Uri? {
        if (item.isSaved) return item.uri
        return runCatching {
            shareDir.mkdirs()
            val file = File(shareDir, item.name)
            if (!file.exists() || file.length() != item.size) {
                context.contentResolver.openInputStream(item.uri)?.use { input ->
                    file.outputStream().use { input.copyTo(it) }
                } ?: return null
            }
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        }.getOrNull()
    }

    /** Removes copies older than one day so the cache never grows unbounded. */
    private fun cleanOldCopies() {
        val cutoff = System.currentTimeMillis() - 24 * 60 * 60 * 1000L
        shareDir.listFiles()?.filter { it.lastModified() < cutoff }?.forEach { it.delete() }
    }
}

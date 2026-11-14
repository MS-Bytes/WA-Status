package com.msbytes.statussaver.data.media

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import com.msbytes.statussaver.data.saf.StatusFolderValidator
import com.msbytes.statussaver.domain.model.DeleteOutcome
import com.msbytes.statussaver.domain.model.MediaType
import com.msbytes.statussaver.domain.model.SaveResult
import com.msbytes.statussaver.domain.model.StatusItem
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileNotFoundException
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Writes files into Pictures/WAVAULT and Movies/WAVAULT through MediaStore.
 * - Android 10+: scoped storage (RELATIVE_PATH + IS_PENDING), no permission needed.
 * - Android 7–9: classic MediaStore DATA path, needs WRITE_EXTERNAL_STORAGE (maxSdk 28).
 * All functions are blocking — call them from Dispatchers.IO.
 */
@Singleton
class MediaStoreSaver @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val resolver get() = context.contentResolver
    private val scoped = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    /** Only relevant on Android 9 and older. */
    fun hasLegacyPermission(): Boolean = scoped ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
        PackageManager.PERMISSION_GRANTED

    /** Copies [item] byte-for-byte (original quality). */
    fun save(item: StatusItem): SaveResult {
        if (!hasLegacyPermission()) return SaveResult.Failed(SaveResult.Reason.NO_PERMISSION)
        findExisting(item.name, item.size, item.type)?.let { return SaveResult.AlreadySaved(it) }
        return write(item.name, item.type, item.size) { resolver.openInputStream(item.uri) }
    }

    /** Saves an exported/edited file as a new media item. */
    fun saveStream(displayName: String, type: MediaType, size: Long, open: () -> InputStream?): SaveResult {
        if (!hasLegacyPermission()) return SaveResult.Failed(SaveResult.Reason.NO_PERMISSION)
        return write(displayName, type, size, open)
    }

    fun querySaved(): List<StatusItem> {
        if (!hasLegacyPermission()) return emptyList()
        return MediaType.entries.flatMap { queryCollection(it) }
    }

    fun delete(uris: List<Uri>): DeleteOutcome {
        var deleted = 0
        val notOwned = mutableListOf<Uri>()
        uris.forEach { uri ->
            try {
                if (resolver.delete(uri, null, null) > 0) deleted++ else notOwned += uri
            } catch (_: SecurityException) {
                notOwned += uri
            }
        }
        val request = if (notOwned.isNotEmpty() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            MediaStore.createDeleteRequest(resolver, notOwned).intentSender
        } else {
            null
        }
        return DeleteOutcome(deleted, request)
    }

    private fun write(name: String, type: MediaType, size: Long, open: () -> InputStream?): SaveResult {
        if (!hasEnoughSpace(availableBytes(), size)) return SaveResult.Failed(SaveResult.Reason.LOW_STORAGE)

        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.MIME_TYPE, StatusFolderValidator.mimeTypeOf(name))
            if (scoped) {
                put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath(type))
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            } else {
                val file = uniqueLegacyFile(type, name)
                put(MediaStore.MediaColumns.DISPLAY_NAME, file.name)
                @Suppress("DEPRECATION")
                put(MediaStore.MediaColumns.DATA, file.absolutePath)
            }
        }
        val target = runCatching { resolver.insert(collection(type), values) }.getOrNull()
            ?: return SaveResult.Failed(SaveResult.Reason.UNKNOWN)

        return try {
            val input = open() ?: throw FileNotFoundException()
            input.use { src ->
                resolver.openOutputStream(target)?.use { dst -> src.copyTo(dst, BUFFER_SIZE) }
                    ?: throw FileNotFoundException()
            }
            if (scoped) {
                values.clear()
                values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(target, values, null, null)
            }
            SaveResult.Saved(target)
        } catch (e: Exception) {
            runCatching { resolver.delete(target, null, null) }
            val reason = if (e is FileNotFoundException) SaveResult.Reason.SOURCE_MISSING else SaveResult.Reason.UNKNOWN
            SaveResult.Failed(reason)
        }
    }

    /** Duplicate check: same name + same size in our folder. */
    private fun findExisting(name: String, size: Long, type: MediaType): Uri? {
        val (pathSelection, pathArg) = folderSelection(type)
        val selection = "${MediaStore.MediaColumns.DISPLAY_NAME}=? AND ${MediaStore.MediaColumns.SIZE}=? AND $pathSelection"
        return runCatching {
            resolver.query(collection(type), arrayOf(MediaStore.MediaColumns._ID), selection, arrayOf(name, size.toString(), pathArg), null)
                ?.use { c -> if (c.moveToFirst()) ContentUris.withAppendedId(collection(type), c.getLong(0)) else null }
        }.getOrNull()
    }

    private fun queryCollection(type: MediaType): List<StatusItem> {
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.DATE_MODIFIED,
            MediaStore.MediaColumns.MIME_TYPE,
        )
        val (selection, arg) = folderSelection(type)
        val collection = collection(type)
        val result = mutableListOf<StatusItem>()
        runCatching {
            resolver.query(collection, projection, selection, arrayOf(arg), null)?.use { c ->
                while (c.moveToNext()) {
                    val name = c.getString(1) ?: continue
                    result += StatusItem(
                        uri = ContentUris.withAppendedId(collection, c.getLong(0)),
                        name = name,
                        size = c.getLong(2),
                        lastModified = c.getLong(3) * 1000L,
                        type = type,
                        mimeType = c.getString(4) ?: StatusFolderValidator.mimeTypeOf(name),
                        source = null,
                    )
                }
            }
        }
        return result
    }

    @Suppress("DEPRECATION")
    private fun folderSelection(type: MediaType): Pair<String, String> = if (scoped) {
        "${MediaStore.MediaColumns.RELATIVE_PATH}=?" to relativePath(type)
    } else {
        "${MediaStore.MediaColumns.DATA} LIKE ?" to "${legacyDir(type).absolutePath}/%"
    }

    @Suppress("DEPRECATION")
    private fun legacyDir(type: MediaType): File {
        val base = Environment.getExternalStoragePublicDirectory(
            if (type == MediaType.IMAGE) Environment.DIRECTORY_PICTURES else Environment.DIRECTORY_MOVIES,
        )
        return File(base, FOLDER_NAME)
    }

    private fun uniqueLegacyFile(type: MediaType, name: String): File {
        val dir = legacyDir(type).apply { mkdirs() }
        var file = File(dir, name)
        var index = 1
        while (file.exists()) {
            file = File(dir, "${name.substringBeforeLast('.')}_$index.${name.substringAfterLast('.', "jpg")}")
            index++
        }
        return file
    }

    @Suppress("DEPRECATION")
    private fun availableBytes(): Long = runCatching {
        StatFs(Environment.getExternalStorageDirectory().path).availableBytes
    }.getOrDefault(Long.MAX_VALUE)

    private fun collection(type: MediaType): Uri = when {
        scoped && type == MediaType.IMAGE -> MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        scoped -> MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        type == MediaType.IMAGE -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        else -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
    }

    companion object {
        const val FOLDER_NAME = "WAVAULT"
        private const val BUFFER_SIZE = 64 * 1024
        private const val SPACE_MARGIN_BYTES = 20L * 1024 * 1024

        fun relativePath(type: MediaType): String = when (type) {
            MediaType.IMAGE -> "${Environment.DIRECTORY_PICTURES}/$FOLDER_NAME/"
            MediaType.VIDEO -> "${Environment.DIRECTORY_MOVIES}/$FOLDER_NAME/"
        }

        /** Keeps a safety margin so saving never fills the device completely. */
        fun hasEnoughSpace(availableBytes: Long, neededBytes: Long): Boolean =
            availableBytes - neededBytes > SPACE_MARGIN_BYTES
    }
}

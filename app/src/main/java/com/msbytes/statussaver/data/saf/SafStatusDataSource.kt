package com.msbytes.statussaver.data.saf

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.DocumentsContract
import com.msbytes.statussaver.domain.model.StatusAccessException
import com.msbytes.statussaver.domain.model.StatusItem
import com.msbytes.statussaver.domain.model.StatusSource
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Storage Access Framework access to the `.Statuses` folders.
 * Lists files with a single [DocumentsContract] query (much faster than DocumentFile).
 */
@Singleton
class SafStatusDataSource @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val resolver get() = context.contentResolver

    fun isAppInstalled(source: StatusSource): Boolean = try {
        context.packageManager.getPackageInfo(source.packageName, 0)
        true
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }

    fun initialUri(source: StatusSource): Uri =
        DocumentsContract.buildDocumentUri(EXTERNAL_STORAGE_AUTHORITY, "primary:${source.folderPaths.first()}")

    fun grantedTreeUri(source: StatusSource): Uri? =
        resolver.persistedUriPermissions
            .firstOrNull { it.isReadPermission && isTreeFor(it.uri, source) }
            ?.uri

    fun persist(source: StatusSource, treeUri: Uri): Boolean {
        if (!isTreeFor(treeUri, source)) return false
        return try {
            resolver.takePersistableUriPermission(treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            true
        } catch (_: SecurityException) {
            false
        }
    }

    fun release(source: StatusSource) {
        val uri = grantedTreeUri(source) ?: return
        runCatching { resolver.releasePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
    }

    /** Blocking — call from Dispatchers.IO. */
    fun list(source: StatusSource): List<StatusItem> {
        val tree = grantedTreeUri(source) ?: throw StatusAccessException.NoAccess()
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
            tree,
            DocumentsContract.getTreeDocumentId(tree),
        )
        val cursor = try {
            resolver.query(childrenUri, PROJECTION, null, null, null)
        } catch (_: SecurityException) {
            throw StatusAccessException.NoAccess()
        } catch (_: Exception) {
            throw StatusAccessException.FolderMissing()
        } ?: throw StatusAccessException.FolderMissing()

        val items = ArrayList<StatusItem>(cursor.count)
        cursor.use { c ->
            val idCol = c.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val nameCol = c.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            val sizeCol = c.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_SIZE)
            val modCol = c.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
            while (c.moveToNext()) {
                val name = c.getString(nameCol) ?: continue
                val type = StatusFolderValidator.mediaTypeOf(name) ?: continue
                val size = c.getLong(sizeCol)
                if (size <= 0L) continue // still downloading or corrupted
                items += StatusItem(
                    uri = DocumentsContract.buildDocumentUriUsingTree(tree, c.getString(idCol)),
                    name = name,
                    size = size,
                    lastModified = c.getLong(modCol),
                    type = type,
                    mimeType = StatusFolderValidator.mimeTypeOf(name),
                    source = source,
                )
            }
        }
        return items
    }

    private fun isTreeFor(uri: Uri, source: StatusSource): Boolean = runCatching {
        uri.authority == EXTERNAL_STORAGE_AUTHORITY &&
            DocumentsContract.isTreeUri(uri) &&
            StatusFolderValidator.matches(DocumentsContract.getTreeDocumentId(uri), source)
    }.getOrDefault(false)

    private companion object {
        const val EXTERNAL_STORAGE_AUTHORITY = "com.android.externalstorage.documents"
        val PROJECTION = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_SIZE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED,
        )
    }
}

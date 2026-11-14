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
 * Storage Access Framework access to the `.Statuses` folders of all supported apps.
 * One grant of `Android/media` covers WhatsApp and WhatsApp Business. Each folder is
 * listed with a single [DocumentsContract] query (much faster than DocumentFile).
 */
@Singleton
class SafStatusDataSource @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val resolver get() = context.contentResolver

    fun installedApps(): List<StatusSource> = StatusSource.entries.filter { source ->
        try {
            context.packageManager.getPackageInfo(source.packageName, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun initialUri(): Uri = DocumentsContract.buildDocumentUri(AUTHORITY, "primary:Android/media")

    /** All persisted read grants that reach at least one status folder. */
    fun grantedTrees(): List<Uri> =
        resolver.persistedUriPermissions
            .filter { it.isReadPermission && coversStatuses(it.uri) }
            .map { it.uri }

    fun persist(treeUri: Uri): Boolean {
        if (!coversStatuses(treeUri)) return false
        return try {
            resolver.takePersistableUriPermission(treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            true
        } catch (_: SecurityException) {
            false
        }
    }

    fun releaseAll() {
        grantedTrees().forEach {
            runCatching { resolver.releasePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        }
    }

    /** Blocking — call from Dispatchers.IO. Merges every reachable status folder. */
    fun list(): List<StatusItem> {
        val trees = grantedTrees()
        if (trees.isEmpty()) throw StatusAccessException.NoAccess()

        val items = LinkedHashMap<String, StatusItem>()
        var foundFolder = false
        var accessDenied = false
        for (tree in trees) {
            val treeId = DocumentsContract.getTreeDocumentId(tree)
            val volume = StatusFolderValidator.volumeOf(treeId)
            for (folder in StatusFolderValidator.coveredFolders(treeId)) {
                try {
                    if (queryFolder(tree, "$volume:${folder.path}", folder.source, items)) foundFolder = true
                } catch (_: SecurityException) {
                    accessDenied = true
                } catch (_: Exception) {
                    // Folder does not exist (app not installed / never opened) — skip it.
                }
            }
        }
        if (!foundFolder) {
            throw if (accessDenied) StatusAccessException.NoAccess() else StatusAccessException.FolderMissing()
        }
        return items.values.toList()
    }

    private fun queryFolder(
        tree: Uri,
        folderDocId: String,
        source: StatusSource,
        out: MutableMap<String, StatusItem>,
    ): Boolean {
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(tree, folderDocId)
        val cursor = resolver.query(childrenUri, PROJECTION, null, null, null) ?: return false
        cursor.use { c ->
            while (c.moveToNext()) {
                val docId = c.getString(0) ?: continue
                val name = c.getString(1) ?: continue
                val type = StatusFolderValidator.mediaTypeOf(name) ?: continue
                val size = c.getLong(2)
                if (size <= 0L || out.containsKey(docId)) continue // still downloading / duplicate grant
                out[docId] = StatusItem(
                    uri = DocumentsContract.buildDocumentUriUsingTree(tree, docId),
                    name = name,
                    size = size,
                    lastModified = c.getLong(3),
                    type = type,
                    mimeType = StatusFolderValidator.mimeTypeOf(name),
                    source = source,
                )
            }
        }
        return true
    }

    private fun coversStatuses(uri: Uri): Boolean = runCatching {
        uri.authority == AUTHORITY &&
            DocumentsContract.isTreeUri(uri) &&
            StatusFolderValidator.coveredFolders(DocumentsContract.getTreeDocumentId(uri)).isNotEmpty()
    }.getOrDefault(false)

    private companion object {
        const val AUTHORITY = "com.android.externalstorage.documents"
        val PROJECTION = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_SIZE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED,
        )
    }
}

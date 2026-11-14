package com.msbytes.statussaver.domain.repository

import android.net.Uri
import com.msbytes.statussaver.domain.model.StatusItem
import com.msbytes.statussaver.domain.model.StatusSource
import kotlinx.coroutines.flow.StateFlow

/** Reads cached statuses of WhatsApp and WhatsApp Business together. */
interface StatusRepository {
    /** Latest scan, newest first. Null until the first successful scan. */
    val statuses: StateFlow<List<StatusItem>?>

    fun installedApps(): List<StatusSource>
    fun hasAccess(): Boolean

    /** Where the folder picker opens (Android/media, which covers both apps). */
    fun initialFolderUri(): Uri

    /** Validates and persists a picked folder. Returns false for an unrelated folder. */
    fun grantAccess(treeUri: Uri): Boolean
    fun revokeAccess()

    /** Rescans all folders. Fails with [com.msbytes.statussaver.domain.model.StatusAccessException]. */
    suspend fun refresh(): Result<List<StatusItem>>
}

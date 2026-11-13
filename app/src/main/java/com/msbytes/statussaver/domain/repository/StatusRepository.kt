package com.msbytes.statussaver.domain.repository

import android.net.Uri
import com.msbytes.statussaver.domain.model.StatusItem
import com.msbytes.statussaver.domain.model.StatusSource
import kotlinx.coroutines.flow.StateFlow

/** Reads cached statuses from the messenger folders the user granted access to. */
interface StatusRepository {
    /** Latest scan per source, newest first. */
    val statuses: StateFlow<Map<StatusSource, List<StatusItem>>>

    fun isAppInstalled(source: StatusSource): Boolean
    fun hasAccess(source: StatusSource): Boolean

    /** Uri passed as EXTRA_INITIAL_URI so the picker opens directly in the .Statuses folder. */
    fun initialFolderUri(source: StatusSource): Uri

    /** Validates and persists a tree uri picked by the user. Returns false for a wrong folder. */
    fun grantAccess(source: StatusSource, treeUri: Uri): Boolean
    fun revokeAccess(source: StatusSource)

    /** Rescans the folder. Fails with [com.msbytes.statussaver.domain.model.StatusAccessException]. */
    suspend fun refresh(source: StatusSource): Result<List<StatusItem>>
}

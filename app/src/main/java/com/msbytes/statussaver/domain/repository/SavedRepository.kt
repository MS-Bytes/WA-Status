package com.msbytes.statussaver.domain.repository

import com.msbytes.statussaver.domain.model.DeleteOutcome
import com.msbytes.statussaver.domain.model.MediaType
import com.msbytes.statussaver.domain.model.SaveResult
import com.msbytes.statussaver.domain.model.StatusItem
import kotlinx.coroutines.flow.StateFlow
import java.io.File

/** Files saved by this app into Pictures/StatusSaver and Movies/StatusSaver. */
interface SavedRepository {
    val saved: StateFlow<List<StatusItem>>

    suspend fun refresh()
    suspend fun save(item: StatusItem): SaveResult
    suspend fun saveFile(file: File, displayName: String, type: MediaType): SaveResult
    suspend fun delete(items: List<StatusItem>): DeleteOutcome
}

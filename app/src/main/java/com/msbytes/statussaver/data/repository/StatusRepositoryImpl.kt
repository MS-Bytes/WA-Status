package com.msbytes.statussaver.data.repository

import android.net.Uri
import com.msbytes.statussaver.data.saf.SafStatusDataSource
import com.msbytes.statussaver.domain.model.StatusItem
import com.msbytes.statussaver.domain.model.StatusSource
import com.msbytes.statussaver.domain.repository.StatusRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StatusRepositoryImpl internal constructor(
    private val dataSource: SafStatusDataSource,
    private val io: CoroutineDispatcher,
) : StatusRepository {

    @Inject
    constructor(dataSource: SafStatusDataSource) : this(dataSource, Dispatchers.IO)

    private val _statuses = MutableStateFlow<List<StatusItem>?>(null)
    override val statuses: StateFlow<List<StatusItem>?> = _statuses.asStateFlow()

    override fun installedApps(): List<StatusSource> = dataSource.installedApps()
    override fun hasAccess(): Boolean = dataSource.grantedTrees().isNotEmpty()
    override fun initialFolderUri(): Uri = dataSource.initialUri()
    override fun grantAccess(treeUri: Uri): Boolean = dataSource.persist(treeUri)

    override fun revokeAccess() {
        dataSource.releaseAll()
        _statuses.value = null
    }

    override suspend fun refresh(): Result<List<StatusItem>> = withContext(io) {
        runCatching { dataSource.list().sortedByDescending { it.lastModified } }
            // StateFlow only emits when the list actually changed (cheap equality "hash" compare).
            .onSuccess { _statuses.value = it }
            .onFailure { _statuses.value = null }
    }
}

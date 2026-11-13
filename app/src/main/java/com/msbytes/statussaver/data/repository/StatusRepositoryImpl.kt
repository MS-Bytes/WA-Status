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
import kotlinx.coroutines.flow.update
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

    private val _statuses = MutableStateFlow<Map<StatusSource, List<StatusItem>>>(emptyMap())
    override val statuses: StateFlow<Map<StatusSource, List<StatusItem>>> = _statuses.asStateFlow()

    override fun isAppInstalled(source: StatusSource) = dataSource.isAppInstalled(source)
    override fun hasAccess(source: StatusSource) = dataSource.grantedTreeUri(source) != null
    override fun initialFolderUri(source: StatusSource): Uri = dataSource.initialUri(source)
    override fun grantAccess(source: StatusSource, treeUri: Uri) = dataSource.persist(source, treeUri)

    override fun revokeAccess(source: StatusSource) {
        dataSource.release(source)
        _statuses.update { it - source }
    }

    override suspend fun refresh(source: StatusSource): Result<List<StatusItem>> = withContext(io) {
        runCatching { dataSource.list(source).sortedByDescending { it.lastModified } }
            .onSuccess { items ->
                // StateFlow only emits when the list actually changed (cheap "hash" compare).
                _statuses.update { it + (source to items) }
            }
            .onFailure { _statuses.update { it - source } }
    }
}

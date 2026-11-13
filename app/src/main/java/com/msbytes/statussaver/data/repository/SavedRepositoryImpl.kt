package com.msbytes.statussaver.data.repository

import com.msbytes.statussaver.data.media.MediaStoreSaver
import com.msbytes.statussaver.domain.model.DeleteOutcome
import com.msbytes.statussaver.domain.model.MediaType
import com.msbytes.statussaver.domain.model.SaveResult
import com.msbytes.statussaver.domain.model.StatusItem
import com.msbytes.statussaver.domain.repository.FavoritesRepository
import com.msbytes.statussaver.domain.repository.SavedRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SavedRepositoryImpl internal constructor(
    private val saver: MediaStoreSaver,
    private val favorites: FavoritesRepository,
    private val io: CoroutineDispatcher,
) : SavedRepository {

    @Inject
    constructor(saver: MediaStoreSaver, favorites: FavoritesRepository) : this(saver, favorites, Dispatchers.IO)

    private val _saved = MutableStateFlow<List<StatusItem>>(emptyList())
    override val saved: StateFlow<List<StatusItem>> = _saved.asStateFlow()

    /** Serialises writes so bulk saves and auto-save never race on the duplicate check. */
    private val writeLock = Mutex()

    override suspend fun refresh() {
        _saved.value = withContext(io) { saver.querySaved().sortedByDescending { it.lastModified } }
    }

    override suspend fun save(item: StatusItem): SaveResult {
        val result = writeLock.withLock { withContext(io) { saver.save(item) } }
        if (result is SaveResult.Saved) refresh()
        return result
    }

    override suspend fun saveFile(file: File, displayName: String, type: MediaType): SaveResult {
        val result = writeLock.withLock {
            withContext(io) { saver.saveStream(displayName, type, file.length()) { file.inputStream() } }
        }
        if (result is SaveResult.Saved) refresh()
        return result
    }

    override suspend fun delete(items: List<StatusItem>): DeleteOutcome {
        val outcome = withContext(io) { saver.delete(items.map { it.uri }) }
        favorites.remove(items.map { it.id })
        refresh()
        return outcome
    }
}

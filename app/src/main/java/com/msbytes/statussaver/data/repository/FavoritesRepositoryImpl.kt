package com.msbytes.statussaver.data.repository

import com.msbytes.statussaver.data.local.db.FavoriteDao
import com.msbytes.statussaver.data.local.db.FavoriteEntity
import com.msbytes.statussaver.domain.model.StatusItem
import com.msbytes.statussaver.domain.repository.FavoritesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FavoritesRepositoryImpl @Inject constructor(
    private val dao: FavoriteDao,
) : FavoritesRepository {

    override val favoriteIds: Flow<Set<String>> = dao.observeIds().map { it.toSet() }

    override suspend fun toggle(item: StatusItem) {
        if (dao.exists(item.id)) {
            dao.delete(listOf(item.id))
        } else {
            dao.insert(FavoriteEntity(item.id, item.name, System.currentTimeMillis()))
        }
    }

    override suspend fun remove(ids: Collection<String>) {
        if (ids.isNotEmpty()) dao.delete(ids.toList())
    }
}

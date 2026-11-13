package com.msbytes.statussaver.domain.repository

import com.msbytes.statussaver.domain.model.StatusItem
import kotlinx.coroutines.flow.Flow

interface FavoritesRepository {
    /** Ids ([StatusItem.id]) of favorite items. */
    val favoriteIds: Flow<Set<String>>

    suspend fun toggle(item: StatusItem)
    suspend fun remove(ids: Collection<String>)
}

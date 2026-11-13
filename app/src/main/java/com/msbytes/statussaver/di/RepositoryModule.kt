package com.msbytes.statussaver.di

import com.msbytes.statussaver.data.local.prefs.DataStoreSettingsRepository
import com.msbytes.statussaver.data.repository.FavoritesRepositoryImpl
import com.msbytes.statussaver.data.repository.SavedRepositoryImpl
import com.msbytes.statussaver.data.repository.StatusRepositoryImpl
import com.msbytes.statussaver.domain.repository.FavoritesRepository
import com.msbytes.statussaver.domain.repository.SavedRepository
import com.msbytes.statussaver.domain.repository.SettingsRepository
import com.msbytes.statussaver.domain.repository.StatusRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds abstract fun bindStatusRepository(impl: StatusRepositoryImpl): StatusRepository
    @Binds abstract fun bindSavedRepository(impl: SavedRepositoryImpl): SavedRepository
    @Binds abstract fun bindFavoritesRepository(impl: FavoritesRepositoryImpl): FavoritesRepository
    @Binds abstract fun bindSettingsRepository(impl: DataStoreSettingsRepository): SettingsRepository
}

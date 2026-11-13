package com.msbytes.statussaver.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val id: String,
    val name: String,
    val addedAt: Long,
)

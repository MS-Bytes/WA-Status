package com.msbytes.statussaver.domain.model

enum class SortOrder {
    NEWEST, OLDEST, LARGEST, SMALLEST;

    fun apply(items: List<StatusItem>): List<StatusItem> = when (this) {
        NEWEST -> items.sortedByDescending { it.lastModified }
        OLDEST -> items.sortedBy { it.lastModified }
        LARGEST -> items.sortedByDescending { it.size }
        SMALLEST -> items.sortedBy { it.size }
    }
}

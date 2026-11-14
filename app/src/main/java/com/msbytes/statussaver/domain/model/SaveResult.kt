package com.msbytes.statussaver.domain.model

import android.net.Uri

sealed interface SaveResult {
    data class Saved(val uri: Uri) : SaveResult
    data class AlreadySaved(val uri: Uri) : SaveResult
    data class Failed(val reason: Reason) : SaveResult

    enum class Reason { LOW_STORAGE, SOURCE_MISSING, NO_PERMISSION, UNKNOWN }
}

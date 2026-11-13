package com.msbytes.statussaver.core

import androidx.annotation.StringRes

/** Generic screen state used across the app. */
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data object Empty : UiState<Nothing>
    data class Error(@StringRes val message: Int) : UiState<Nothing>
}

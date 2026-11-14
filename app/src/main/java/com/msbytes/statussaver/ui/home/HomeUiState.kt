package com.msbytes.statussaver.ui.home

import com.msbytes.statussaver.core.UiState
import com.msbytes.statussaver.domain.model.SortOrder
import com.msbytes.statussaver.domain.model.StatusItem

enum class AccessState { CHECKING, GRANTED, NEEDED, WRONG_FOLDER, REVOKED, FOLDER_MISSING }

data class HomeUiState(
    /** True when WhatsApp or WhatsApp Business is installed. */
    val messengerInstalled: Boolean = true,
    val access: AccessState = AccessState.CHECKING,
    val content: UiState<List<StatusItem>> = UiState.Loading,
    val isRefreshing: Boolean = false,
    /** Items modified after this time get the NEW badge. */
    val newSince: Long = Long.MAX_VALUE,
    val savedNames: Set<String> = emptySet(),
    val savingIds: Set<String> = emptySet(),
    val selection: Set<String> = emptySet(),
    val sortOrder: SortOrder = SortOrder.NEWEST,
    /** Ask once for notification permission after access is granted (Android 13+). */
    val askNotifications: Boolean = false,
)

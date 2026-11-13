package com.msbytes.statussaver.domain.model

import android.content.IntentSender

/**
 * @param deletedCount files removed directly.
 * @param confirmationRequest Android 11+ system dialog for files the app no longer owns
 * (e.g. after a reinstall). Null when nothing else needs confirmation.
 */
data class DeleteOutcome(
    val deletedCount: Int,
    val confirmationRequest: IntentSender?,
)

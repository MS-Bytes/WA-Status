package com.msbytes.statussaver.core

import android.content.Intent
import android.content.IntentSender
import android.net.Uri
import androidx.annotation.StringRes

/** One-off events sent from ViewModels to screens. */
sealed interface UiEvent {
    data class Message(@StringRes val text: Int) : UiEvent
    data class Plural(val pluralRes: Int, val count: Int) : UiEvent

    /** Shown after a save with Open / Share actions. */
    data class Saved(val uri: Uri, val mimeType: String) : UiEvent
    data class LaunchIntent(val intent: Intent, val chooserTitle: Int? = null) : UiEvent
    data class ConfirmDelete(val sender: IntentSender) : UiEvent
}

package com.msbytes.statussaver.ui.common

import com.msbytes.statussaver.R
import com.msbytes.statussaver.ads.AdManager
import com.msbytes.statussaver.core.UiEvent
import com.msbytes.statussaver.domain.model.SaveResult
import com.msbytes.statussaver.domain.model.StatusItem
import com.msbytes.statussaver.domain.repository.SavedRepository
import com.msbytes.statussaver.domain.repository.SettingsRepository
import com.msbytes.statussaver.play.PlayStoreHelper
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Shared "user pressed save" logic: saves, builds the feedback event and
 * notifies the ad manager (interstitial frequency cap) and review prompt.
 */
@Singleton
class SaveActions @Inject constructor(
    private val savedRepository: SavedRepository,
    private val settingsRepository: SettingsRepository,
    private val adManager: AdManager,
    private val playStoreHelper: PlayStoreHelper,
) {
    suspend fun save(item: StatusItem): UiEvent {
        val result = savedRepository.save(item)
        if (result is SaveResult.Saved) onUserSaved()
        return result.toEvent(item.mimeType)
    }

    suspend fun saveMany(items: List<StatusItem>): UiEvent {
        var saved = 0
        var already = 0
        var lastFailure: SaveResult.Failed? = null
        items.forEach { item ->
            when (val r = savedRepository.save(item)) {
                is SaveResult.Saved -> saved++
                is SaveResult.AlreadySaved -> already++
                is SaveResult.Failed -> lastFailure = r
            }
        }
        if (saved > 0) onUserSaved()
        return when {
            saved > 0 -> UiEvent.Plural(R.plurals.saved_count, saved)
            already > 0 -> UiEvent.Message(R.string.save_already)
            else -> (lastFailure ?: SaveResult.Failed(SaveResult.Reason.UNKNOWN)).toEvent("")
        }
    }

    /** Also used after an editor export. */
    suspend fun onUserSaved() {
        adManager.onSaveCompleted()
        if (settingsRepository.incrementSaveCount() == REVIEW_AFTER_SAVES) playStoreHelper.requestReview()
    }

    private companion object {
        const val REVIEW_AFTER_SAVES = 5
    }
}

fun SaveResult.toEvent(mimeType: String): UiEvent = when (this) {
    is SaveResult.Saved -> UiEvent.Saved(uri, mimeType)
    is SaveResult.AlreadySaved -> UiEvent.Message(R.string.save_already)
    is SaveResult.Failed -> UiEvent.Message(
        when (reason) {
            SaveResult.Reason.LOW_STORAGE -> R.string.error_low_storage
            SaveResult.Reason.SOURCE_MISSING -> R.string.error_file_missing
            SaveResult.Reason.UNKNOWN -> R.string.save_failed
        },
    )
}

package com.msbytes.statussaver.ui.navigation

import com.msbytes.statussaver.domain.model.MediaType
import com.msbytes.statussaver.domain.model.StatusSource
import kotlinx.serialization.Serializable

@Serializable data object OnboardingRoute
@Serializable data object HomeRoute
@Serializable data object SettingsRoute
@Serializable data object AboutRoute
@Serializable data object FaqRoute
@Serializable data object DirectChatRoute

/** @param collection see [ViewerCollection]. */
@Serializable data class ViewerRoute(val collection: String, val startId: String)

@Serializable data class EditorRoute(val uri: String, val isVideo: Boolean, val name: String)

/** Which list the viewer pages through, encoded as a string for the route. */
object ViewerCollection {
    const val SAVED = "SAVED"

    fun of(source: StatusSource, type: MediaType) = "${source.name}_${type.name}"

    fun parse(value: String): Pair<StatusSource, MediaType>? {
        if (value == SAVED) return null
        val source = StatusSource.entries.firstOrNull { value.startsWith(it.name + "_") } ?: return null
        val type = MediaType.valueOf(value.removePrefix(source.name + "_"))
        return source to type
    }
}

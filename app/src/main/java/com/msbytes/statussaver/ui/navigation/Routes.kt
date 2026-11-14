package com.msbytes.statussaver.ui.navigation

import kotlinx.serialization.Serializable

@Serializable data object OnboardingRoute
@Serializable data object HomeRoute
@Serializable data object SettingsRoute
@Serializable data object AboutRoute
@Serializable data object FaqRoute

/** @param collection one of [ViewerCollection]. */
@Serializable data class ViewerRoute(val collection: String, val startId: String)

@Serializable data class EditorRoute(val uri: String, val isVideo: Boolean, val name: String)

/** Which list the viewer pages through. */
object ViewerCollection {
    const val IMAGES = "IMAGES"
    const val VIDEOS = "VIDEOS"
    const val SAVED = "SAVED"
}

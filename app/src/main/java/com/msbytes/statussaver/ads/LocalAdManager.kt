package com.msbytes.statussaver.ads

import androidx.compose.runtime.staticCompositionLocalOf

/** Gives composables (banner / native views) access to the singleton [AdManager]. */
val LocalAdManager = staticCompositionLocalOf<AdManager?> { null }

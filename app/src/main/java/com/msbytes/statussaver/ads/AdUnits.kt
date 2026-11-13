package com.msbytes.statussaver.ads

import com.msbytes.statussaver.BuildConfig

/**
 * Ad unit ids come from BuildConfig (Gradle): Google test ids for debug builds,
 * real ids from local.properties / CI secrets for release builds.
 */
object AdUnits {
    const val BANNER = BuildConfig.ADMOB_BANNER_ID
    const val INTERSTITIAL = BuildConfig.ADMOB_INTERSTITIAL_ID
    const val NATIVE = BuildConfig.ADMOB_NATIVE_ID
    const val APP_OPEN = BuildConfig.ADMOB_APP_OPEN_ID
    const val REWARDED = BuildConfig.ADMOB_REWARDED_ID

    /** A native ad row is inserted after every N grid items. */
    const val NATIVE_EVERY_N_ITEMS = 12
}

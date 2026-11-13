package com.msbytes.statussaver.ads

/**
 * Pure frequency-cap rules (unit-tested). All times are epoch millis.
 */
class AdFrequencyPolicy(
    private val interstitialIntervalMs: Long = 3 * 60 * 1000L,
    private val appOpenIntervalMs: Long = 4 * 60 * 60 * 1000L,
) {
    /**
     * Interstitial after a save: never on the first save of a session,
     * max one every 3 minutes, and never while the rewarded ad-free period is active.
     */
    fun canShowInterstitial(now: Long, lastShownAt: Long, savesThisSession: Int, adFreeUntil: Long): Boolean =
        savesThisSession >= 2 &&
            now >= adFreeUntil &&
            (lastShownAt == 0L || now - lastShownAt >= interstitialIntervalMs)

    /** App Open: never on the very first launch, max once every 4 hours. */
    fun canShowAppOpen(now: Long, lastShownAt: Long, isFirstSession: Boolean, adFreeUntil: Long): Boolean =
        !isFirstSession &&
            now >= adFreeUntil &&
            (lastShownAt == 0L || now - lastShownAt >= appOpenIntervalMs)

    /** Google: app open ads expire 4 hours after loading. */
    fun isAppOpenAdFresh(now: Long, loadedAt: Long): Boolean = now - loadedAt < appOpenIntervalMs
}

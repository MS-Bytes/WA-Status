package com.msbytes.statussaver.ads

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AdFrequencyPolicyTest {

    private val policy = AdFrequencyPolicy()
    private val minute = 60_000L
    private val hour = 60 * minute
    private val now = 10 * 24 * hour

    @Test
    fun `interstitial never shows on the first save of a session`() {
        assertThat(policy.canShowInterstitial(now, lastShownAt = 0, savesThisSession = 1, adFreeUntil = 0)).isFalse()
    }

    @Test
    fun `interstitial shows on the second save when never shown before`() {
        assertThat(policy.canShowInterstitial(now, lastShownAt = 0, savesThisSession = 2, adFreeUntil = 0)).isTrue()
    }

    @Test
    fun `interstitial respects the 3 minute cap`() {
        assertThat(policy.canShowInterstitial(now, now - 2 * minute, 5, 0)).isFalse()
        assertThat(policy.canShowInterstitial(now, now - 3 * minute, 5, 0)).isTrue()
    }

    @Test
    fun `interstitial is paused during the rewarded ad-free period`() {
        assertThat(policy.canShowInterstitial(now, 0, 5, adFreeUntil = now + minute)).isFalse()
        assertThat(policy.canShowInterstitial(now, 0, 5, adFreeUntil = now - 1)).isTrue()
    }

    @Test
    fun `app open never shows in the first session`() {
        assertThat(policy.canShowAppOpen(now, 0, isFirstSession = true, adFreeUntil = 0)).isFalse()
    }

    @Test
    fun `app open is capped to once every 4 hours`() {
        assertThat(policy.canShowAppOpen(now, now - 3 * hour, false, 0)).isFalse()
        assertThat(policy.canShowAppOpen(now, now - 4 * hour, false, 0)).isTrue()
        assertThat(policy.canShowAppOpen(now, 0, false, 0)).isTrue()
    }

    @Test
    fun `app open ad expires 4 hours after loading`() {
        assertThat(policy.isAppOpenAdFresh(now, now - hour)).isTrue()
        assertThat(policy.isAppOpenAdFresh(now, now - 4 * hour)).isFalse()
    }
}

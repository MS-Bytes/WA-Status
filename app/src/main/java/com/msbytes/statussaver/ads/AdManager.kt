package com.msbytes.statussaver.ads

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.os.SystemClock
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.msbytes.statussaver.BuildConfig
import com.msbytes.statussaver.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single place for every ad format: consent-aware initialisation, preloading,
 * frequency caps and lifecycle. Ads fail silently (no internet = no ads, no crash).
 */
@Singleton
class AdManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val consent: ConsentManager,
    private val settings: SettingsRepository,
) : Application.ActivityLifecycleCallbacks, DefaultLifecycleObserver {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val policy = AdFrequencyPolicy()
    private val prefs = context.getSharedPreferences("ads_state", Context.MODE_PRIVATE)
    private val initStarted = AtomicBoolean(false)

    private val sdkReady = MutableStateFlow(false)

    /** Future "Remove Ads" purchase only needs to set this to true. */
    private val adsRemovedByPurchase = MutableStateFlow(false)

    /** True when ads may be shown: kill-switch on, consent given, SDK ready, not purchased. */
    val adsEnabled: StateFlow<Boolean> = combine(sdkReady, adsRemovedByPurchase) { ready, removed ->
        BuildConfig.ADS_ENABLED && ready && !removed
    }.stateIn(scope, SharingStarted.Eagerly, false)

    /** Synchronous version of [adsEnabled] for internal checks. */
    private val active: Boolean
        get() = BuildConfig.ADS_ENABLED && sdkReady.value && !adsRemovedByPurchase.value

    private val _nativeAds = MutableStateFlow<List<NativeAd>>(emptyList())
    val nativeAds: StateFlow<List<NativeAd>> = _nativeAds.asStateFlow()

    private var currentActivity: WeakReference<Activity>? = null
    private var interstitial: InterstitialAd? = null
    private var appOpenAd: AppOpenAd? = null
    private var appOpenLoadedAt = 0L
    private var rewarded: RewardedAd? = null
    private var loadingInterstitial = false
    private var loadingAppOpen = false
    private var loadingRewarded = false
    private var loadingNative = false

    private var isShowingFullScreen = false
    private var savesThisSession = 0
    private var adFreeUntil = 0L
    private var isFirstSession = true
    private var wentToBackground = false
    private var suppressNextAppOpen = false
    private val processStartedAt = SystemClock.elapsedRealtime()

    /**
     * Set by the UI: true only on screens where a full-screen ad is acceptable
     * (never on onboarding, permission, editor, viewer playback or App Lock).
     */
    @Volatile
    var fullScreenAllowed: Boolean = false

    /** True while the folder-permission guide is on screen (no full-screen ads there). */
    @Volatile
    var permissionScreenVisible: Boolean = false

    fun attach(application: Application) {
        application.registerActivityLifecycleCallbacks(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        scope.launch {
            isFirstSession = !settings.settings.first().onboardingDone
            settings.settings.collect { adFreeUntil = it.adFreeUntil }
        }
    }

    /** Called once consent allows it. MobileAds is initialised off the main thread. */
    fun initializeIfAllowed() {
        if (!BuildConfig.ADS_ENABLED || !consent.canRequestAds) return
        if (!initStarted.compareAndSet(false, true)) return
        scope.launch {
            withContext(Dispatchers.IO) { MobileAds.initialize(context) {} }
            sdkReady.value = true
            loadInterstitial()
            loadAppOpen()
            loadRewarded()
            loadNativeAds()
        }
    }

    // ---------------------------------------------------------------- Interstitial

    /** Call after every successful user-initiated save / edit-save. */
    fun onSaveCompleted() {
        savesThisSession++
        val ad = interstitial
        if (ad == null) {
            loadInterstitial()
            return
        }
        if (!canShowFullScreen()) return
        if (!policy.canShowInterstitial(now(), lastInterstitialAt, savesThisSession, adFreeUntil)) return
        scope.launch {
            delay(SHOW_DELAY_MS) // let the success animation finish first
            val activity = currentActivity?.get() ?: return@launch
            if (!canShowFullScreen() || interstitial !== ad) return@launch
            ad.fullScreenContentCallback = fullScreenCallback {
                interstitial = null
                loadInterstitial()
            }
            isShowingFullScreen = true
            lastInterstitialAt = now()
            ad.show(activity)
        }
    }

    private fun loadInterstitial() {
        if (!active || interstitial != null || loadingInterstitial) return
        loadingInterstitial = true
        InterstitialAd.load(context, AdUnits.INTERSTITIAL, AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    loadingInterstitial = false
                    interstitial = ad
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    loadingInterstitial = false
                }
            })
    }

    // ---------------------------------------------------------------- App Open

    /** Call before launching another app (share, repost, Play Store) so returning doesn't show an ad. */
    fun suppressNextAppOpen() {
        suppressNextAppOpen = true
    }

    override fun onStop(owner: LifecycleOwner) {
        wentToBackground = true
    }

    override fun onStart(owner: LifecycleOwner) {
        if (!wentToBackground) return
        wentToBackground = false
        if (suppressNextAppOpen) {
            suppressNextAppOpen = false
            return
        }
        tryShowAppOpen()
    }

    private fun tryShowAppOpen() {
        val ad = appOpenAd
        if (ad == null || !policy.isAppOpenAdFresh(now(), appOpenLoadedAt)) {
            appOpenAd = null
            loadAppOpen()
            return
        }
        if (!canShowFullScreen()) return
        if (!policy.canShowAppOpen(now(), lastAppOpenAt, isFirstSession, adFreeUntil)) return
        val activity = currentActivity?.get() ?: return
        ad.fullScreenContentCallback = fullScreenCallback {
            appOpenAd = null
            loadAppOpen()
        }
        isShowingFullScreen = true
        lastAppOpenAt = now()
        ad.show(activity)
    }

    private fun loadAppOpen() {
        if (!active || appOpenAd != null || loadingAppOpen) return
        loadingAppOpen = true
        AppOpenAd.load(context, AdUnits.APP_OPEN, AdRequest.Builder().build(),
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    loadingAppOpen = false
                    appOpenAd = ad
                    appOpenLoadedAt = now()
                    // Cold start: show only if the ad arrived quickly; never make the user wait.
                    val elapsed = SystemClock.elapsedRealtime() - processStartedAt
                    if (elapsed < COLD_START_WINDOW_MS && !wentToBackground) tryShowAppOpen()
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    loadingAppOpen = false
                }
            })
    }

    // ---------------------------------------------------------------- Rewarded

    val isRewardedReady: Boolean get() = rewarded != null

    /** Shows a rewarded ad; on reward, full-screen ads pause for 30 minutes. */
    fun showRewarded(activity: Activity, onResult: (rewarded: Boolean) -> Unit) {
        val ad = rewarded
        if (ad == null || !active || isShowingFullScreen) {
            loadRewarded()
            onResult(false)
            return
        }
        var earned = false
        ad.fullScreenContentCallback = fullScreenCallback {
            rewarded = null
            loadRewarded()
            onResult(earned)
        }
        isShowingFullScreen = true
        ad.show(activity) {
            earned = true
            scope.launch { settings.setAdFreeUntil(now() + AD_FREE_DURATION_MS) }
        }
    }

    private fun loadRewarded() {
        if (!active || rewarded != null || loadingRewarded) return
        loadingRewarded = true
        RewardedAd.load(context, AdUnits.REWARDED, AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    loadingRewarded = false
                    rewarded = ad
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    loadingRewarded = false
                }
            })
    }

    // ---------------------------------------------------------------- Native

    /** Keeps a small pool of native ads for the status grid. */
    fun loadNativeAds() {
        if (!active || loadingNative || _nativeAds.value.size >= NATIVE_POOL_SIZE) return
        loadingNative = true
        val loader = AdLoader.Builder(context, AdUnits.NATIVE)
            .forNativeAd { ad -> _nativeAds.value = (_nativeAds.value + ad).takeLast(NATIVE_POOL_SIZE) }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    loadingNative = false
                }

                override fun onAdLoaded() {
                    loadingNative = false
                }
            })
            .withNativeAdOptions(
                NativeAdOptions.Builder()
                    .setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_RIGHT)
                    .build(),
            )
            .build()
        loader.loadAds(AdRequest.Builder().build(), NATIVE_POOL_SIZE)
    }

    // ---------------------------------------------------------------- Helpers

    private fun canShowFullScreen(): Boolean =
        active && fullScreenAllowed && !permissionScreenVisible && !isShowingFullScreen && currentActivity?.get() != null

    private fun fullScreenCallback(onFinished: () -> Unit) = object : FullScreenContentCallback() {
        override fun onAdDismissedFullScreenContent() {
            isShowingFullScreen = false
            onFinished()
        }

        override fun onAdFailedToShowFullScreenContent(error: AdError) {
            isShowingFullScreen = false
            onFinished()
        }
    }

    private var lastInterstitialAt: Long
        get() = prefs.getLong(KEY_LAST_INTERSTITIAL, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_INTERSTITIAL, value).apply()

    private var lastAppOpenAt: Long
        get() = prefs.getLong(KEY_LAST_APP_OPEN, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_APP_OPEN, value).apply()

    private fun now() = System.currentTimeMillis()

    // ActivityLifecycleCallbacks: track the foreground activity (ads need one to show).
    override fun onActivityResumed(activity: Activity) {
        currentActivity = WeakReference(activity)
    }

    override fun onActivityDestroyed(activity: Activity) {
        if (currentActivity?.get() === activity) currentActivity = null
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
    override fun onActivityStarted(activity: Activity) = Unit
    override fun onActivityPaused(activity: Activity) = Unit
    override fun onActivityStopped(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit

    private companion object {
        const val SHOW_DELAY_MS = 800L
        const val COLD_START_WINDOW_MS = 4_000L
        const val AD_FREE_DURATION_MS = 30 * 60 * 1000L
        const val NATIVE_POOL_SIZE = 3
        const val KEY_LAST_INTERSTITIAL = "last_interstitial"
        const val KEY_LAST_APP_OPEN = "last_app_open"
    }
}

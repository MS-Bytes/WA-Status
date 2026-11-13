package com.msbytes.statussaver.ads

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

/** Adaptive anchored banner. Renders nothing when ads are disabled or consent is missing. */
@Composable
fun BannerAd(modifier: Modifier = Modifier) {
    val adManager = LocalAdManager.current ?: return
    val enabled by adManager.adsEnabled.collectAsStateWithLifecycle()
    if (!enabled) return

    val context = LocalContext.current
    BoxWithConstraints(modifier.fillMaxWidth().padding(top = 8.dp)) {
        val widthDp = maxWidth.value.toInt()
        val adView = remember(widthDp) {
            AdView(context).apply {
                adUnitId = AdUnits.BANNER
                setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, widthDp))
                loadAd(AdRequest.Builder().build())
            }
        }
        val lifecycle = LocalLifecycleOwner.current.lifecycle
        DisposableEffect(adView, lifecycle) {
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_RESUME -> adView.resume()
                    Lifecycle.Event.ON_PAUSE -> adView.pause()
                    else -> Unit
                }
            }
            lifecycle.addObserver(observer)
            onDispose {
                lifecycle.removeObserver(observer)
                adView.destroy()
            }
        }
        AndroidView(factory = { adView }, modifier = Modifier.fillMaxWidth())
    }
}

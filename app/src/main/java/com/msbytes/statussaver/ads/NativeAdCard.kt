package com.msbytes.statussaver.ads

import android.view.LayoutInflater
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.msbytes.statussaver.R

/**
 * Full-width native ad row shown inside the status grid. Clearly labelled "Ad",
 * styled with the app theme but visually distinct from status tiles.
 */
@Composable
fun NativeAdCard(ad: NativeAd, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val textColor = colors.onSurface.toArgb()
    val subColor = colors.onSurfaceVariant.toArgb()
    val badgeBg = colors.tertiary.toArgb()
    val badgeText = colors.onTertiary.toArgb()
    val ctaBg = colors.primary.toArgb()
    val ctaText = colors.onPrimary.toArgb()

    Card(
        modifier = modifier.fillMaxWidth().padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerHigh),
    ) {
        AndroidView(
            modifier = Modifier.fillMaxWidth(),
            factory = { ctx ->
                @Suppress("InflateParams") // AndroidView provides the parent
                LayoutInflater.from(ctx).inflate(R.layout.native_ad_card, null) as NativeAdView
            },
            update = { view ->
                val headline = view.findViewById<TextView>(R.id.ad_headline)
                val body = view.findViewById<TextView>(R.id.ad_body)
                val icon = view.findViewById<ImageView>(R.id.ad_icon)
                val cta = view.findViewById<Button>(R.id.ad_call_to_action)
                val badge = view.findViewById<TextView>(R.id.ad_badge)
                val media = view.findViewById<MediaView>(R.id.ad_media)

                headline.text = ad.headline
                headline.setTextColor(textColor)
                body.text = ad.body
                body.setTextColor(subColor)
                badge.setTextColor(badgeText)
                badge.background?.mutate()?.setTint(badgeBg)
                cta.text = ad.callToAction
                cta.setTextColor(ctaText)
                cta.background?.mutate()?.setTint(ctaBg)
                cta.visibility = if (ad.callToAction == null) android.view.View.GONE else android.view.View.VISIBLE
                val iconDrawable = ad.icon?.drawable
                icon.setImageDrawable(iconDrawable)
                icon.visibility = if (iconDrawable == null) android.view.View.GONE else android.view.View.VISIBLE

                view.headlineView = headline
                view.bodyView = body
                view.iconView = icon
                view.callToActionView = cta
                view.mediaView = media
                view.setNativeAd(ad)
            },
        )
    }
}

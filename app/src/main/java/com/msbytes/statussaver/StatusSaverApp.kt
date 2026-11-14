package com.msbytes.statussaver

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import android.os.Build
import coil3.gif.AnimatedImageDecoder
import coil3.gif.GifDecoder
import coil3.request.crossfade
import coil3.video.VideoFrameDecoder
import com.msbytes.statussaver.ads.AdManager
import com.msbytes.statussaver.notifications.Notifier
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class StatusSaverApp : Application(), Configuration.Provider, SingletonImageLoader.Factory {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var adManager: AdManager
    @Inject lateinit var notifier: Notifier

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        notifier.createChannels()
        adManager.attach(this)
    }

    /** Coil with video-frame thumbnails and animated GIF/WebP support. */
    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .components {
                add(VideoFrameDecoder.Factory())
                // Android 9+ decodes GIF/animated WebP natively; older versions use Coil's GIF decoder.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) add(AnimatedImageDecoder.Factory()) else add(GifDecoder.Factory())
            }
            .crossfade(true)
            .build()
}

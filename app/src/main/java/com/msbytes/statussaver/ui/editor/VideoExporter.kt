package com.msbytes.statussaver.ui.editor

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Trims / mutes a video with Media3 Transformer into a new temporary file. */
@Singleton
@OptIn(UnstableApi::class)
class VideoExporter @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    suspend fun export(
        uri: Uri,
        startMs: Long,
        endMs: Long,
        removeAudio: Boolean,
        onProgress: (Int) -> Unit,
    ): File = withContext(Dispatchers.Main) {
        val output = File(context.cacheDir, "editor/edit_${System.currentTimeMillis()}.mp4")
        output.parentFile?.mkdirs()

        val mediaItem = MediaItem.Builder()
            .setUri(uri)
            .setClippingConfiguration(
                MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(startMs)
                    .setEndPositionMs(endMs)
                    .build(),
            )
            .build()
        val edited = EditedMediaItem.Builder(mediaItem).setRemoveAudio(removeAudio).build()

        suspendCancellableCoroutine { cont ->
            val handler = Handler(Looper.getMainLooper())
            lateinit var poll: Runnable
            val transformer = Transformer.Builder(context)
                .addListener(object : Transformer.Listener {
                    override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                        handler.removeCallbacks(poll)
                        if (cont.isActive) cont.resume(output)
                    }

                    override fun onError(
                        composition: Composition,
                        exportResult: ExportResult,
                        exportException: ExportException,
                    ) {
                        handler.removeCallbacks(poll)
                        output.delete()
                        if (cont.isActive) cont.resumeWithException(exportException)
                    }
                })
                .build()

            val holder = ProgressHolder()
            poll = Runnable {
                if (transformer.getProgress(holder) == Transformer.PROGRESS_STATE_AVAILABLE) onProgress(holder.progress)
                handler.postDelayed(poll, 250)
            }
            transformer.start(edited, output.absolutePath)
            handler.post(poll)

            cont.invokeOnCancellation {
                handler.post {
                    handler.removeCallbacks(poll)
                    transformer.cancel()
                    output.delete()
                }
            }
        }
    }
}

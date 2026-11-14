package com.msbytes.statussaver.ui.editor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.graphics.Canvas
import android.graphics.ColorMatrixColorFilter
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.Typeface
import android.net.Uri
import kotlin.math.max
import kotlin.math.roundToInt

/** Bitmap work for the image editor (decode, rotate/flip, final render). Call off the main thread. */
object ImageRenderer {

    private const val MAX_DIMENSION = 2560

    fun decode(context: Context, uri: Uri): Bitmap {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return decodeLegacy(context, uri)
        val source = ImageDecoder.createSource(context.contentResolver, uri)
        return ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE // needed for Canvas drawing
            decoder.isMutableRequired = false
            val largest = max(info.size.width, info.size.height)
            if (largest > MAX_DIMENSION) {
                val scale = MAX_DIMENSION.toFloat() / largest
                decoder.setTargetSize((info.size.width * scale).roundToInt(), (info.size.height * scale).roundToInt())
            }
        }
    }

    /** Android 7–8: BitmapFactory with power-of-two downsampling. */
    private fun decodeLegacy(context: Context, uri: Uri): Bitmap {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / sample > MAX_DIMENSION) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        return context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
            ?: error("Cannot decode image")
    }

    fun transform(source: Bitmap, rotation: Int, flipH: Boolean, flipV: Boolean): Bitmap {
        if (rotation % 360 == 0 && !flipH && !flipV) return source
        val matrix = Matrix().apply {
            postRotate(rotation.toFloat())
            postScale(if (flipH) -1f else 1f, if (flipV) -1f else 1f)
        }
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    fun thumbnail(source: Bitmap, size: Int = 160): Bitmap {
        val scale = size.toFloat() / max(source.width, source.height)
        return Bitmap.createScaledBitmap(source, (source.width * scale).roundToInt().coerceAtLeast(1),
            (source.height * scale).roundToInt().coerceAtLeast(1), true)
    }

    /** Applies crop → color → drawing → text and returns the final bitmap. */
    fun render(transformed: Bitmap, edits: ImageEdits): Bitmap {
        val crop = edits.crop
        val src = Rect(
            (crop.left * transformed.width).roundToInt(),
            (crop.top * transformed.height).roundToInt(),
            (crop.right * transformed.width).roundToInt(),
            (crop.bottom * transformed.height).roundToInt(),
        )
        val w = src.width().coerceAtLeast(1)
        val h = src.height().coerceAtLeast(1)
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)

        val imagePaint = Paint(Paint.FILTER_BITMAP_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(FilterPreset.combined(edits))
        }
        canvas.drawBitmap(transformed, src, Rect(0, 0, w, h), imagePaint)

        if (edits.strokes.isNotEmpty()) {
            val layer = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val layerCanvas = Canvas(layer)
            edits.strokes.forEach { stroke -> drawStroke(layerCanvas, stroke, w, h) }
            canvas.drawBitmap(layer, 0f, 0f, null)
            layer.recycle()
        }

        edits.texts.forEach { text ->
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = text.color
                textSize = text.sizeFraction * w
                typeface = Typeface.DEFAULT_BOLD
                textAlign = Paint.Align.CENTER
                setShadowLayer(textSize * 0.08f, 0f, textSize * 0.04f, 0x99000000.toInt())
            }
            val baseline = text.y * h - (paint.descent() + paint.ascent()) / 2f
            canvas.drawText(text.text, text.x * w, baseline, paint)
        }
        return out
    }

    private fun drawStroke(canvas: Canvas, stroke: DrawStroke, w: Int, h: Int) {
        if (stroke.points.isEmpty()) return
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            strokeWidth = stroke.widthFraction * w
            color = stroke.color
            if (stroke.eraser) xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        }
        val path = Path()
        stroke.points.forEachIndexed { i, p ->
            if (i == 0) path.moveTo(p.x * w, p.y * h) else path.lineTo(p.x * w, p.y * h)
        }
        if (stroke.points.size == 1) path.lineTo(stroke.points[0].x * w + 0.1f, stroke.points[0].y * h)
        canvas.drawPath(path, paint)
    }
}

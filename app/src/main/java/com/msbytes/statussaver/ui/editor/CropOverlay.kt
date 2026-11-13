package com.msbytes.statussaver.ui.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.hypot

private const val MIN_SIZE = 0.1f

/**
 * Crop rectangle over the image, in normalized coordinates.
 * Drag inside to move, drag a corner to resize (keeps [aspect] when set).
 */
@Composable
fun CropOverlay(
    crop: Rect,
    aspect: Float?,
    imageAspect: Float,
    onCropChange: (Rect) -> Unit,
    onCropEnd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentCrop by rememberUpdatedState(crop)
    val currentAspect by rememberUpdatedState(aspect)
    Canvas(
        modifier
            .fillMaxSize()
            .pointerInput(imageAspect) {
                var mode = -1 // 0..3 corners, 4 move, -1 none
                val touchRadius = 36.dp.toPx()
                detectDragGestures(
                    onDragStart = { start ->
                        val r = currentCrop
                        val corners = listOf(
                            Offset(r.left * size.width, r.top * size.height),
                            Offset(r.right * size.width, r.top * size.height),
                            Offset(r.left * size.width, r.bottom * size.height),
                            Offset(r.right * size.width, r.bottom * size.height),
                        )
                        val nearest = corners.withIndex().minBy { (_, c) -> hypot(c.x - start.x, c.y - start.y) }
                        mode = when {
                            hypot(nearest.value.x - start.x, nearest.value.y - start.y) < touchRadius -> nearest.index
                            start.x / size.width in r.left..r.right && start.y / size.height in r.top..r.bottom -> 4
                            else -> -1
                        }
                    },
                    onDragEnd = { if (mode != -1) onCropEnd(); mode = -1 },
                    onDragCancel = { if (mode != -1) onCropEnd(); mode = -1 },
                    onDrag = { change, drag ->
                        if (mode == -1) return@detectDragGestures
                        change.consume()
                        val dx = drag.x / size.width
                        val dy = drag.y / size.height
                        val ratio = currentAspect?.let { it / imageAspect } // aspect in normalized units
                        onCropChange(
                            if (mode == 4) move(currentCrop, dx, dy) else resize(currentCrop, mode, dx, dy, ratio),
                        )
                    },
                )
            },
    ) {
        val r = Rect(
            crop.left * size.width,
            crop.top * size.height,
            crop.right * size.width,
            crop.bottom * size.height,
        )
        val dim = Color.Black.copy(alpha = 0.55f)
        drawRect(dim, Offset.Zero, Size(size.width, r.top))
        drawRect(dim, Offset(0f, r.bottom), Size(size.width, size.height - r.bottom))
        drawRect(dim, Offset(0f, r.top), Size(r.left, r.height))
        drawRect(dim, Offset(r.right, r.top), Size(size.width - r.right, r.height))

        drawRect(Color.White, r.topLeft, r.size, style = Stroke(2.dp.toPx()))
        val thin = Color.White.copy(alpha = 0.5f)
        for (i in 1..2) {
            val x = r.left + r.width * i / 3
            val y = r.top + r.height * i / 3
            drawLine(thin, Offset(x, r.top), Offset(x, r.bottom), 1.dp.toPx())
            drawLine(thin, Offset(r.left, y), Offset(r.right, y), 1.dp.toPx())
        }
        val handle = 18.dp.toPx()
        val w = 4.dp.toPx()
        listOf(r.topLeft to Offset(1f, 1f), r.topRight to Offset(-1f, 1f), r.bottomLeft to Offset(1f, -1f), r.bottomRight to Offset(-1f, -1f))
            .forEach { (p, dir) ->
                drawLine(Color.White, p, p + Offset(handle * dir.x, 0f), w)
                drawLine(Color.White, p, p + Offset(0f, handle * dir.y), w)
            }
    }
}

private fun move(r: Rect, dx: Float, dy: Float): Rect {
    val nx = (r.left + dx).coerceIn(0f, 1f - r.width)
    val ny = (r.top + dy).coerceIn(0f, 1f - r.height)
    return Rect(nx, ny, nx + r.width, ny + r.height)
}

/** @param ratio desired width/height in normalized units, or null for free resize. */
private fun resize(r: Rect, corner: Int, dx: Float, dy: Float, ratio: Float?): Rect {
    var l = r.left
    var t = r.top
    var rt = r.right
    var b = r.bottom
    val movesLeft = corner == 0 || corner == 2
    val movesTop = corner == 0 || corner == 1
    if (movesLeft) l = (l + dx).coerceIn(0f, rt - MIN_SIZE) else rt = (rt + dx).coerceIn(l + MIN_SIZE, 1f)
    if (ratio == null) {
        if (movesTop) t = (t + dy).coerceIn(0f, b - MIN_SIZE) else b = (b + dy).coerceIn(t + MIN_SIZE, 1f)
        return Rect(l, t, rt, b)
    }
    // Height follows width; shrink width if the height would leave the image.
    var w = rt - l
    var h = w / ratio
    val maxH = if (movesTop) b else 1f - t
    if (h > maxH) {
        h = maxH
        w = h * ratio
        if (movesLeft) l = rt - w else rt = l + w
    }
    if (movesTop) t = b - h else b = t + h
    return if (abs(w) < MIN_SIZE / 2) r else Rect(l, t, rt, b)
}

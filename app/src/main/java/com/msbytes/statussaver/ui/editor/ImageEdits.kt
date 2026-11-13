package com.msbytes.statussaver.ui.editor

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect

/** Text overlay. Position is the normalized center; size is relative to image width. */
data class TextLayer(
    val id: Long,
    val text: String,
    val color: Int,
    val sizeFraction: Float,
    val x: Float,
    val y: Float,
)

/** Freehand stroke in normalized (0..1) coordinates of the cropped image. */
data class DrawStroke(
    val points: List<Offset>,
    val color: Int,
    val widthFraction: Float,
    val eraser: Boolean,
)

enum class CropAspect(val ratio: Float?) {
    FREE(null), SQUARE(1f), PORTRAIT_4_5(4f / 5f), STORY_9_16(9f / 16f), WIDE_16_9(16f / 9f)
}

/** Immutable snapshot of every edit — undo/redo simply stores previous snapshots. */
data class ImageEdits(
    val rotation: Int = 0,
    val flipHorizontal: Boolean = false,
    val flipVertical: Boolean = false,
    val crop: Rect = FULL,
    val aspect: CropAspect = CropAspect.FREE,
    val brightness: Float = 0f,
    val contrast: Float = 0f,
    val saturation: Float = 0f,
    val filter: FilterPreset = FilterPreset.ORIGINAL,
    val texts: List<TextLayer> = emptyList(),
    val strokes: List<DrawStroke> = emptyList(),
) {
    companion object {
        val FULL = Rect(0f, 0f, 1f, 1f)
    }
}

package com.msbytes.statussaver.ui.editor

import android.graphics.ColorMatrix
import androidx.annotation.StringRes
import com.msbytes.statussaver.R

enum class FilterPreset(@StringRes val label: Int) {
    ORIGINAL(R.string.filter_original),
    VIVID(R.string.filter_vivid),
    WARM(R.string.filter_warm),
    COOL(R.string.filter_cool),
    MONO(R.string.filter_mono),
    SEPIA(R.string.filter_sepia),
    FADE(R.string.filter_fade),
    NOIR(R.string.filter_noir),
    ;

    fun matrix(): ColorMatrix = when (this) {
        ORIGINAL -> ColorMatrix()
        VIVID -> ColorMatrix().apply { setSaturation(1.45f); postConcat(contrast(1.1f)) }
        WARM -> ColorMatrix().apply { setScale(1.12f, 1.02f, 0.88f, 1f) }
        COOL -> ColorMatrix().apply { setScale(0.9f, 1.0f, 1.12f, 1f) }
        MONO -> ColorMatrix().apply { setSaturation(0f) }
        SEPIA -> ColorMatrix(
            floatArrayOf(
                0.393f, 0.769f, 0.189f, 0f, 0f,
                0.349f, 0.686f, 0.168f, 0f, 0f,
                0.272f, 0.534f, 0.131f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f,
            ),
        )
        FADE -> contrast(0.82f).apply { postConcat(brightness(0.12f)) }
        NOIR -> ColorMatrix().apply { setSaturation(0f); postConcat(contrast(1.45f)) }
    }

    companion object {
        /** Combined matrix for a filter plus brightness/contrast/saturation sliders (-1..1). */
        fun combined(edits: ImageEdits): ColorMatrix = edits.filter.matrix().apply {
            postConcat(ColorMatrix().apply { setSaturation(1f + edits.saturation) })
            postConcat(contrast(1f + edits.contrast * 0.8f))
            postConcat(brightness(edits.brightness * 0.4f))
        }

        private fun contrast(c: Float): ColorMatrix {
            val t = (1f - c) * 128f
            return ColorMatrix(
                floatArrayOf(
                    c, 0f, 0f, 0f, t,
                    0f, c, 0f, 0f, t,
                    0f, 0f, c, 0f, t,
                    0f, 0f, 0f, 1f, 0f,
                ),
            )
        }

        private fun brightness(b: Float): ColorMatrix {
            val v = b * 255f
            return ColorMatrix(
                floatArrayOf(
                    1f, 0f, 0f, 0f, v,
                    0f, 1f, 0f, 0f, v,
                    0f, 0f, 1f, 0f, v,
                    0f, 0f, 0f, 1f, 0f,
                ),
            )
        }
    }
}

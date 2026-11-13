package com.msbytes.statussaver.ui.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.roundToInt

/** Brush settings for the draw tool. */
data class BrushSettings(val color: Int, val widthFraction: Float, val eraser: Boolean)

/** Shows the image with all edits applied, and handles crop / text / draw gestures. */
@Composable
fun EditorCanvas(
    image: ImageBitmap,
    edits: ImageEdits,
    tool: EditorTool,
    brush: BrushSettings,
    onCropChange: (androidx.compose.ui.geometry.Rect) -> Unit,
    onCropEnd: () -> Unit,
    onTextMove: (id: Long, dx: Float, dy: Float) -> Unit,
    onTextMoveEnd: () -> Unit,
    onTextTap: (TextLayer) -> Unit,
    onStroke: (DrawStroke) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colorFilter = remember(edits.filter, edits.brightness, edits.contrast, edits.saturation) {
        ColorFilter.colorMatrix(ColorMatrix(FilterPreset.combined(edits).array))
    }
    val imageAspect = image.width.toFloat() / image.height

    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (tool == EditorTool.CROP) {
            Box(Modifier.aspectRatio(imageAspect)) {
                Image(image, null, Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds, colorFilter = colorFilter)
                CropOverlay(
                    crop = edits.crop,
                    aspect = edits.aspect.ratio,
                    imageAspect = imageAspect,
                    onCropChange = onCropChange,
                    onCropEnd = onCropEnd,
                )
            }
        } else {
            val crop = edits.crop
            val srcOffset = IntOffset((crop.left * image.width).roundToInt(), (crop.top * image.height).roundToInt())
            val srcSize = IntSize(
                (crop.width * image.width).roundToInt().coerceAtLeast(1),
                (crop.height * image.height).roundToInt().coerceAtLeast(1),
            )
            val currentStroke = remember { mutableStateListOf<Offset>() }

            BoxWithConstraints(Modifier.aspectRatio(srcSize.width.toFloat() / srcSize.height)) {
                val widthPx = constraints.maxWidth.toFloat()
                val heightPx = constraints.maxHeight.toFloat()

                Canvas(Modifier.fillMaxSize()) {
                    drawImage(
                        image = image,
                        srcOffset = srcOffset,
                        srcSize = srcSize,
                        dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
                        colorFilter = colorFilter,
                    )
                }

                // Drawing layer (offscreen so the eraser only clears strokes, never the photo).
                Canvas(
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                        .then(
                            if (tool == EditorTool.DRAW) {
                                Modifier.pointerInput(brush) {
                                    detectDragGestures(
                                        onDragStart = { currentStroke.clear(); currentStroke.add(normalize(it, size.width, size.height)) },
                                        onDrag = { change, _ ->
                                            change.consume()
                                            currentStroke.add(normalize(change.position, size.width, size.height))
                                        },
                                        onDragEnd = {
                                            if (currentStroke.isNotEmpty()) {
                                                onStroke(DrawStroke(currentStroke.toList(), brush.color, brush.widthFraction, brush.eraser))
                                            }
                                            currentStroke.clear()
                                        },
                                    )
                                }
                            } else {
                                Modifier
                            },
                        ),
                ) {
                    edits.strokes.forEach { drawStroke(it.points, it.color, it.widthFraction, it.eraser) }
                    if (currentStroke.isNotEmpty()) {
                        drawStroke(currentStroke, brush.color, brush.widthFraction, brush.eraser)
                    }
                }

                // Text layers.
                val density = LocalDensity.current
                edits.texts.forEach { layer ->
                    val fontSize = with(density) { (layer.sizeFraction * widthPx).toSp() }
                    Text(
                        text = layer.text,
                        style = TextStyle(
                            color = Color(layer.color),
                            fontSize = fontSize,
                            fontWeight = FontWeight.Bold,
                            shadow = Shadow(Color.Black.copy(alpha = 0.6f), Offset(0f, 2f), 6f),
                        ),
                        modifier = Modifier
                            .layout { measurable, constraints ->
                                val placeable = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
                                layout(constraints.maxWidth, constraints.maxHeight) {
                                    placeable.place(
                                        (layer.x * constraints.maxWidth - placeable.width / 2f).roundToInt(),
                                        (layer.y * constraints.maxHeight - placeable.height / 2f).roundToInt(),
                                    )
                                }
                            }
                            .then(
                                if (tool == EditorTool.TEXT) {
                                    Modifier
                                        .pointerInput(layer.id) { detectTapGestures { onTextTap(layer) } }
                                        .pointerInput(layer.id, widthPx, heightPx) {
                                            detectDragGestures(onDragEnd = onTextMoveEnd, onDragCancel = onTextMoveEnd) { change, drag ->
                                                change.consume()
                                                onTextMove(layer.id, drag.x / widthPx, drag.y / heightPx)
                                            }
                                        }
                                } else {
                                    Modifier
                                },
                            ),
                    )
                }
            }
        }
    }
}

private fun normalize(p: Offset, w: Int, h: Int) =
    Offset((p.x / w).coerceIn(0f, 1f), (p.y / h).coerceIn(0f, 1f))

private fun DrawScope.drawStroke(points: List<Offset>, color: Int, widthFraction: Float, eraser: Boolean) {
    if (points.isEmpty()) return
    val path = Path().apply {
        points.forEachIndexed { i, p ->
            val x = p.x * size.width
            val y = p.y * size.height
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        if (points.size == 1) lineTo(points[0].x * size.width + 0.1f, points[0].y * size.height)
    }
    drawPath(
        path = path,
        color = Color(color),
        style = Stroke(width = widthFraction * size.width, cap = StrokeCap.Round, join = StrokeJoin.Round),
        blendMode = if (eraser) BlendMode.Clear else BlendMode.SrcOver,
    )
}

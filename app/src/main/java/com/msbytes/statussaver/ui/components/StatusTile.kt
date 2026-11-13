package com.msbytes.statussaver.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.msbytes.statussaver.R
import com.msbytes.statussaver.core.Formatters
import com.msbytes.statussaver.core.MediaPreviewCache
import com.msbytes.statussaver.domain.model.StatusItem

const val TILE_ASPECT = 0.8f
val GRID_MIN_CELL = 110.dp

@Composable
fun StatusTile(
    item: StatusItem,
    isNew: Boolean,
    isSaved: Boolean,
    isSaving: Boolean,
    isFavorite: Boolean,
    selectionMode: Boolean,
    isSelected: Boolean,
    showDownload: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDownload: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val duration by produceState<Long?>(null, item.uri, item.isVideo) {
        if (item.isVideo) value = MediaPreviewCache.videoDuration(context, item.uri)
    }
    val description = if (item.isVideo) {
        stringResource(R.string.cd_status_video, duration?.let { Formatters.duration(it) } ?: "")
    } else {
        stringResource(R.string.cd_status_image)
    }
    val shape = MaterialTheme.shapes.medium

    Box(
        modifier
            .aspectRatio(TILE_ASPECT)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .then(if (isSelected) Modifier.border(3.dp, MaterialTheme.colorScheme.primary, shape) else Modifier)
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                },
            )
            .semantics { contentDescription = description },
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context).data(item.uri).size(THUMB_SIZE_PX).build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )

        if (item.isVideo) {
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.45f),
                modifier = Modifier.align(Alignment.Center).size(40.dp),
            ) {
                Icon(Icons.Rounded.PlayArrow, null, tint = Color.White, modifier = Modifier.padding(6.dp))
            }
            duration?.let {
                Text(
                    text = Formatters.duration(it),
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(6.dp)
                        .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
        }

        if (isNew) {
            Text(
                text = stringResource(R.string.badge_new),
                color = MaterialTheme.colorScheme.onTertiary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
                    .background(MaterialTheme.colorScheme.tertiary, RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            )
        } else if (isFavorite) {
            Icon(
                Icons.Rounded.Favorite,
                contentDescription = stringResource(R.string.cd_favorite),
                tint = Color(0xFFFF4D6D),
                modifier = Modifier.align(Alignment.TopStart).padding(6.dp).size(18.dp),
            )
        }

        if (selectionMode) {
            Icon(
                imageVector = if (isSelected) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                contentDescription = stringResource(if (isSelected) R.string.cd_selected else R.string.cd_not_selected),
                tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.White,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .background(Color.Black.copy(alpha = 0.3f), CircleShape),
            )
        } else if (showDownload) {
            DownloadButton(
                isSaved = isSaved,
                isSaving = isSaving,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onDownload()
                },
                modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp),
            )
        }
    }
}

@Composable
private fun DownloadButton(isSaved: Boolean, isSaving: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val state = when {
        isSaving -> 1
        isSaved -> 2
        else -> 0
    }
    Surface(
        shape = CircleShape,
        color = if (isSaved) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.5f),
        modifier = modifier.size(40.dp),
    ) {
        IconButton(onClick = onClick, enabled = state == 0) {
            AnimatedContent(
                targetState = state,
                transitionSpec = {
                    (scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn()) togetherWith fadeOut()
                },
                label = "download",
            ) { s ->
                when (s) {
                    1 -> CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    2 -> Icon(Icons.Rounded.Done, stringResource(R.string.cd_downloaded), tint = MaterialTheme.colorScheme.onPrimary)
                    else -> Icon(Icons.Rounded.Download, stringResource(R.string.cd_download), tint = Color.White)
                }
            }
        }
    }
}

private const val THUMB_SIZE_PX = 360

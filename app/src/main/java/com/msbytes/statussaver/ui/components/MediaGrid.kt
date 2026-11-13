package com.msbytes.statussaver.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.android.gms.ads.nativead.NativeAd
import com.msbytes.statussaver.ads.AdUnits
import com.msbytes.statussaver.ads.NativeAdCard
import com.msbytes.statussaver.domain.model.StatusItem

private sealed interface GridEntry {
    val key: String

    data class Media(val item: StatusItem) : GridEntry {
        override val key get() = item.id
    }

    data class Ad(val slot: Int) : GridEntry {
        override val key get() = "ad_$slot"
    }
}

/**
 * Responsive grid (3 columns on phones, more on tablets / landscape) with stable keys.
 * When [nativeAds] is not empty a full-width native ad row is inserted every 12 items.
 */
@Composable
fun MediaGrid(
    items: List<StatusItem>,
    savedNames: Set<String>,
    savingIds: Set<String>,
    favoriteIds: Set<String>,
    selection: Set<String>,
    newSince: Long,
    showDownload: Boolean,
    nativeAds: List<NativeAd>,
    onClick: (StatusItem) -> Unit,
    onLongClick: (StatusItem) -> Unit,
    onDownload: (StatusItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val entries = remember(items, nativeAds.size) {
        buildList {
            var slot = 0
            items.forEachIndexed { index, item ->
                add(GridEntry.Media(item))
                if ((index + 1) % AdUnits.NATIVE_EVERY_N_ITEMS == 0 && slot < nativeAds.size && index < items.lastIndex) {
                    add(GridEntry.Ad(slot++))
                }
            }
        }
    }
    val selectionMode = selection.isNotEmpty()

    LazyVerticalGrid(
        columns = GridCells.Adaptive(GRID_MIN_CELL),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 96.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(
            items = entries,
            key = { it.key },
            span = { if (it is GridEntry.Ad) GridItemSpan(maxLineSpan) else GridItemSpan(1) },
            contentType = { if (it is GridEntry.Ad) "ad" else "media" },
        ) { entry ->
            when (entry) {
                is GridEntry.Media -> {
                    val item = entry.item
                    StatusTile(
                        item = item,
                        isNew = item.lastModified > newSince,
                        isSaved = item.name in savedNames,
                        isSaving = item.id in savingIds,
                        isFavorite = item.id in favoriteIds,
                        selectionMode = selectionMode,
                        isSelected = item.id in selection,
                        showDownload = showDownload,
                        onClick = { onClick(item) },
                        onLongClick = { onLongClick(item) },
                        onDownload = { onDownload(item) },
                        modifier = Modifier.animateItem(),
                    )
                }
                is GridEntry.Ad -> nativeAds.getOrNull(entry.slot)?.let { NativeAdCard(it) }
            }
        }
    }
}

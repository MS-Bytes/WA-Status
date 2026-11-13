package com.msbytes.statussaver.ui.saved

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.msbytes.statussaver.R
import com.msbytes.statussaver.core.Formatters
import com.msbytes.statussaver.core.UiState
import com.msbytes.statussaver.domain.model.StatusItem
import com.msbytes.statussaver.ui.components.MediaGrid
import com.msbytes.statussaver.ui.components.MessageState
import com.msbytes.statussaver.ui.components.ShimmerGrid

@Composable
fun SavedTabContent(
    state: SavedUiState,
    onFilter: (SavedFilter) -> Unit,
    onRefresh: () -> Unit,
    onItemClick: (StatusItem) -> Unit,
    onItemLongClick: (StatusItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SavedFilter.entries.forEach { filter ->
                FilterChip(
                    selected = state.filter == filter,
                    onClick = { onFilter(filter) },
                    label = {
                        Text(
                            stringResource(
                                when (filter) {
                                    SavedFilter.ALL -> R.string.filter_all
                                    SavedFilter.IMAGES -> R.string.filter_images
                                    SavedFilter.VIDEOS -> R.string.filter_videos
                                    SavedFilter.FAVORITES -> R.string.filter_favorites
                                },
                            ),
                        )
                    },
                )
            }
        }
        Text(
            text = stringResource(
                R.string.settings_storage_summary,
                Formatters.size(state.totalBytes),
                state.imageCount,
                state.videoCount,
            ),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
        PullToRefreshBox(isRefreshing = false, onRefresh = onRefresh, modifier = Modifier.fillMaxSize()) {
            when (val content = state.content) {
                UiState.Loading -> ShimmerGrid()
                UiState.Empty -> if (state.filter == SavedFilter.FAVORITES) {
                    MessageState(
                        Icons.Rounded.FavoriteBorder,
                        stringResource(R.string.empty_favorites_title),
                        stringResource(R.string.empty_favorites_body),
                    )
                } else {
                    MessageState(
                        Icons.Rounded.DownloadDone,
                        stringResource(R.string.empty_saved_title),
                        stringResource(R.string.empty_saved_body),
                    )
                }
                is UiState.Error -> MessageState(
                    Icons.Rounded.CloudOff,
                    stringResource(R.string.error_title),
                    stringResource(content.message),
                    actionLabel = stringResource(R.string.action_retry),
                    onAction = onRefresh,
                )
                is UiState.Success -> MediaGrid(
                    items = content.data,
                    savedNames = emptySet(),
                    savingIds = emptySet(),
                    favoriteIds = state.favoriteIds,
                    selection = state.selection,
                    newSince = Long.MAX_VALUE,
                    showDownload = false,
                    nativeAds = emptyList(),
                    onClick = onItemClick,
                    onLongClick = onItemLongClick,
                    onDownload = {},
                )
            }
        }
    }
}

package com.msbytes.statussaver.ui.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.GetApp
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.google.android.gms.ads.nativead.NativeAd
import com.msbytes.statussaver.R
import com.msbytes.statussaver.core.UiState
import com.msbytes.statussaver.domain.model.MediaType
import com.msbytes.statussaver.domain.model.StatusItem
import com.msbytes.statussaver.domain.model.StatusSource
import com.msbytes.statussaver.ui.components.MediaGrid
import com.msbytes.statussaver.ui.components.MessageState
import com.msbytes.statussaver.ui.components.ShimmerGrid

@Composable
fun StatusTabContent(
    state: HomeUiState,
    type: MediaType,
    nativeAds: List<NativeAd>,
    onGrantAccess: () -> Unit,
    onRefresh: () -> Unit,
    onInstall: (StatusSource) -> Unit,
    onOpenMessenger: (StatusSource) -> Unit,
    onItemClick: (StatusItem) -> Unit,
    onItemLongClick: (StatusItem) -> Unit,
    onDownload: (StatusItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val source = state.source
    when {
        !state.installed && state.access != AccessState.GRANTED -> MessageState(
            icon = Icons.Rounded.GetApp,
            title = stringResource(R.string.not_installed_title, source.displayName),
            body = stringResource(R.string.not_installed_body, source.displayName),
            actionLabel = stringResource(R.string.action_install),
            onAction = { onInstall(source) },
            modifier = modifier,
        )

        state.access == AccessState.CHECKING -> ShimmerGrid(modifier)

        state.access != AccessState.GRANTED -> PermissionContent(
            source = source,
            access = state.access,
            onGrant = onGrantAccess,
            modifier = modifier,
        )

        else -> PullToRefreshBox(isRefreshing = state.isRefreshing, onRefresh = onRefresh, modifier = modifier) {
            when (val content = state.content) {
                UiState.Loading -> ShimmerGrid()
                is UiState.Error -> MessageState(
                    icon = Icons.Rounded.CloudOff,
                    title = stringResource(R.string.error_title),
                    body = stringResource(content.message),
                    actionLabel = stringResource(R.string.action_retry),
                    onAction = onRefresh,
                )
                else -> {
                    val items = (content as? UiState.Success)?.data.orEmpty().filter { it.type == type }
                    if (items.isEmpty()) {
                        MessageState(
                            icon = if (type == MediaType.IMAGE) Icons.Rounded.Image else Icons.Rounded.VideoLibrary,
                            title = stringResource(
                                if (type == MediaType.IMAGE) R.string.empty_images_title else R.string.empty_videos_title,
                            ),
                            body = stringResource(R.string.empty_statuses_body),
                            actionLabel = stringResource(
                                if (source == StatusSource.WHATSAPP) R.string.open_whatsapp else R.string.open_whatsapp_business,
                            ),
                            onAction = { onOpenMessenger(source) },
                        )
                    } else {
                        MediaGrid(
                            items = items,
                            savedNames = state.savedNames,
                            savingIds = state.savingIds,
                            favoriteIds = emptySet(),
                            selection = state.selection,
                            newSince = state.newSince,
                            showDownload = true,
                            nativeAds = nativeAds,
                            onClick = onItemClick,
                            onLongClick = onItemLongClick,
                            onDownload = onDownload,
                        )
                    }
                }
            }
        }
    }
}

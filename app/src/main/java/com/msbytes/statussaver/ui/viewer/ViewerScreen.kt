package com.msbytes.statussaver.ui.viewer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.automirrored.rounded.Reply
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil3.compose.AsyncImage
import com.msbytes.statussaver.R
import com.msbytes.statussaver.ads.BannerAd
import com.msbytes.statussaver.domain.model.StatusItem
import com.msbytes.statussaver.ui.common.AppSnackbarHost
import com.msbytes.statussaver.ui.common.HandleUiEvents
import com.msbytes.statussaver.ui.components.ConfirmDialog
import com.msbytes.statussaver.ui.components.InfoSheet

@Composable
fun ViewerScreen(
    onBack: () -> Unit,
    onEdit: (StatusItem) -> Unit,
    viewModel: ViewerViewModel = hiltViewModel(),
) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    val favorites by viewModel.favoriteIds.collectAsStateWithLifecycle()
    val savedNames by viewModel.savedNames.collectAsStateWithLifecycle()
    val saving by viewModel.saving.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    HandleUiEvents(viewModel.events, snackbarHostState, onDeleteConfirmed = viewModel::refreshSaved)

    if (items.isEmpty()) {
        LaunchedEffect(Unit) { onBack() }
        return
    }

    val startIndex = remember { items.indexOfFirst { it.id == viewModel.startId }.coerceAtLeast(0) }
    val pagerState = rememberPagerState(initialPage = startIndex, pageCount = { items.size })
    val current = items.getOrNull(pagerState.currentPage) ?: items.last()

    val context = LocalContext.current
    var muted by remember { mutableStateOf(false) }
    var looping by remember { mutableStateOf(true) }
    val player = remember {
        ExoPlayer.Builder(context).build().apply { repeatMode = Player.REPEAT_MODE_ONE }
    }
    DisposableEffect(Unit) { onDispose { player.release() } }

    // Pause playback whenever the screen is not visible (ads, share sheet, background).
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) player.pause()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(current.id) {
        if (current.isVideo) {
            player.setMediaItem(MediaItem.fromUri(current.uri))
            player.prepare()
            player.playWhenReady = true
        } else {
            player.stop()
        }
    }
    LaunchedEffect(muted) { player.volume = if (muted) 0f else 1f }
    LaunchedEffect(looping) { player.repeatMode = if (looping) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF }

    var showInfo by remember { mutableStateOf<StatusItem?>(null) }
    var confirmDelete by remember { mutableStateOf<StatusItem?>(null) }
    var menuOpen by remember { mutableStateOf(false) }
    val repostTitle = stringResource(R.string.repost_chooser_title)

    Scaffold(
        containerColor = Color.Black,
        contentColor = Color.White,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black.copy(alpha = 0.6f),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White,
                ),
                title = { Text(stringResource(R.string.viewer_position, pagerState.currentPage + 1, items.size)) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.cd_back)) }
                },
                actions = {
                    if (current.isVideo) {
                        IconButton(onClick = { muted = !muted }) {
                            Icon(
                                if (muted) Icons.AutoMirrored.Rounded.VolumeOff else Icons.AutoMirrored.Rounded.VolumeUp,
                                stringResource(if (muted) R.string.viewer_unmute else R.string.viewer_mute),
                            )
                        }
                        IconButton(onClick = { looping = !looping }) {
                            Icon(
                                if (looping) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                                stringResource(if (looping) R.string.viewer_loop_on else R.string.viewer_loop_off),
                            )
                        }
                    }
                    val isFavorite = current.id in favorites
                    IconButton(onClick = { viewModel.toggleFavorite(current) }) {
                        Icon(
                            if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            stringResource(if (isFavorite) R.string.action_favorite_remove else R.string.action_favorite_add),
                        )
                    }
                    if (!current.isSaved) {
                        val saved = current.name in savedNames
                        IconButton(onClick = { viewModel.save(current) }, enabled = !saved && current.id !in saving) {
                            when {
                                current.id in saving -> CircularProgressIndicator(Modifier.padding(12.dp), color = Color.White, strokeWidth = 2.dp)
                                saved -> Icon(Icons.Rounded.Done, stringResource(R.string.cd_downloaded), tint = Color.White)
                                else -> Icon(Icons.Rounded.Download, stringResource(R.string.cd_download))
                            }
                        }
                    }
                    IconButton(onClick = { viewModel.share(current) }) {
                        Icon(Icons.Rounded.Share, stringResource(R.string.action_share))
                    }
                    Box {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(Icons.Rounded.MoreVert, stringResource(R.string.cd_more_options))
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_repost)) },
                                leadingIcon = { Icon(Icons.AutoMirrored.Rounded.Reply, null) },
                                onClick = { menuOpen = false; viewModel.repost(current, repostTitle) },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_edit)) },
                                leadingIcon = { Icon(Icons.Rounded.Edit, null) },
                                onClick = { menuOpen = false; player.pause(); onEdit(current) },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_info)) },
                                leadingIcon = { Icon(Icons.Rounded.Info, null) },
                                onClick = { menuOpen = false; showInfo = current },
                            )
                            if (current.isSaved) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.action_delete)) },
                                    leadingIcon = { Icon(Icons.Rounded.Delete, null) },
                                    onClick = { menuOpen = false; confirmDelete = current },
                                )
                            }
                        }
                    }
                },
            )
        },
        // Banner is at the bottom, far from the save/download controls in the top bar.
        bottomBar = { Column(Modifier.navigationBarsPadding()) { BannerAd() } },
        snackbarHost = { AppSnackbarHost(snackbarHostState) },
    ) { padding ->
        HorizontalPager(
            state = pagerState,
            key = { items[it].id },
            modifier = Modifier.fillMaxSize().padding(padding).background(Color.Black),
        ) { page ->
            val item = items[page]
            if (item.isVideo) {
                if (page == pagerState.currentPage) {
                    AndroidView(
                        factory = { ctx -> PlayerView(ctx).apply { this.player = player } },
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    AsyncImage(item.uri, null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                }
            } else {
                ZoomableImage(item)
            }
        }
    }

    showInfo?.let { InfoSheet(it) { showInfo = null } }
    confirmDelete?.let { item ->
        ConfirmDialog(
            title = stringResource(R.string.delete_confirm_title),
            text = pluralStringResource(R.plurals.delete_confirm_body, 1, 1),
            confirmLabel = stringResource(R.string.action_delete),
            onConfirm = { viewModel.delete(item) },
            onDismiss = { confirmDelete = null },
        )
    }
}


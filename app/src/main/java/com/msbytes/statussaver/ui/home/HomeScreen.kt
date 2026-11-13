package com.msbytes.statussaver.ui.home

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.msbytes.statussaver.R
import com.msbytes.statussaver.ads.BannerAd
import com.msbytes.statussaver.ads.LocalAdManager
import com.msbytes.statussaver.core.IntentUtils
import com.msbytes.statussaver.domain.model.MediaType
import com.msbytes.statussaver.domain.model.SortOrder
import com.msbytes.statussaver.domain.model.StatusItem
import com.msbytes.statussaver.domain.model.StatusSource
import com.msbytes.statussaver.ui.common.AppSnackbarHost
import com.msbytes.statussaver.ui.common.HandleUiEvents
import com.msbytes.statussaver.ui.components.ConfirmDialog
import com.msbytes.statussaver.ui.navigation.ViewerCollection
import com.msbytes.statussaver.ui.saved.SavedTabContent
import com.msbytes.statussaver.ui.saved.SavedViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val PAGE_IMAGES = 0
private const val PAGE_VIDEOS = 1
private const val PAGE_SAVED = 2
private const val RESCAN_INTERVAL_MS = 10_000L

@Composable
fun HomeScreen(
    onOpenViewer: (collection: String, startId: String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenDirectChat: () -> Unit,
    onOpenFaq: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
    savedViewModel: SavedViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val savedState by savedViewModel.uiState.collectAsStateWithLifecycle()
    val adManager = LocalAdManager.current
    val nativeAds = adManager?.nativeAds?.collectAsStateWithLifecycle()?.value.orEmpty()
    val adsEnabled = adManager?.adsEnabled?.collectAsStateWithLifecycle()?.value ?: false

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val pagerState = rememberPagerState(pageCount = { 3 })
    val page = pagerState.currentPage
    var confirmDelete by remember { mutableStateOf(false) }

    HandleUiEvents(viewModel.events, snackbarHostState)
    HandleUiEvents(savedViewModel.events, snackbarHostState, onDeleteConfirmed = savedViewModel::refresh)

    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) {
        viewModel.onFolderPicked(it)
    }

    // Lifecycle: refresh on resume, mark as seen on stop, light periodic rescan while visible.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> viewModel.onStart()
                Lifecycle.Event.ON_STOP -> viewModel.onStop()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                delay(RESCAN_INTERVAL_MS)
                viewModel.refresh(silent = true)
            }
        }
    }
    LaunchedEffect(page) {
        viewModel.clearSelection()
        savedViewModel.clearSelection()
        if (page == PAGE_SAVED) savedViewModel.refresh() else viewModel.refresh(silent = true)
    }

    // No full-screen ads while the permission guide is visible.
    val permissionVisible = page != PAGE_SAVED && state.access != AccessState.GRANTED
    DisposableEffect(permissionVisible, adManager) {
        adManager?.permissionScreenVisible = permissionVisible
        onDispose { adManager?.permissionScreenVisible = false }
    }

    val selectionCount = if (page == PAGE_SAVED) savedState.selection.size else state.selection.size
    val selectionMode = selectionCount > 0
    BackHandler(enabled = selectionMode) {
        viewModel.clearSelection()
        savedViewModel.clearSelection()
    }
    val currentType = if (page == PAGE_VIDEOS) MediaType.VIDEO else MediaType.IMAGE

    Scaffold(
        topBar = {
            if (selectionMode) {
                TopAppBar(
                    title = { Text(pluralStringResource(R.plurals.selected_count, selectionCount, selectionCount)) },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.clearSelection(); savedViewModel.clearSelection() }) {
                            Icon(Icons.Rounded.Close, stringResource(R.string.action_clear_selection))
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            if (page == PAGE_SAVED) savedViewModel.selectAll() else viewModel.selectAll(currentType)
                        }) { Icon(Icons.Rounded.SelectAll, stringResource(R.string.action_select_all)) }
                    },
                )
            } else {
                HomeTopBar(
                    sortOrder = state.sortOrder,
                    onSort = viewModel::setSortOrder,
                    onDirectChat = onOpenDirectChat,
                    onSettings = onOpenSettings,
                    onHelp = onOpenFaq,
                )
            }
        },
        bottomBar = {
            Column(Modifier.navigationBarsPadding()) {
                if (selectionMode) {
                    SelectionBar(
                        isSavedTab = page == PAGE_SAVED,
                        onSave = { viewModel.saveSelected(currentType) },
                        onShare = { if (page == PAGE_SAVED) savedViewModel.shareSelected() else viewModel.shareSelected(currentType) },
                        onDelete = { confirmDelete = true },
                    )
                } else if (page == PAGE_SAVED && adsEnabled) {
                    BannerAd()
                }
            }
        },
        snackbarHost = { AppSnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            PrimaryTabRow(selectedTabIndex = page) {
                listOf(R.string.tab_images, R.string.tab_videos, R.string.tab_saved).forEachIndexed { index, title ->
                    Tab(
                        selected = page == index,
                        onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                        text = { Text(stringResource(title)) },
                    )
                }
            }
            AnimatedVisibility(visible = page != PAGE_SAVED) {
                SourceSwitcher(
                    selected = state.source,
                    onSelect = viewModel::selectSource,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize(), beyondViewportPageCount = 1) { index ->
                val onItemClick: (StatusItem) -> Unit = { item ->
                    if (state.selection.isNotEmpty()) {
                        viewModel.toggleSelection(item)
                    } else {
                        onOpenViewer(ViewerCollection.of(state.source, item.type), item.id)
                    }
                }
                when (index) {
                    PAGE_IMAGES, PAGE_VIDEOS -> StatusTabContent(
                        state = state,
                        type = if (index == PAGE_IMAGES) MediaType.IMAGE else MediaType.VIDEO,
                        nativeAds = if (adsEnabled) nativeAds else emptyList(),
                        onGrantAccess = { folderPicker.launch(viewModel.initialFolderUri()) },
                        onRefresh = { viewModel.refresh() },
                        onInstall = { source ->
                            adManager?.suppressNextAppOpen()
                            runCatching { context.startActivity(IntentUtils.playStoreApp(source.packageName)) }
                        },
                        onOpenMessenger = { source ->
                            IntentUtils.launchApp(context, source.packageName)?.let {
                                adManager?.suppressNextAppOpen()
                                runCatching { context.startActivity(it) }
                            }
                        },
                        onItemClick = onItemClick,
                        onItemLongClick = viewModel::toggleSelection,
                        onDownload = viewModel::save,
                    )

                    else -> SavedTabContent(
                        state = savedState,
                        onFilter = savedViewModel::setFilter,
                        onRefresh = savedViewModel::refresh,
                        onItemClick = { item ->
                            if (savedState.selection.isNotEmpty()) {
                                savedViewModel.toggleSelection(item)
                            } else {
                                onOpenViewer(ViewerCollection.SAVED, item.id)
                            }
                        },
                        onItemLongClick = savedViewModel::toggleSelection,
                    )
                }
            }
        }
    }

    if (confirmDelete) {
        val count = savedState.selection.size
        ConfirmDialog(
            title = stringResource(R.string.delete_confirm_title),
            text = pluralStringResource(R.plurals.delete_confirm_body, count, count),
            confirmLabel = stringResource(R.string.action_delete),
            onConfirm = savedViewModel::deleteSelected,
            onDismiss = { confirmDelete = false },
        )
    }
}

@Composable
private fun HomeTopBar(
    sortOrder: SortOrder,
    onSort: (SortOrder) -> Unit,
    onDirectChat: () -> Unit,
    onSettings: () -> Unit,
    onHelp: () -> Unit,
) {
    var sortMenu by rememberSaveable { mutableStateOf(false) }
    TopAppBar(
        title = {
            Column {
                Text(stringResource(R.string.app_short_name), style = MaterialTheme.typography.titleLarge)
                Text(
                    stringResource(R.string.works_with),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        actions = {
            IconButton(onClick = onDirectChat) {
                Icon(Icons.AutoMirrored.Rounded.Chat, stringResource(R.string.menu_direct_chat))
            }
            IconButton(onClick = { sortMenu = true }) {
                Icon(Icons.AutoMirrored.Rounded.Sort, stringResource(R.string.action_sort))
            }
            DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                SortOrder.entries.forEach { order ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                stringResource(
                                    when (order) {
                                        SortOrder.NEWEST -> R.string.sort_newest
                                        SortOrder.OLDEST -> R.string.sort_oldest
                                        SortOrder.LARGEST -> R.string.sort_largest
                                        SortOrder.SMALLEST -> R.string.sort_smallest
                                    },
                                ),
                            )
                        },
                        leadingIcon = { RadioButton(selected = order == sortOrder, onClick = null) },
                        onClick = { sortMenu = false; onSort(order) },
                    )
                }
            }
            IconButton(onClick = onHelp) {
                Icon(Icons.AutoMirrored.Rounded.HelpOutline, stringResource(R.string.menu_how_to_use))
            }
            IconButton(onClick = onSettings) {
                Icon(Icons.Rounded.Settings, stringResource(R.string.cd_settings))
            }
        },
    )
}

@Composable
private fun SourceSwitcher(selected: StatusSource, onSelect: (StatusSource) -> Unit, modifier: Modifier = Modifier) {
    SingleChoiceSegmentedButtonRow(modifier.fillMaxWidth()) {
        StatusSource.entries.forEachIndexed { index, source ->
            SegmentedButton(
                selected = selected == source,
                onClick = { onSelect(source) },
                shape = SegmentedButtonDefaults.itemShape(index, StatusSource.entries.size),
            ) {
                Text(stringResource(if (source == StatusSource.WHATSAPP) R.string.source_whatsapp else R.string.source_business))
            }
        }
    }
}

@Composable
private fun SelectionBar(isSavedTab: Boolean, onSave: () -> Unit, onShare: () -> Unit, onDelete: () -> Unit) {
    BottomAppBar {
        if (!isSavedTab) {
            TextButton(onClick = onSave) {
                Icon(Icons.Rounded.Download, null)
                Text(stringResource(R.string.bulk_save), Modifier.padding(start = 8.dp))
            }
        }
        TextButton(onClick = onShare) {
            Icon(Icons.Rounded.Share, null)
            Text(stringResource(R.string.bulk_share), Modifier.padding(start = 8.dp))
        }
        if (isSavedTab) {
            TextButton(onClick = onDelete) {
                Icon(Icons.Rounded.Delete, null)
                Text(stringResource(R.string.bulk_delete), Modifier.padding(start = 8.dp))
            }
        }
    }
}


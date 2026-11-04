package com.nuvio.tv.ui.screens.library

import android.view.KeyEvent as AndroidKeyEvent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MenuDefaults
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.nuvio.tv.ui.launcher.LauncherStyle
import com.nuvio.tv.core.cloud.CloudLibraryFile
import com.nuvio.tv.core.cloud.CloudLibraryItem
import com.nuvio.tv.core.cloud.CloudLibraryItemType
import com.nuvio.tv.core.cloud.CloudLibraryPlaybackInfo
import com.nuvio.tv.domain.model.LibraryListTab
import com.nuvio.tv.domain.model.LibrarySourceMode
import com.nuvio.tv.domain.model.PosterShape
import com.nuvio.tv.data.local.PlayerPreference
import com.nuvio.tv.ui.components.EmptyScreenState
import com.nuvio.tv.ui.components.GridContentCard
import com.nuvio.tv.ui.screens.detail.requestFocusAfterFrames
import com.nuvio.tv.ui.screens.home.HeroBackdropState
import com.nuvio.tv.ui.screens.stream.PlayerChoiceDialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.nuvio.tv.ui.components.PosterCardDefaults
import com.nuvio.tv.domain.model.localizedTitle
import com.nuvio.tv.ui.components.LoadingIndicator
import com.nuvio.tv.ui.components.NuvioDialog
import com.nuvio.tv.ui.theme.NuvioTheme
import com.nuvio.tv.ui.util.localizedContentType
import com.nuvio.tv.ui.util.localizedGenreLabel
import kotlinx.coroutines.delay
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.TextStyle
import com.nuvio.tv.R

private const val KEY_REPEAT_THROTTLE_MS = 80L

private enum class LibraryViewMode {
    Saved,
    Cloud,
    Servers
}

@Composable
private fun localizedTypeLabel(key: String): String = when (key.lowercase()) {
    LibraryTypeTab.ALL_KEY -> stringResource(R.string.library_type_all)
    else -> localizedContentType(key)
}

@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel = hiltViewModel(),
    serversViewModel: LibraryServersViewModel = hiltViewModel(),
    showBuiltInHeader: Boolean = true,
    onNavigateToDetail: (String, String, String?) -> Unit,
    onNavigateToCatalogSeeAll: (String, String, String) -> Unit = { _, _, _ -> },
    onCloudPlaybackResolved: (CloudLibraryPlaybackInfo) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val watchedMovieIds by viewModel.watchedMovieIds.collectAsState()
    val watchedSeriesIds by viewModel.watchedSeriesIds.collectAsState()
    val activityContext = LocalContext.current
    val scope = rememberCoroutineScope()
    var showDeleteConfirm by remember(uiState.showManageDialog) { mutableStateOf(false) }
    var expandedPicker by remember { mutableStateOf<String?>(null) }
    var selectedViewMode by rememberSaveable { mutableStateOf(LibraryViewMode.Saved) }
    val servers by serversViewModel.servers.collectAsState()
    val serverShelves by serversViewModel.shelves.collectAsState()
    val hasServers = servers.enabledConnections.isNotEmpty()
    val viewModes = LibraryViewMode.entries.filter { it != LibraryViewMode.Servers || hasServers }
    val viewMode = selectedViewMode.takeIf { it in viewModes } ?: LibraryViewMode.Saved
    var activeCloudItem by remember { mutableStateOf<CloudLibraryItem?>(null) }
    var pendingCloudPlayback by remember { mutableStateOf<CloudLibraryPlaybackInfo?>(null) }
    var showCloudPlayerChoice by remember { mutableStateOf(false) }
    val primaryFocusRequester = remember { FocusRequester() }
    val selectorFocusRequester = remember { FocusRequester() }
    val gridState = rememberLazyGridState()
    var pendingPrimaryFocus by remember { mutableStateOf(true) }
    var serverFocusTarget by remember {
        mutableStateOf(serversViewModel.focusedShelfKey?.let { key -> serversViewModel.focusedIndexes[key]?.let { key to it } })
    }
    var lastFocusedPosterKey by rememberSaveable { mutableStateOf<String?>(null) }
    val visibleItemKeys = remember(uiState.visibleItems) {
        uiState.visibleItems.map { "${it.type}:${it.id}" }
    }
    val visibleItemIndexByKey = remember(visibleItemKeys) {
        visibleItemKeys.withIndex().associate { (index, key) -> key to index }
    }
    val posterFocusRequesters = remember(visibleItemKeys) {
        visibleItemKeys.associateWith { FocusRequester() }
    }
    val layoutDirection = LocalLayoutDirection.current
    val firstVisiblePosterKey = visibleItemKeys.firstOrNull()
    val firstVisibleCardKey = visibleItemKeys.let { keys ->
        if (layoutDirection == LayoutDirection.Rtl) {
            keys.lastOrNull()  // Last in logical order = rightmost in RTL
        } else {
            keys.firstOrNull() // First in logical order = leftmost in LTR
        }
    }
    val posterCardStyle = PosterCardDefaults.Style.copy(
        width = uiState.posterCardWidthDp.dp,
        height = (uiState.posterCardWidthDp * 3 / 2).dp,
        cornerRadius = uiState.posterCardCornerRadiusDp.dp
    )

    val routeCloudPlayback: (CloudLibraryPlaybackInfo) -> Unit = { info ->
        scope.launch {
            when (viewModel.getPlayerPreference()) {
                PlayerPreference.INTERNAL -> onCloudPlaybackResolved(info)
                PlayerPreference.EXTERNAL -> {
                    viewModel.launchCloudPlaybackExternally(info, activityContext)
                }
                PlayerPreference.ASK_EVERY_TIME -> {
                    pendingCloudPlayback = info
                    showCloudPlayerChoice = true
                }
            }
        }
    }

    LaunchedEffect(viewMode, uiState.cloudLibrary.isEnabled, uiState.cloudLibrary.isLoaded, uiState.cloudLibrarySettingsVersion) {
        if (viewMode == LibraryViewMode.Cloud) {
            viewModel.ensureCloudLibraryLoaded()
        }
    }

    LaunchedEffect(viewMode, servers.revision) {
        if (viewMode == LibraryViewMode.Servers) serversViewModel.load()
    }

    LaunchedEffect(uiState.isLoading) {
        if (uiState.isLoading) {
            pendingPrimaryFocus = true
        }
    }

    LaunchedEffect(uiState.isLoading, uiState.sourceMode, uiState.listTabs.size) {
        if (!uiState.isLoading && pendingPrimaryFocus && viewMode == LibraryViewMode.Servers && serverFocusTarget != null) {
            pendingPrimaryFocus = false
        }
        if (!uiState.isLoading && pendingPrimaryFocus) {
            val restoreKey = lastFocusedPosterKey
            val restoreIndex = restoreKey?.let { visibleItemIndexByKey[it] }
            val restoreRequester = restoreKey?.let { posterFocusRequesters[it] }

            var focused = false
            if (restoreIndex != null && restoreRequester != null) {
                if (gridState.firstVisibleItemIndex == 0 && gridState.firstVisibleItemScrollOffset == 0) {
                    runCatching { gridState.scrollToItem(restoreIndex) }
                }
                focused = runCatching { restoreRequester.requestFocus() }.isSuccess
                if (!focused) {
                    delay(16)
                    focused = runCatching { restoreRequester.requestFocus() }.isSuccess
                }
            }

            if (!focused) {
                focused = runCatching { primaryFocusRequester.requestFocus() }.isSuccess
            }
            if (!focused) {
                delay(16)
                runCatching { primaryFocusRequester.requestFocus() }
            }
            pendingPrimaryFocus = false
        }
    }

    LaunchedEffect(uiState.sortSelectionVersion, firstVisiblePosterKey) {
        if (uiState.sortSelectionVersion <= 0L) return@LaunchedEffect
        val targetKey = firstVisiblePosterKey ?: return@LaunchedEffect
        runCatching { gridState.scrollToItem(0) }
        var focused = false
        repeat(6) {
            focused = posterFocusRequesters[targetKey]
                ?.let { requester -> runCatching { requester.requestFocus() }.isSuccess }
                ?: false
            if (focused) return@LaunchedEffect
            delay(24)
        }
    }

    // Nuvio Launcher: the blurred picture of the last focused title instead of a solid page.
    com.nuvio.tv.ui.launcher.LauncherBlurredBackdrop(com.nuvio.tv.LauncherChrome.lastBackdrop.value)

    if (uiState.isLoading) {
        val loadingFocusRequester = remember { FocusRequester() }
        val loadingLabel = stringResource(R.string.library_syncing_library)
        LaunchedEffect(uiState.isLoading) {
            loadingFocusRequester.requestFocus()
        }

        Box(
            modifier = Modifier
                .fillMaxSize(),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .focusRequester(loadingFocusRequester)
                    .semantics { contentDescription = loadingLabel }
                    .focusable()
            )
            Column(
                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                LoadingIndicator()
                Text(
                    text = stringResource(R.string.library_syncing_library),
                    style = MaterialTheme.typography.bodyMedium,
                    color = NuvioTheme.colors.TextSecondary
                )
            }
        }
        return
    }

    val lastKeyRepeatTime = remember { longArrayOf(0L) }

    val globalLandscape = com.nuvio.tv.ui.components.LocalLandscapePosterMode.current

    // Nuvio Launcher: the Home tiles' size, spacing and zoom (Settings → Launcher → catalog tiles).
    val launcherSettings by com.nuvio.tv.data.local.LauncherSettingsHolder.settings.collectAsState()
    val tileScale = launcherSettings.catalogScale
    val tileZoom = launcherSettings.catalogZoom
    val tileWidth = libraryTileWidth(globalLandscape, tileScale)
    val tileGap = com.nuvio.tv.ui.launcher.zoomSafeGap(launcherSettings.catalogTileSpacingDp.dp, tileWidth, tileZoom)
    val savedGrid = viewMode == LibraryViewMode.Saved

    LazyVerticalGrid(
        // Saved: fixed-width launcher tiles from the left edge; Cloud and Servers: full-width rows.
        columns = if (savedGrid) GridCells.FixedSize(tileWidth) else GridCells.Fixed(1),
        state = gridState,
        modifier = Modifier
            .fillMaxSize()
            .focusRestorer {
                val lastKey = lastFocusedPosterKey
                (if (lastKey != null && lastKey in posterFocusRequesters) {
                    posterFocusRequesters[lastKey]
                } else {
                    posterFocusRequesters[firstVisibleCardKey]
                }) ?: FocusRequester()
            }
            .onPreviewKeyEvent { event ->
                val native = event.nativeKeyEvent
                if (native.action == AndroidKeyEvent.ACTION_DOWN && native.repeatCount > 0) {
                    val now = System.currentTimeMillis()
                    if (now - lastKeyRepeatTime[0] < KEY_REPEAT_THROTTLE_MS) {
                        return@onPreviewKeyEvent true
                    }
                    lastKeyRepeatTime[0] = now
                }
                false
            },
        // Title at ~64, pills at ~108, first tiles at ~160 (mockup). The end padding is smaller so six
        // posters fit across at the default size, as on Home.
        contentPadding = PaddingValues(
            start = LauncherStyle.Start,
            end = if (savedGrid) 24.dp else LauncherStyle.Start,
            top = 60.dp,
            bottom = 48.dp
        ),
        horizontalArrangement = Arrangement.spacedBy(if (savedGrid) tileGap else 0.dp),
        verticalArrangement = Arrangement.spacedBy(26.dp)
    ) {
        item(key = "library_header", span = { GridItemSpan(maxLineSpan) }) {
            Column {
                Text(
                    text = stringResource(R.string.library_title),
                    color = Color.White,
                    fontSize = 26.sp,
                    lineHeight = 32.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(16.dp))
                // One compact row: source tabs, then the filters of the current source.
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    LibraryViewModePills(
                        modes = viewModes,
                        selectedMode = viewMode,
                        primaryFocusRequester = primaryFocusRequester,
                        onSelected = { mode ->
                            selectedViewMode = mode
                            expandedPicker = null
                        }
                    )
                    if (viewMode != LibraryViewMode.Servers) {
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    when (viewMode) {
                        LibraryViewMode.Saved -> {
                            LibraryFilterPills(
                                sourceMode = uiState.sourceMode,
                                listTabs = uiState.listTabs,
                                typeTabs = uiState.availableTypeTabs,
                                sortOptions = uiState.availableSortOptions,
                                genres = uiState.availableGenres,
                                years = uiState.availableYears,
                                selectedListKey = uiState.selectedListKey,
                                selectedTypeTab = uiState.selectedTypeTab,
                                selectedSortOption = uiState.selectedSortOption,
                                selectedGenre = uiState.selectedGenre,
                                selectedYear = uiState.selectedYear,
                                selectedWatchedFilter = uiState.selectedWatchedFilter,
                                primaryFocusRequester = selectorFocusRequester,
                                expandedPicker = expandedPicker,
                                onExpandedChange = { picker, shouldExpand ->
                                    expandedPicker = if (shouldExpand) picker else null
                                },
                                onSelectList = { key ->
                                    viewModel.onSelectListTab(key)
                                    expandedPicker = null
                                },
                                onSelectType = { type ->
                                    viewModel.onSelectTypeTab(type)
                                    expandedPicker = null
                                },
                                onSelectSort = { sort ->
                                    viewModel.onSelectSortOption(sort)
                                    expandedPicker = null
                                },
                                onSelectGenre = { key ->
                                    viewModel.onSelectGenre(key)
                                    expandedPicker = null
                                },
                                onSelectYear = { key ->
                                    viewModel.onSelectYear(key)
                                    expandedPicker = null
                                },
                                onSelectWatchedFilter = { filter ->
                                    viewModel.onSelectWatchedFilter(filter)
                                    expandedPicker = null
                                }
                            )
                            if (uiState.listManagement != null && uiState.isTrackingAuthenticated) {
                                LibraryActionPills(
                                    pending = uiState.pendingOperation,
                                    isSyncing = uiState.isSyncing,
                                    showManageLists = uiState.listManagement != null,
                                    onManageLists = viewModel::onOpenManageLists,
                                    onRefresh = viewModel::onRefresh
                                )
                            }
                        }
                        LibraryViewMode.Cloud -> {
                            CloudLibraryFilterPills(
                                providerOptions = uiState.availableCloudProviders,
                                typeOptions = uiState.availableCloudTypes,
                                selectedProviderId = uiState.selectedCloudProviderId,
                                selectedType = uiState.selectedCloudType,
                                expandedPicker = expandedPicker,
                                onExpandedChange = { picker, shouldExpand ->
                                    expandedPicker = if (shouldExpand) picker else null
                                },
                                onSelectProvider = { providerId ->
                                    viewModel.onSelectCloudProvider(providerId)
                                    expandedPicker = null
                                },
                                onSelectType = { type ->
                                    viewModel.onSelectCloudType(type)
                                    expandedPicker = null
                                }
                            )
                            // Refresh belongs to the cloud view only, in line with its filters.
                            LibraryPill(
                                text = if (uiState.cloudLibrary.isRefreshing) {
                                    stringResource(R.string.library_syncing_btn)
                                } else {
                                    stringResource(R.string.cloud_library_refresh)
                                },
                                selected = false,
                                enabled = !uiState.cloudLibrary.isRefreshing,
                                onClick = viewModel::refreshCloudLibrary
                            )
                        }
                        LibraryViewMode.Servers -> Unit
                    }
                }
            }
        }

        if (viewMode == LibraryViewMode.Saved) {
            if (uiState.visibleItems.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    val selectedTypeLabel = uiState.selectedTypeTab?.let { localizedTypeLabel(it.key) }?.lowercase() ?: stringResource(R.string.library_type_items)
                    val title = when {
                        uiState.sourceMode == LibrarySourceMode.TRAKT && !uiState.isTrackingAuthenticated -> stringResource(R.string.library_empty_trakt_not_auth_title)
                        uiState.sourceMode == LibrarySourceMode.SIMKL && !uiState.isTrackingAuthenticated -> stringResource(R.string.library_empty_simkl_not_auth_title)
                        uiState.sourceMode == LibrarySourceMode.TRAKT || uiState.sourceMode == LibrarySourceMode.MDBLIST -> stringResource(R.string.library_empty_trakt_title, selectedTypeLabel)
                        uiState.sourceMode == LibrarySourceMode.SIMKL -> stringResource(R.string.library_empty_simkl_title, selectedTypeLabel)
                        else -> stringResource(R.string.library_empty_local_title, selectedTypeLabel)
                    }
                    val subtitle = when {
                        uiState.sourceMode == LibrarySourceMode.TRAKT && !uiState.isTrackingAuthenticated -> stringResource(R.string.library_empty_trakt_not_auth_subtitle)
                        uiState.sourceMode == LibrarySourceMode.SIMKL && !uiState.isTrackingAuthenticated -> stringResource(R.string.library_empty_simkl_not_auth_subtitle)
                        uiState.sourceMode == LibrarySourceMode.TRAKT || uiState.sourceMode == LibrarySourceMode.MDBLIST -> stringResource(R.string.library_empty_trakt_subtitle)
                        uiState.sourceMode == LibrarySourceMode.SIMKL -> stringResource(R.string.library_empty_simkl_subtitle)
                        else -> stringResource(R.string.library_empty_local_subtitle)
                    }
                    EmptyScreenState(
                        title = title,
                        subtitle = subtitle,
                        icon = Icons.Default.BookmarkBorder
                    )
                }
            }

            items(uiState.visibleItems, key = { "${it.type}:${it.id}" }) { item ->
                val focusKey = "${item.type}:${item.id}"
                val isSeries = item.type.equals("series", ignoreCase = true) || item.type.equals("tv", ignoreCase = true)
                val previewForLongPress = remember(item) {
                    item.toMetaPreview().copy(posterShape = PosterShape.POSTER)
                }
                LibraryLauncherTile(
                    item = previewForLongPress,
                    landscape = globalLandscape,
                    scale = tileScale,
                    zoom = tileZoom,
                    isWatched = if (isSeries) item.id in watchedSeriesIds else item.id in watchedMovieIds,
                    focusRequester = posterFocusRequesters[focusKey],
                    onFocused = {
                        lastFocusedPosterKey = focusKey
                        viewModel.prefetchMetaOnFocus(item.id, item.type)
                    },
                    onClick = {
                        lastFocusedPosterKey = focusKey
                        val backdrop = viewModel.getCachedBackdrop(item.id, item.type)
                        HeroBackdropState.update(backdrop)
                        onNavigateToDetail(item.id, item.type, item.addonBaseUrl)
                    },
                    onLongPress = {
                        lastFocusedPosterKey = focusKey
                        viewModel.posterOptions.show(previewForLongPress, item.addonBaseUrl)
                    }
                )
            }
        } else if (viewMode == LibraryViewMode.Servers) {
            libraryServerContent(
                shelves = serverShelves,
                posterCardStyle = posterCardStyle,
                rowInset = LauncherStyle.Start,
                focusTarget = serverFocusTarget,
                focusedIndexes = serversViewModel.focusedIndexes,
                onItemFocused = { shelfKey, index ->
                    val target = serverFocusTarget
                    if (target == null || target == shelfKey to index) {
                        serverFocusTarget = null
                        serversViewModel.onItemFocused(shelfKey, index)
                    }
                },
                isWatched = { item ->
                    val isSeries = item.apiType.equals("series", ignoreCase = true) || item.apiType.equals("tv", ignoreCase = true)
                    if (isSeries) item.id in watchedSeriesIds else item.id in watchedMovieIds
                },
                onItemClick = { itemId, itemType, addonBaseUrl -> onNavigateToDetail(itemId, itemType, addonBaseUrl) },
                onSeeAll = { row -> onNavigateToCatalogSeeAll(row.catalogId, row.addonId, row.apiType) },
                onItemLongPress = { item, addonBaseUrl -> viewModel.posterOptions.show(item, addonBaseUrl) }
            )
        } else {
            item(span = { GridItemSpan(maxLineSpan) }) {
                CloudLibrarySearchRow(
                    query = uiState.cloudSearchQuery,
                    onQueryChange = viewModel::onCloudSearchQueryChange
                )
            }

            if (uiState.cloudLibrary.isRefreshing && uiState.visibleCloudItems.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    CloudLibraryLoadingState()
                }
            } else if (!uiState.cloudLibrary.isEnabled) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    EmptyScreenState(
                        title = stringResource(R.string.cloud_library_disabled_title),
                        subtitle = stringResource(R.string.cloud_library_disabled_message),
                        icon = Icons.Default.BookmarkBorder,
                        height = 260.dp
                    )
                }
            } else if (!uiState.cloudLibrary.hasConnectedProvider) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    EmptyScreenState(
                        title = stringResource(R.string.cloud_library_connect_title),
                        subtitle = stringResource(R.string.cloud_library_connect_message),
                        icon = Icons.Default.BookmarkBorder,
                        height = 260.dp
                    )
                }
            } else if (uiState.visibleCloudItems.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    EmptyScreenState(
                        title = stringResource(R.string.cloud_library_empty_title),
                        subtitle = stringResource(R.string.cloud_library_empty_message),
                        icon = Icons.Default.BookmarkBorder,
                        height = 260.dp
                    )
                }
            }

            items(
                items = uiState.visibleCloudItems,
                key = { it.stableKey },
                span = { GridItemSpan(maxLineSpan) }
            ) { item ->
                CloudLibraryCard(
                    item = item,
                    isResolving = uiState.resolvingCloudFileKey?.startsWith(item.stableKey) == true,
                    onClick = {
                        val playableFiles = item.playableFiles
                        when (playableFiles.size) {
                            0 -> viewModel.onCloudItemHasNoPlayableFiles()
                            1 -> viewModel.resolveCloudPlayback(item, playableFiles.first(), routeCloudPlayback)
                            else -> activeCloudItem = item
                        }
                    }
                )
            }
        }

        item(span = { GridItemSpan(maxLineSpan) }) { Spacer(modifier = Modifier.height(NuvioTheme.spacing.sm)) }
    }

    if (uiState.showManageDialog && uiState.listManagement != null) {
        ManageListsDialog(
            capabilities = requireNotNull(uiState.listManagement),
            tabs = uiState.listTabs,
            selectedKey = uiState.manageSelectedListKey,
            errorMessage = uiState.errorMessage,
            pending = uiState.pendingOperation,
            onSelect = viewModel::onSelectManageList,
            onCreate = viewModel::onStartCreateList,
            onEdit = viewModel::onStartEditList,
            onMoveUp = viewModel::onMoveSelectedListUp,
            onMoveDown = viewModel::onMoveSelectedListDown,
            onDelete = { showDeleteConfirm = true },
            onDismiss = viewModel::onCloseManageLists
        )
    }

    if (showDeleteConfirm && uiState.showManageDialog) {
        ConfirmDeleteDialog(
            pending = uiState.pendingOperation,
            onConfirm = {
                showDeleteConfirm = false
                viewModel.onDeleteSelectedList()
            },
            onCancel = { showDeleteConfirm = false }
        )
    }

    val listEditor = uiState.listEditorState
    if (listEditor != null && uiState.showManageDialog && uiState.listManagement != null) {
        ListEditorDialog(
            capabilities = requireNotNull(uiState.listManagement),
            state = listEditor,
            pending = uiState.pendingOperation,
            onNameChanged = viewModel::onUpdateEditorName,
            onDescriptionChanged = viewModel::onUpdateEditorDescription,
            onPrivacyChanged = viewModel::onUpdateEditorPrivacy,
            onSave = viewModel::onSubmitEditor,
            onCancel = viewModel::onCancelEditor
        )
    }

    activeCloudItem?.let { item ->
        CloudFilePickerDialog(
            item = item,
            resolvingFileKey = uiState.resolvingCloudFileKey,
            onPlay = { file ->
                viewModel.resolveCloudPlayback(item, file) { info ->
                    activeCloudItem = null
                    routeCloudPlayback(info)
                }
            },
            onDismiss = { activeCloudItem = null }
        )
    }

    if (showCloudPlayerChoice && pendingCloudPlayback != null) {
        PlayerChoiceDialog(
            onInternalSelected = {
                showCloudPlayerChoice = false
                pendingCloudPlayback?.let(onCloudPlaybackResolved)
                pendingCloudPlayback = null
            },
            onExternalSelected = {
                showCloudPlayerChoice = false
                val info = pendingCloudPlayback
                pendingCloudPlayback = null
                if (info != null) {
                    scope.launch { viewModel.launchCloudPlaybackExternally(info, activityContext) }
                }
            },
            onDismiss = {
                showCloudPlayerChoice = false
                pendingCloudPlayback = null
            }
        )
    }

    val transientMessage = uiState.transientMessage
    if (!transientMessage.isNullOrBlank()) {
        Box(
            modifier = Modifier
                .fillMaxSize(),
            contentAlignment = androidx.compose.ui.Alignment.TopCenter
        ) {
            Text(
                text = transientMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = NuvioTheme.colors.TextPrimary,
                modifier = Modifier
                    .padding(top = NuvioTheme.spacing.xl)
                    .background(NuvioTheme.colors.BackgroundElevated, RoundedCornerShape(10.dp))
                    .padding(horizontal = 18.dp, vertical = 10.dp)
            )
        }
    }

    val posterOptionsState by viewModel.posterOptions.state.collectAsState()
    com.nuvio.tv.ui.components.posteroptions.PosterOptionsHost(
        state = posterOptionsState,
        controller = viewModel.posterOptions,
        onNavigateToDetail = { id, type, addonBaseUrl ->
            onNavigateToDetail(id, type, addonBaseUrl.takeIf { it.isNotBlank() })
        }
    )
}

/** The source tabs (Saved / Cloud / Servers, only those that exist) as compact launcher pills. */
@Composable
private fun LibraryViewModePills(
    modes: List<LibraryViewMode>,
    selectedMode: LibraryViewMode,
    primaryFocusRequester: FocusRequester,
    onSelected: (LibraryViewMode) -> Unit
) {
    modes.forEach { mode ->
        val selected = mode == selectedMode
        LibraryPill(
            text = when (mode) {
                LibraryViewMode.Saved -> stringResource(R.string.library_source_saved)
                LibraryViewMode.Cloud -> stringResource(R.string.library_source_cloud)
                LibraryViewMode.Servers -> stringResource(R.string.library_source_servers)
            },
            selected = selected,
            onClick = { onSelected(mode) },
            modifier = Modifier
                .then(if (selected) Modifier.focusRequester(primaryFocusRequester) else Modifier)
                .semantics {
                    role = Role.Tab
                    this.selected = selected
                }
        )
    }
}

@Composable
private fun CloudLibraryFilterPills(
    providerOptions: List<FilterOption>,
    typeOptions: List<FilterOption>,
    selectedProviderId: String?,
    selectedType: CloudLibraryItemType?,
    expandedPicker: String?,
    onExpandedChange: (String, Boolean) -> Unit,
    onSelectProvider: (String?) -> Unit,
    onSelectType: (CloudLibraryItemType?) -> Unit
) {
    val allLabel = stringResource(R.string.cloud_library_provider_all)
    val typeAllLabel = stringResource(R.string.cloud_library_type_all)
    val selectedProviderLabel = providerOptions.firstOrNull { it.key == selectedProviderId }?.label ?: allLabel
    val selectedTypeLabel = selectedType?.localizedLabel() ?: typeAllLabel

    LibraryDropdownPicker(
        title = "Provider",
        value = selectedProviderLabel,
        selectedValue = selectedProviderId ?: "__all__",
        active = selectedProviderId != null,
        expanded = expandedPicker == "cloud_provider",
        options = listOf(LibraryOption(allLabel, "__all__")) + providerOptions.map {
            LibraryOption("${it.label} (${it.count})", it.key)
        },
        onExpandedChange = { onExpandedChange("cloud_provider", it) },
        onSelect = { option ->
            onSelectProvider(if (option.value == "__all__") null else option.value)
        }
    )

    LibraryDropdownPicker(
        title = stringResource(R.string.library_filter_type),
        value = selectedTypeLabel,
        selectedValue = selectedType?.name ?: "__all__",
        active = selectedType != null,
        expanded = expandedPicker == "cloud_type",
        options = listOf(LibraryOption(typeAllLabel, "__all__")) + typeOptions.map {
            LibraryOption("${it.label} (${it.count})", it.key)
        },
        onExpandedChange = { onExpandedChange("cloud_type", it) },
        onSelect = { option ->
            onSelectType(CloudLibraryItemType.entries.firstOrNull { it.name == option.value })
        }
    )
}

@Composable
private fun CloudLibraryItemType.localizedLabel(): String =
    when (this) {
        CloudLibraryItemType.Torrent -> stringResource(R.string.cloud_library_type_torrents)
        CloudLibraryItemType.Usenet -> stringResource(R.string.cloud_library_type_usenet)
        CloudLibraryItemType.WebDownload -> stringResource(R.string.cloud_library_type_web)
        CloudLibraryItemType.File -> stringResource(R.string.cloud_library_type_files)
    }

/**
 * Free-text filter over the already-loaded cloud library. Purely local: it never triggers a
 * provider request, it just narrows [LibraryUiState.visibleCloudItems]. Filtering is applied on
 * every keystroke, so results narrow live as you type.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun CloudLibrarySearchRow(
    query: String,
    onQueryChange: (String) -> Unit
) {
    var editing by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val fieldFocusRequester = remember { FocusRequester() }
    val editorFocusRequester = remember { FocusRequester() }

    LaunchedEffect(editing) {
        if (editing) {
            editorFocusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(NuvioTheme.spacing.md),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        Surface(
            onClick = { editing = true },
            modifier = Modifier
                .weight(1f)
                .focusRequester(fieldFocusRequester),
            colors = ClickableSurfaceDefaults.colors(
                containerColor = NuvioTheme.colors.BackgroundCard,
                focusedContainerColor = NuvioTheme.colors.BackgroundCard
            ),
            border = ClickableSurfaceDefaults.border(
                border = Border(
                    border = BorderStroke(NuvioTheme.spacing.hairline, NuvioTheme.colors.Border),
                    shape = RoundedCornerShape(NuvioTheme.radii.md)
                ),
                focusedBorder = Border(
                    border = NuvioTheme.focusRing.border(NuvioTheme.spacing.xxs),
                    shape = RoundedCornerShape(NuvioTheme.radii.md)
                )
            ),
            shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(NuvioTheme.radii.md)),
            scale = ClickableSurfaceDefaults.scale(focusedScale = 1f)
        ) {
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = NuvioTheme.spacing.lg, vertical = 14.dp)
                    .focusRequester(editorFocusRequester)
                    .focusProperties { canFocus = editing }
                    .onFocusChanged {
                        if (!it.isFocused && editing) {
                            editing = false
                            keyboardController?.hide()
                        }
                    }
                    .onPreviewKeyEvent { event ->
                        val native = event.nativeKeyEvent
                        if (native.action != AndroidKeyEvent.ACTION_DOWN) {
                            return@onPreviewKeyEvent false
                        }
                        val direction = when (native.keyCode) {
                            AndroidKeyEvent.KEYCODE_DPAD_UP -> FocusDirection.Up
                            AndroidKeyEvent.KEYCODE_DPAD_DOWN -> FocusDirection.Down
                            else -> return@onPreviewKeyEvent false
                        }
                        editing = false
                        keyboardController?.hide()
                        focusManager.moveFocus(direction)
                        true
                    },
                readOnly = !editing,
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        editing = false
                        keyboardController?.hide()
                        fieldFocusRequester.requestFocus()
                    }
                ),
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = NuvioTheme.colors.TextPrimary,
                                            textDirection = TextDirection.Content
                ),
                cursorBrush = SolidColor(
                    if (editing) NuvioTheme.colors.FocusRing else Color.Transparent
                ),
                decorationBox = { innerTextField ->
                    if (query.isEmpty()) {
                        Text(
                            text = stringResource(R.string.cloud_library_search_label),
                            style = MaterialTheme.typography.bodyMedium,
                            color = NuvioTheme.colors.TextTertiary
                        )
                    }
                    innerTextField()
                }
            )
        }

        if (query.isNotEmpty()) {
            Button(
                onClick = {
                    onQueryChange("")
                    editing = false
                },
                colors = ButtonDefaults.colors(
                    containerColor = NuvioTheme.colors.BackgroundCard,
                    contentColor = NuvioTheme.colors.TextPrimary
                )
            ) {
                Text(stringResource(R.string.cloud_library_search_clear))
            }
        }
    }
}

@Composable
private fun CloudLibraryLoadingState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp),
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        LoadingIndicator()
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = stringResource(R.string.library_syncing_library),
            style = MaterialTheme.typography.bodyMedium,
            color = NuvioTheme.colors.TextSecondary
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun CloudLibraryCard(
    item: CloudLibraryItem,
    isResolving: Boolean,
    onClick: () -> Unit
) {
    val fileLine = cloudLibraryFileLine(item)
    Card(
        onClick = { if (!isResolving) onClick() },
        modifier = Modifier.fillMaxWidth(),
        shape = CardDefaults.shape(RoundedCornerShape(10.dp)),
        colors = CardDefaults.colors(
            containerColor = NuvioTheme.colors.BackgroundCard,
            focusedContainerColor = NuvioTheme.colors.FocusBackground
        ),
        border = CardDefaults.border(
            border = Border(border = BorderStroke(NuvioTheme.spacing.hairline, NuvioTheme.colors.Border), shape = RoundedCornerShape(10.dp)),
            focusedBorder = Border(border = NuvioTheme.focusRing.border(NuvioTheme.spacing.xxs), shape = RoundedCornerShape(10.dp))
        ),
        scale = CardDefaults.scale(focusedScale = 1.02f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.titleSmall,
                color = NuvioTheme.colors.TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
            fileLine?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = NuvioTheme.colors.TextSecondary
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Text(
                    modifier = Modifier.weight(1f),
                    text = cloudLibraryMetadata(item),
                    style = MaterialTheme.typography.bodySmall,
                    color = NuvioTheme.colors.TextTertiary
                )
                Text(
                    text = if (isResolving) {
                        stringResource(R.string.cloud_library_opening)
                    } else {
                        cloudLibraryPlayableLabel(item)
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = if (item.playableFiles.isEmpty()) NuvioTheme.colors.TextTertiary else NuvioTheme.colors.Primary
                )
            }
        }
    }
}

@Composable
private fun cloudLibraryFileLine(item: CloudLibraryItem): String? =
    when (val count = item.playableFiles.size) {
        0 -> stringResource(R.string.cloud_library_no_playable_files)
        1 -> item.playableFiles.first().name.takeIf { it != item.name }
        else -> stringResource(R.string.cloud_library_playable_file_count, count)
    }

@Composable
private fun cloudLibraryPlayableLabel(item: CloudLibraryItem): String =
    when (val count = item.playableFiles.size) {
        0 -> stringResource(R.string.cloud_library_no_playable_files)
        1 -> stringResource(R.string.cloud_library_one_playable_file)
        else -> stringResource(R.string.cloud_library_playable_file_count, count)
    }

@Composable
private fun cloudLibraryMetadata(item: CloudLibraryItem): String {
    val parts = listOfNotNull(
        item.providerName.takeIf { it.isNotBlank() },
        item.type.localizedLabel(),
        item.status?.takeIf { it.isNotBlank() } ?: stringResource(R.string.cloud_library_status_ready),
        formatCloudSize(item.sizeBytes)
    )
    return parts.joinToString(" • ")
}

private fun formatCloudSize(sizeBytes: Long?): String? {
    val bytes = sizeBytes ?: return null
    if (bytes <= 0L) return null
    val gb = bytes / 1_000_000_000.0
    return if (gb >= 1.0) {
        String.format(java.util.Locale.US, "%.1f GB", gb)
    } else {
        val mb = bytes / 1_000_000.0
        String.format(java.util.Locale.US, "%.0f MB", mb)
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun CloudFilePickerDialog(
    item: CloudLibraryItem,
    resolvingFileKey: String?,
    onPlay: (CloudLibraryFile) -> Unit,
    onDismiss: () -> Unit
) {
    NuvioDialog(
        onDismiss = onDismiss,
        title = stringResource(R.string.cloud_library_file_picker_title),
        subtitle = item.name,
        width = 860.dp,
        suppressFirstKeyUp = false
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 420.dp),
            verticalArrangement = Arrangement.spacedBy(NuvioTheme.spacing.sm)
        ) {
            items(item.playableFiles, key = { it.stableKey }) { file ->
                val resolving = resolvingFileKey == "${item.stableKey}:${file.stableKey}"
                Card(
                    onClick = { if (!resolving) onPlay(file) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.colors(
                        containerColor = NuvioTheme.colors.BackgroundCard,
                        focusedContainerColor = NuvioTheme.colors.FocusBackground
                    ),
                    shape = CardDefaults.shape(RoundedCornerShape(10.dp)),
                    scale = CardDefaults.scale(focusedScale = 1f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(NuvioTheme.spacing.sm)
                    ) {
                        Text(
                            text = file.name,
                            style = MaterialTheme.typography.bodyLarge,
                            color = NuvioTheme.colors.TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        formatCloudSize(file.sizeBytes)?.let { size ->
                            Text(
                                text = size,
                                style = MaterialTheme.typography.bodySmall,
                                color = NuvioTheme.colors.TextSecondary
                            )
                        }
                        Text(
                            text = stringResource(R.string.cloud_library_play_file),
                            style = MaterialTheme.typography.labelMedium,
                            color = NuvioTheme.colors.Primary
                        )
                    }
                }
            }
        }
    }
}

/** The type pills plus the list, sort, genre, year and watched filters as compact dropdown pills. */
@Composable
private fun LibraryFilterPills(
    sourceMode: LibrarySourceMode,
    listTabs: List<LibraryListTab>,
    typeTabs: List<LibraryTypeTab>,
    sortOptions: List<LibrarySortOption>,
    genres: List<FilterOption>,
    years: List<FilterOption>,
    selectedListKey: String?,
    selectedTypeTab: LibraryTypeTab?,
    selectedSortOption: LibrarySortOption,
    selectedGenre: String?,
    selectedYear: String?,
    selectedWatchedFilter: LibraryWatchedFilter,
    primaryFocusRequester: FocusRequester,
    expandedPicker: String?,
    onExpandedChange: (String, Boolean) -> Unit,
    onSelectList: (String) -> Unit,
    onSelectType: (LibraryTypeTab) -> Unit,
    onSelectSort: (LibrarySortOption) -> Unit,
    onSelectGenre: (String?) -> Unit,
    onSelectYear: (String?) -> Unit,
    onSelectWatchedFilter: (LibraryWatchedFilter) -> Unit
) {
    val selectedListLabel = listTabs.firstOrNull { it.key == selectedListKey }?.localizedTitle()
        ?: stringResource(R.string.action_select)
    val selectedSortLabel = stringResource(selectedSortOption.labelResId)
    val allLabel = stringResource(R.string.library_type_all)
    val selectedGenreLabel = selectedGenre?.let { localizedGenreLabel(it) } ?: allLabel
    val selectedYearLabel = selectedYear ?: allLabel
    val selectedTypeKey = selectedTypeTab?.key ?: LibraryTypeTab.ALL_KEY

    if (sourceMode != LibrarySourceMode.LOCAL) {
        LibraryDropdownPicker(
            modifier = Modifier.focusRequester(primaryFocusRequester),
            title = stringResource(R.string.library_filter_list),
            value = selectedListLabel,
            selectedValue = selectedListKey,
            expanded = expandedPicker == "list",
            options = listTabs.map { LibraryOption(it.localizedTitle(), it.key) },
            onExpandedChange = { onExpandedChange("list", it) },
            onSelect = { onSelectList(it.value) }
        )
    }

    // Type: one pill per type (All / Movies / Shows / ...), the selected one highlighted.
    val types = typeTabs.ifEmpty { listOf(LibraryTypeTab.All) }
    types.forEachIndexed { index, tab ->
        val selected = tab.key == selectedTypeKey
        LibraryPill(
            text = libraryTypePillLabel(tab.key),
            selected = selected,
            onClick = { onSelectType(tab) },
            modifier = Modifier
                .then(
                    if (index == 0 && sourceMode == LibrarySourceMode.LOCAL) Modifier.focusRequester(primaryFocusRequester)
                    else Modifier
                )
                .semantics {
                    role = Role.Tab
                    this.selected = selected
                }
        )
    }

    if (sortOptions.isNotEmpty()) {
        LibraryDropdownPicker(
            title = stringResource(R.string.library_filter_sort),
            value = selectedSortLabel,
            selectedValue = selectedSortOption.key,
            expanded = expandedPicker == "sort",
            options = sortOptions.map { LibraryOption(stringResource(it.labelResId), it.key) },
            onExpandedChange = { onExpandedChange("sort", it) },
            onSelect = { option ->
                sortOptions.firstOrNull { it.key == option.value }?.let(onSelectSort)
            }
        )
    }

    if (genres.isNotEmpty()) {
        val genreAllOption = LibraryOption(allLabel, "__all__")
        LibraryDropdownPicker(
            title = stringResource(R.string.library_filter_genre),
            value = selectedGenreLabel,
            selectedValue = selectedGenre ?: "__all__",
            active = selectedGenre != null,
            expanded = expandedPicker == "genre",
            options = listOf(genreAllOption) + genres.map {
                LibraryOption("${localizedGenreLabel(it.label)} (${it.count})", it.key)
            },
            onExpandedChange = { onExpandedChange("genre", it) },
            onSelect = { option ->
                onSelectGenre(if (option.value == "__all__") null else option.value)
            }
        )
    }

    if (years.isNotEmpty()) {
        val yearAllOption = LibraryOption(allLabel, "__all__")
        LibraryDropdownPicker(
            title = stringResource(R.string.library_filter_year),
            value = selectedYearLabel,
            selectedValue = selectedYear ?: "__all__",
            active = selectedYear != null,
            expanded = expandedPicker == "year",
            options = listOf(yearAllOption) + years.map {
                LibraryOption("${it.label} (${it.count})", it.key)
            },
            onExpandedChange = { onExpandedChange("year", it) },
            onSelect = { option ->
                onSelectYear(if (option.value == "__all__") null else option.value)
            }
        )
    }

    val watchedFilterLabel = stringResource(selectedWatchedFilter.labelResId)
    LibraryDropdownPicker(
        title = stringResource(R.string.library_filter_watched),
        value = watchedFilterLabel,
        selectedValue = selectedWatchedFilter.key,
        active = selectedWatchedFilter != LibraryWatchedFilter.ALL,
        expanded = expandedPicker == "watched",
        options = LibraryWatchedFilter.entries.map {
            LibraryOption(stringResource(it.labelResId), it.key)
        },
        onExpandedChange = { onExpandedChange("watched", it) },
        onSelect = { option ->
            LibraryWatchedFilter.entries.firstOrNull { it.key == option.value }
                ?.let(onSelectWatchedFilter)
        }
    )
}

/** "All", "Movies", "Shows", or the type's own name for anything else (anime, channels...). */
@Composable
private fun libraryTypePillLabel(key: String): String = when (key.lowercase()) {
    LibraryTypeTab.ALL_KEY -> stringResource(R.string.library_type_all)
    "movie" -> stringResource(R.string.type_movies)
    "series", "tv", "show" -> stringResource(R.string.trakt_stat_shows)
    else -> localizedTypeLabel(key)
}

/**
 * A filter as one compact dropdown pill, "Sort: Added ↓ ⌄". The menu is the same as before; it opens under the
 * pill, at least 220 wide. [active] shows the pill as selected when the filter is narrowing the library.
 */
@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun LibraryDropdownPicker(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    selectedValue: String?,
    active: Boolean = false,
    expanded: Boolean,
    options: List<LibraryOption>,
    onExpandedChange: (Boolean) -> Unit,
    onSelect: (LibraryOption) -> Unit
) {
    var anchorSize by remember { mutableStateOf(IntSize.Zero) }
    // Seed focused option with the current selection so reopen highlights the right row
    // even before focus lands (fixes #2848 / incomplete #2507 race on TV).
    var focusedOptionValue by remember(expanded) {
        mutableStateOf(if (expanded) selectedValue else null)
    }
    val selectedItemFocusRequester = remember { FocusRequester() }
    val selectedBringIntoViewRequester = remember { BringIntoViewRequester() }

    // Popup content attaches focus targets a few frames after expand. A fixed 50ms
    // delay was flaky on TV and left focus on the first item for non-top selections.
    LaunchedEffect(expanded, selectedValue) {
        if (!expanded || selectedValue == null) return@LaunchedEffect
        var focused = selectedItemFocusRequester.requestFocusAfterFrames(frames = 3)
        var attempt = 0
        while (!focused && attempt < 6) {
            delay(32)
            focused = runCatching { selectedItemFocusRequester.requestFocus() }.getOrDefault(false)
            attempt++
        }
        if (!focused) return@LaunchedEffect
        runCatching { selectedBringIntoViewRequester.bringIntoView() }
        // Material DropdownMenu may still move initial focus to the first item after
        // the popup settles; re-assert once so long lists keep the real selection.
        delay(48)
        if (runCatching { selectedItemFocusRequester.requestFocus() }.getOrDefault(false)) {
            runCatching { selectedBringIntoViewRequester.bringIntoView() }
        }
    }

    val pillDescription = if (expanded) stringResource(R.string.cd_collapse, title) else stringResource(R.string.cd_expand, title)
    Box {
        LibraryPill(
            text = "$title: $value",
            selected = active,
            dropdown = true,
            expanded = expanded,
            onClick = { onExpandedChange(!expanded) },
            modifier = modifier
                .onSizeChanged { anchorSize = it }
                .semantics { contentDescription = pillDescription }
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                focusedOptionValue = null
                onExpandedChange(false)
            },
            modifier = Modifier
                .width(maxOf(with(LocalDensity.current) { anchorSize.width.toDp() }, 220.dp))
                .heightIn(max = 320.dp),
            shape = RoundedCornerShape(14.dp),
            containerColor = NuvioTheme.colors.BackgroundCard,
            tonalElevation = NuvioTheme.spacing.none,
            shadowElevation = NuvioTheme.spacing.sm,
            border = BorderStroke(NuvioTheme.spacing.hairline, NuvioTheme.colors.Border)
        ) {
            options.forEach { option ->
                val isSelected = option.value == selectedValue
                val isOptionFocused = option.value == focusedOptionValue
                val itemTextColor = when {
                    isOptionFocused -> NuvioTheme.colors.OnSecondary
                    isSelected -> NuvioTheme.colors.TextPrimary
                    else -> NuvioTheme.colors.TextPrimary
                }
                val itemBackgroundColor = when {
                    isOptionFocused -> NuvioTheme.colors.Secondary
                    isSelected -> NuvioTheme.colors.FocusBackground
                    else -> Color.Transparent
                }

                DropdownMenuItem(
                    modifier = Modifier
                        .then(
                            if (isSelected) {
                                Modifier
                                    .focusRequester(selectedItemFocusRequester)
                                    .bringIntoViewRequester(selectedBringIntoViewRequester)
                            } else {
                                Modifier
                            }
                        )
                        .padding(horizontal = 6.dp, vertical = NuvioTheme.spacing.xxs)
                        .background(
                            color = itemBackgroundColor,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .semantics {
                            // The current option is only distinguished by a background tint,
                            // so expose the selection state explicitly.
                            role = Role.RadioButton
                            this.selected = isSelected
                        }
                        .onFocusChanged { state ->
                            val hasFocus = state.isFocused || state.hasFocus
                            focusedOptionValue = when {
                                hasFocus -> option.value
                                focusedOptionValue == option.value -> null
                                else -> focusedOptionValue
                            }
                        },
                    text = {
                        Text(
                            text = option.label,
                            color = itemTextColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    onClick = { onSelect(option) },
                    colors = MenuDefaults.itemColors(
                        textColor = itemTextColor,
                        disabledTextColor = NuvioTheme.colors.TextDisabled
                    )
                )
            }
        }
    }
}

private data class LibraryOption(
    val label: String,
    val value: String
)

/** Manage lists and Sync (Trakt / Simkl), as pills at the end of the filter row. */
@Composable
private fun LibraryActionPills(
    pending: Boolean,
    isSyncing: Boolean,
    showManageLists: Boolean,
    onManageLists: () -> Unit,
    onRefresh: () -> Unit
) {
    if (showManageLists) {
        LibraryPill(
            text = stringResource(R.string.library_manage_lists),
            selected = false,
            enabled = !pending && !isSyncing,
            onClick = onManageLists
        )
    }
    LibraryPill(
        text = if (isSyncing) stringResource(R.string.library_syncing_btn) else stringResource(R.string.library_sync_btn),
        selected = false,
        enabled = !pending && !isSyncing,
        onClick = onRefresh
    )
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ConfirmDeleteDialog(
    pending: Boolean,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    NuvioDialog(
        onDismiss = onCancel,
        title = stringResource(R.string.library_delete_title),
        subtitle = stringResource(R.string.library_delete_subtitle),
        width = 420.dp
    ) {
        Button(
            onClick = onConfirm,
            enabled = !pending,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.colors(
                containerColor = Color(0xFF4A2323),
                contentColor = NuvioTheme.colors.TextPrimary
            )
        ) {
            Text(stringResource(R.string.library_list_delete))
        }
    }
}

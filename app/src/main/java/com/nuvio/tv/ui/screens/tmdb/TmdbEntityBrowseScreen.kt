package com.nuvio.tv.ui.screens.tmdb

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.request.crossfade
import com.nuvio.tv.LauncherChrome
import com.nuvio.tv.R
import com.nuvio.tv.core.tmdb.TmdbEntityBrowseData
import com.nuvio.tv.core.tmdb.TmdbEntityKind
import com.nuvio.tv.core.tmdb.TmdbEntityMediaType
import com.nuvio.tv.core.tmdb.TmdbEntityRail
import com.nuvio.tv.core.tmdb.TmdbEntityRailType
import com.nuvio.tv.data.local.LauncherSettingsHolder
import com.nuvio.tv.domain.model.MetaPreview
import com.nuvio.tv.domain.model.PosterShape
import com.nuvio.tv.ui.components.EmptyScreenState
import com.nuvio.tv.ui.components.ErrorState
import com.nuvio.tv.ui.components.LoadingIndicator
import com.nuvio.tv.ui.launcher.LauncherBlurredBackdrop
import com.nuvio.tv.ui.launcher.LauncherStyle
import kotlinx.coroutines.flow.distinctUntilChanged

/*
 * Nuvio Launcher: a TMDB production company / network page in the launcher's style, laid out like a
 * collection folder (LauncherFolderContent).
 *
 *  - Backdrop: the focused tile's picture, blurred, following focus (the entity's first title until then).
 *  - Top: "Production Company" / "Network", the entity's name, its country / place, a 2-line description,
 *    and its logo on the right.
 *  - Rows: one per rail, titled at 16 (white on the focused row), launcher tiles (120x180 / 224x126, scaled,
 *    zoomed and spaced per Settings → Launcher), every tile titled, the focused row kept where the first row
 *    sits, straight Up/Down to the tile last focused in that row, Right stops at the end.
 */

private val RowsTop = 176.dp
private val TileTopAboveRowPad = 41.dp  // row title (12 + 21) + spacer 8; the tiles' top is this plus the row's top padding
private val HeaderTop = 34.dp
private val NameTop = 50.dp             // label (34 + 14) + 2
private val LogoMaxWidth = 160.dp
private val LogoMaxHeight = 60.dp

/** Tile size (1 = 120x180 / 224x126 / 126x126), spacing and focus zoom, from Settings → Launcher. */
private data class EntityTileLook(val scale: Float, val gap: Dp, val zoom: Float) {
    /** Room above the tiles: 12, plus however much more a zoomed poster grows than at the default size and zoom. */
    val rowTopPad: Dp
        get() = 12.dp + maxOf(0.dp, (180.dp * scale * (zoom - 1f) - 180.dp * 0.11f) / 2f)
}

private class EntityRow(val key: String, val rail: TmdbEntityRail)

private fun railKeyOf(rail: TmdbEntityRail): String = "${rail.mediaType.value}_${rail.railType.value}"

private fun entityTileImage(m: MetaPreview): String? = when (m.posterShape) {
    PosterShape.LANDSCAPE -> m.landscapePoster ?: m.background ?: m.poster
    else -> m.poster ?: m.background
}

/** The page's picture before anything is focused: the first title of the rail matching where we came from. */
private fun fallbackBackdrop(data: TmdbEntityBrowseData, sourceType: String): String? {
    val preferred = if (sourceType.trim().equals("movie", ignoreCase = true)) TmdbEntityMediaType.MOVIE else TmdbEntityMediaType.TV
    val first = data.rails.firstOrNull { it.mediaType == preferred }?.items?.firstOrNull()
        ?: data.rails.firstOrNull()?.items?.firstOrNull()
    return first?.let { it.background ?: entityTileImage(it) }
}

/** Nuvio's watched tick on tiles (kept as in Nuvio): which titles are watched, provided by the screen. */
private val LocalEntityWatched = androidx.compose.runtime.staticCompositionLocalOf<(MetaPreview) -> Boolean> { { false } }

@Composable
fun TmdbEntityBrowseScreen(
    viewModel: TmdbEntityBrowseViewModel = hiltViewModel(),
    onBackPress: () -> Unit,
    onNavigateToDetail: (itemId: String, itemType: String, addonBaseUrl: String?) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val screenMode = when (uiState) {
        TmdbEntityBrowseUiState.Loading -> 0
        is TmdbEntityBrowseUiState.Error -> 1
        is TmdbEntityBrowseUiState.Success -> 2
    }

    BackHandler { onBackPress() }

    // The time pill would sit on the entity's logo.
    LaunchedEffect(Unit) { LauncherChrome.pillVisible.value = false }
    DisposableEffect(Unit) { onDispose { LauncherChrome.pillVisible.value = true } }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(LauncherStyle.GreyTop, LauncherStyle.GreyBottom)))
    ) {
        Crossfade(
            targetState = screenMode,
            label = "TmdbEntityBrowseState"
        ) { mode ->
            when (mode) {
                0 -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        LoadingIndicator()
                    }
                }

                1 -> {
                    val errorState = uiState as? TmdbEntityBrowseUiState.Error
                    ErrorState(
                        message = errorState?.message ?: stringResource(R.string.tmdb_entity_error_load),
                        onRetry = { viewModel.retry() }
                    )
                }

                else -> {
                    val successState = uiState as? TmdbEntityBrowseUiState.Success ?: return@Crossfade
                    val watchedMovies by viewModel.watchedMovieIds.collectAsStateWithLifecycle()
                    val watchedSeries by viewModel.watchedSeriesIds.collectAsStateWithLifecycle()
                    androidx.compose.runtime.CompositionLocalProvider(LocalEntityWatched provides { m: MetaPreview ->
                        val series = m.apiType.equals("series", true) || m.apiType.equals("tv", true)
                        if (series) m.id in watchedSeries else m.id in watchedMovies
                    }) {
                    TmdbEntityBrowseContent(
                        data = successState.data,
                        sourceType = viewModel.sourceType,
                        onNavigateToDetail = onNavigateToDetail,
                        onItemLongPress = { item ->
                            viewModel.posterOptions.show(item, null)
                        },
                        onLoadMoreRail = { mediaType, railType ->
                            viewModel.loadMoreRail(mediaType = mediaType, railType = railType)
                        }
                    )
                    }
                }
            }
        }

        val posterOptionsState by viewModel.posterOptions.state.collectAsStateWithLifecycle()
        com.nuvio.tv.ui.components.posteroptions.PosterOptionsHost(
            state = posterOptionsState,
            controller = viewModel.posterOptions,
            onNavigateToDetail = { id, type, addonBaseUrl ->
                onNavigateToDetail(id, type, addonBaseUrl.takeIf { it.isNotBlank() })
            }
        )
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun TmdbEntityBrowseContent(
    data: TmdbEntityBrowseData,
    sourceType: String,
    onNavigateToDetail: (itemId: String, itemType: String, addonBaseUrl: String?) -> Unit,
    onItemLongPress: (MetaPreview) -> Unit,
    onLoadMoreRail: (TmdbEntityMediaType, TmdbEntityRailType) -> Unit
) {
    val launcherSettings by LauncherSettingsHolder.settings.collectAsState()
    val look = EntityTileLook(
        launcherSettings.catalogScale,
        com.nuvio.tv.ui.launcher.zoomSafeGap(launcherSettings.catalogTileSpacingDp.dp, 224.dp * launcherSettings.catalogScale, launcherSettings.catalogZoom),
        launcherSettings.catalogZoom
    )
    // Rails that came back empty are left out.
    val rows = remember(data.rails) {
        data.rails.filter { it.items.isNotEmpty() }.map { EntityRow(railKeyOf(it), it) }
    }
    val fallback = remember(data, sourceType) { fallbackBackdrop(data, sourceType) }

    // Survive a trip to a title's page and back.
    var focusedRowKey by rememberSaveable(data.header.id) { mutableStateOf<String?>(null) }
    var focusedItemIndex by rememberSaveable(data.header.id) { mutableIntStateOf(0) }
    var focusPlaced by remember { mutableStateOf(false) }
    // Read only by the backdrop, so a Left/Right press redraws that alone, not every row.
    val backdrop = remember { mutableStateOf<String?>(null) }

    // Each row's composed tiles, so Up/Down can move focus to a row's remembered tile.
    val tiles = remember { mutableMapOf<Int, MutableMap<Int, FocusRequester>>() }
    // Each row remembers the tile that last had focus; Up/Down returns to it.
    val lastIndex = remember { mutableMapOf<Int, Int>() }

    // Which tile takes focus when the page opens: the one focused before, else the first tile of the first row.
    val savedRow = focusedRowKey?.let { k -> rows.indexOfFirst { it.key == k } }?.takeIf { it >= 0 }
    val initialRow = if (focusPlaced) -1 else (savedRow ?: if (rows.isNotEmpty()) 0 else -1)

    val defaultSpec = LocalBringIntoViewSpec.current
    // Every focused row sits exactly where the first row sits: its tiles' top at tileTopInRow from the top of the rows area.
    val tileTopInRow = TileTopAboveRowPad + look.rowTopPad
    val tileTopPx = with(LocalDensity.current) { tileTopInRow.toPx() }
    val anchoringSpec = remember(defaultSpec, tileTopPx) {
        @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
        object : BringIntoViewSpec {
            override val scrollAnimationSpec = defaultSpec.scrollAnimationSpec
            override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float =
                (offset - tileTopPx).let { d -> if (kotlin.math.abs(d) < tileTopPx) 0f else d }   // no bounce on Left/Right
        }
    }

    Box(Modifier.fillMaxSize()) {
        EntityBackdrop { backdrop.value ?: fallback }

        EntityHeader(data)

        if (rows.isEmpty()) {
            EmptyScreenState(
                title = stringResource(R.string.tmdb_entity_empty_title),
                subtitle = stringResource(R.string.tmdb_entity_empty_subtitle),
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
            CompositionLocalProvider(LocalBringIntoViewSpec provides anchoringSpec) {
                LazyColumn(
                    state = rememberLazyListState(),
                    modifier = Modifier.fillMaxSize().padding(top = RowsTop),
                    contentPadding = PaddingValues(bottom = 400.dp)   // room for the last row to scroll up to the first row's place
                ) {
                    itemsIndexed(rows, key = { _, r -> r.key }) { ri, row ->
                        CompositionLocalProvider(LocalBringIntoViewSpec provides defaultSpec) {
                            EntityRowBlock(
                                rowIndex = ri,
                                rail = row.rail,
                                title = railTitle(row.rail),
                                focused = focusedRowKey == row.key,
                                look = look,
                                tiles = tiles,
                                initialFocusIndex = if (ri == initialRow) (if (ri == savedRow) focusedItemIndex else 0) else null,
                                onInitialFocusPlaced = { focusPlaced = true },
                                onTileFocused = { i, item ->
                                    lastIndex[ri] = i
                                    focusPlaced = true
                                    focusedRowKey = row.key
                                    focusedItemIndex = i
                                    backdrop.value = item.background ?: entityTileImage(item)
                                    LauncherChrome.lastBackdrop.value = backdrop.value
                                },
                                onVertical = { fromRow, down ->
                                    val target = if (down) fromRow + 1 else fromRow - 1
                                    val cand = tiles[target].orEmpty()
                                    if (cand.isEmpty()) {
                                        false
                                    } else {
                                        val want = lastIndex[target] ?: 0
                                        val req = cand[want] ?: cand.entries.minByOrNull { it.key }?.value
                                        req?.let { runCatching { it.requestFocus() }.isSuccess } ?: false
                                    }
                                },
                                onClick = { item -> onNavigateToDetail(item.id, item.apiType, null) },
                                onLongClick = onItemLongPress,
                                onLoadMore = onLoadMoreRail
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EntityBackdrop(url: () -> String?) {
    LauncherBlurredBackdrop(url = url())
}

@Composable
private fun EntityHeader(data: TmdbEntityBrowseData) {
    val hasLogo = !data.header.logo.isNullOrBlank()
    val textEnd = if (hasLogo) LauncherStyle.Start + LogoMaxWidth + 24.dp else LauncherStyle.Start
    Box(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(start = LauncherStyle.Start, top = HeaderTop, end = textEnd)) {
            Text(
                entityKindLabel(data.header.kind), color = LauncherStyle.Soft, fontSize = 11.sp, lineHeight = 14.sp,
                fontWeight = FontWeight.SemiBold, letterSpacing = 0.4.sp, maxLines = 1
            )
            Text(
                data.header.name, color = Color.White, fontSize = 26.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp)
            )
            val metaLine = listOfNotNull(
                data.header.originCountry?.takeIf { it.isNotBlank() },
                data.header.secondaryLabel?.takeIf { it.isNotBlank() }
            ).joinToString(" · ")
            if (metaLine.isNotBlank()) {
                Text(
                    metaLine, color = LauncherStyle.Soft, fontSize = 11.5.sp, lineHeight = 15.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            data.header.description?.trim()?.takeIf { it.isNotEmpty() }?.let { description ->
                Text(
                    description, color = LauncherStyle.Soft, fontSize = 10.5.sp, lineHeight = 14.5.sp,
                    maxLines = 2, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 8.dp).widthIn(max = 480.dp)
                )
            }
        }
        if (hasLogo) {
            EntityLogo(
                url = data.header.logo.orEmpty(),
                name = data.header.name,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = NameTop, end = LauncherStyle.Start)
                    .widthIn(max = LogoMaxWidth)
                    .heightIn(max = LogoMaxHeight)
            )
        }
    }
}

@Composable
private fun EntityLogo(url: String, name: String, modifier: Modifier) {
    val context = LocalContext.current
    val logoPainter = rememberAsyncImagePainter(
        model = remember(url) {
            ImageRequest.Builder(context)
                .data(url)
                .crossfade(true)
                .allowHardware(false) // needed to read pixels
                .build()
        }
    )
    // Dark monochrome logos are tinted white so they show on the dark backdrop.
    var logoColorFilter by remember { mutableStateOf<ColorFilter?>(null) }
    val painterState by logoPainter.state.collectAsState()
    LaunchedEffect(painterState) {
        val success = painterState as? AsyncImagePainter.State.Success ?: return@LaunchedEffect
        val bitmap = (success.result.image as? coil3.BitmapImage)?.bitmap ?: return@LaunchedEffect
        logoColorFilter = if (isLogoDarkAndMonochrome(bitmap)) ColorFilter.tint(Color.White, BlendMode.SrcIn) else null
    }
    Image(
        painter = logoPainter,
        contentDescription = name,
        modifier = modifier,
        contentScale = ContentScale.Fit,
        alignment = Alignment.TopEnd,
        colorFilter = logoColorFilter
    )
}

@Composable
private fun EntityRowBlock(
    rowIndex: Int,
    rail: TmdbEntityRail,
    title: String,
    focused: Boolean,
    look: EntityTileLook,
    tiles: MutableMap<Int, MutableMap<Int, FocusRequester>>,
    initialFocusIndex: Int?,
    onInitialFocusPlaced: () -> Unit,
    onTileFocused: (Int, MetaPreview) -> Unit,
    onVertical: (Int, Boolean) -> Boolean,
    onClick: (MetaPreview) -> Unit,
    onLongClick: (MetaPreview) -> Unit,
    onLoadMore: (TmdbEntityMediaType, TmdbEntityRailType) -> Unit
) {
    val items = rail.items
    val rowState = rememberLazyListState(initialFirstVisibleItemIndex = (initialFocusIndex ?: 0).coerceIn(0, maxOf(0, items.lastIndex)))
    val rowTiles = remember(rowIndex) { mutableMapOf<Int, FocusRequester>() }
    DisposableEffect(rowIndex) { tiles[rowIndex] = rowTiles; onDispose { tiles.remove(rowIndex) } }

    // Next page when the end of the row comes into view (as before).
    var lastLoadMoreRequestTotal by remember(rail.mediaType, rail.railType) { mutableIntStateOf(-1) }
    LaunchedEffect(rail.mediaType, rail.railType, rowState, rail.hasMore, rail.isLoading) {
        if (!rail.hasMore) return@LaunchedEffect
        snapshotFlow {
            val layoutInfo = rowState.layoutInfo
            val total = layoutInfo.totalItemsCount
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisible to total
        }
            .distinctUntilChanged()
            .collect { (lastVisible, total) ->
                if (total <= 0) return@collect
                val isNearEnd = lastVisible >= total - 4
                if (!isNearEnd) {
                    lastLoadMoreRequestTotal = -1
                    return@collect
                }
                if (rail.hasMore && !rail.isLoading && lastLoadMoreRequestTotal != total) {
                    lastLoadMoreRequestTotal = total
                    onLoadMore(rail.mediaType, rail.railType)
                }
            }
    }

    Column(Modifier.fillMaxWidth()) {
        Text(
            title, color = LauncherStyle.TileTitleIdle, fontSize = 16.sp, lineHeight = 21.sp, fontWeight = FontWeight.Bold,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 60.dp, end = LauncherStyle.Start, top = 12.dp)
        )
        Spacer(Modifier.height(8.dp))
        LazyRow(
            state = rowState,
            contentPadding = PaddingValues(start = LauncherStyle.Start, end = LauncherStyle.Start, top = look.rowTopPad, bottom = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(look.gap)
        ) {
            itemsIndexed(items, key = { i, m -> "${m.id}_$i" }) { i, m ->
                val req = remember { FocusRequester() }
                DisposableEffect(i) { rowTiles[i] = req; onDispose { rowTiles.remove(i) } }
                if (initialFocusIndex != null && i == initialFocusIndex.coerceIn(0, items.lastIndex)) {
                    LaunchedEffect(Unit) {
                        repeat(6) {
                            withFrameNanos { }
                            if (runCatching { req.requestFocus() }.isSuccess) { onInitialFocusPlaced(); return@LaunchedEffect }
                        }
                    }
                }
                EntityTile(
                    item = m,
                    look = look,
                    modifier = Modifier
                        .focusRequester(req)
                        .onPreviewKeyEvent { e ->
                            if (e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                            when (e.key) {
                                Key.DirectionRight -> i == items.lastIndex   // the end of a row stops focus
                                Key.DirectionDown -> onVertical(rowIndex, true)
                                Key.DirectionUp -> onVertical(rowIndex, false)
                                else -> false
                            }
                        },
                    onFocused = { onTileFocused(i, m) },
                    onClick = { onClick(m) },
                    onLongClick = { onLongClick(m) }
                )
            }
        }
    }
}

@Composable
private fun EntityTile(
    item: MetaPreview,
    look: EntityTileLook,
    modifier: Modifier,
    onFocused: () -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val landscape = item.posterShape == PosterShape.LANDSCAPE
    val square = item.posterShape == PosterShape.SQUARE
    val w = (if (landscape) 224.dp else if (square) 126.dp else 120.dp) * look.scale
    val baseH = if (landscape || square) 126.dp else 180.dp
    val h = baseH * look.scale
    // How far the focused title moves down: 10 at the default size and zoom, plus whatever more the zoomed tile grows.
    val titleLift = maxOf(4.dp, 10.dp + (h * (look.zoom - 1f) - baseH * 0.11f) / 2f)
    val image = entityTileImage(item)
    val scale by animateFloatAsState(if (focused) look.zoom else 1f, tween(160), label = "zoom")
    Column(Modifier.width(w)) {
        Box(
            Modifier
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .shadow(elevation = if (focused) 17.dp else 7.dp, shape = LauncherStyle.Corner, ambientColor = Color.Black, spotColor = Color.Black)
        ) {
            Card(
                onClick = onClick,
                onLongClick = onLongClick,
                modifier = modifier.size(w, h).onFocusChanged { focused = it.hasFocus; if (it.hasFocus) onFocused() },
                shape = CardDefaults.shape(LauncherStyle.Corner),
                scale = CardDefaults.scale(focusedScale = 1f),
                border = CardDefaults.border(border = Border.None, focusedBorder = Border.None),
                colors = CardDefaults.colors(containerColor = Color(0xFF2A2C33), focusedContainerColor = Color(0xFF2A2C33))
            ) {
                Box(Modifier.fillMaxSize()) {
                    val watched = LocalEntityWatched.current(item)
                    if (image != null) {
                        AsyncImage(model = image, contentDescription = item.name, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    } else {
                        Text(
                            item.name, color = LauncherStyle.Soft, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 3,
                            modifier = Modifier.align(Alignment.Center).padding(8.dp)
                        )
                    }
                    if (watched) {
                        // Nuvio's own watched tick, kept as in Nuvio
                        com.nuvio.tv.ui.components.WatchedMarker(modifier = Modifier.align(Alignment.TopEnd).padding(top = 8.dp, end = 8.dp))
                    }
                }
            }
        }
        Text(
            item.name, color = if (focused) Color.White else LauncherStyle.TileTitleIdle, fontSize = 12.sp, lineHeight = 16.sp,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
            // Same layout height whether focused or not; the focused title only moves down visually, clear of the zoom.
            modifier = Modifier.padding(top = 8.dp).graphicsLayer { translationY = if (focused) titleLift.toPx() else 0f }
        )
    }
}

@Composable
private fun entityKindLabel(kind: TmdbEntityKind): String = when (kind) {
    TmdbEntityKind.COMPANY -> stringResource(R.string.tmdb_entity_kind_company)
    TmdbEntityKind.NETWORK -> stringResource(R.string.tmdb_entity_kind_network)
}

@Composable
private fun railTitle(rail: TmdbEntityRail): String {
    val mediaLabel = when (rail.mediaType) {
        TmdbEntityMediaType.MOVIE -> stringResource(R.string.type_movie)
        TmdbEntityMediaType.TV -> stringResource(R.string.type_series)
    }
    val railLabel = when (rail.railType) {
        TmdbEntityRailType.POPULAR -> stringResource(R.string.tmdb_entity_rail_popular)
        TmdbEntityRailType.TOP_RATED -> stringResource(R.string.tmdb_entity_rail_top_rated)
        TmdbEntityRailType.RECENT -> stringResource(R.string.tmdb_entity_rail_recent)
    }
    return "$mediaLabel · $railLabel"
}

/**
 * Samples pixels from a [Bitmap] and returns true only if the non-transparent
 * pixels are both predominantly dark AND low-saturation (grayscale / monochrome).
 * This avoids treating colorful logos (red, blue, etc.) as "black".
 */
private fun isLogoDarkAndMonochrome(bitmap: Bitmap): Boolean {
    val width = bitmap.width
    val height = bitmap.height
    val step = maxOf(1, minOf(width, height) / 20) // sample ~20×20 grid
    var totalLuminance = 0.0
    var totalSaturation = 0.0
    var count = 0
    val hsv = FloatArray(3)
    var x = 0
    while (x < width) {
        var y = 0
        while (y < height) {
            val pixel = bitmap.getPixel(x, y)
            val alpha = (pixel ushr 24) and 0xFF
            if (alpha > 50) { // skip transparent / nearly-transparent pixels
                val r = ((pixel ushr 16) and 0xFF) / 255.0
                val g = ((pixel ushr 8) and 0xFF) / 255.0
                val b = (pixel and 0xFF) / 255.0
                totalLuminance += 0.2126 * r + 0.7152 * g + 0.0722 * b
                android.graphics.Color.colorToHSV(pixel, hsv)
                totalSaturation += hsv[1]
                count++
            }
            y += step
        }
        x += step
    }
    if (count == 0) return false
    val avgLuminance = totalLuminance / count
    val avgSaturation = totalSaturation / count
    // Dark (luminance < 0.3) AND grayscale (saturation < 0.2)
    return avgLuminance < 0.3 && avgSaturation < 0.2
}

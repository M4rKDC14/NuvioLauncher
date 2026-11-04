package com.nuvio.tv.ui.screens.collection

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.nuvio.tv.LauncherChrome
import com.nuvio.tv.domain.model.CollectionFolder
import com.nuvio.tv.domain.model.MetaPreview
import com.nuvio.tv.domain.model.PosterShape
import com.nuvio.tv.ui.launcher.LauncherBlurredBackdrop
import com.nuvio.tv.ui.launcher.LauncherStyle
import com.nuvio.tv.ui.screens.home.HeroBackdropState
import com.nuvio.tv.ui.util.localizedContentType

/*
 * Nuvio Launcher: a collection folder in the launcher's style.
 *
 *  - Backdrop: the focused tile's picture, blurred, following focus.
 *  - Top: the folder's name, then the focused title's name, genre line and a 2-line description.
 *  - Rows: one per catalog in the folder (whatever the folder's view mode), titled at 16 (white on the
 *    focused row), tiles 120x180 / 224x126 / 126x126, 29 apart, every tile titled, 111% zoom with a soft
 *    shadow, the focused row kept in the middle of the rows area, straight Up/Down, Right stops at the end.
 */

private val RowsTop = 176.dp            // where the rows area starts, under the details block
private const val LoadMoreAhead = 6     // ask for the next page this many tiles before the end

private class FolderRow(val key: String, val title: String, val tab: FolderTab)

private fun folderYear(m: MetaPreview): String? =
    (m.releaseInfo ?: m.released)?.trim()?.take(4)?.takeIf { it.length == 4 && it.all(Char::isDigit) }

private fun folderMetaLine(m: MetaPreview, typeLabel: String): String = buildList {
    if (typeLabel.isNotBlank()) add(typeLabel)
    m.genres.firstOrNull()?.takeIf { it.isNotBlank() }?.let { add(it) }
    folderYear(m)?.let { add(it) }
    m.imdbRating?.takeIf { it > 0f }?.let { add("★ " + String.format(java.util.Locale.US, "%.1f", it)) }
    m.runtime?.trim()?.takeIf { it.isNotEmpty() }?.let { add(it) }
}.joinToString(" · ")

private fun tileImage(m: MetaPreview): String? = when (m.posterShape) {
    PosterShape.LANDSCAPE -> m.landscapePoster ?: m.background ?: m.poster
    else -> m.poster ?: m.background
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
internal fun LauncherFolderContent(
    uiState: FolderDetailUiState,
    folder: CollectionFolder,
    enrichedPreviews: State<Map<String, MetaPreview>>,
    onNavigateToDetail: (String, String, String) -> Unit,
    onLoadMoreCatalog: (String, String, String) -> Unit,
    onItemFocus: (MetaPreview) -> Unit,
    onItemLongPress: (MetaPreview, String) -> Unit
) {
    val context = LocalContext.current
    val rows = remember(uiState.tabs, uiState.catalogTypeSuffixEnabled) {
        uiState.tabs.mapIndexedNotNull { i, tab ->
            if (tab.isAllTab) return@mapIndexedNotNull null
            // Sources that came back empty are hidden, as before.
            if (!tab.isLoading && tab.error == null && tab.catalogRow != null && tab.catalogRow.items.isEmpty()) return@mapIndexedNotNull null
            val typeLabel = localizedContentType(context, tab.rawType)
            val title = if (uiState.catalogTypeSuffixEnabled && tab.label != tab.typeLabel && typeLabel.isNotEmpty()) "${tab.label} · $typeLabel" else tab.label
            FolderRow("tab_$i", title, tab)
        }
    }

    // Survive a trip to a title's page and back.
    var focusedRowKey by rememberSaveable { mutableStateOf<String?>(null) }
    var focusedItemIndex by rememberSaveable { mutableIntStateOf(0) }
    var focusPlaced by remember { mutableStateOf(false) }
    var focusedItem by remember { mutableStateOf<MetaPreview?>(null) }
    var backdrop by remember { mutableStateOf<String?>(null) }

    // Tiles report where they are, so Up/Down can pick the tile directly above or below.
    val tiles = remember { mutableStateMapOf<Int, MutableMap<Int, Pair<FocusRequester, Float>>>() }

    // The time pill would sit on the folder's name.
    LaunchedEffect(Unit) { LauncherChrome.pillVisible.value = false }
    DisposableEffect(Unit) { onDispose { LauncherChrome.pillVisible.value = true } }

    // Which tile takes focus when the page opens: the one focused before, else the first loaded tile.
    val firstLoadedRow = rows.indexOfFirst { r -> !r.tab.isLoading && r.tab.catalogRow?.items?.isNotEmpty() == true }
    val savedRow = focusedRowKey?.let { k -> rows.indexOfFirst { it.key == k } }?.takeIf { it >= 0 }
    val initialRow = if (focusPlaced) -1 else (savedRow ?: firstLoadedRow)

    val defaultSpec = LocalBringIntoViewSpec.current
    val centeringSpec = remember(defaultSpec) {
        @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
        object : BringIntoViewSpec {
            override val scrollAnimationSpec = defaultSpec.scrollAnimationSpec
            override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float =
                (offset + size / 2f) - containerSize / 2f
        }
    }

    Box(Modifier.fillMaxSize()) {
        LauncherBlurredBackdrop(url = backdrop ?: folder.heroBackdropUrl ?: folder.coverImageUrl)

        Text(
            folder.title, color = Color.White, fontSize = 26.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = LauncherStyle.Start, top = 34.dp, end = LauncherStyle.Start)
        )

        val shown = focusedItem?.let { enrichedPreviews.value[it.id] ?: it }
        if (shown != null) {
            val typeLabel = remember(shown.apiType) { localizedContentType(context, shown.apiType) }
            Column(Modifier.padding(start = LauncherStyle.Start, top = 86.dp, end = LauncherStyle.Start)) {
                Text(shown.name, color = Color.White, fontSize = 18.sp, lineHeight = 23.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val meta = folderMetaLine(shown, typeLabel)
                if (meta.isNotEmpty()) {
                    Text(meta, color = Color.White, fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, modifier = Modifier.padding(top = 6.dp))
                }
                val desc = shown.description?.trim().orEmpty()
                if (desc.isNotEmpty()) {
                    Text(
                        desc, color = LauncherStyle.Soft, fontSize = 10.5.sp, lineHeight = 14.5.sp, maxLines = 2, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 6.dp).widthIn(max = 480.dp)
                    )
                }
            }
        }

        // Focus in the rows keeps the focused row in the middle of the rows area.
        CompositionLocalProvider(LocalBringIntoViewSpec provides centeringSpec) {
            LazyColumn(
                state = rememberLazyListState(),
                modifier = Modifier.fillMaxSize().padding(top = RowsTop),
                contentPadding = PaddingValues(bottom = 180.dp)
            ) {
                itemsIndexed(rows, key = { _, r -> r.key }) { ri, row ->
                    CompositionLocalProvider(LocalBringIntoViewSpec provides defaultSpec) {
                        FolderRowBlock(
                            rowIndex = ri,
                            row = row,
                            focused = focusedRowKey == row.key,
                            tiles = tiles,
                            initialFocusIndex = if (ri == initialRow) (if (ri == savedRow) focusedItemIndex else 0) else null,
                            onInitialFocusPlaced = { focusPlaced = true },
                            onTileFocused = { i, item ->
                                focusPlaced = true
                                focusedRowKey = row.key
                                focusedItemIndex = i
                                focusedItem = item
                                backdrop = tileImage(item)
                                onItemFocus(item)
                                val cr = row.tab.catalogRow
                                if (cr != null && cr.hasMore && !cr.isLoading && i >= cr.items.size - LoadMoreAhead) {
                                    onLoadMoreCatalog(cr.catalogId, cr.addonId, cr.apiType)
                                }
                            },
                            onVertical = { fromRow, x, down ->
                                val target = if (down) fromRow + 1 else fromRow - 1
                                val cand = tiles[target]?.values?.toList().orEmpty()
                                if (cand.isEmpty()) false
                                else {
                                    val under = cand.filter { it.second <= x + 8f }.maxByOrNull { it.second } ?: cand.minByOrNull { it.second }
                                    under?.first?.let { runCatching { it.requestFocus() }.isSuccess } ?: false
                                }
                            },
                            onClick = { item ->
                                HeroBackdropState.update(item.backdropUrl)
                                onNavigateToDetail(item.id, item.apiType, row.tab.catalogRow?.addonBaseUrl ?: "")
                            },
                            onLongClick = { item -> onItemLongPress(item, row.tab.catalogRow?.addonBaseUrl ?: "") }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FolderRowBlock(
    rowIndex: Int,
    row: FolderRow,
    focused: Boolean,
    tiles: MutableMap<Int, MutableMap<Int, Pair<FocusRequester, Float>>>,
    initialFocusIndex: Int?,
    onInitialFocusPlaced: () -> Unit,
    onTileFocused: (Int, MetaPreview) -> Unit,
    onVertical: (Int, Float, Boolean) -> Boolean,
    onClick: (MetaPreview) -> Unit,
    onLongClick: (MetaPreview) -> Unit
) {
    val tab = row.tab
    val items: List<MetaPreview> = if (tab.isLoading) emptyList()
    else tab.catalogRow?.items.orEmpty().filterNot { it.id.startsWith("__placeholder_") }

    Column(Modifier.fillMaxWidth()) {
        Text(
            row.title, color = if (focused) Color.White else LauncherStyle.Soft, fontSize = 16.sp, lineHeight = 21.sp, fontWeight = FontWeight.Bold,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 60.dp, end = LauncherStyle.Start, top = 12.dp)
        )
        Spacer(Modifier.height(8.dp))
        when {
            tab.error != null && items.isEmpty() -> {
                Text(
                    tab.error, color = LauncherStyle.Dim, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 60.dp, end = LauncherStyle.Start, top = 12.dp, bottom = 26.dp)
                )
            }
            items.isEmpty() -> {
                // Not loaded yet: quiet placeholders where the posters will appear.
                Row(
                    Modifier.padding(start = LauncherStyle.Start, top = 12.dp, bottom = 44.dp),
                    horizontalArrangement = Arrangement.spacedBy(LauncherStyle.Gap)
                ) {
                    repeat(6) { Box(Modifier.size(120.dp, 180.dp).clip(LauncherStyle.Corner).background(LauncherStyle.CardIdle)) }
                }
            }
            else -> {
                val rowTiles = remember(rowIndex) { mutableMapOf<Int, Pair<FocusRequester, Float>>() }
                DisposableEffect(rowIndex) { tiles[rowIndex] = rowTiles; onDispose { tiles.remove(rowIndex) } }
                val rowState = rememberLazyListState(initialFirstVisibleItemIndex = (initialFocusIndex ?: 0).coerceIn(0, items.lastIndex))
                LazyRow(
                    state = rowState,
                    contentPadding = PaddingValues(start = LauncherStyle.Start, end = LauncherStyle.Start, top = 12.dp, bottom = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(LauncherStyle.Gap)
                ) {
                    itemsIndexed(items, key = { i, m -> "${m.id}_$i" }) { i, m ->
                        val req = remember { FocusRequester() }
                        DisposableEffect(i) { onDispose { rowTiles.remove(i) } }
                        if (initialFocusIndex != null && i == initialFocusIndex.coerceIn(0, items.lastIndex)) {
                            LaunchedEffect(Unit) {
                                repeat(6) {
                                    withFrameNanos { }
                                    if (runCatching { req.requestFocus() }.isSuccess) { onInitialFocusPlaced(); return@LaunchedEffect }
                                }
                            }
                        }
                        FolderTile(
                            item = m,
                            modifier = Modifier
                                .focusRequester(req)
                                .onGloballyPositioned { rowTiles[i] = req to it.positionInRoot().x }
                                .onPreviewKeyEvent { e ->
                                    if (e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                                    when (e.key) {
                                        Key.DirectionRight -> i == items.lastIndex   // the end of a row stops focus
                                        Key.DirectionDown -> onVertical(rowIndex, rowTiles[i]?.second ?: 0f, true)
                                        Key.DirectionUp -> onVertical(rowIndex, rowTiles[i]?.second ?: 0f, false)
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
    }
}

@Composable
private fun FolderTile(
    item: MetaPreview,
    modifier: Modifier,
    onFocused: () -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val landscape = item.posterShape == PosterShape.LANDSCAPE
    val square = item.posterShape == PosterShape.SQUARE
    val w = if (landscape) 224.dp else if (square) 126.dp else 120.dp
    val h = if (landscape || square) 126.dp else 180.dp
    val image = tileImage(item)
    val scale by animateFloatAsState(if (focused) LauncherStyle.Zoom else 1f, tween(160), label = "zoom")
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
                    if (image != null) {
                        AsyncImage(model = image, contentDescription = item.name, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    } else {
                        Text(
                            item.name, color = LauncherStyle.Soft, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 3,
                            modifier = Modifier.align(Alignment.Center).padding(8.dp)
                        )
                    }
                }
            }
        }
        Text(
            item.name, color = if (focused) Color.White else LauncherStyle.Soft, fontSize = 12.sp, lineHeight = 16.sp,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
            // Same layout height whether focused or not; the focused title only moves down visually, clear of the zoom.
            modifier = Modifier.padding(top = 8.dp).graphicsLayer { translationY = if (focused) 10.dp.toPx() else 0f }
        )
    }
}

package com.nuvio.tv.ui.screens.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.nuvio.tv.LauncherChrome
import com.nuvio.tv.domain.model.CatalogRow
import com.nuvio.tv.domain.model.MetaPreview
import com.nuvio.tv.domain.model.PosterShape
import kotlinx.coroutines.delay

/*
 * Nuvio Launcher: the Catalog page, in the launcher's Apple TV style, drawing Nuvio's own Home data
 * (rows in Nuvio's order, Continue Watching, collections, lazily loaded catalogs).
 *
 *  - Carousel: up to 8 featured titles, bright backdrop, title or logo, genre line, a 268-wide 3-line synopsis,
 *    Play / + / i / ›, dots; it moves on every 9 s while nothing is pressed.
 *  - Rows: titles at 16 (white only on the focused row), tiles 224x126 or 120x180, 29 apart, every tile titled,
 *    111% zoom with a soft shadow, the focused row kept in the middle of the screen, straight Up/Down,
 *    Right stops at the end of a row, Back returns to the carousel.
 */

private val Soft = Color(0xB8FFFFFF)          // the synopsis's soft white (72%)
private val Corner = RoundedCornerShape(7.dp)
private const val Zoom = 1.11f
private val Gap = 29.dp
private val Start = 56.dp
private const val RotateMs = 9_000L

private sealed class LRow(val key: String, val title: String) {
    class Continue(val items: List<ContinueWatchingItem>) : LRow("cw", "Continue Watching")
    class Catalog(val row: CatalogRow, title: String) : LRow("cat_" + row.addonId + "_" + row.catalogId + "_" + row.apiType, title)
    class Collection(val c: com.nuvio.tv.domain.model.Collection) : LRow("col_" + c.id, c.title)
    class Pending(val catalogKey: String, title: String) : LRow("ph_$catalogKey", title)
}

private fun rowsOf(state: HomeUiState): List<LRow> {
    val out = ArrayList<LRow>()
    if (state.continueWatchingItems.isNotEmpty()) out += LRow.Continue(state.continueWatchingItems)
    if (state.homeRows.isNotEmpty()) {
        for (r in state.homeRows) when (r) {
            is HomeRow.Catalog -> if (r.row.items.isNotEmpty()) out += LRow.Catalog(r.row, r.row.catalogName)
            is HomeRow.CollectionRow -> if (r.collection.folders.isNotEmpty()) out += LRow.Collection(r.collection)
            is HomeRow.PlaceholderCatalog -> out += LRow.Pending(r.catalogKey, r.displayTitle)
        }
    } else {
        for (c in state.catalogRows) if (c.items.isNotEmpty()) out += LRow.Catalog(c, c.catalogName)
    }
    return out
}

private fun yearOf(m: MetaPreview): String? = (m.releaseInfo ?: m.released)?.take(4)?.takeIf { it.all(Char::isDigit) }

private fun metaLine(m: MetaPreview): String = buildList {
    add(if (m.apiTypeOrRaw().equals("series", true)) "TV Show" else "Movie")
    m.genres.firstOrNull()?.let { add(it) }
    yearOf(m)?.let { add(it) }
    m.imdbRating?.let { add("★ " + String.format(java.util.Locale.US, "%.1f", it)) }
}.joinToString(" · ")

private fun MetaPreview.apiTypeOrRaw(): String = rawType.ifBlank { type.toApiString() }

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
internal fun LauncherHomeContent(
    uiState: HomeUiState,
    onNavigateToDetail: (String, String, String) -> Unit,
    onContinueWatchingClick: (ContinueWatchingItem) -> Unit,
    onNavigateToFolderDetail: (String, String) -> Unit,
    onPlayItem: (MetaPreview) -> Unit,
    onToggleLibrary: (MetaPreview) -> Unit,
    onRequestLazyCatalogLoad: (String) -> Unit
) {
    val rows = remember(uiState.continueWatchingItems, uiState.homeRows, uiState.catalogRows) { rowsOf(uiState) }
    val hero = remember(uiState.heroItems, uiState.catalogRows) {
        (uiState.heroItems.ifEmpty { uiState.catalogRows.firstOrNull { it.items.isNotEmpty() }?.items.orEmpty() })
            .filter { !it.background.isNullOrBlank() || !it.poster.isNullOrBlank() }.take(8)
    }
    var heroIndex by remember { mutableIntStateOf(0) }
    var heroFocused by remember { mutableStateOf(true) }
    var focusedRow by remember { mutableIntStateOf(-1) }
    var lastKeyAt by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val listState = rememberLazyListState()
    val playFocus = remember { FocusRequester() }

    // Tiles report where they are, so Up/Down can pick the tile directly above or below.
    val tiles = remember { mutableStateMapOf<Int, MutableMap<Int, Pair<FocusRequester, Float>>>() }

    // The pill shows only while the carousel has focus.
    LaunchedEffect(heroFocused) { LauncherChrome.pillVisible.value = heroFocused }
    DisposableEffect(Unit) { onDispose { LauncherChrome.pillVisible.value = true } }

    // Carousel: next title every 9 s while nothing is pressed.
    LaunchedEffect(hero.size) {
        while (hero.size > 1) {
            delay(1_000)
            if (System.currentTimeMillis() - lastKeyAt >= RotateMs) { heroIndex = (heroIndex + 1) % hero.size; lastKeyAt = System.currentTimeMillis() }
        }
    }
    LaunchedEffect(Unit) { runCatching { playFocus.requestFocus() } }

    BackHandler(enabled = !heroFocused) {
        focusedRow = -1
        runCatching { playFocus.requestFocus() }
    }

    val current = hero.getOrNull(heroIndex.coerceAtMost((hero.size - 1).coerceAtLeast(0)))
    // As the carousel scrolls up, its picture fades out to the launcher's grey, as in the old launcher.
    val density = androidx.compose.ui.platform.LocalDensity.current
    val heroFadePx = with(density) { 300.dp.toPx() }   // about 55% of the screen height
    val heroAlpha by remember {
        androidx.compose.runtime.derivedStateOf {
            if (listState.firstVisibleItemIndex > 0) 0f
            else (1f - listState.firstVisibleItemScrollOffset / heroFadePx).coerceIn(0f, 1f)
        }
    }
    val defaultSpec = LocalBringIntoViewSpec.current

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF2A2B30), Color(0xFF1E1F23))))
            .onPreviewKeyEvent { if (it.type == KeyEventType.KeyDown) lastKeyAt = System.currentTimeMillis(); false }
    ) {
        val screenH = maxHeight
        // Backdrop of the current carousel title, bright, with only a light shade on the left.
        Box(Modifier.fillMaxSize().graphicsLayer { alpha = heroAlpha }) {
            Crossfade(targetState = current?.background ?: current?.poster, animationSpec = tween(600), label = "backdrop") { url ->
                if (url != null) AsyncImage(model = url, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            }
            Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(0f to Color(0x73000000), 0.45f to Color(0x1F000000), 0.65f to Color.Transparent)))
            // bottom of the picture blends into the grey below it
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.6f to Color.Transparent, 0.85f to Color(0x80000000), 1f to Color(0xFF1E1F23))))
        }

        // Focus in the rows keeps that row in the middle; focus in the carousel keeps the page at the top.
        val centeringSpec = remember(heroFocused) {
            @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
            object : BringIntoViewSpec {
                override val scrollAnimationSpec = defaultSpec.scrollAnimationSpec
                override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float =
                    if (heroFocused) -100_000f else (offset + size / 2f) - containerSize / 2f
            }
        }
        CompositionLocalProvider(LocalBringIntoViewSpec provides centeringSpec) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = screenH / 2)
            ) {
                item(key = "hero") {
                    Hero(
                        item = current,
                        count = hero.size,
                        index = heroIndex,
                        height = 458.dp,
                        playFocus = playFocus,
                        onFocused = { heroFocused = true; focusedRow = -1 },
                        onPlay = { current?.let(onPlayItem) },
                        onAdd = { current?.let(onToggleLibrary) },
                        onInfo = { current?.let { onNavigateToDetail(it.id, it.apiTypeOrRaw(), it.sourceAddonBaseUrl ?: "") } },
                        onNext = { if (hero.isNotEmpty()) { heroIndex = (heroIndex + 1) % hero.size } }
                    )
                }
                itemsIndexed(rows, key = { _, r -> r.key }) { ri, row ->
                    CompositionLocalProvider(LocalBringIntoViewSpec provides defaultSpec) {
                        RowBlock(
                            rowIndex = ri,
                            row = row,
                            focused = focusedRow == ri,
                            first = ri == 0,
                            tiles = tiles,
                            onRowFocused = { heroFocused = false; focusedRow = ri },
                            onVertical = { fromRow, x, down ->
                                val target = if (down) fromRow + 1 else fromRow - 1
                                if (target < 0) { runCatching { playFocus.requestFocus() }; true }
                                else {
                                    val cand = tiles[target]?.values?.toList().orEmpty()
                                    if (cand.isEmpty()) false
                                    else {
                                        val under = cand.filter { it.second <= x + 8f }.maxByOrNull { it.second } ?: cand.minByOrNull { it.second }
                                        under?.first?.let { runCatching { it.requestFocus() }.isSuccess } ?: false
                                    }
                                }
                            },
                            onNavigateToDetail = onNavigateToDetail,
                            onContinueWatchingClick = onContinueWatchingClick,
                            onNavigateToFolderDetail = onNavigateToFolderDetail,
                            onRequestLazyCatalogLoad = onRequestLazyCatalogLoad
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Hero(
    item: MetaPreview?,
    count: Int,
    index: Int,
    height: androidx.compose.ui.unit.Dp,
    playFocus: FocusRequester,
    onFocused: () -> Unit,
    onPlay: () -> Unit,
    onAdd: () -> Unit,
    onInfo: () -> Unit,
    onNext: () -> Unit
) {
    Box(Modifier.fillMaxWidth().height(height)) {
        if (item != null) {
            Column(Modifier.padding(start = 57.dp, top = 240.dp).width(290.dp)) {
                if (!item.logo.isNullOrBlank()) {
                    AsyncImage(
                        model = item.logo, contentDescription = item.name, contentScale = ContentScale.Fit,
                        alignment = Alignment.CenterStart, modifier = Modifier.widthIn(max = 160.dp).height(57.dp)
                    )
                } else {
                    Text(item.name, color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Bold, lineHeight = 34.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                Text(metaLine(item), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 12.dp), maxLines = 1)
                Text(
                    item.description.orEmpty(), color = Soft, fontSize = 10.5.sp, lineHeight = 14.5.sp,
                    maxLines = 3, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp).width(268.dp)
                )
            }
        }
        Row(Modifier.padding(start = 57.dp, top = 377.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            HeroButton(width = 106.dp, primary = true, modifier = Modifier.focusRequester(playFocus), onFocused = onFocused, onClick = onPlay) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp), tint = it)
                    Spacer(Modifier.width(4.dp)); Text("Play", color = it, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            HeroButton(width = 29.dp, onFocused = onFocused, onClick = onAdd) { Icon(Icons.Default.Add, contentDescription = "Add to library", modifier = Modifier.size(15.dp), tint = it) }
            HeroButton(width = 29.dp, onFocused = onFocused, onClick = onInfo) { Icon(Icons.Default.Info, contentDescription = "Details", modifier = Modifier.size(14.dp), tint = it) }
            HeroButton(width = 29.dp, plain = true, onFocused = onFocused, onClick = onNext) { Text("›", color = it, fontSize = 16.sp, fontWeight = FontWeight.Bold) }
        }
        if (count > 1) {
            Row(
                Modifier.align(Alignment.TopCenter).offset(y = 440.dp).clip(RoundedCornerShape(99.dp)).background(Color(0x40000000)).padding(horizontal = 6.dp, vertical = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                repeat(count) { i ->
                    Box(Modifier.width(if (i == index) 12.dp else 5.dp).height(5.dp).clip(RoundedCornerShape(9.dp)).background(if (i == index) Color.White else Color(0x8CFFFFFF)))
                }
            }
        }
    }
}

@Composable
private fun HeroButton(
    width: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    plain: Boolean = false,
    onFocused: () -> Unit,
    onClick: () -> Unit,
    content: @Composable (Color) -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val bg = when {
        focused -> Color(0xFFF4F4F6)
        primary -> Color(0xFFF4F4F6)
        plain -> Color.Transparent
        else -> Color(0x8C141418)
    }
    val fg = if (focused || primary) Color(0xFF121316) else Color.White
    Card(
        onClick = onClick,
        modifier = modifier.size(width, if (primary) 30.dp else 29.dp).onFocusChanged { focused = it.hasFocus; if (it.hasFocus) onFocused() },
        shape = CardDefaults.shape(RoundedCornerShape(99.dp)),
        scale = CardDefaults.scale(focusedScale = 1.06f),
        border = CardDefaults.border(border = Border.None, focusedBorder = Border.None),
        colors = CardDefaults.colors(containerColor = bg, focusedContainerColor = bg)
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { content(fg) }
    }
}

private class Tile(val key: String, val title: String, val image: String?, val landscape: Boolean, val square: Boolean = false, val progress: Float? = null, val badge: String? = null, val onClick: () -> Unit)

@Composable
private fun RowBlock(
    rowIndex: Int,
    row: LRow,
    focused: Boolean,
    first: Boolean,
    tiles: MutableMap<Int, MutableMap<Int, Pair<FocusRequester, Float>>>,
    onRowFocused: () -> Unit,
    onVertical: (Int, Float, Boolean) -> Boolean,
    onNavigateToDetail: (String, String, String) -> Unit,
    onContinueWatchingClick: (ContinueWatchingItem) -> Unit,
    onNavigateToFolderDetail: (String, String) -> Unit,
    onRequestLazyCatalogLoad: (String) -> Unit
) {
    val items: List<Tile> = remember(row) {
        when (row) {
            is LRow.Continue -> row.items.map { cw ->
                when (cw) {
                    is ContinueWatchingItem.InProgress -> {
                        val p = cw.progress
                        val pct = (p.progressPercent?.div(100f)) ?: (if (p.duration > 0) p.position.toFloat() / p.duration else 0f)
                        val left = if (p.duration > 0) ((p.duration - p.position) / 60_000L).coerceAtLeast(1) else null
                        val ep = if (p.season != null && p.episode != null) " · S${p.season} E${p.episode}" else ""
                        Tile("cw_" + p.contentId + "_" + p.videoId, p.name + ep, cw.customLandscapePoster ?: cw.episodeThumbnail ?: p.backdrop ?: p.poster, true,
                            progress = pct.coerceIn(0f, 1f), badge = left?.let { "▶ $it min left" }) { onContinueWatchingClick(cw) }
                    }
                    is ContinueWatchingItem.NextUp -> {
                        val i = cw.info
                        Tile("nu_" + i.contentId + "_" + i.videoId, i.name + " · S${i.season} E${i.episode}", cw.customLandscapePoster ?: i.thumbnail ?: i.backdrop ?: i.poster, true,
                            badge = "▶ Next episode") { onContinueWatchingClick(cw) }
                    }
                }
            }
            is LRow.Catalog -> row.row.items.map { m ->
                val land = m.posterShape == PosterShape.LANDSCAPE
                Tile("m_" + m.id, m.name, if (land) (m.landscapePoster ?: m.background ?: m.poster) else (m.poster ?: m.background), land) {
                    onNavigateToDetail(m.id, m.apiTypeOrRaw(), row.row.addonBaseUrl)
                }
            }
            is LRow.Collection -> row.c.folders.map { f ->
                Tile("f_" + f.id, if (f.hideTitle) "" else f.title, f.coverImageUrl, f.tileShape == PosterShape.LANDSCAPE, square = f.tileShape == PosterShape.SQUARE) {
                    onNavigateToFolderDetail(row.c.id, f.id)
                }
            }
            is LRow.Pending -> emptyList()
        }
    }
    if (row is LRow.Pending) LaunchedEffect(row.catalogKey) { onRequestLazyCatalogLoad(row.catalogKey) }

    Column(Modifier.fillMaxWidth()) {
        Text(
            row.title, color = if (focused) Color.White else Soft, fontSize = 16.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 60.dp, top = if (first) 0.dp else 26.dp), maxLines = 1
        )
        Spacer(Modifier.height(if (first) 12.dp else 22.dp))
        if (items.isEmpty()) {
            // Not loaded yet: quiet placeholders where the posters will appear.
            Row(Modifier.padding(start = Start), horizontalArrangement = Arrangement.spacedBy(Gap)) {
                repeat(6) { Box(Modifier.size(120.dp, 180.dp).clip(Corner).background(Color(0x14FFFFFF))) }
            }
        } else {
            val rowTiles = remember(rowIndex) { mutableMapOf<Int, Pair<FocusRequester, Float>>() }
            DisposableEffect(rowIndex) { tiles[rowIndex] = rowTiles; onDispose { tiles.remove(rowIndex) } }
            LazyRow(
                contentPadding = PaddingValues(start = Start, end = Start, top = 6.dp, bottom = 26.dp),
                horizontalArrangement = Arrangement.spacedBy(Gap)
            ) {
                itemsIndexed(items, key = { _, t -> t.key }) { i, t ->
                    val req = remember { FocusRequester() }
                    DisposableEffect(i) { onDispose { rowTiles.remove(i) } }
                    TileView(
                        tile = t,
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
                        onFocused = onRowFocused
                    )
                }
            }
        }
    }
}

@Composable
private fun TileView(tile: Tile, modifier: Modifier, onFocused: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val w = if (tile.landscape) 224.dp else if (tile.square) 126.dp else 120.dp
    val h = if (tile.landscape || tile.square) 126.dp else 180.dp
    val scale by animateFloatAsState(if (focused) Zoom else 1f, tween(160), label = "zoom")
    Column(Modifier.width(w)) {
        Box(
            Modifier
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .shadow(elevation = if (focused) 17.dp else 7.dp, shape = Corner, ambientColor = Color.Black, spotColor = Color.Black)
        ) {
            Card(
                onClick = tile.onClick,
                modifier = modifier.size(w, h).onFocusChanged { focused = it.hasFocus; if (it.hasFocus) onFocused() },
                shape = CardDefaults.shape(Corner),
                scale = CardDefaults.scale(focusedScale = 1f),
                border = CardDefaults.border(border = Border.None, focusedBorder = Border.None),
                colors = CardDefaults.colors(containerColor = Color(0xFF2A2C33), focusedContainerColor = Color(0xFF2A2C33))
            ) {
                Box(Modifier.fillMaxSize()) {
                    if (tile.image != null) AsyncImage(model = tile.image, contentDescription = tile.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    else Text(tile.title, color = Soft, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Center).padding(8.dp), maxLines = 3)
                    if (tile.badge != null || tile.progress != null) {
                        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.45f to Color.Transparent, 1f to Color(0xB3000000))))
                        Column(Modifier.align(Alignment.BottomStart).padding(10.dp).fillMaxWidth()) {
                            tile.badge?.let { Text(it, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1) }
                            tile.progress?.let { p ->
                                Box(Modifier.padding(top = 6.dp).fillMaxWidth().height(4.dp).clip(RoundedCornerShape(3.dp)).background(Color(0x66000000))) {
                                    Box(Modifier.fillMaxWidth(p).height(4.dp).clip(RoundedCornerShape(3.dp)).background(Color.White))
                                }
                            }
                        }
                    }
                }
            }
        }
        if (tile.title.isNotEmpty()) {
            Text(
                tile.title, color = if (focused) Color.White else Soft, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = if (focused) 18.dp else 8.dp)
            )
        }
    }
}

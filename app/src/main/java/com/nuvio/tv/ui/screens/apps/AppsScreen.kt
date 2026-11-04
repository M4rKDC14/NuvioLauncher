package com.nuvio.tv.ui.screens.apps

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.Text
import com.nuvio.tv.LocalContentFocusRequester
import com.nuvio.tv.data.local.LauncherSettingsHolder
import com.nuvio.tv.ui.launcher.LauncherStyle
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

/*
 * Nuvio Launcher: the TV's installed apps, one sideways row per category, as in the 0.23 launcher.
 * Tiles are the apps' TV banners at 224 x 126, zoom to 120% with a soft shadow, and show the
 * app's name under every tile (white and moved down when focused). Up/Down returns to the app each row last had focus on. Size, spacing and zoom come from Settings → Launcher, and the
 * apps hidden there are left out.
 */

private val TileWidth = 224.dp
private val TileHeight = 126.dp
/** Pictures are drawn for the largest tile size Settings → Launcher allows, so they stay sharp. */
private const val ArtScale = 1.3f
private val Pad = 56.dp
private val TileShape = RoundedCornerShape(7.dp)

/** An installed app that can be opened from the TV's home screen. Also listed by Settings → Launcher → Hide apps. */
internal class LauncherApp(val label: String, val pkg: String, val art: ImageBitmap?, val isBanner: Boolean, val group: Int) {
    val groupName: String get() = GROUPS[group]
}

private val GROUPS = listOf("Streaming", "Music & Media", "Games", "Tools", "TV & System", "Other")

private fun has(s: String, vararg words: String) = words.any { s.contains(it) }

/** The app's row: names we know first, then the category the app declares to Android, then TCL/system packages. */
private fun groupOf(label: String, pkg: String, info: ApplicationInfo): Int {
    val n = label.lowercase(Locale.ROOT)
    val p = pkg.lowercase(Locale.ROOT)
    if (p == "com.android.vending") return 3
    if (has(p, "com.google.android.play.games") || has(n, "games")) return 2
    if (has(p, "com.google.android.videos") || has(n, "nuvio", "youtube", "netflix", "prime video", "disney", "hbo", "apple tv", "dazn", "plex", "stremio", "kodi", "twitch", "sport", "movies", "crunchyroll", "paramount", "peacock", "hulu", "jellyfin", "emby")) return 0
    if (has(n, "music", "spotify", "player", "gallery", "photo", "media", "magiconnect", "exhibition", "t-solo", "tidal", "deezer", "podcast", "radio")) return 1
    if (has(n, "downloader", "vpn", "speed", "browser", "file", "cleaner", "send files", "analy")) return 3
    if (has(n, "setting", "guide", "quick", "safety", "panel", "tcl", "home") || n == "tv" || has(p, "com.tcl.", "com.android.", "com.google.android.tv")) return 4
    if ((info.flags and ApplicationInfo.FLAG_IS_GAME) != 0) return 2
    return when (info.category) {
        ApplicationInfo.CATEGORY_VIDEO -> 0
        ApplicationInfo.CATEGORY_AUDIO, ApplicationInfo.CATEGORY_IMAGE -> 1
        ApplicationInfo.CATEGORY_GAME -> 2
        ApplicationInfo.CATEGORY_PRODUCTIVITY, ApplicationInfo.CATEGORY_ACCESSIBILITY -> 3
        else -> 5
    }
}

private fun art(d: Drawable?, w: Int, h: Int): ImageBitmap? =
    runCatching { d?.toBitmap(w, h)?.asImageBitmap() }.getOrNull()

/** Every launchable app (Leanback launcher first, then ordinary launcher apps), in category order, then by name. */
internal fun loadLauncherApps(context: Context): List<LauncherApp> {
    val pm = context.packageManager
    val density = context.resources.displayMetrics.density * ArtScale
    val bw = (224 * density).toInt(); val bh = (126 * density).toInt(); val iw = (72 * density).toInt()
    val seen = HashSet<String>()
    val apps = ArrayList<LauncherApp>()
    for (category in listOf(Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER)) {
        for (ri in pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(category), 0)) {
            val ai = ri.activityInfo ?: continue
            val pkg = ai.packageName
            if (pkg == context.packageName || !seen.add(pkg)) continue
            if (category == Intent.CATEGORY_LAUNCHER && pm.getLeanbackLaunchIntentForPackage(pkg) == null && pm.getLaunchIntentForPackage(pkg) == null) continue
            val label = ri.loadLabel(pm).toString()
            val banner = ai.loadBanner(pm) ?: ai.applicationInfo.loadBanner(pm)
            val bitmap = if (banner != null) art(banner, bw, bh) else art(ri.loadIcon(pm), iw, iw)
            apps += LauncherApp(label, pkg, bitmap, banner != null, groupOf(label, pkg, ai.applicationInfo))
        }
    }
    return apps.sortedWith(
        compareBy<LauncherApp> { it.group }
            .thenBy { !it.label.lowercase().contains("nuvio") }
            .thenBy { it.label.lowercase() }
    )
}

private fun groupApps(apps: List<LauncherApp>): List<Pair<String, List<LauncherApp>>> =
    GROUPS.indices.mapNotNull { g ->
        val inGroup = apps.filter { it.group == g }
        if (inGroup.isEmpty()) null else GROUPS[g] to inGroup
    }

private fun launchApp(context: Context, pkg: String) {
    val pm = context.packageManager
    val intent = pm.getLeanbackLaunchIntentForPackage(pkg) ?: pm.getLaunchIntentForPackage(pkg) ?: return
    runCatching { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun AppsScreen() {
    val context = LocalContext.current
    var allApps by remember { mutableStateOf<List<LauncherApp>>(emptyList()) }
    LaunchedEffect(Unit) { allApps = withContext(Dispatchers.IO) { loadLauncherApps(context) } }
    val settings by LauncherSettingsHolder.settings.collectAsState()
    val hidden = settings.hiddenPackages
    val groups = remember(allApps, hidden) { groupApps(allApps.filterNot { it.pkg in hidden }) }
    val tileScale = settings.appsScale
    // never less than the focused banner's zoom overhang (17 at 120% zoom let it overlap its neighbours)
    val tileGap = com.nuvio.tv.ui.launcher.zoomSafeGap(settings.appsTileSpacingDp.dp, 224.dp * settings.appsScale, settings.appsZoom)
    val tileZoom = settings.appsZoom
    val firstTile = LocalContentFocusRequester.current
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Room above the zoomed banner, so it never runs into the row title, and below it for the focused app's name,
    // which moves down as on Home.
    val tileHeight = TileHeight * tileScale
    val zoomOverhang = tileHeight * ((tileZoom - 1f).coerceAtLeast(0f) / 2f)
    val labelLift = maxOf(4.dp, 10.dp + (tileHeight * (tileZoom - 1f) - TileHeight * 0.11f) / 2f)
    val rowTopPad = maxOf(16.dp, zoomOverhang + 4.dp)
    val rowBottomPad = labelLift + 4.dp

    // One focus target per app (each app is in one row only), each row's scroll position, and the app each row
    // last had focus on. Up/Down goes to that app (the first one if the row was never visited).
    val requesters = remember(groups) { groups.flatMap { it.second }.associate { it.pkg to FocusRequester() } }
    val rowStates = remember(groups.size) { List(groups.size) { LazyListState() } }
    val lastFocused = remember { mutableMapOf<Int, String>() }

    fun focusRow(target: Int): Boolean {
        if (target !in groups.indices) return false
        val apps = groups[target].second
        if (apps.isEmpty()) return true
        val pkg = lastFocused[target]?.takeIf { p -> apps.any { it.pkg == p } } ?: apps.first().pkg
        val req = requesters[pkg] ?: return true
        val appIndex = apps.indexOfFirst { it.pkg == pkg }.coerceAtLeast(0)
        scope.launch {
            // Scroll the page only when the row isn't wholly on screen; it then comes to the top.
            val info = listState.layoutInfo
            val shown = info.visibleItemsInfo.firstOrNull { it.index == target }
            val whollyShown = shown != null && shown.offset >= 0 && shown.offset + shown.size <= info.viewportEndOffset
            if (!whollyShown) listState.animateScrollToItem(target)
            repeat(20) { attempt ->
                if (runCatching { req.requestFocus() }.getOrDefault(false)) return@launch
                // the remembered app may be off the row's visible part: bring it in, then try again
                if (attempt == 3) runCatching { rowStates.getOrNull(target)?.scrollToItem(appIndex) }
                withFrameNanos { }
            }
        }
        return true
    }

    // Focus moves inside a row never scroll the page; only Up/Down (above) does. The rows themselves scroll
    // sideways as usual.
    val defaultSpec = LocalBringIntoViewSpec.current
    val pageSpec = remember(defaultSpec) {
        @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
        object : BringIntoViewSpec {
            override val scrollAnimationSpec = defaultSpec.scrollAnimationSpec
            override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float = 0f
        }
    }

    // 1.4: the blurred picture of the last focused title behind the apps, like Settings, Search and Library
    Box(Modifier.fillMaxSize()) {
        com.nuvio.tv.ui.launcher.LauncherBlurredBackdrop(url = com.nuvio.tv.LauncherChrome.lastBackdrop.value)
        CompositionLocalProvider(LocalBringIntoViewSpec provides pageSpec) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                // the first category title at ~70 from the top, under the time pill
                contentPadding = PaddingValues(top = 68.dp, bottom = 280.dp)
            ) {
                groups.forEachIndexed { gi, (title, apps) ->
                    item(key = title) {
                        CompositionLocalProvider(LocalBringIntoViewSpec provides defaultSpec) {
                            var rowFocused by remember { mutableStateOf(false) }
                            Column(Modifier.onFocusChanged { rowFocused = it.hasFocus }) {
                                Text(
                                    text = title,
                                    color = if (rowFocused) Color.White else LauncherStyle.Soft,
                                    fontSize = 16.sp,
                                    lineHeight = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    modifier = Modifier.padding(start = Pad, end = Pad)
                                )
                                LazyRow(
                                    state = rowStates[gi],
                                    contentPadding = PaddingValues(start = Pad, end = Pad, top = rowTopPad, bottom = rowBottomPad),
                                    horizontalArrangement = Arrangement.spacedBy(tileGap)
                                ) {
                                    itemsIndexed(apps, key = { _, app -> app.pkg }) { ai, app ->
                                        val isFirst = gi == 0 && ai == 0
                                        AppTile(
                                            app = app,
                                            sizeScale = tileScale,
                                            zoom = tileZoom,
                                            labelLift = labelLift,
                                            modifier = Modifier
                                                .then(if (isFirst) Modifier.focusRequester(firstTile) else Modifier)
                                                .then(requesters[app.pkg]?.let { Modifier.focusRequester(it) } ?: Modifier)
                                                .onPreviewKeyEvent { e ->
                                                    if (e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                                                    when (e.key) {
                                                        // the end of a row stops focus, so it never jumps to another row
                                                        Key.DirectionRight -> ai == apps.lastIndex
                                                        Key.DirectionDown -> focusRow(gi + 1)
                                                        Key.DirectionUp -> focusRow(gi - 1)
                                                        else -> false
                                                    }
                                                },
                                            onFocused = { lastFocused[gi] = app.pkg }
                                        ) { launchApp(context, app.pkg) }
                                    }
                                }
                                Spacer(Modifier.height(maxOf(8.dp, 44.dp - rowBottomPad)))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppTile(
    app: LauncherApp,
    sizeScale: Float,
    zoom: Float,
    labelLift: Dp,
    modifier: Modifier,
    onFocused: () -> Unit,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val tileWidth = TileWidth * sizeScale
    val tileHeight = TileHeight * sizeScale
    Column(modifier = Modifier.width(tileWidth)) {
        Card(
            onClick = onClick,
            modifier = modifier
                .size(tileWidth, tileHeight)
                .shadow(elevation = if (focused) 17.dp else 7.dp, shape = TileShape, ambientColor = Color.Black, spotColor = Color.Black)
                .onFocusChanged {
                    focused = it.hasFocus
                    if (it.hasFocus) onFocused()
                },
            shape = CardDefaults.shape(TileShape),
            scale = CardDefaults.scale(focusedScale = zoom),
            border = CardDefaults.border(border = Border.None, focusedBorder = Border.None),
            colors = CardDefaults.colors(containerColor = Color(0xFF2E3036), focusedContainerColor = Color(0xFF2E3036))
        ) {
            Box(modifier = Modifier.fillMaxSize().background(Color(0xFF2E3036)), contentAlignment = Alignment.Center) {
                val bmp = app.art
                if (bmp != null) {
                    Image(
                        bitmap = bmp, contentDescription = app.label,
                        contentScale = if (app.isBanner) ContentScale.Crop else ContentScale.Fit,
                        modifier = if (app.isBanner) Modifier.fillMaxSize() else Modifier.size(72.dp * sizeScale)
                    )
                } else {
                    Text(text = app.label, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        // The app's name under every banner: white and moved down clear of the zoomed banner when focused
        // (the layout stays the same, so the page never jumps), soft white otherwise.
        Text(
            text = app.label,
            color = if (focused) Color.White else LauncherStyle.Soft,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .padding(top = 8.dp)
                .graphicsLayer { translationY = if (focused) labelLift.toPx() else 0f }
        )
    }
}

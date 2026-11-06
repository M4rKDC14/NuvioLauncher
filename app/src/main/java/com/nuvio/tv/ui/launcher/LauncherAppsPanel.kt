package com.nuvio.tv.ui.launcher

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.nuvio.tv.LauncherChrome
import com.nuvio.tv.data.local.LauncherSettingsHolder
import com.nuvio.tv.ui.components.NuvioDialog
import com.nuvio.tv.ui.screens.apps.LauncherApp
import com.nuvio.tv.ui.screens.apps.launchApp
import com.nuvio.tv.ui.screens.apps.loadLauncherApps
import com.nuvio.tv.ui.screens.player.LauncherScaled
import com.nuvio.tv.ui.screens.settings.LauncherSettingsViewModel
import com.nuvio.tv.ui.screens.settings.LocalSettingsGlass
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Nuvio Launcher: the Applications panel (approved mockup), opened by Applications in the side panel instead of the
 * Applications page. Centred on the screen, the Inputs popup's width and glass; one row per category (Streaming,
 * Music & Media, Games, Tools, TV & System, Other), three rows in view; Up/Down move between categories, Left/Right
 * along a row. Long-press an app: Open, Move, Hide, App Info.
 *
 * Every size below is the mockup's (1dp = its 2px) before its 80% scale; the panel is drawn at 80%:
 * panel 960dp wide, 24dp corners, 20dp in at the top and 24dp at the sides; "Applications" 18sp bold white, the app
 * count 12sp grey on the right 3dp lower; 16dp to the rows; rows area 465dp tall (3 rows and the next row's top);
 * row title 14sp grey (white bold for the row with focus), 3dp above it, 7dp below it; banners 166.4x93.6dp, 20dp
 * apart, 7dp corners; name 10sp grey at the bottom-left over a fade (8dp in, 6dp up), white bold when focused,
 * banner zooms 1.11; 19dp from a row's banners to the next row title's top margin.
 */

@Composable
fun LauncherAppsHost() {
    val open = LauncherChrome.appsOpen.value
    AnimatedVisibility(visible = open, enter = fadeIn(tween(180)), exit = fadeOut(tween(150))) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)))
    }
    AnimatedVisibility(
        visible = open,
        enter = fadeIn(tween(180)) + scaleIn(tween(220), initialScale = 0.96f),
        exit = fadeOut(tween(150)) + scaleOut(tween(180), targetScale = 0.96f),
        modifier = Modifier.fillMaxSize()
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            AppsPanel(onClose = { LauncherChrome.appsOpen.value = false })
        }
    }
}

/** Apps of each category in the saved order (moved apps first, in the order they were moved to; the rest by name). */
private fun ordered(apps: List<LauncherApp>, order: List<String>): List<Pair<String, MutableList<LauncherApp>>> {
    val rank = order.withIndex().associate { (i, p) -> p to i }
    return apps.groupBy { it.group }.toSortedMap().map { (_, list) ->
        list.first().groupName to list.sortedWith(compareBy { rank[it.pkg] ?: Int.MAX_VALUE }).toMutableList()
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun AppsPanel(onClose: () -> Unit) {
    val context = LocalContext.current
    val vm: LauncherSettingsViewModel = hiltViewModel()
    val settings by LauncherSettingsHolder.settings.collectAsState()
    var allApps by remember { mutableStateOf<List<LauncherApp>?>(null) }
    LaunchedEffect(Unit) { allApps = withContext(Dispatchers.IO) { loadLauncherApps(context) } }

    val visible = allApps.orEmpty().filterNot { it.pkg in settings.hiddenPackages }
    // The rows as shown; Move reorders a row here and the order is saved when the move ends.
    val rows = remember(allApps, settings.hiddenPackages) {
        mutableStateOf(ordered(visible, settings.appOrder))
    }
    var focusedRow by remember { mutableStateOf(0) }
    var menuFor by remember { mutableStateOf<LauncherApp?>(null) }
    var moving by remember { mutableStateOf<String?>(null) }
    val requesters = remember { mutableStateMapOf<String, FocusRequester>() }
    val first = remember { FocusRequester() }

    fun saveOrder() {
        vm.setAppOrder(rows.value.flatMap { (_, apps) -> apps.map { it.pkg } })
    }

    BackHandler {
        if (moving != null) { moving = null; saveOrder() } else onClose()
    }
    LaunchedEffect(allApps) { if (!allApps.isNullOrEmpty()) runCatching { first.requestFocus() } }

    LauncherScaled(0.8f) {
        LauncherGlass(
            modifier = Modifier.width(960.dp),
            shape = RoundedCornerShape(24.dp),
            tint = Color(0xF0121316)   // .94, as the mockup
        ) {
            Column {
                Box(Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 20.dp)) {
                    Text("Applications", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(
                        "${visible.size} apps", fontSize = 12.sp, color = LauncherStyle.TileTitleIdle,
                        modifier = Modifier.align(Alignment.TopEnd).padding(top = 3.dp)
                    )
                }
                Spacer(Modifier.height(16.dp))
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().height(465.dp)
                ) {
                    itemsIndexed(rows.value, key = { _, r -> r.first }) { ri, (title, apps) ->
                        Column(Modifier.onFocusChanged { if (it.hasFocus) focusedRow = ri }) {
                            Text(
                                title,
                                fontSize = 14.sp,
                                fontWeight = if (focusedRow == ri) FontWeight.Bold else FontWeight.Normal,
                                color = if (focusedRow == ri) Color.White else LauncherStyle.TileTitleIdle,
                                modifier = Modifier.padding(start = 24.dp, top = 3.dp)
                            )
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(20.dp),
                                contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 7.dp, bottom = 10.dp)
                            ) {
                                items(apps, key = { it.pkg }) { app ->
                                    val req = requesters.getOrPut(app.pkg) { FocusRequester() }
                                    AppBanner(
                                        app = app,
                                        moving = moving == app.pkg,
                                        modifier = Modifier
                                            .then(if (ri == 0 && app == apps.first()) Modifier.focusRequester(first) else Modifier)
                                            .focusRequester(req)
                                            .onPreviewKeyEvent { e ->
                                                if (moving != app.pkg) return@onPreviewKeyEvent false
                                                if (e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent e.key in MoveKeys
                                                when (e.key) {
                                                    Key.DirectionLeft, Key.DirectionRight -> {
                                                        val list = rows.value[ri].second
                                                        val i = list.indexOfFirst { it.pkg == app.pkg }
                                                        val j = if (e.key == Key.DirectionLeft) i - 1 else i + 1
                                                        if (i >= 0 && j in list.indices) {
                                                            val copy = rows.value.toMutableList()
                                                            val moved = list.toMutableList().apply { add(j, removeAt(i)) }
                                                            copy[ri] = title to moved
                                                            rows.value = copy
                                                        }
                                                        true
                                                    }
                                                    Key.DirectionUp, Key.DirectionDown -> true   // a move stays in its row
                                                    Key.DirectionCenter, Key.Enter -> { moving = null; saveOrder(); true }
                                                    else -> false
                                                }
                                            },
                                        onClick = { if (moving == null) { onClose(); launchApp(context, app.pkg) } },
                                        onLongClick = { if (moving == null) menuFor = app }
                                    )
                                }
                            }
                            Spacer(Modifier.height(9.dp))
                        }
                    }
                }
            }
        }
    }

    menuFor?.let { app ->
        val isFavourite = app.pkg in settings.favourites
        AppOptionsDialog(
            label = app.label,
            onDismiss = { menuFor = null },
            options = listOf(
                "Open" to { menuFor = null; onClose(); launchApp(context, app.pkg) },
                "Move" to {
                    menuFor = null
                    moving = app.pkg
                    runCatching { requesters[app.pkg]?.requestFocus() }
                },
                // Nuvio Launcher: pins the app to (or takes it off) Home's Favourites row
                (if (isFavourite) "Remove from Favourites" else "Add to Favourites") to {
                    menuFor = null
                    vm.setFavourites(if (isFavourite) settings.favourites - app.pkg else settings.favourites + app.pkg)
                },
                "Hide" to { menuFor = null; vm.setAppHidden(app.pkg, true) },
                "App Info" to { menuFor = null; openAppInfo(context, app.pkg) }
            )
        )
    }
}

private val MoveKeys = setOf(Key.DirectionLeft, Key.DirectionRight, Key.DirectionUp, Key.DirectionDown, Key.DirectionCenter, Key.Enter)

internal fun openAppInfo(context: Context, pkg: String) {
    runCatching {
        context.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", pkg, null))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun AppBanner(app: LauncherApp, moving: Boolean, modifier: Modifier, onClick: () -> Unit, onLongClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val zoom by animateFloatAsState(if (focused) LauncherStyle.Zoom else 1f, tween(160), label = "appBannerZoom")
    val shape = RoundedCornerShape(7.dp)
    Box(
        Modifier
            .graphicsLayer { scaleX = zoom; scaleY = zoom }
            .shadow(if (focused) 9.dp else 4.dp, shape, ambientColor = Color.Black, spotColor = Color.Black)
            // while it is being moved: a white outline
            .then(if (moving) Modifier.border(BorderStroke(2.dp, LauncherStyle.FocusWhite), shape) else Modifier)
    ) {
        Card(
            onClick = onClick,
            onLongClick = onLongClick,
            modifier = modifier
                .size(width = 166.4.dp, height = 93.6.dp)
                .onFocusChanged { focused = it.isFocused },
            shape = CardDefaults.shape(shape),
            scale = CardDefaults.scale(focusedScale = 1f, pressedScale = 1f),
            colors = CardDefaults.colors(containerColor = Color(0xFF2E3036), focusedContainerColor = Color(0xFF2E3036))
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                val bmp = app.art
                if (bmp != null) {
                    Image(
                        bitmap = bmp, contentDescription = app.label,
                        contentScale = if (app.isBanner) ContentScale.Crop else ContentScale.Fit,
                        modifier = if (app.isBanner) Modifier.fillMaxSize() else Modifier.size(40.dp)
                    )
                } else {
                    Text(app.label, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
                }
                Box(
                    Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.65f))))
                        .padding(start = 8.dp, end = 8.dp, top = 15.dp, bottom = 6.dp)
                ) {
                    Text(
                        app.label,
                        fontSize = 10.sp,
                        fontWeight = if (focused) FontWeight.Bold else FontWeight.Normal,
                        color = if (focused) Color.White else LauncherStyle.TileTitleIdle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/** Long-press menu: the Resume popup's look (narrow glass, centred pills 16dp apart, drawn at 85%). */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
internal fun AppOptionsDialog(
    label: String,
    onDismiss: () -> Unit,
    options: List<Pair<String, () -> Unit>>
) {
    val openFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { openFocus.requestFocus() } }
    LauncherScaled(0.85f) {
        CompositionLocalProvider(LocalSettingsGlass provides true) {
            NuvioDialog(onDismiss = onDismiss, title = "", width = 282.dp) {
                Column {
                    Text(label, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("App options", fontSize = 14.sp, color = LauncherStyle.TileTitleIdle, modifier = Modifier.padding(top = 4.dp))
                }
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    options.forEachIndexed { i, (text, action) ->
                        MenuPill(text, action, if (i == 0) Modifier.focusRequester(openFocus) else Modifier)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun MenuPill(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    var focused by remember { mutableStateOf(false) }
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(40.dp).onFocusChanged { focused = it.isFocused },
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.White.copy(alpha = 0.12f),
            focusedContainerColor = LauncherStyle.FocusWhite
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(999.dp))
    ) {
        Box(Modifier.fillMaxSize().padding(horizontal = 18.dp), contentAlignment = Alignment.Center) {
            Text(
                text, fontSize = 15.sp,
                fontWeight = if (focused) FontWeight.Bold else FontWeight.Normal,
                color = if (focused) LauncherStyle.FocusText else LauncherStyle.TileTitleIdle
            )
        }
    }
}

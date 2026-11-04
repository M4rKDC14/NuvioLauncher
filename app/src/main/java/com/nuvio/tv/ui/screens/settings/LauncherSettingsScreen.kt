@file:OptIn(ExperimentalTvMaterial3Api::class, ExperimentalLayoutApi::class)

package com.nuvio.tv.ui.screens.settings

import android.text.format.DateFormat
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.nuvio.tv.ui.launcher.LauncherStyle
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.nuvio.tv.data.local.LauncherSettings
import com.nuvio.tv.ui.components.NuvioDialog
import com.nuvio.tv.ui.screens.apps.LauncherApp
import com.nuvio.tv.ui.screens.apps.loadLauncherApps
import com.nuvio.tv.ui.theme.NuvioTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/*
 * Nuvio Launcher: Settings → Launcher, the launcher's own options. Kept on this TV (LauncherSettingsDataStore),
 * except "Carousel catalogs", which is Nuvio's own Hero Catalogs selection (LayoutSettingsViewModel).
 */

private const val LauncherTitle = "Launcher"
private const val LauncherSubtitle = "Nuvio Launcher's own options"

@Composable
internal fun LauncherSettingsContent(
    initialFocusRequester: FocusRequester? = null,
    viewModel: LauncherSettingsViewModel = hiltViewModel(),
    layoutViewModel: LayoutSettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val layoutState by layoutViewModel.uiState.collectAsStateWithLifecycle()
    var showHideApps by rememberSaveable { mutableStateOf(false) }
    var returnToHideAppsRow by remember { mutableStateOf(false) }
    val hideAppsRowFocus = remember { FocusRequester() }

    if (showHideApps) {
        HideAppsPage(
            hiddenPackages = settings.hiddenPackages,
            onToggle = { pkg -> viewModel.setAppHidden(pkg, pkg !in settings.hiddenPackages) },
            onBack = {
                showHideApps = false
                returnToHideAppsRow = true
            }
        )
        return
    }

    LaunchedEffect(returnToHideAppsRow) {
        if (returnToHideAppsRow) {
            hideAppsRowFocus.requestFocusSoon()
            returnToHideAppsRow = false
        }
    }

    LauncherSettingsMainPage(
        settings = settings,
        layoutState = layoutState,
        onToggleHeroCatalog = { key -> layoutViewModel.onEvent(LayoutSettingsEvent.ToggleHeroCatalog(key)) },
        viewModel = viewModel,
        initialFocusRequester = initialFocusRequester,
        hideAppsRowFocus = hideAppsRowFocus,
        onOpenHideApps = { showHideApps = true }
    )
}

@Composable
private fun LauncherSettingsMainPage(
    settings: LauncherSettings,
    layoutState: LayoutSettingsUiState,
    onToggleHeroCatalog: (String) -> Unit,
    viewModel: LauncherSettingsViewModel,
    initialFocusRequester: FocusRequester?,
    hideAppsRowFocus: FocusRequester,
    onOpenHideApps: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    var showResetDialog by remember { mutableStateOf(false) }
    val resetRowFocus = remember { FocusRequester() }
    val catalogs = layoutState.availableCatalogs

    // The title stays put and the sections scroll inside one glass panel below it, as in the mockup.
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        SettingsDetailHeader(title = LauncherTitle, subtitle = LauncherSubtitle)

        SettingsGlassScrollPanel(
            scrollState = scrollState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            // ---- Carousel ----
            SettingsGroupCard(modifier = Modifier.fillMaxWidth(), title = "Carousel") {
                // Title, description and the catalog chips together in one row card.
                LauncherRowCard(
                    title = "Carousel catalogs",
                    subtitle = "Which catalogs the Home carousel takes its titles from. Same list as Nuvio's \"Hero Catalogs\". None ticked: the first Home row."
                ) {
                    if (catalogs.isEmpty()) {
                        Text(
                            text = "No Home catalogs yet",
                            style = SettingsGlassType.RowSubtitle,
                            color = SettingsGlassColors.TextIdleSecondary
                        )
                    } else {
                        val firstCatalogFocus = remember { FocusRequester() }
                        FlowRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .settingsOptionRow(firstCatalogFocus),
                            horizontalArrangement = Arrangement.spacedBy(7.dp),
                            verticalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            catalogs.forEachIndexed { index, catalog ->
                                key(catalog.key) {
                                    SettingsChoiceChip(
                                        label = catalogChipLabel(catalog),
                                        selected = catalog.key in layoutState.heroCatalogKeys,
                                        onClick = { onToggleHeroCatalog(catalog.key) },
                                        multiSelect = true,
                                        modifier = if (index == 0) {
                                            Modifier
                                                .focusRequester(firstCatalogFocus)
                                                .then(initialFocusRequester?.let { Modifier.focusRequester(it) } ?: Modifier)
                                        } else {
                                            Modifier
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
                LauncherChoiceRow(
                    title = "Rotation speed",
                    subtitle = "Time before the carousel moves to the next title",
                    options = LauncherSettings.ROTATION_CHOICES.map { s -> s to (if (s == 0) "Off" else "$s s") },
                    selected = settings.rotationSeconds,
                    onSelect = viewModel::setRotationSeconds,
                    firstOptionModifier = if (catalogs.isEmpty() && initialFocusRequester != null) {
                        Modifier.focusRequester(initialFocusRequester)
                    } else {
                        Modifier
                    }
                )
                SliderSettingsItem(
                    title = "Number of titles",
                    subtitle = "How many titles the carousel cycles through",
                    value = settings.heroCount,
                    valueText = settings.heroCount.toString(),
                    minValue = LauncherSettings.MIN_HERO_COUNT,
                    maxValue = LauncherSettings.MAX_HERO_COUNT,
                    step = 1,
                    onValueChange = viewModel::setHeroCount
                )
            }

            // ---- Clock ----
            SettingsGroupCard(modifier = Modifier.fillMaxWidth(), title = "Clock") {
                SettingsToggleRow(
                    title = "Show time pill",
                    subtitle = "The pill with the time at the top left",
                    checked = settings.showTimePill,
                    onToggle = { viewModel.setShowTimePill(!settings.showTimePill) }
                )
                val uses24Hour = settings.clock24Hour ?: DateFormat.is24HourFormat(context)
                LauncherChoiceRow(
                    title = "Clock format",
                    subtitle = "How the time is written",
                    options = listOf(false to "12-hour", true to "24-hour"),
                    selected = uses24Hour,
                    onSelect = viewModel::setClock24Hour
                )
            }

            // ---- Tiles · Catalog ----
            SettingsGroupCard(modifier = Modifier.fillMaxWidth(), title = "Tiles · Catalog") {
                TilePreviewStrip(
                    sizePercent = settings.catalogTileSizePercent,
                    spacingDp = settings.catalogTileSpacingDp,
                    zoomPercent = settings.catalogFocusZoomPercent
                )
                TileSliders(
                    sizeSubtitle = "Catalog page, folders and search results",
                    spacingSubtitle = "Distance between tiles in a row",
                    zoomSubtitle = "How much the focused tile grows",
                    sizePercent = settings.catalogTileSizePercent,
                    spacingDp = settings.catalogTileSpacingDp,
                    zoomPercent = settings.catalogFocusZoomPercent,
                    onSize = viewModel::setCatalogTileSize,
                    onSpacing = viewModel::setCatalogTileSpacing,
                    onZoom = viewModel::setCatalogFocusZoom
                )
            }

            // ---- Tiles · Applications ----
            SettingsGroupCard(modifier = Modifier.fillMaxWidth(), title = "Tiles · Applications") {
                TilePreviewStrip(
                    sizePercent = settings.appsTileSizePercent,
                    spacingDp = settings.appsTileSpacingDp,
                    zoomPercent = settings.appsFocusZoomPercent
                )
                TileSliders(
                    sizeSubtitle = "App banners on the Applications page",
                    spacingSubtitle = "Distance between app banners",
                    zoomSubtitle = "How much the focused app grows",
                    sizePercent = settings.appsTileSizePercent,
                    spacingDp = settings.appsTileSpacingDp,
                    zoomPercent = settings.appsFocusZoomPercent,
                    onSize = viewModel::setAppsTileSize,
                    onSpacing = viewModel::setAppsTileSpacing,
                    onZoom = viewModel::setAppsFocusZoom
                )
            }

            // ---- More ----
            SettingsGroupCard(modifier = Modifier.fillMaxWidth(), title = "More") {
                val hiddenCount = settings.hiddenPackages.size
                SettingsActionRow(
                    title = "Hide apps",
                    subtitle = "Choose which apps show on the Applications page",
                    value = if (hiddenCount == 0) "All shown" else "$hiddenCount hidden",
                    onClick = onOpenHideApps,
                    modifier = Modifier.focusRequester(hideAppsRowFocus)
                )
                SettingsActionRow(
                    title = "Reset launcher settings",
                    subtitle = "Puts every setting on this page back to its default",
                    value = "Reset",
                    onClick = { showResetDialog = true },
                    modifier = Modifier.focusRequester(resetRowFocus)
                )
            }
        }
    }

    if (showResetDialog) {
        ResetLauncherSettingsDialog(
            onCancel = { showResetDialog = false },
            onReset = {
                viewModel.reset()
                showResetDialog = false
            }
        )
    }
    // Focus goes back to the Reset row once the dialog closes.
    var dialogWasOpen by remember { mutableStateOf(false) }
    LaunchedEffect(showResetDialog) {
        if (showResetDialog) {
            dialogWasOpen = true
        } else if (dialogWasOpen) {
            dialogWasOpen = false
            resetRowFocus.requestFocusSoon()
        }
    }
}

private fun catalogChipLabel(catalog: CatalogInfo): String {
    // The type is added because addons often have two catalogs with the same name ("Popular" for movies and series).
    val type = when (catalog.type.lowercase()) {
        "movie" -> "Movies"
        "series", "tv" -> "Series"
        "" -> ""
        else -> catalog.type.replaceFirstChar { it.uppercase() }
    }
    return listOf(catalog.addonName, catalog.name, type).filter { it.isNotBlank() }.joinToString(" · ")
}

/**
 * A row card that is not itself focusable: title and description, then [content] (chips) 8 below,
 * on the row's white .07 ground, 9 above, 16 on the sides and 11 below, as in the mockup.
 */
@Composable
private fun LauncherRowCard(
    title: String,
    subtitle: String?,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(settingsRowShape())
            .background(SettingsGlassColors.RowIdle)
            .padding(start = 16.dp, end = 16.dp, top = 9.dp, bottom = 11.dp)
    ) {
        Text(
            text = title,
            style = SettingsGlassType.RowTitle,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (!subtitle.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = SettingsGlassType.RowSubtitle,
                color = SettingsGlassColors.TextIdleSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        content()
    }
}

/** Title and description on the left, a one-row choice on the right, moved along with Left/Right. */
@Composable
private fun <T> LauncherChoiceRow(
    title: String,
    subtitle: String?,
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    firstOptionModifier: Modifier = Modifier
) {
    val firstOptionFocus = remember { FocusRequester() }
    // A row card (white .07, 50 high, 8 by 16 inside) with the choice chips 6 apart at its right.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(settingsRowShape())
            .background(SettingsGlassColors.RowIdle)
            .heightIn(min = SettingsGlassColors.RowMinHeight)
            .padding(horizontal = SettingsGlassColors.RowPaddingH, vertical = SettingsGlassColors.RowPaddingV),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = SettingsGlassType.RowTitle,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = SettingsGlassType.RowSubtitle,
                    color = SettingsGlassColors.TextIdleSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Row(
            modifier = Modifier.settingsOptionRow(firstOptionFocus),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            options.forEachIndexed { index, (value, label) ->
                SettingsChoiceChip(
                    label = label,
                    selected = value == selected,
                    onClick = { if (value != selected) onSelect(value) },
                    modifier = if (index == 0) {
                        Modifier.focusRequester(firstOptionFocus).then(firstOptionModifier)
                    } else {
                        Modifier
                    }
                )
            }
        }
    }
}

@Composable
private fun TileSliders(
    sizeSubtitle: String,
    spacingSubtitle: String,
    zoomSubtitle: String,
    sizePercent: Int,
    spacingDp: Int,
    zoomPercent: Int,
    onSize: (Int) -> Unit,
    onSpacing: (Int) -> Unit,
    onZoom: (Int) -> Unit
) {
    SliderSettingsItem(
        title = "Tile size",
        subtitle = sizeSubtitle,
        value = sizePercent,
        valueText = "$sizePercent%",
        minValue = LauncherSettings.MIN_SIZE,
        maxValue = LauncherSettings.MAX_SIZE,
        step = LauncherSettings.SIZE_STEP,
        onValueChange = onSize
    )
    SliderSettingsItem(
        title = "Tile spacing",
        subtitle = spacingSubtitle,
        value = spacingDp,
        valueText = spacingDp.toString(),
        minValue = LauncherSettings.MIN_SPACING,
        maxValue = LauncherSettings.MAX_SPACING,
        step = LauncherSettings.SPACING_STEP,
        onValueChange = onSpacing
    )
    SliderSettingsItem(
        title = "Focus zoom",
        subtitle = zoomSubtitle,
        value = zoomPercent,
        valueText = "$zoomPercent%",
        minValue = LauncherSettings.MIN_ZOOM,
        maxValue = LauncherSettings.MAX_ZOOM,
        step = LauncherSettings.ZOOM_STEP,
        onValueChange = onZoom
    )
}

/** Six landscape tiles at a quarter of their real size, the gap between them, and the second one zoomed as if focused. */
@Composable
private fun TilePreviewStrip(sizePercent: Int, spacingDp: Int, zoomPercent: Int) {
    val factor = 0.25f
    val scale = sizePercent / 100f
    val zoom = zoomPercent / 100f
    val tileW = 224.dp * scale * factor
    val tileH = 126.dp * scale * factor
    val gap = spacingDp.dp * factor
    val shape = RoundedCornerShape(3.dp)
    // Launcher look: the tiles on a dark well (black .25, radius 10, 70 high), 14 in from its left,
    // with a small "Preview" at its bottom right.
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 70.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color.Black.copy(alpha = 0.25f))
    ) {
        Text(
            text = "Preview",
            style = settingsGlassText(8.5.sp),
            color = SettingsGlassColors.TextIdleSecondary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 10.dp, bottom = 6.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp)
                // room for the largest size at the largest zoom, so the rows below do not move
                .height(
                    maxOf(
                        70.dp,
                        126.dp * (LauncherSettings.MAX_SIZE / 100f) * (LauncherSettings.MAX_ZOOM / 100f) * factor + 8.dp
                    )
                )
                .clipToBounds(),
            horizontalArrangement = Arrangement.spacedBy(gap),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(6) { i ->
                val focused = i == 1
                Box(
                    modifier = Modifier
                        .requiredSize(tileW, tileH)
                        .graphicsLayer {
                            if (focused) {
                                scaleX = zoom
                                scaleY = zoom
                            }
                        }
                        .clip(shape)
                        .background(
                            Color.White.copy(alpha = if (focused) 0.55f else 0.16f)
                        )
                )
            }
        }
    }
}

@Composable
private fun ResetLauncherSettingsDialog(onCancel: () -> Unit, onReset: () -> Unit) {
    val cancelFocus = remember { FocusRequester() }
    NuvioDialog(
        onDismiss = onCancel,
        title = "Reset launcher settings?",
        suppressFirstKeyUp = false
    ) {
        // Pills 8 apart at the right: the focused one white with dark text, the other white .18.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SettingsDialogActionButton(
                text = "Cancel",
                onClick = onCancel,
                modifier = Modifier.focusRequester(cancelFocus)
            )
            SettingsDialogActionButton(
                text = "Reset",
                onClick = onReset,
                primary = true
            )
        }
    }
    // Cancel has focus first, so a stray OK changes nothing.
    LaunchedEffect(Unit) { cancelFocus.requestFocusSoon() }
}

/** Settings → Launcher → Hide apps: every launchable app, with its banner, its category and whether it shows. */
@Composable
private fun HideAppsPage(
    hiddenPackages: Set<String>,
    onToggle: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var apps by remember { mutableStateOf<List<LauncherApp>?>(null) }
    LaunchedEffect(Unit) { apps = withContext(Dispatchers.IO) { loadLauncherApps(context) } }
    BackHandler(onBack = onBack)
    val firstRowFocus = remember { FocusRequester() }
    val listState = rememberLazyListState()
    val loaded = apps
    LaunchedEffect(loaded != null) { if (!loaded.isNullOrEmpty()) firstRowFocus.requestFocusSoon() }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // A sub-page: "Launcher ›" above the title.
        CompositionLocalProvider(LocalSettingsBreadcrumb provides "$LauncherTitle ›") {
            SettingsDetailHeader(title = "Hide apps", subtitle = LauncherSubtitle)
        }
        SettingsGroupCard(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            title = "Applications · Show or hide",
            subtitle = "OK switches between Shown and Hidden. Hidden apps disappear from Applications but stay installed; they come back here."
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    loaded == null -> Unit
                    loaded.isEmpty() -> Text(
                        text = "No apps found",
                        style = MaterialTheme.typography.bodyMedium,
                        color = NuvioTheme.colors.TextSecondary,
                        modifier = Modifier.padding(SettingsRowContentInset)
                    )
                    else -> LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 18.dp),
                        verticalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        items(items = loaded, key = { it.pkg }) { app ->
                            HideAppRow(
                                app = app,
                                hidden = app.pkg in hiddenPackages,
                                onClick = { onToggle(app.pkg) },
                                modifier = if (app === loaded.first()) Modifier.focusRequester(firstRowFocus) else Modifier
                            )
                        }
                    }
                }
                SettingsVerticalScrollIndicators(state = listState)
            }
        }
    }
}

@Composable
private fun HideAppRow(
    app: LauncherApp,
    hidden: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged { isFocused = it.isFocused },
        colors = settingsRowColors(),
        border = settingsRowBorder(),
        shape = CardDefaults.shape(settingsRowShape()),
        scale = CardDefaults.scale(focusedScale = 1f, pressedScale = 1f)
    ) {
        // Launcher look: 7 by 14 inside, the 64 by 36 banner (radius 4), the name 12.5 SemiBold and
        // the state 11 at the right.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(width = 64.dp, height = 36.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .graphicsLayer { alpha = if (hidden) 0.45f else 1f },
                contentAlignment = Alignment.Center
            ) {
                val art = app.art
                if (art != null) {
                    Image(
                        bitmap = art,
                        contentDescription = null,
                        contentScale = if (app.isBanner) ContentScale.Crop else ContentScale.Fit,
                        modifier = if (app.isBanner) Modifier.fillMaxSize() else Modifier.size(28.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = app.label,
                style = settingsGlassText(12.5.sp, FontWeight.SemiBold),
                color = settingsTitleColor(isFocused).copy(alpha = if (hidden) 0.45f else 1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "${app.groupName} · ${if (hidden) "Hidden" else "Shown"}",
                style = settingsGlassText(11.sp),
                color = if (isFocused) LauncherStyle.FocusText.copy(alpha = 0.65f) else Color.White.copy(alpha = if (hidden) 0.3f else 0.65f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** Asks for focus over the next few frames, while the target composes. */
private suspend fun FocusRequester.requestFocusSoon() {
    repeat(8) {
        withFrameNanos { }
        if (runCatching { requestFocus() }.getOrDefault(false)) return
    }
}

package com.nuvio.tv.ui.screens.library

import android.view.KeyEvent as AndroidKeyEvent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.nuvio.tv.domain.model.MetaPreview
import com.nuvio.tv.ui.components.MonochromePosterPlaceholder
import com.nuvio.tv.ui.components.WatchedMarker
import com.nuvio.tv.ui.launcher.LauncherStyle
import com.nuvio.tv.ui.util.rememberLongPressKeyTracker

/*
 * Nuvio Launcher: the Library's poster grid and filter pills in the launcher's look (the Home tiles and the
 * sidebar pills). Same tile as Home: zoom via graphicsLayer, soft shadow, 7dp corners, no focus ring, and the
 * title under every tile, white and moved down when focused, soft white otherwise, one line with an ellipsis.
 */

/** Base size of a library tile before the Settings → Launcher size: a 120 x 180 poster or a 224 x 126 landscape card. */
internal fun libraryTileWidth(landscape: Boolean, scale: Float): Dp = (if (landscape) 224.dp else 120.dp) * scale

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
internal fun LibraryLauncherTile(
    item: MetaPreview,
    landscape: Boolean,
    scale: Float,
    zoom: Float,
    isWatched: Boolean,
    focusRequester: FocusRequester?,
    onFocused: () -> Unit,
    onClick: () -> Unit,
    onLongPress: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    var longPressTriggered by remember { mutableStateOf(false) }
    val longPressKeyTracker = rememberLongPressKeyTracker()
    val w = libraryTileWidth(landscape, scale)
    val baseH = if (landscape) 126.dp else 180.dp
    val h = baseH * scale
    // How far the focused title moves down: 10 at the default size and zoom, plus whatever more the zoomed tile grows.
    val titleLift = maxOf(4.dp, 10.dp + (h * (zoom - 1f) - baseH * 0.11f) / 2f)
    val tileScale by animateFloatAsState(if (focused) zoom else 1f, tween(160), label = "libraryZoom")
    val context = LocalContext.current
    val density = LocalDensity.current
    val imageUrl = if (landscape) item.landscapePoster ?: item.background ?: item.poster else item.poster
    val widthPx = remember(density, w) { with(density) { w.roundToPx() }.coerceAtLeast(1) }
    val heightPx = remember(density, h) { with(density) { h.roundToPx() }.coerceAtLeast(1) }
    val revalidationKey = com.nuvio.tv.core.image.rememberImageRevalidationKey(imageUrl)
    val imageModel = remember(imageUrl, widthPx, heightPx, revalidationKey) {
        val builder = ImageRequest.Builder(context)
            .data(imageUrl)
            .crossfade(true)
            .size(width = widthPx, height = heightPx)
            .memoryCacheKey("${imageUrl}_${widthPx}x${heightPx}_v$revalidationKey")
        if (revalidationKey > 0) {
            builder.placeholderMemoryCacheKey("${imageUrl}_${widthPx}x${heightPx}_v${revalidationKey - 1}")
        }
        val fallbackUrl = item.rawPosterUrl
        if (!fallbackUrl.isNullOrBlank() && fallbackUrl != imageUrl) {
            builder.memoryCacheKeyExtras(
                mapOf(com.nuvio.tv.core.image.CustomPosterFallbackInterceptor.FALLBACK_URL_KEY to fallbackUrl)
            )
        }
        builder.build()
    }

    Column(Modifier.width(w)) {
        Box(
            Modifier
                .graphicsLayer { scaleX = tileScale; scaleY = tileScale }
                .shadow(elevation = if (focused) 17.dp else 7.dp, shape = LauncherStyle.Corner, ambientColor = Color.Black, spotColor = Color.Black)
        ) {
            Card(
                onClick = {
                    if (longPressTriggered) longPressTriggered = false else onClick()
                },
                modifier = Modifier
                    .size(w, h)
                    .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
                    .onFocusChanged { state ->
                        focused = state.isFocused
                        if (state.isFocused) onFocused()
                    }
                    .onPreviewKeyEvent { event ->
                        val native = event.nativeKeyEvent
                        if (native.action == AndroidKeyEvent.ACTION_DOWN && native.keyCode == AndroidKeyEvent.KEYCODE_MENU) {
                            longPressTriggered = true
                            onLongPress()
                            return@onPreviewKeyEvent true
                        }
                        if (longPressKeyTracker.handle(native, ::isLibrarySelectKey) {
                                longPressTriggered = true
                                onLongPress()
                            }
                        ) {
                            if (native.action == AndroidKeyEvent.ACTION_UP) longPressTriggered = false
                            return@onPreviewKeyEvent true
                        }
                        if (native.action == AndroidKeyEvent.ACTION_UP && longPressTriggered &&
                            (isLibrarySelectKey(native.keyCode) || native.keyCode == AndroidKeyEvent.KEYCODE_MENU)
                        ) {
                            longPressTriggered = false
                            return@onPreviewKeyEvent true
                        }
                        false
                    },
                shape = CardDefaults.shape(LauncherStyle.Corner),
                scale = CardDefaults.scale(focusedScale = 1f),
                border = CardDefaults.border(border = Border.None, focusedBorder = Border.None),
                colors = CardDefaults.colors(containerColor = Color(0xFF2A2C33), focusedContainerColor = Color(0xFF2A2C33))
            ) {
                Box(Modifier.fillMaxSize()) {
                    if (imageUrl.isNullOrBlank()) {
                        MonochromePosterPlaceholder(contentDescription = item.name)
                    } else {
                        AsyncImage(
                            model = imageModel,
                            contentDescription = item.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    if (isWatched) {
                        WatchedMarker(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(end = 8.dp, top = 8.dp)
                        )
                    }
                }
            }
        }
        Text(
            item.name,
            color = if (focused) Color.White else LauncherStyle.Soft,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            // Same layout height whether focused or not; the focused title only moves down visually.
            modifier = Modifier.padding(top = 8.dp).graphicsLayer { translationY = if (focused) titleLift.toPx() else 0f }
        )
    }
}

private fun isLibrarySelectKey(keyCode: Int): Boolean =
    keyCode == AndroidKeyEvent.KEYCODE_DPAD_CENTER ||
        keyCode == AndroidKeyEvent.KEYCODE_ENTER ||
        keyCode == AndroidKeyEvent.KEYCODE_NUMPAD_ENTER

/**
 * A compact launcher pill: 11.5sp SemiBold, fully rounded. Idle white .12 with soft text, selected white .30 with
 * white text, focused the launcher's white with dark text. [dropdown] adds the small chevron of a menu pill.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
internal fun LibraryPill(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    dropdown: Boolean = false,
    expanded: Boolean = false
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(50)
    val textColor = when {
        focused -> LauncherStyle.FocusText
        selected -> Color.White
        else -> LauncherStyle.Soft
    }
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.onFocusChanged { focused = it.isFocused || it.hasFocus },
        shape = ClickableSurfaceDefaults.shape(shape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (selected) LauncherStyle.PillSelected else LauncherStyle.PillIdle,
            contentColor = textColor,
            focusedContainerColor = LauncherStyle.FocusWhite,
            focusedContentColor = LauncherStyle.FocusText,
            pressedContainerColor = LauncherStyle.FocusWhite,
            pressedContentColor = LauncherStyle.FocusText,
            disabledContainerColor = LauncherStyle.PillIdle,
            disabledContentColor = LauncherStyle.Dim
        ),
        border = ClickableSurfaceDefaults.border(border = Border.None, focusedBorder = Border.None),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f, pressedScale = 1f)
    ) {
        Row(
            modifier = Modifier.padding(start = 13.dp, end = if (dropdown) 9.dp else 13.dp, top = 5.dp, bottom = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                color = if (enabled) textColor else LauncherStyle.Dim,
                fontSize = 11.5.sp,
                lineHeight = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (dropdown) {
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.padding(start = 3.dp).size(14.dp)
                )
            }
        }
    }
}

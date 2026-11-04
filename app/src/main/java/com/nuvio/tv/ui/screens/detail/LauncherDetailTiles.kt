package com.nuvio.tv.ui.screens.detail

import com.nuvio.tv.ui.launcher.launcherShadow

import android.view.KeyEvent as AndroidKeyEvent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
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
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.nuvio.tv.domain.model.MetaPreview
import com.nuvio.tv.ui.components.WatchedMarker
import com.nuvio.tv.ui.launcher.LauncherStyle
import com.nuvio.tv.ui.util.rememberLongPressKeyTracker

/*
 * Nuvio Launcher: the details page's and person page's rows, in the Home rows' style:
 * titles at 16 (white only on the focused row), tiles with 7dp corners, a soft shadow (7, 17 focused),
 * 111% zoom on focus and a 12sp title under every tile that moves down, clear of the zoomed tile.
 */

/** A row title: 16sp bold, white while the row has focus, soft white otherwise. */
@Composable
internal fun LauncherRowTitle(
    text: String,
    highlighted: Boolean,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null
) {
    androidx.compose.foundation.layout.Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            color = if (highlighted) Color.White else LauncherStyle.Soft,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        trailing?.invoke()
    }
}

/**
 * A title tile ([width] x [height]) with the launcher's zoom, shadow and title rules.
 * Landscape tiles prefer the landscape poster / backdrop, portrait tiles the poster.
 */
@Composable
internal fun LauncherMetaTile(
    item: MetaPreview,
    width: Dp,
    height: Dp,
    landscape: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongPress: (() -> Unit)? = null,
    isWatched: Boolean = false,
    focusRequester: FocusRequester? = null,
    upFocusRequester: FocusRequester? = null,
    downFocusRequester: FocusRequester? = null,
    subtitle: String? = null,
    onFocused: () -> Unit = {}
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    var isFocused by remember { mutableStateOf(false) }
    var longPressTriggered by remember { mutableStateOf(false) }
    val longPressKeyTracker = rememberLongPressKeyTracker()
    val scale by animateFloatAsState(
        targetValue = if (isFocused) LauncherStyle.Zoom else 1f,
        animationSpec = tween(160),
        label = "launcherTileZoom"
    )
    val imageUrl = if (landscape) {
        item.landscapePoster ?: item.background ?: item.poster
    } else {
        item.poster ?: item.background
    }
    val widthPx = remember(width, density) { with(density) { width.roundToPx() }.coerceAtLeast(1) }
    val heightPx = remember(height, density) { with(density) { height.roundToPx() }.coerceAtLeast(1) }
    val imageModel = remember(imageUrl, widthPx, heightPx) {
        imageUrl?.takeIf { it.isNotBlank() }?.let {
            ImageRequest.Builder(context)
                .data(it)
                .crossfade(true)
                .size(width = widthPx, height = heightPx)
                .build()
        }
    }

    Column(modifier = modifier.width(width)) {
        Box(
            modifier = Modifier
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .launcherShadow(
                    elevation = if (isFocused) 17.dp else 7.dp,
                    shape = LauncherStyle.Corner,
                    ambientColor = Color.Black,
                    spotColor = Color.Black
                )
        ) {
            Card(
                onClick = {
                    if (longPressTriggered) {
                        longPressTriggered = false
                    } else {
                        onClick()
                    }
                },
                modifier = Modifier
                    .size(width, height)
                    .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
                    .then(
                        if (upFocusRequester != null || downFocusRequester != null) {
                            Modifier.focusProperties {
                                if (upFocusRequester != null) up = upFocusRequester
                                if (downFocusRequester != null) down = downFocusRequester
                            }
                        } else {
                            Modifier
                        }
                    )
                    .onFocusChanged { state ->
                        isFocused = state.isFocused
                        if (state.isFocused) onFocused()
                    }
                    .onPreviewKeyEvent { event ->
                        if (onLongPress == null) return@onPreviewKeyEvent false
                        val native = event.nativeKeyEvent
                        if (native.action == AndroidKeyEvent.ACTION_DOWN && native.keyCode == AndroidKeyEvent.KEYCODE_MENU) {
                            longPressTriggered = true
                            onLongPress()
                            return@onPreviewKeyEvent true
                        }
                        if (longPressKeyTracker.handle(native, ::isLauncherTileSelectKey) {
                                longPressTriggered = true
                                onLongPress()
                            }
                        ) {
                            if (native.action == AndroidKeyEvent.ACTION_UP) {
                                longPressTriggered = false
                            }
                            return@onPreviewKeyEvent true
                        }
                        if (native.action == AndroidKeyEvent.ACTION_UP &&
                            longPressTriggered &&
                            (isLauncherTileSelectKey(native.keyCode) || native.keyCode == AndroidKeyEvent.KEYCODE_MENU)
                        ) {
                            longPressTriggered = false
                            return@onPreviewKeyEvent true
                        }
                        false
                    },
                shape = CardDefaults.shape(LauncherStyle.Corner),
                scale = CardDefaults.scale(focusedScale = 1f),
                border = CardDefaults.border(border = Border.None, focusedBorder = Border.None),
                colors = CardDefaults.colors(
                    containerColor = Color(0xFF2A2C33),
                    focusedContainerColor = Color(0xFF2A2C33)
                )
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (imageModel != null) {
                        AsyncImage(
                            model = imageModel,
                            contentDescription = item.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Text(
                            text = item.name,
                            color = LauncherStyle.Soft,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(8.dp)
                        )
                    }
                    if (isWatched) {
                        WatchedMarker(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(end = 6.dp, top = 6.dp)
                        )
                    }
                }
            }
        }
        LauncherTileCaption(title = item.name, subtitle = subtitle, focused = isFocused)
    }
}

/** The 12sp title (and optional dim subtitle) under a tile; moves down 10dp, visually only, when focused. */
@Composable
internal fun LauncherTileCaption(title: String, subtitle: String?, focused: Boolean) {
    Column(
        modifier = Modifier
            .padding(top = 8.dp)
            .graphicsLayer { translationY = if (focused) 10.dp.toPx() else 0f }
    ) {
        if (title.isNotBlank()) {
            Text(
                text = title,
                color = if (focused) Color.White else LauncherStyle.Soft,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (!subtitle.isNullOrBlank()) {
            Text(
                text = subtitle,
                color = LauncherStyle.Dim,
                fontSize = 10.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

private fun isLauncherTileSelectKey(keyCode: Int): Boolean {
    return keyCode == AndroidKeyEvent.KEYCODE_DPAD_CENTER ||
        keyCode == AndroidKeyEvent.KEYCODE_ENTER ||
        keyCode == AndroidKeyEvent.KEYCODE_NUMPAD_ENTER
}

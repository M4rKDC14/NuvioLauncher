@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.nuvio.tv.ui.screens.stream

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.nuvio.tv.R
import com.nuvio.tv.ui.components.SourceChipItem
import com.nuvio.tv.ui.components.SourceChipStatus
import com.nuvio.tv.ui.components.shimmer
import com.nuvio.tv.ui.launcher.LauncherStyle
import com.nuvio.tv.ui.screens.player.focusChip
import com.nuvio.tv.ui.theme.NuvioTheme
import com.nuvio.tv.ui.util.contentTextDirection
import kotlinx.coroutines.delay as coroutineDelay
import kotlinx.coroutines.launch as coroutineLaunch

/*
 * Nuvio Launcher: the sources page's pills, in the sidebar's style (mockup sources-mockup.html).
 * Soft see-through white pills with 72% white text, the selected filter a little brighter
 * with white text, and the focused one the sidebar's bright white with dark text.
 */
internal val StreamPillIdle = Color.White.copy(alpha = 0.18f)
internal val StreamFocusWhite = LauncherStyle.FocusWhite.copy(alpha = 0.88f)
internal val StreamPillHeight = 27.dp
private val StreamPillShape = RoundedCornerShape(99.dp)

@Composable
internal fun streamPillTextStyle() = MaterialTheme.typography.labelLarge.copy(
    fontSize = 11.5.sp,
    lineHeight = 15.sp,
    fontWeight = FontWeight.Medium
)

/**
 * Same behaviour as [com.nuvio.tv.ui.screens.player.AddonFilterChips] (focus, keys, refresh,
 * selection); only the chips are drawn as launcher pills.
 */
@Composable
internal fun StreamLauncherFilterChips(
    addons: List<String>,
    sourceChips: List<SourceChipItem> = emptyList(),
    selectedAddon: String?,
    isStillFetching: Boolean = false,
    onRefresh: (() -> Unit)? = null,
    onAddonSelected: (String?) -> Unit,
    externalFocusRequesters: List<FocusRequester>? = null,
    externalOrderedNames: List<String>? = null,
    onUpKey: (() -> Unit)? = null,
    onFocusIndexChanged: (Int) -> Unit = {},
    chipListState: LazyListState = rememberLazyListState()
) {
    val isRtl = androidx.compose.ui.platform.LocalLayoutDirection.current == androidx.compose.ui.unit.LayoutDirection.Rtl
    val chipMap = sourceChips.associateBy { it.name }
    val orderedNames = externalOrderedNames ?: remember(addons, sourceChips) {
        buildList {
            addAll(addons)
            sourceChips.forEach { chip -> if (chip.name !in this) add(chip.name) }
        }
    }
    val hasRefresh = onRefresh != null
    val refreshFocusRequester = remember { FocusRequester() }
    val allFocusRequester = remember { FocusRequester() }
    val addonFocusRequesters = remember { mutableMapOf<String, FocusRequester>() }
    val focusRequesters = externalFocusRequesters ?: remember(orderedNames) {
        buildList {
            if (hasRefresh) add(refreshFocusRequester)
            add(allFocusRequester)
            orderedNames.forEach { addon ->
                add(addonFocusRequesters.getOrPut(addon) { FocusRequester() })
            }
        }
    }

    var chipRowHasFocus by remember { mutableStateOf(false) }
    var refreshHasFocus by remember { mutableStateOf(false) }
    var focusedChipIndex by remember { mutableStateOf(
        if (hasRefresh) {
            if (selectedAddon == null) 1 else (orderedNames.indexOf(selectedAddon) + 2).coerceAtLeast(1)
        } else {
            if (selectedAddon == null) 0 else (orderedNames.indexOf(selectedAddon) + 1).coerceAtLeast(0)
        }
    ) }
    LaunchedEffect(selectedAddon, orderedNames) {
        if (refreshHasFocus || focusedChipIndex == 0) return@LaunchedEffect
        val maxIndex = if (hasRefresh) orderedNames.size + 1 else orderedNames.size
        if (focusedChipIndex > maxIndex) {
            focusedChipIndex = maxIndex.coerceAtLeast(if (hasRefresh) 1 else 0)
        }
        val currentAddonAtFocus = if (hasRefresh) {
            if (focusedChipIndex == 1) null else orderedNames.getOrNull(focusedChipIndex - 2)
        } else {
            if (focusedChipIndex == 0) null else orderedNames.getOrNull(focusedChipIndex - 1)
        }
        if (currentAddonAtFocus == selectedAddon) return@LaunchedEffect
        val idx = if (hasRefresh) {
            if (selectedAddon == null) 1 else (orderedNames.indexOf(selectedAddon) + 2).coerceAtLeast(1)
        } else {
            if (selectedAddon == null) 0 else (orderedNames.indexOf(selectedAddon) + 1).coerceAtLeast(0)
        }
        focusedChipIndex = idx.coerceIn(0, maxIndex)
        if (chipRowHasFocus) {
            withFrameNanos {}
            chipListState.focusChip(idx, focusRequesters)
        }
    }
    val scope = rememberCoroutineScope()
    val lastKeyRepeatDispatchRef = remember { java.util.concurrent.atomic.AtomicLong(0L) }
    fun moveFocusTo(targetIndex: Int) {
        focusedChipIndex = targetIndex
        if (hasRefresh && targetIndex == 0) {
            refreshHasFocus = true
            scope.coroutineLaunch {
                withFrameNanos {}
                chipListState.focusChip(0, focusRequesters)
            }
            return
        }

        refreshHasFocus = false
        val selectedFilter = if (hasRefresh) {
            if (targetIndex == 1) null else orderedNames.getOrNull(targetIndex - 2)
        } else {
            if (targetIndex == 0) null else orderedNames.getOrNull(targetIndex - 1)
        }
        onAddonSelected(selectedFilter)
        scope.coroutineLaunch {
            withFrameNanos {}
            chipListState.focusChip(targetIndex, focusRequesters)
        }
    }

    LaunchedEffect(focusedChipIndex) {
        val visibleItems = chipListState.layoutInfo.visibleItemsInfo
        if (focusedChipIndex >= 0 && !chipRowHasFocus &&
            visibleItems.isNotEmpty() && visibleItems.none { it.index == focusedChipIndex }
        ) {
            chipListState.scrollToItem(focusedChipIndex)
        }
    }

    LaunchedEffect(selectedAddon, orderedNames) {
        if (selectedAddon != null && selectedAddon !in orderedNames) {
            val lastIndex = if (hasRefresh) orderedNames.size + 1 else orderedNames.size
            moveFocusTo(lastIndex.coerceAtLeast(if (hasRefresh) 1 else 0))
        }
    }

    LazyRow(
        state = chipListState,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        contentPadding = PaddingValues(vertical = 2.dp),
        modifier = Modifier
            .onFocusChanged { focusState ->
                val hasFocus = focusState.hasFocus
                if (hasFocus && !chipRowHasFocus && isRtl) {
                    scope.coroutineLaunch {
                        withFrameNanos {}
                        chipListState.focusChip(focusedChipIndex, focusRequesters)
                    }
                }
                chipRowHasFocus = hasFocus
            }
            .onKeyEvent { event ->
                if (event.nativeKeyEvent.action != android.view.KeyEvent.ACTION_DOWN) return@onKeyEvent false

                if (event.nativeKeyEvent.repeatCount > 0) {
                    val now = android.os.SystemClock.uptimeMillis()
                    if (now - lastKeyRepeatDispatchRef.get() < 112L) return@onKeyEvent true
                    lastKeyRepeatDispatchRef.set(now)
                }

                if (event.key == Key.DirectionUp && onUpKey != null) {
                    onUpKey()
                    return@onKeyEvent true
                }

                val lastIndex = if (hasRefresh) orderedNames.size + 1 else orderedNames.size
                val currentIdx = focusedChipIndex.coerceIn(0, lastIndex)
                when (event.key) {
                    Key.DirectionLeft -> {
                        if (isRtl) {
                            if (currentIdx < lastIndex) { moveFocusTo(currentIdx + 1); true } else true
                        } else {
                            if (currentIdx > 0) { moveFocusTo(currentIdx - 1); true } else true
                        }
                    }
                    Key.DirectionRight -> {
                        if (isRtl) {
                            if (currentIdx > 0) { moveFocusTo(currentIdx - 1); true } else true
                        } else {
                            if (currentIdx < lastIndex) { moveFocusTo(currentIdx + 1); true } else true
                        }
                    }
                    else -> false
                }
            }
    ) {
        if (onRefresh != null) {
            item {
                StreamRefreshPill(
                    onClick = onRefresh,
                    isLoading = isStillFetching,
                    onFocusChanged = { isFocused ->
                        refreshHasFocus = isFocused
                        if (isFocused) {
                            focusedChipIndex = 0
                            onFocusIndexChanged(0)
                        }
                    },
                    modifier = Modifier
                        .focusRequester(focusRequesters[0])
                        .focusProperties { canFocus = focusedChipIndex == 0 }
                )
            }
        }

        item {
            val isAllSelected = selectedAddon == null && !refreshHasFocus
            val allChipIndex = if (hasRefresh) 1 else 0
            StreamFilterPill(
                name = stringResource(R.string.stream_filter_all),
                isSelected = isAllSelected,
                status = SourceChipStatus.SUCCESS,
                isSelectable = true,
                onClick = { onAddonSelected(null) },
                modifier = Modifier
                    .focusRequester(focusRequesters[allChipIndex])
                    .onFocusChanged {
                        if (it.isFocused) {
                            focusedChipIndex = allChipIndex
                            refreshHasFocus = false
                            onFocusIndexChanged(allChipIndex)
                        }
                    }
            )
        }

        items(orderedNames.size) { i ->
            val addon = orderedNames[i]
            val chipStatus = chipMap[addon]?.status ?: SourceChipStatus.SUCCESS
            val isSelectable = addon in addons && chipStatus == SourceChipStatus.SUCCESS
            val requesterIdx = if (hasRefresh) i + 2 else i + 1
            StreamFilterPill(
                name = addon,
                isSelected = selectedAddon == addon,
                status = chipStatus,
                isSelectable = isSelectable,
                onClick = { if (isSelectable) onAddonSelected(addon) },
                modifier = Modifier
                    .focusRequester(focusRequesters[requesterIdx])
                    .onFocusChanged {
                        if (it.isFocused) {
                            focusedChipIndex = requesterIdx
                            refreshHasFocus = false
                            onFocusIndexChanged(requesterIdx)
                        }
                    }
            )
        }
    }
}

/** A launcher pill: idle white .18, selected white .30, focused bright white with dark text. */
@Composable
private fun StreamPillSurface(
    onClick: () -> Unit,
    isFocused: Boolean,
    isSelected: Boolean,
    isError: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val background = when {
        isFocused -> StreamFocusWhite
        isError -> NuvioTheme.colors.Error.copy(alpha = 0.14f)
        isSelected -> LauncherStyle.PillSelected
        else -> StreamPillIdle
    }
    Card(
        onClick = onClick,
        modifier = modifier,
        colors = CardDefaults.colors(containerColor = background, focusedContainerColor = background),
        border = CardDefaults.border(border = Border.None, focusedBorder = Border.None),
        shape = CardDefaults.shape(shape = StreamPillShape),
        scale = CardDefaults.scale(focusedScale = 1f, pressedScale = 1f)
    ) {
        content()
    }
}

@Composable
private fun StreamFilterPill(
    name: String,
    isSelected: Boolean,
    status: SourceChipStatus,
    isSelectable: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    val isError = status == SourceChipStatus.ERROR
    val isLoading = status == SourceChipStatus.LOADING
    val shakeOffsetPx = remember { Animatable(0f) }
    val alphaPulse = remember { Animatable(1f) }

    LaunchedEffect(isError) {
        shakeOffsetPx.snapTo(0f)
        alphaPulse.snapTo(1f)
        if (!isError) return@LaunchedEffect
        alphaPulse.animateTo(0.9f, tween(durationMillis = 120, easing = FastOutSlowInEasing))
        alphaPulse.animateTo(1f, tween(durationMillis = 160, easing = FastOutSlowInEasing))
        shakeOffsetPx.animateTo(3f, tween(80, easing = FastOutSlowInEasing))
        shakeOffsetPx.animateTo(-3f, tween(110, easing = FastOutSlowInEasing))
        shakeOffsetPx.animateTo(2f, tween(90, easing = FastOutSlowInEasing))
        shakeOffsetPx.animateTo(-2f, tween(90, easing = FastOutSlowInEasing))
        shakeOffsetPx.animateTo(0f, tween(140, easing = FastOutSlowInEasing))
    }

    val textColor = when {
        isFocused -> LauncherStyle.FocusText
        isError -> NuvioTheme.colors.Error
        isSelected -> Color.White
        else -> LauncherStyle.Soft
    }

    StreamPillSurface(
        onClick = { if (isSelectable) onClick() },
        isFocused = isFocused,
        isSelected = isSelected,
        isError = isError,
        modifier = modifier
            .graphicsLayer {
                alpha = alphaPulse.value
                translationX = shakeOffsetPx.value
            }
            .onFocusChanged { isFocused = it.isFocused }
            .semantics { selected = isSelected }
    ) {
        Box(
            modifier = Modifier
                .height(StreamPillHeight)
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = name,
                modifier = Modifier.shimmer(isLoading),
                style = streamPillTextStyle().copy(textDirection = name.contentTextDirection()),
                color = textColor,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun StreamRefreshPill(
    onClick: () -> Unit,
    isLoading: Boolean,
    onFocusChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    val contentColor = if (isFocused) LauncherStyle.FocusText else LauncherStyle.Soft

    val rotationAnimatable = remember { Animatable(0f) }
    var hasCompletedFullRotation by remember { mutableStateOf(false) }
    LaunchedEffect(isLoading) {
        if (isLoading) {
            coroutineDelay(100)
            hasCompletedFullRotation = false
            while (true) {
                val remaining = 360f - rotationAnimatable.value
                rotationAnimatable.animateTo(
                    targetValue = 360f,
                    animationSpec = tween(
                        durationMillis = (remaining / 360f * 1000).toInt(),
                        easing = LinearEasing
                    )
                )
                hasCompletedFullRotation = true
                rotationAnimatable.snapTo(0f)
            }
        } else {
            if (hasCompletedFullRotation && rotationAnimatable.value > 0f) {
                val remaining = 360f - rotationAnimatable.value
                rotationAnimatable.animateTo(
                    targetValue = 360f,
                    animationSpec = tween(
                        durationMillis = (remaining / 360f * 1000).toInt(),
                        easing = LinearEasing
                    )
                )
            }
            rotationAnimatable.snapTo(0f)
            hasCompletedFullRotation = false
        }
    }

    StreamPillSurface(
        onClick = onClick,
        isFocused = isFocused,
        isSelected = false,
        isError = false,
        modifier = modifier.onFocusChanged {
            isFocused = it.isFocused
            onFocusChanged(it.isFocused)
        }
    ) {
        Box(
            modifier = Modifier
                .width(30.dp)
                .height(StreamPillHeight),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Refresh,
                contentDescription = stringResource(R.string.cd_refresh),
                modifier = Modifier
                    .size(15.dp)
                    .graphicsLayer { rotationZ = rotationAnimatable.value },
                tint = contentColor
            )
        }
    }
}

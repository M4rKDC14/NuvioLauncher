@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.nuvio.tv.ui.screens.settings

import com.nuvio.tv.ui.theme.NuvioTheme
import com.nuvio.tv.ui.util.keepLtr

import android.view.KeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.nuvio.tv.R
import com.nuvio.tv.ui.launcher.LauncherStyle
import kotlin.math.roundToInt

@Composable
internal fun SliderSettingsItem(
    title: String,
    value: Int,
    valueText: String,
    minValue: Int,
    maxValue: Int,
    step: Int,
    onValueChange: (Int) -> Unit,
    subtitle: String? = null,
    onFocused: () -> Unit = {},
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val span = (maxValue - minValue).toFloat()
    val progress = if (span > 0f) (value - minValue).toFloat() / span else 0f

    SliderSettingsItemLayout(
        title = title,
        valueText = valueText,
        subtitle = subtitle,
        enabled = enabled,
        progressFraction = progress,
        sliderValue = value.toFloat(),
        sliderRange = minValue.toFloat()..maxValue.toFloat(),
        onSliderProgressChange = { target ->
            val newValue = target.roundToInt().coerceIn(minValue, maxValue)
            if (newValue != value) onValueChange(newValue)
        },
        onDecrease = {
            val newValue = (value - step).coerceAtLeast(minValue)
            if (newValue != value) onValueChange(newValue)
        },
        onIncrease = {
            val newValue = (value + step).coerceAtMost(maxValue)
            if (newValue != value) onValueChange(newValue)
        },
        onFocused = onFocused,
        modifier = modifier
    )
}

@Composable
internal fun SliderSettingsItem(
    title: String,
    values: List<Int>,
    selected: Int,
    valueText: String,
    onValueChange: (Int) -> Unit,
    subtitle: String? = null,
    onFocused: () -> Unit = {},
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    require(values.isNotEmpty()) { "SliderSettingsItem.values must not be empty" }

    val index = values.indexOf(selected).coerceAtLeast(0)
    val lastIndex = values.lastIndex
    val progress = if (lastIndex > 0) index.toFloat() / lastIndex.toFloat() else 0f

    SliderSettingsItemLayout(
        title = title,
        valueText = valueText,
        subtitle = subtitle,
        enabled = enabled,
        progressFraction = progress,
        sliderValue = index.toFloat(),
        sliderRange = 0f..lastIndex.toFloat(),
        onSliderProgressChange = { target ->
            val newValue = values[target.roundToInt().coerceIn(0, lastIndex)]
            if (newValue != selected) onValueChange(newValue)
        },
        onDecrease = {
            val newValue = values[(index - 1).coerceAtLeast(0)]
            if (newValue != selected) onValueChange(newValue)
        },
        onIncrease = {
            val newValue = values[(index + 1).coerceAtMost(lastIndex)]
            if (newValue != selected) onValueChange(newValue)
        },
        onFocused = onFocused,
        modifier = modifier
    )
}

@Composable
private fun SliderSettingsItemLayout(
    title: String,
    valueText: String,
    subtitle: String?,
    enabled: Boolean,
    progressFraction: Float,
    sliderValue: Float,
    sliderRange: ClosedFloatingPointRange<Float>,
    onSliderProgressChange: (Float) -> Unit,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    onFocused: () -> Unit,
    modifier: Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    val contentAlpha = if (enabled) 1f else 0.4f
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val shape = settingsTallRowShape()
    val glass = isSettingsGlass()
    // Launcher look: white value and bar on the glass, dark ones on the focused white pill.
    val accentColor = when {
        !glass -> NuvioTheme.colors.Primary
        isFocused -> LauncherStyle.FocusText
        else -> Color.White
    }
    val trackColor = when {
        !glass -> NuvioTheme.colors.BackgroundElevated
        isFocused -> LauncherStyle.FocusText.copy(alpha = 0.15f)
        else -> Color.White.copy(alpha = 0.18f)
    }

    Card(
        onClick = { },
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged { state ->
                val nowFocused = state.isFocused
                if (isFocused != nowFocused) {
                    isFocused = nowFocused
                    if (nowFocused) onFocused()
                }
            }
            .onKeyEvent { event ->
                if (!enabled) return@onKeyEvent false
                if (event.nativeKeyEvent.action != KeyEvent.ACTION_DOWN) return@onKeyEvent false
                when (event.nativeKeyEvent.keyCode) {
                    KeyEvent.KEYCODE_DPAD_LEFT -> {
                        if (isRtl) onIncrease() else onDecrease()
                        true
                    }
                    KeyEvent.KEYCODE_DPAD_RIGHT -> {
                        if (isRtl) onDecrease() else onIncrease()
                        true
                    }
                    else -> false
                }
            }
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(sliderValue, sliderRange)
                setProgress { target ->
                    onSliderProgressChange(target)
                    true
                }
            },
        colors = settingsRowColors(),
        border = settingsRowBorder(contentAlpha = if (enabled) 1f else 0.3f, shape = shape),
        shape = CardDefaults.shape(shape = shape),
        scale = CardDefaults.scale(focusedScale = 1f, pressedScale = 1f)
    ) {
        if (glass) {
            GlassSliderContent(
                title = title,
                subtitle = subtitle,
                valueText = valueText,
                rowFocused = isFocused,
                enabled = enabled,
                progressFraction = progressFraction,
                onDecrease = onDecrease,
                onIncrease = onIncrease,
                onFocused = onFocused
            )
            return@Card
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = SettingsRowContentInset, vertical = NuvioTheme.spacing.md)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge,
                        color = settingsTitleColor(isFocused).copy(alpha = contentAlpha),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (subtitle != null) {
                        Spacer(modifier = Modifier.height(NuvioTheme.spacing.xxs))
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = settingsSecondaryColor(isFocused).copy(alpha = contentAlpha),
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(NuvioTheme.spacing.md))

                Text(
                    text = valueText.keepLtr(),
                    style = MaterialTheme.typography.bodyLarge,
                    color = accentColor.copy(alpha = contentAlpha)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(NuvioTheme.spacing.md),
                modifier = Modifier.fillMaxWidth()
            ) {
                SliderStepButton(
                    icon = Icons.Default.Remove,
                    contentDescription = stringResource(R.string.cd_decrease),
                    rowFocused = isFocused,
                    enabled = enabled,
                    onClick = onDecrease,
                    onFocused = onFocused
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(NuvioTheme.spacing.sm)
                        .clip(RoundedCornerShape(NuvioTheme.radii.xs))
                        .background(trackColor)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progressFraction.coerceIn(0f, 1f))
                            .height(NuvioTheme.spacing.sm)
                            .clip(RoundedCornerShape(NuvioTheme.radii.xs))
                            .background(accentColor.copy(alpha = contentAlpha))
                    )
                }

                SliderStepButton(
                    icon = Icons.Default.Add,
                    contentDescription = stringResource(R.string.cd_increase),
                    rowFocused = isFocused,
                    enabled = enabled,
                    onClick = onIncrease,
                    onFocused = onFocused
                )
            }
        }
    }
}

/**
 * The launcher look of a slider row: 9 above, 16 on the sides, 11 below; title and subtitle with the
 * value (13 SemiBold) at the right; 8 below, a 20 "−" button, the 4 tall track with its 12 knob and a
 * 20 "+" button, 10 apart. White on the glass, dark on the focused white row.
 */
@Composable
private fun GlassSliderContent(
    title: String,
    subtitle: String?,
    valueText: String,
    rowFocused: Boolean,
    enabled: Boolean,
    progressFraction: Float,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    onFocused: () -> Unit
) {
    val contentAlpha = if (enabled) 1f else 0.4f
    val ink = if (rowFocused) LauncherStyle.FocusText else Color.White
    val trackColor = if (rowFocused) LauncherStyle.FocusText.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.18f)
    val fraction = progressFraction.coerceIn(0f, 1f)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 9.dp, bottom = 11.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = SettingsGlassType.RowTitle,
                    color = settingsTitleColor(rowFocused).copy(alpha = contentAlpha),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = SettingsGlassType.RowSubtitle,
                        color = settingsSecondaryColor(rowFocused).copy(alpha = contentAlpha),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = valueText.keepLtr(),
                style = SettingsGlassType.SliderValue,
                color = ink.copy(alpha = contentAlpha),
                maxLines = 1
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            SliderStepButton(
                icon = Icons.Default.Remove,
                contentDescription = stringResource(R.string.cd_decrease),
                rowFocused = rowFocused,
                enabled = enabled,
                onClick = onDecrease,
                onFocused = onFocused
            )
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .height(12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                val trackWidth = maxWidth
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(trackColor)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(ink.copy(alpha = contentAlpha))
                    )
                }
                // The knob is centred on the end of the fill, as in the mockup (white, with a small shadow).
                Box(
                    modifier = Modifier
                        .offset(x = trackWidth * fraction - 6.dp)
                        .size(12.dp)
                        .shadow(1.5.dp, CircleShape)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = contentAlpha))
                )
            }
            SliderStepButton(
                icon = Icons.Default.Add,
                contentDescription = stringResource(R.string.cd_increase),
                rowFocused = rowFocused,
                enabled = enabled,
                onClick = onIncrease,
                onFocused = onFocused
            )
        }
    }
}

@Composable
private fun SliderStepButton(
    icon: ImageVector,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
    onFocused: () -> Unit,
    rowFocused: Boolean = false
) {
    var isFocused by remember { mutableStateOf(false) }
    val contentAlpha = if (enabled) 1f else 0.4f
    val flat = isFlatSettingsStyle()
    val glass = isSettingsGlass()

    Card(
        onClick = { if (enabled) onClick() },
        modifier = Modifier.onFocusChanged { state ->
            val nowFocused = state.isFocused
            if (isFocused != nowFocused) {
                isFocused = nowFocused
                if (nowFocused) onFocused()
            }
        },
        colors = if (glass) {
            // On the focused white row the idle button is a faint dark disc (22,23,27 at .12) instead
            // of the white .14 one.
            CardDefaults.colors(
                containerColor = if (rowFocused) LauncherStyle.FocusText.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.14f),
                focusedContainerColor = LauncherStyle.FocusWhite
            )
        } else CardDefaults.colors(
            containerColor = NuvioTheme.colors.Background,
            focusedContainerColor = if (flat) settingsFocusFillColor() else NuvioTheme.colors.Background
        ),
        border = if (flat || glass) {
            CardDefaults.border(border = Border.None, focusedBorder = Border.None)
        } else {
            CardDefaults.border(
                focusedBorder = Border(
                    border = NuvioTheme.focusRing.border(NuvioTheme.spacing.xxs),
                    shape = CircleShape
                )
            )
        },
        shape = CardDefaults.shape(shape = CircleShape),
        scale = CardDefaults.scale(focusedScale = 1.1f)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            // Launcher look: a 20 circle with a 12 glyph.
            modifier = Modifier.size(if (glass) 20.dp else 38.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = (when {
                    glass && (isFocused || rowFocused) -> LauncherStyle.FocusText
                    glass -> Color.White
                    isFocused -> NuvioTheme.colors.OnPrimary
                    else -> NuvioTheme.colors.TextPrimary
                }).copy(alpha = contentAlpha),
                modifier = Modifier.size(if (glass) 12.dp else 20.dp)
            )
        }
    }
}

@Composable
internal fun ColorSettingsItem(
    title: String,
    currentColor: Color,
    showTransparent: Boolean = false,
    onClick: () -> Unit,
    onFocused: () -> Unit = {},
    enabled: Boolean = true
) {
    var isFocused by remember { mutableStateOf(false) }
    val contentAlpha = if (enabled) 1f else 0.4f
    val glass = isSettingsGlass()

    Card(
        onClick = { if (enabled) onClick() },
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { state ->
                val nowFocused = state.isFocused
                if (isFocused != nowFocused) {
                    isFocused = nowFocused
                    if (nowFocused) onFocused()
                }
            },
        colors = settingsRowColors(),
        border = settingsRowBorder(contentAlpha = if (enabled) 1f else 0.3f),
        shape = CardDefaults.shape(shape = settingsRowShape()),
        scale = CardDefaults.scale(focusedScale = 1f, pressedScale = 1f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = if (glass) SettingsGlassColors.RowMinHeight else 62.dp)
                .padding(
                    horizontal = if (glass) SettingsGlassColors.RowPaddingH else SettingsRowContentInset,
                    vertical = if (glass) SettingsGlassColors.RowPaddingV else NuvioTheme.spacing.md
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = if (glass) SettingsGlassType.RowTitle else MaterialTheme.typography.bodyLarge,
                color = settingsTitleColor(isFocused).copy(alpha = contentAlpha),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(NuvioTheme.spacing.md))

            if (showTransparent || currentColor.alpha == 0f) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color.Gray)
                        .border(NuvioTheme.spacing.xxs, NuvioTheme.colors.Border, CircleShape)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(brush = Brush.linearGradient(colors = listOf(Color.White, Color.Gray, Color.White)))
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(currentColor)
                        .border(NuvioTheme.spacing.xxs, NuvioTheme.colors.Border, CircleShape)
                )
            }
        }
    }
}

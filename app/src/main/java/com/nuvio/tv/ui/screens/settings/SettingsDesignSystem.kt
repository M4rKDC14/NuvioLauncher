@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.nuvio.tv.ui.screens.settings

import com.nuvio.tv.ui.theme.NuvioTheme

import androidx.annotation.RawRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.text.TextStyle
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.ui.res.stringResource
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Card
import androidx.tv.material3.CardBorder
import androidx.tv.material3.CardColors
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.nuvio.tv.R
import com.nuvio.tv.domain.model.SettingsUiStyle
import com.nuvio.tv.ui.components.FocusMarqueeText
import com.nuvio.tv.ui.components.BrandWordmark
import com.nuvio.tv.ui.util.contentTextDirection
import com.nuvio.tv.ui.components.NuvioDialog
import com.nuvio.tv.ui.screens.detail.requestFocusAfterFrames
import com.nuvio.tv.ui.theme.NuvioComponents
import com.nuvio.tv.ui.theme.NuvioRadii
import com.nuvio.tv.ui.util.rememberRawSvgPainter
import com.nuvio.tv.ui.launcher.LauncherGlass
import com.nuvio.tv.ui.launcher.LauncherStyle
import dev.chrisbanes.haze.HazeState

internal val SettingsContainerRadius = NuvioComponents.tokens.settings.containerRadius
internal val SettingsPillRadius = NuvioRadii.tokens.full
internal val SettingsSecondaryCardRadius = NuvioComponents.tokens.settings.secondaryCardRadius
internal val SettingsRailItemHeight = NuvioComponents.tokens.settings.railItemHeight

internal val SettingsZenRowShape = RoundedCornerShape(12.dp)
internal val SettingsHorizonRowShape = RoundedCornerShape(10.dp)
internal val SettingsHorizonGroupShape = RoundedCornerShape(16.dp)

@Composable
@androidx.compose.runtime.ReadOnlyComposable
internal fun settingsGroupShape(): RoundedCornerShape =
    if (LocalSettingsGlass.current) {
        SettingsGlassColors.GroupShape
    } else if (isFlatSettingsStyle()) {
        SettingsHorizonGroupShape
    } else {
        RoundedCornerShape(SettingsSecondaryCardRadius)
    }

/**
 * Marks a row that holds several options side by side. Without it, moving into the row uses a
 * geometric search and lands on whichever option sits nearest the full width row above, which is
 * the middle one. With it, coming back to the row takes the option last used, and anything else
 * takes [fallbackOption], which callers point at the first option.
 *
 * The fallback has to be named. Left at the default it hands the choice back to the same geometric
 * search, which puts focus in the middle again.
 */
internal fun Modifier.settingsOptionRow(fallbackOption: FocusRequester): Modifier = this
    .focusRestorer(fallbackOption)
    .focusGroup()

@Composable
@androidx.compose.runtime.ReadOnlyComposable
internal fun isFlatSettingsStyle(): Boolean = NuvioTheme.settingsUiStyle != SettingsUiStyle.CLASSIC

@Composable
@androidx.compose.runtime.ReadOnlyComposable
internal fun settingsFocusFillColor(): Color = NuvioTheme.colors.TextPrimary.copy(alpha = 0.1f)

@Composable
@androidx.compose.runtime.ReadOnlyComposable
internal fun settingsHeaderInset(): Dp = when (NuvioTheme.settingsUiStyle) {
    SettingsUiStyle.CLASSIC -> 0.dp
    SettingsUiStyle.ZEN -> NuvioTheme.spacing.md
    SettingsUiStyle.HORIZON -> NuvioTheme.spacing.xs
}

@Composable
@androidx.compose.runtime.ReadOnlyComposable
internal fun settingsRowShape(): RoundedCornerShape = if (LocalSettingsGlass.current) SettingsGlassColors.RowShape else when (NuvioTheme.settingsUiStyle) {
    SettingsUiStyle.CLASSIC -> RoundedCornerShape(SettingsPillRadius)
    SettingsUiStyle.ZEN -> SettingsZenRowShape
    SettingsUiStyle.HORIZON -> SettingsHorizonRowShape
}

@Composable
@androidx.compose.runtime.ReadOnlyComposable
internal fun settingsTallRowShape(): RoundedCornerShape =
    if (isFlatSettingsStyle() || LocalSettingsGlass.current) settingsRowShape() else RoundedCornerShape(SettingsSecondaryCardRadius)

@Composable
internal fun settingsRowColors(): CardColors = if (isSettingsGlass()) {
    settingsGlassPillColors()
} else if (isFlatSettingsStyle()) {
    CardDefaults.colors(
        containerColor = Color.Transparent,
        focusedContainerColor = settingsFocusFillColor()
    )
} else {
    CardDefaults.colors(
        containerColor = NuvioTheme.colors.Background,
        focusedContainerColor = NuvioTheme.colors.Background
    )
}

@Composable
internal fun settingsRowBorder(
    contentAlpha: Float = 1f,
    shape: RoundedCornerShape = settingsRowShape()
): CardBorder = if (isFlatSettingsStyle() || isSettingsGlass()) {
    CardDefaults.border(border = Border.None, focusedBorder = Border.None)
} else {
    CardDefaults.border(
        focusedBorder = Border(
            border = NuvioTheme.focusRing.border(NuvioTheme.spacing.xxs, alpha = contentAlpha),
            shape = shape
        )
    )
}

/**
 * Nuvio Launcher: true while drawing inside the Settings screen, which sits over the blurred
 * picture of the last focused title. Rows become see-through, the focused row is the launcher's
 * white pill with dark text, and the main card is the launcher glass. Off everywhere else, so
 * other screens that reuse these components keep Nuvio's own look.
 */
internal val LocalSettingsGlass = staticCompositionLocalOf { false }

/** The blurred backdrop's haze source, for the glass cards. Null below Android 12 (no blur). */
internal val LocalSettingsHazeState = staticCompositionLocalOf<HazeState?> { null }

/** True inside a glass card, so a card nested in it draws as a plain group instead of more glass. */
private val LocalSettingsInGlassCard = staticCompositionLocalOf { false }

internal object SettingsGlassColors {
    val RowIdle = Color(0x12FFFFFF)              // white .07
    val RailSelected = Color(0x2EFFFFFF)         // white .18
    val RailFocused = Color(0x47FFFFFF)          // white .28
    val TextIdleSecondary = Color(0x99FFFFFF)    // white .6
    val TextIdleTertiary = Color(0x80FFFFFF)     // white .5
    val Divider = Color(0x1FFFFFFF)              // white .12
    val TrackOn = Color(0xFFE8E8EA)
    val TrackOff = Color(0x2EFFFFFF)
    val KnobOn = Color(0xFF111111)
    val KnobOff = Color(0xFF8B8D94)             // grey knob of an off switch
    val GroupShape = RoundedCornerShape(16.dp)
    val RowShape = RoundedCornerShape(12.dp)
    val RailShape = RoundedCornerShape(8.dp)

    // Sizes of the approved launcher Settings mockup (960x540 dp screen).
    val RailItemHeight = 30.dp   // 30 high, 34 apart (2 above and below)
    val RowMinHeight = 50.dp
    val RowPaddingH = 16.dp
    val RowPaddingV = 8.dp
    val RowGap = 8.dp
    val PanelPadding = 14.dp
    val SectionLabelTop = 12.dp
}

/**
 * Inside a scrolling glass panel: 14 on the sides and bottom, 2 on top because every section label
 * inside the panel brings its own 12 above it (so the first label sits 14 below the panel's edge).
 */
internal val SettingsGlassPanelContentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 2.dp, bottom = 14.dp)

/** The page's breadcrumb ("Integrations ›") shown above a sub-page's title on the launcher glass. */
internal val LocalSettingsBreadcrumb = staticCompositionLocalOf<String?> { null }

/** Text of the launcher glass at the mockup's exact sizes, on the app's font, with no extra letter spacing. */
@Composable
@androidx.compose.runtime.ReadOnlyComposable
internal fun settingsGlassText(
    size: TextUnit,
    weight: FontWeight = FontWeight.Normal,
    lineHeight: Float = 1.3f,
    letterSpacing: TextUnit = 0.sp
): TextStyle = MaterialTheme.typography.bodyMedium.copy(
    fontSize = size,
    fontWeight = weight,
    lineHeight = size * lineHeight,
    letterSpacing = letterSpacing
)

internal object SettingsGlassType {
    val RowTitle: TextStyle @Composable @androidx.compose.runtime.ReadOnlyComposable get() = settingsGlassText(13.sp, FontWeight.SemiBold)
    val RowSubtitle: TextStyle @Composable @androidx.compose.runtime.ReadOnlyComposable get() = settingsGlassText(10.5.sp, lineHeight = 1.35f)
    val Value: TextStyle @Composable @androidx.compose.runtime.ReadOnlyComposable get() = settingsGlassText(12.sp)
    val SectionLabel: TextStyle @Composable @androidx.compose.runtime.ReadOnlyComposable get() =
        settingsGlassText(10.5.sp, FontWeight.Bold, lineHeight = 1.25f, letterSpacing = 0.08.em)
    val Chip: TextStyle @Composable @androidx.compose.runtime.ReadOnlyComposable get() = settingsGlassText(11.sp, FontWeight.SemiBold)
    val SliderValue: TextStyle @Composable @androidx.compose.runtime.ReadOnlyComposable get() = settingsGlassText(13.sp, FontWeight.SemiBold)
    val PageTitle: TextStyle @Composable @androidx.compose.runtime.ReadOnlyComposable get() = settingsGlassText(20.sp, FontWeight.SemiBold)
    val PageSubtitle: TextStyle @Composable @androidx.compose.runtime.ReadOnlyComposable get() = settingsGlassText(11.5.sp)
    val Breadcrumb: TextStyle @Composable @androidx.compose.runtime.ReadOnlyComposable get() = settingsGlassText(11.sp)
    val DialogTitle: TextStyle @Composable @androidx.compose.runtime.ReadOnlyComposable get() = settingsGlassText(18.sp, FontWeight.Bold)
    val DialogButton: TextStyle @Composable @androidx.compose.runtime.ReadOnlyComposable get() = settingsGlassText(12.sp, FontWeight.SemiBold)
    @Composable
    @androidx.compose.runtime.ReadOnlyComposable
    fun rail(selected: Boolean): TextStyle = settingsGlassText(11.7.sp, if (selected) FontWeight.SemiBold else FontWeight.Medium)
}

@Composable
@androidx.compose.runtime.ReadOnlyComposable
internal fun isSettingsGlass(): Boolean = LocalSettingsGlass.current

/** Title colour of a row: white, or the launcher's dark text on the focused white pill. */
@Composable
@androidx.compose.runtime.ReadOnlyComposable
internal fun settingsTitleColor(focused: Boolean): Color = when {
    !LocalSettingsGlass.current -> NuvioTheme.colors.TextPrimary
    focused -> LauncherStyle.FocusText
    else -> Color.White
}

@Composable
@androidx.compose.runtime.ReadOnlyComposable
internal fun settingsSecondaryColor(focused: Boolean): Color = when {
    !LocalSettingsGlass.current -> NuvioTheme.colors.TextSecondary
    focused -> LauncherStyle.FocusText.copy(alpha = 0.65f)
    else -> SettingsGlassColors.TextIdleSecondary
}

@Composable
@androidx.compose.runtime.ReadOnlyComposable
internal fun settingsTertiaryColor(focused: Boolean): Color = when {
    !LocalSettingsGlass.current -> NuvioTheme.colors.TextTertiary
    focused -> LauncherStyle.FocusText.copy(alpha = 0.5f)
    else -> SettingsGlassColors.TextIdleTertiary
}

/** Card colours for a launcher pill (step buttons, chips, reset): white .07, white when focused. */
@Composable
internal fun settingsGlassPillColors(selected: Boolean = false): CardColors = CardDefaults.colors(
    containerColor = if (selected) SettingsGlassColors.RailSelected else SettingsGlassColors.RowIdle,
    contentColor = Color.White,
    focusedContainerColor = LauncherStyle.FocusWhite,
    focusedContentColor = LauncherStyle.FocusText
)

internal object SettingsTestTags {
    const val ROW_ICON = "settings_row_icon"
}

@Composable
private fun SettingsRowLeadingIcon(
    icon: ImageVector,
    enabled: Boolean,
    focused: Boolean
) {
    val contentAlpha = if (enabled) 1f else 0.4f
    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = (if (isSettingsGlass()) {
            settingsTitleColor(focused && enabled)
        } else if (focused && enabled) NuvioTheme.colors.TextPrimary else NuvioTheme.colors.TextSecondary)
            .copy(alpha = contentAlpha),
        modifier = Modifier
            .size(NuvioTheme.spacing.xl)
            .testTag(SettingsTestTags.ROW_ICON)
    )
}

internal data class SettingsPickerOption<T>(
    val value: T,
    val title: String,
    val description: String? = null,
    val trailing: String? = null,
    val titleFontFamily: FontFamily? = null
)

@Composable
internal fun SettingsStandaloneScaffold(
    title: String,
    subtitle: String,
    classicContainer: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = NuvioTheme.spacing.xxl, vertical = NuvioTheme.spacing.xl)
    ) {
        SettingsWorkspaceSurface(
            modifier = Modifier
                .fillMaxSize(),
            classicContainer = classicContainer
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(NuvioTheme.spacing.lg)
            ) {
                content()
            }
        }
    }
}

@Composable
internal fun SettingsBrandPanel(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    showBuiltInHeader: Boolean = true
) {
    val titleColor = if (showBuiltInHeader) NuvioTheme.colors.TextPrimary else Color.Transparent
    val subtitleColor = if (showBuiltInHeader) NuvioTheme.colors.TextSecondary else Color.Transparent

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(SettingsContainerRadius))
            .background(NuvioTheme.colors.BackgroundElevated)
            .border(
                width = NuvioTheme.spacing.hairline,
                color = NuvioTheme.colors.Border,
                shape = RoundedCornerShape(SettingsContainerRadius)
            )
            .padding(26.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(NuvioTheme.colors.BackgroundCard),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = titleColor
                )
            }
            Spacer(modifier = Modifier.width(NuvioTheme.spacing.md))
            Text(
                text = stringResource(R.string.nav_settings),
                style = MaterialTheme.typography.titleLarge,
                color = titleColor
            )
        }

        Spacer(modifier = Modifier.height(26.dp))

        BrandWordmark(
            contentDescription = stringResource(R.string.cd_nuvio_logo),
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .height(72.dp),
            contentScale = ContentScale.Fit
        )

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = titleColor,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(NuvioTheme.spacing.sm))

        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = subtitleColor,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = stringResource(R.string.settings_rounded_ui),
            style = MaterialTheme.typography.labelMedium,
            letterSpacing = 1.2.sp,
            color = subtitleColor
        )
    }
}

@Composable
internal fun SettingsWorkspaceSurface(
    modifier: Modifier = Modifier,
    classicContainer: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    if (isFlatSettingsStyle() || !classicContainer || isSettingsGlass()) {
        Box(
            modifier = modifier.padding(horizontal = NuvioTheme.spacing.md, vertical = NuvioTheme.spacing.sm),
            content = content
        )
    } else {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(SettingsContainerRadius))
                .background(NuvioTheme.colors.BackgroundElevated)
                .border(
                    width = NuvioTheme.spacing.hairline,
                    color = NuvioTheme.colors.Border,
                    shape = RoundedCornerShape(SettingsContainerRadius)
                )
                .padding(20.dp),
            content = content
        )
    }
}

@Composable
internal fun SettingsRailButton(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    onFocused: () -> Unit = {},
    icon: ImageVector? = null,
    rawIconRes: Int? = null,
    suppressSelectedVisual: Boolean = false
) {
    var isFocused by remember { mutableStateOf(false) }
    val glass = isSettingsGlass()
    // The launcher look has no accent bar or chevron, like the flat style.
    val zen = isFlatSettingsStyle() || glass
    val railShape = when {
        glass -> SettingsGlassColors.RailShape
        zen -> SettingsZenRowShape
        else -> RoundedCornerShape(SettingsPillRadius)
    }
    val appliedModifier = if (focusRequester != null) {
        modifier.focusRequester(focusRequester)
    } else {
        modifier
    }
    val showSelected = isSelected && !suppressSelectedVisual
    val railContentColor = when {
        glass && (isFocused || isSelected) -> Color.White
        glass -> LauncherStyle.Soft
        isSelected || isFocused -> NuvioTheme.colors.TextPrimary
        else -> NuvioTheme.colors.TextSecondary
    }

    Card(
        onClick = onClick,
        modifier = appliedModifier
            .padding(top = NuvioTheme.spacing.xxs, bottom = NuvioTheme.spacing.xxs)
            .fillMaxWidth()
            .then(
                // Launcher look: exactly 52 tall, 2 above and below, like the mockup's rail.
                if (glass) Modifier.height(SettingsGlassColors.RailItemHeight)
                else Modifier.heightIn(min = SettingsRailItemHeight)
            )
            .onFocusChanged { state ->
                val nowFocused = state.isFocused
                if (isFocused != nowFocused) {
                    isFocused = nowFocused
                    if (nowFocused) onFocused()
                }
            }
            .semantics {
                contentDescription = title
                role = Role.Tab
                this.selected = isSelected
            },
        colors = if (glass) {
            // Straight on the picture: idle items have no fill, the selected one a white .18 pill.
            CardDefaults.colors(
                containerColor = if (showSelected) SettingsGlassColors.RailSelected else Color.Transparent,
                contentColor = Color.White,
                // Mockup: the rail item is a see-through white pill with white text, never solid white.
                // Focused is a little brighter than selected so the moving focus stays visible.
                focusedContainerColor = SettingsGlassColors.RailFocused,
                focusedContentColor = Color.White
            )
        } else CardDefaults.colors(
            containerColor = when {
                zen -> Color.Transparent
                showSelected -> NuvioTheme.colors.BackgroundCard
                else -> NuvioTheme.colors.Background
            },
            focusedContainerColor = if (zen) settingsFocusFillColor() else NuvioTheme.colors.BackgroundCard
        ),
        border = if (zen) {
            CardDefaults.border(border = Border.None, focusedBorder = Border.None)
        } else {
            CardDefaults.border(
                border = if (showSelected) Border(
                    border = NuvioTheme.focusRing.border(NuvioTheme.spacing.hairline),
                    shape = RoundedCornerShape(SettingsPillRadius)
                ) else Border.None,
                focusedBorder = Border(
                    border = NuvioTheme.focusRing.border(NuvioTheme.spacing.xxs),
                    shape = RoundedCornerShape(SettingsPillRadius)
                )
            )
        },
        shape = CardDefaults.shape(railShape),
        scale = CardDefaults.scale(focusedScale = 1f, pressedScale = 1f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (glass) Modifier.height(SettingsGlassColors.RailItemHeight)
                    else Modifier.heightIn(min = SettingsRailItemHeight)
                ),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = if (zen) 14.dp else 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (zen && !glass) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(16.dp)
                                .clip(RoundedCornerShape(SettingsPillRadius))
                                .background(
                                    if (showSelected) NuvioTheme.colors.Secondary else Color.Transparent
                                )
                        )
                        Spacer(modifier = Modifier.width(NuvioTheme.spacing.md))
                    }
                    if (glass) {
                        // approved mockup: text only, no icons
                    } else if (rawIconRes != null) {
                        Image(
                            painter = rememberRawSvgPainter(rawIconRes),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            contentScale = ContentScale.Fit,
                            colorFilter = ColorFilter.tint(railContentColor)
                        )
                    } else if (icon != null) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = railContentColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    if (!glass && (rawIconRes != null || icon != null)) {
                        Spacer(modifier = Modifier.width(if (glass) 12.dp else 10.dp))
                    }

                    FocusMarqueeText(
                        text = title,
                        focused = isFocused,
                        modifier = Modifier.clearAndSetSemantics {},
                        style = if (glass) {
                            // 13, weight 500; the current section 600.
                            SettingsGlassType.rail(selected = isSelected || isFocused)
                        } else MaterialTheme.typography.titleMedium.copy(
                            fontWeight = if (isSelected || isFocused) FontWeight.SemiBold else FontWeight.Medium
                        ),
                        color = railContentColor,
                    )
                }

                if (!zen) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = NuvioTheme.colors.TextTertiary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
internal fun SettingsTopBarTab(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    onFocused: () -> Unit = {},
    icon: ImageVector? = null,
    rawIconRes: Int? = null,
    onFocusedTabPositioned: ((LayoutCoordinates) -> Unit)? = null,
    suppressSelectedVisual: Boolean = false
) {
    var isFocused by remember { mutableStateOf(false) }
    var tabCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val glideIndicator = onFocusedTabPositioned != null
    val appliedModifier = if (focusRequester != null) {
        modifier.focusRequester(focusRequester)
    } else {
        modifier
    }
    val showSelected = isSelected && !suppressSelectedVisual
    val contentColor by animateColorAsState(
        targetValue = when {
            isFocused -> NuvioTheme.colors.OnSecondary
            showSelected -> NuvioTheme.colors.TextPrimary
            isSettingsGlass() -> LauncherStyle.Soft
            else -> NuvioTheme.colors.TextSecondary
        },
        animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
        label = "topBarTabContentColor"
    )

    Card(
        onClick = onClick,
        modifier = appliedModifier
            .semantics {
                role = Role.Tab
                this.selected = isSelected
            }
            .onGloballyPositioned { coordinates ->
                tabCoordinates = coordinates
                if (isFocused) onFocusedTabPositioned?.invoke(coordinates)
            }
            .onFocusChanged { state ->
                val nowFocused = state.isFocused
                if (isFocused != nowFocused) {
                    isFocused = nowFocused
                    if (nowFocused) {
                        onFocused()
                        tabCoordinates?.let { onFocusedTabPositioned?.invoke(it) }
                    }
                }
            },
        colors = CardDefaults.colors(
            containerColor = if (showSelected && isSettingsGlass()) {
                SettingsGlassColors.RailSelected
            } else if (showSelected) {
                NuvioTheme.colors.Secondary.copy(alpha = 0.18f)
            } else {
                Color.Transparent
            },
            focusedContainerColor = if (glideIndicator) {
                Color.Transparent
            } else {
                NuvioTheme.colors.Secondary
            }
        ),
        border = CardDefaults.border(border = Border.None, focusedBorder = Border.None),
        shape = CardDefaults.shape(RoundedCornerShape(SettingsPillRadius)),
        scale = CardDefaults.scale(focusedScale = 1f, pressedScale = 1f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (rawIconRes != null) {
                Image(
                    painter = rememberRawSvgPainter(rawIconRes),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    contentScale = ContentScale.Fit,
                    colorFilter = ColorFilter.tint(contentColor)
                )
                Spacer(modifier = Modifier.width(NuvioTheme.spacing.sm))
            } else if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(NuvioTheme.spacing.sm))
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = if (showSelected || isFocused) FontWeight.SemiBold else FontWeight.Medium
                ),
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
internal fun SettingsDetailHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    if (isSettingsGlass()) {
        // Launcher look: breadcrumb 11 (sub-pages only), title 20 SemiBold 2 below it, subtitle 11.5 3 below.
        // The 14 down to the panel is the page column's own spacing.
        Column(modifier = modifier.fillMaxWidth()) {
            val breadcrumb = LocalSettingsBreadcrumb.current
            if (!breadcrumb.isNullOrBlank()) {
                Text(
                    text = breadcrumb,
                    style = SettingsGlassType.Breadcrumb,
                    color = SettingsGlassColors.TextIdleSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = title,
                style = SettingsGlassType.PageTitle,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp)
            )
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    style = SettingsGlassType.PageSubtitle,
                    color = SettingsGlassColors.TextIdleSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 3.dp)
                )
            }
        }
        return
    }
    when (NuvioTheme.settingsUiStyle) {
        SettingsUiStyle.ZEN -> Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(start = settingsHeaderInset(), bottom = NuvioTheme.spacing.sm),
            verticalArrangement = Arrangement.spacedBy(NuvioTheme.spacing.sm)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
                color = NuvioTheme.colors.TextPrimary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isSettingsGlass()) SettingsGlassColors.TextIdleSecondary else NuvioTheme.colors.TextTertiary
            )
        }
        SettingsUiStyle.HORIZON -> Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(start = settingsHeaderInset(), bottom = NuvioTheme.spacing.sm),
            verticalArrangement = Arrangement.spacedBy(NuvioTheme.spacing.xs)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.SemiBold),
                color = NuvioTheme.colors.TextPrimary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = settingsSecondaryColor(false)
            )
        }
        SettingsUiStyle.CLASSIC -> Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = NuvioTheme.colors.TextPrimary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = settingsSecondaryColor(false)
            )
        }
    }
}

@Composable
internal fun SettingsGroupCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    subtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    if (isSettingsGlass()) {
        SettingsGlassGroupCard(modifier = modifier, title = title, subtitle = subtitle, content = content)
        return
    }
    when (NuvioTheme.settingsUiStyle) {
        SettingsUiStyle.ZEN, SettingsUiStyle.HORIZON -> Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(top = NuvioTheme.spacing.xs),
            verticalArrangement = Arrangement.spacedBy(NuvioTheme.spacing.sm)
        ) {
            if (!title.isNullOrBlank()) {
                Text(
                    text = title.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    letterSpacing = 1.2.sp,
                    color = NuvioTheme.colors.TextTertiary,
                    modifier = Modifier.padding(start = NuvioTheme.spacing.lg)
                )
            }
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = NuvioTheme.colors.TextTertiary.copy(alpha = 0.8f),
                    modifier = Modifier.padding(start = NuvioTheme.spacing.lg)
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(settingsGroupShape())
                    .background(NuvioTheme.colors.BackgroundCard.copy(alpha = 0.55f))
                    .padding(NuvioTheme.spacing.sm),
                verticalArrangement = Arrangement.spacedBy(NuvioTheme.spacing.xxs)
            ) {
                content()
            }
        }
        SettingsUiStyle.CLASSIC -> Column(
            modifier = modifier
                .fillMaxWidth()
                .clip(settingsGroupShape())
                .background(NuvioTheme.colors.BackgroundCard)
                .border(
                    width = NuvioTheme.spacing.hairline,
                    color = NuvioTheme.colors.Border,
                    shape = settingsGroupShape()
                )
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (!title.isNullOrBlank()) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = NuvioTheme.colors.TextPrimary
                )
            }
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = NuvioTheme.colors.TextSecondary
                )
            }
            content()
        }
    }
}

/**
 * The launcher look of [SettingsGroupCard]: the launcher glass with 14 inside on every side, the
 * title as an uppercase section label right at the top. A card inside another card is only a
 * group: no second layer of glass.
 */
@Composable
private fun SettingsGlassGroupCard(
    modifier: Modifier,
    title: String?,
    subtitle: String?,
    content: @Composable ColumnScope.() -> Unit
) {
    val nested = LocalSettingsInGlassCard.current
    val header: @Composable ColumnScope.() -> Unit = {
        if (!title.isNullOrBlank()) {
            // Section label: 10.5 Bold uppercase, 0.08em apart, white .6; 12 above it inside a panel
            // (none when it opens the panel), 8 below (the column's spacing).
            Text(
                text = title.uppercase(),
                style = SettingsGlassType.SectionLabel,
                color = SettingsGlassColors.TextIdleSecondary,
                modifier = Modifier.padding(start = 4.dp, top = if (nested) SettingsGlassColors.SectionLabelTop else 0.dp)
            )
        }
        if (!subtitle.isNullOrBlank()) {
            Text(
                text = subtitle,
                style = SettingsGlassType.RowSubtitle,
                color = SettingsGlassColors.TextIdleSecondary,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
    if (nested) {
        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(SettingsGlassColors.RowGap)
        ) {
            header()
            content()
        }
        return
    }
    LauncherGlass(
        modifier = modifier.fillMaxWidth(),
        shape = SettingsGlassColors.GroupShape,
        hazeState = LocalSettingsHazeState.current
    ) {
        CompositionLocalProvider(LocalSettingsInGlassCard provides true) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(SettingsGlassColors.PanelPadding),
                verticalArrangement = Arrangement.spacedBy(SettingsGlassColors.RowGap)
            ) {
                header()
                content()
            }
        }
    }
}

/**
 * The launcher glass panel with nothing laid out in it, for a page whose own list scrolls inside the
 * panel (pass [SettingsGlassPanelContentPadding] to that list). Cards inside draw as plain groups with
 * section labels. Off the glass it is a plain box.
 */
@Composable
internal fun SettingsGlassPanel(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    if (!isSettingsGlass()) {
        Box(modifier = modifier, content = content)
        return
    }
    LauncherGlass(
        modifier = modifier.fillMaxWidth(),
        shape = SettingsGlassColors.GroupShape,
        hazeState = LocalSettingsHazeState.current
    ) {
        val scope = this
        CompositionLocalProvider(LocalSettingsInGlassCard provides true) {
            scope.content()
        }
    }
}

/**
 * One glass panel holding a page's sections, scrolling inside the panel (the page title stays put),
 * as in the launcher Settings mockup. Off the glass it is a plain scrolling column.
 */
@Composable
internal fun SettingsGlassScrollPanel(
    scrollState: ScrollState,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    if (!isSettingsGlass()) {
        Column(
            modifier = modifier.fillMaxWidth().verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            content = content
        )
        return
    }
    SettingsGlassPanel(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(SettingsGlassPanelContentPadding),
            verticalArrangement = Arrangement.spacedBy(SettingsGlassColors.RowGap),
            content = content
        )
    }
}

@Composable
internal fun SettingsToggleRow(
    title: String,
    subtitle: String?,
    checked: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    onFocused: () -> Unit = {},
    enabled: Boolean = true,
    titleTrailingIcon: ImageVector? = null,
    titleTrailingIconTint: Color = NuvioTheme.colors.TextPrimary,
    expandSubtitleOnFocus: Boolean = false
) {
    val contentAlpha = if (enabled) 1f else 0.4f
    var isFocused by remember { mutableStateOf(false) }
    val glass = isSettingsGlass()

    Card(
        onClick = {
            if (enabled) onToggle()
        },
        modifier = modifier
            .fillMaxWidth()
            // On the glass the minimum is on the content row, so short rows stay centred.
            .then(if (glass) Modifier else Modifier.heightIn(min = 62.dp))
            .focusProperties { canFocus = enabled }
            .semantics {
                if (!enabled) disabled()
                role = Role.Switch
                toggleableState = if (checked) ToggleableState.On else ToggleableState.Off
            }
            .onFocusChanged { state ->
                val nowFocused = state.isFocused
                if (isFocused != nowFocused) {
                    isFocused = nowFocused
                    if (nowFocused) onFocused()
                }
            },
        colors = settingsRowColors(),
        border = settingsRowBorder(contentAlpha),
        shape = CardDefaults.shape(settingsRowShape()),
        scale = CardDefaults.scale(focusedScale = 1f, pressedScale = 1f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (glass) {
                        Modifier
                            .heightIn(min = SettingsGlassColors.RowMinHeight)
                            .padding(horizontal = SettingsGlassColors.RowPaddingH, vertical = SettingsGlassColors.RowPaddingV)
                    } else {
                        Modifier.padding(horizontal = 18.dp, vertical = NuvioTheme.spacing.md)
                    }
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = if (glass) SettingsGlassType.RowTitle else MaterialTheme.typography.bodyLarge,
                        color = settingsTitleColor(isFocused).copy(alpha = contentAlpha),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (titleTrailingIcon != null) {
                        Spacer(modifier = Modifier.width(NuvioTheme.spacing.sm))
                        Icon(
                            imageVector = titleTrailingIcon,
                            contentDescription = null,
                            tint = (if (isSettingsGlass() && titleTrailingIconTint == NuvioTheme.colors.TextPrimary) {
                                settingsTitleColor(isFocused)
                            } else titleTrailingIconTint).copy(alpha = contentAlpha),
                            modifier = Modifier.size(NuvioTheme.spacing.lg)
                        )
                    }
                }
                if (!subtitle.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(NuvioTheme.spacing.xxs))
                    Text(
                        text = subtitle,
                        style = if (glass) SettingsGlassType.RowSubtitle else MaterialTheme.typography.bodySmall,
                        color = settingsSecondaryColor(isFocused).copy(alpha = contentAlpha),
                        maxLines = if (expandSubtitleOnFocus && isFocused) 8 else if (glass) 2 else 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(modifier = Modifier.width(if (glass) 16.dp else NuvioTheme.spacing.md))
            SettingsTogglePill(
                checked = checked,
                enabled = enabled,
                focused = isFocused
            )
        }
    }
}

@Composable
internal fun SettingsActionRow(
    title: String,
    subtitle: String?,
    value: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onFocused: () -> Unit = {},
    enabled: Boolean = true,
    trailingIcon: ImageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
    titleTrailingIcon: ImageVector? = null,
    titleTrailingIconTint: Color = NuvioTheme.colors.TextPrimary,
    leadingIcon: ImageVector? = null,
    @RawRes leadingRawIconRes: Int? = null,
    leadingArtworkSize: Dp = NuvioTheme.spacing.xl,
    valueColor: Color = NuvioTheme.colors.TextSecondary
) {
    val contentAlpha = if (enabled) 1f else 0.4f
    var isFocused by remember { mutableStateOf(false) }
    val glass = isSettingsGlass()

    Card(
        onClick = { if (enabled) onClick() },
        modifier = modifier
            // On the glass rows are exactly 8 apart: no extra space around each one.
            .then(if (glass) Modifier else Modifier.padding(top = NuvioTheme.spacing.xxs, bottom = NuvioTheme.spacing.xxs))
            .fillMaxWidth()
            .focusProperties { canFocus = enabled }
            .semantics {
                if (!enabled) disabled()
                role = Role.Button
            }
            .onFocusChanged { state ->
                val nowFocused = state.isFocused
                if (isFocused != nowFocused) {
                    isFocused = nowFocused
                    if (nowFocused) onFocused()
                }
            },
        colors = settingsRowColors(),
        border = settingsRowBorder(contentAlpha),
        shape = CardDefaults.shape(settingsRowShape()),
        scale = CardDefaults.scale(focusedScale = 1f, pressedScale = 1f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (glass) {
                        Modifier
                            .heightIn(min = SettingsGlassColors.RowMinHeight)
                            .padding(horizontal = SettingsGlassColors.RowPaddingH, vertical = SettingsGlassColors.RowPaddingV)
                    } else {
                        Modifier
                            .heightIn(min = 62.dp)
                            .padding(horizontal = 18.dp, vertical = NuvioTheme.spacing.md)
                    }
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leadingRawIconRes != null) {
                Image(
                    painter = rememberRawSvgPainter(leadingRawIconRes, leadingArtworkSize),
                    contentDescription = null,
                    modifier = Modifier
                        .size(leadingArtworkSize)
                        .alpha(contentAlpha),
                    contentScale = ContentScale.Fit
                )
                Spacer(modifier = Modifier.width(if (glass) 12.dp else NuvioTheme.spacing.lg))
            } else if (leadingIcon != null) {
                SettingsRowLeadingIcon(icon = leadingIcon, enabled = enabled, focused = isFocused)
                Spacer(modifier = Modifier.width(if (glass) 12.dp else NuvioTheme.spacing.lg))
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = if (glass) SettingsGlassType.RowTitle else MaterialTheme.typography.bodyLarge,
                        color = settingsTitleColor(isFocused).copy(alpha = contentAlpha),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (titleTrailingIcon != null) {
                        Spacer(modifier = Modifier.width(NuvioTheme.spacing.sm))
                        Icon(
                            imageVector = titleTrailingIcon,
                            contentDescription = null,
                            tint = (if (isSettingsGlass() && titleTrailingIconTint == NuvioTheme.colors.TextPrimary) {
                                settingsTitleColor(isFocused)
                            } else titleTrailingIconTint).copy(alpha = contentAlpha),
                            modifier = Modifier.size(NuvioTheme.spacing.lg)
                        )
                    }
                }
                if (!subtitle.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(NuvioTheme.spacing.xxs))
                    Text(
                        text = subtitle,
                        style = if (glass) SettingsGlassType.RowSubtitle else MaterialTheme.typography.bodySmall,
                        color = settingsSecondaryColor(isFocused).copy(alpha = contentAlpha),
                        maxLines = if (glass) 2 else 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (glass) {
                // Launcher look: the value in 12 white .72 followed by " ›" (or "⌄" for a section that
                // opens), dark on the focused white row.
                val defaultValueColor = if (isFocused) LauncherStyle.FocusText.copy(alpha = 0.65f) else LauncherStyle.Soft
                val shownValueColor = if (valueColor != NuvioTheme.colors.TextSecondary) valueColor else defaultValueColor
                val glyph = when (trailingIcon) {
                    Icons.AutoMirrored.Filled.KeyboardArrowRight -> "›"
                    Icons.Default.ExpandMore -> "⌄"
                    else -> null
                }
                Spacer(modifier = Modifier.width(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!value.isNullOrBlank()) {
                        Text(
                            text = value,
                            style = SettingsGlassType.Value,
                            color = shownValueColor.copy(alpha = contentAlpha),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = 260.dp)
                        )
                    }
                    if (glyph != null) {
                        Text(
                            text = if (value.isNullOrBlank()) glyph else " $glyph",
                            style = SettingsGlassType.Value,
                            color = defaultValueColor.copy(alpha = contentAlpha),
                            maxLines = 1
                        )
                    } else {
                        if (!value.isNullOrBlank()) Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = trailingIcon,
                            contentDescription = null,
                            tint = defaultValueColor.copy(alpha = contentAlpha),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                return@Row
            }

            if (!value.isNullOrBlank()) {
                Spacer(modifier = Modifier.width(10.dp))
                // On the glass, the default grey value reads as soft white, or dark on the focused pill.
                val shownValueColor = when {
                    !isSettingsGlass() || valueColor != NuvioTheme.colors.TextSecondary -> valueColor
                    isFocused -> LauncherStyle.FocusText.copy(alpha = 0.65f)
                    else -> LauncherStyle.Soft
                }
                Text(
                    text = value,
                    style = MaterialTheme.typography.labelLarge,
                    color = shownValueColor.copy(alpha = contentAlpha),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 260.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))
            Icon(
                imageVector = trailingIcon,
                contentDescription = null,
                tint = settingsTertiaryColor(isFocused).copy(alpha = contentAlpha),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
internal fun <T> SettingsSingleChoiceDialog(
    title: String,
    options: List<SettingsPickerOption<T>>,
    selectedValue: T,
    onOptionSelected: (T) -> Unit,
    onDismiss: () -> Unit,
    subtitle: String? = null,
    width: Dp = 420.dp,
    maxHeight: Dp = 320.dp
) {
    val focusRequester = remember { FocusRequester() }
    val focusedIndex = options.indexOfFirst { it.value == selectedValue }
        .let { if (it >= 0) it else 0 }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = focusedIndex)

    LaunchedEffect(focusedIndex) {
        focusRequester.requestFocusAfterFrames()
    }

    NuvioDialog(
        onDismiss = onDismiss,
        title = title,
        subtitle = subtitle,
        width = width,
        suppressFirstKeyUp = false
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxHeight)
        ) {
            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(NuvioTheme.spacing.sm),
                contentPadding = PaddingValues(vertical = NuvioTheme.spacing.xs)
            ) {
                itemsIndexed(
                    items = options,
                    key = { index, option -> "$index-${option.value}" }
                ) { index, option ->
                    val isSelected = option.value == selectedValue
                    Card(
                        onClick = { onOptionSelected(option.value) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(if (index == focusedIndex) Modifier.focusRequester(focusRequester) else Modifier)
                            .semantics {
                                role = Role.RadioButton
                                this.selected = isSelected
                            },
                        colors = CardDefaults.colors(
                            containerColor = if (isSelected) NuvioTheme.colors.FocusBackground else NuvioTheme.colors.BackgroundCard,
                            focusedContainerColor = NuvioTheme.colors.FocusBackground
                        ),
                        shape = CardDefaults.shape(RoundedCornerShape(10.dp)),
                        scale = CardDefaults.scale(focusedScale = 1f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(NuvioTheme.spacing.lg),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = option.title,
                                    color = if (isSelected) NuvioTheme.colors.Primary else NuvioTheme.colors.TextPrimary,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontFamily = option.titleFontFamily
                                )
                                if (!option.description.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(NuvioTheme.spacing.xs))
                                    Text(
                                        text = option.description,
                                        color = NuvioTheme.colors.TextSecondary,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                            if (!option.trailing.isNullOrBlank()) {
                                Spacer(modifier = Modifier.width(NuvioTheme.spacing.md))
                                Text(
                                    text = option.trailing,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = NuvioTheme.colors.TextSecondary
                                )
                            }
                            if (isSelected) {
                                Spacer(modifier = Modifier.width(NuvioTheme.spacing.md))
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = NuvioTheme.colors.Primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun <T> SettingsMultiChoiceDialog(
    title: String,
    options: List<SettingsPickerOption<T>>,
    selectedValues: List<T>,
    onValuesSelected: (List<T>) -> Unit,
    onDismiss: () -> Unit,
    subtitle: String? = null,
    width: Dp = 520.dp,
    maxHeight: Dp = 420.dp,
    maxSelections: Int? = null
) {
    val focusRequester = remember { FocusRequester() }
    val selected = remember(selectedValues) { mutableStateListOf<T>().also { it.addAll(selectedValues) } }
    val firstSelectedIndex = options.indexOfFirst { option -> selectedValues.contains(option.value) }
        .let { if (it >= 0) it else 0 }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = firstSelectedIndex)

    LaunchedEffect(firstSelectedIndex) {
        focusRequester.requestFocusAfterFrames()
    }

    NuvioDialog(
        onDismiss = onDismiss,
        title = title,
        subtitle = subtitle,
        width = width,
        suppressFirstKeyUp = false
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxHeight),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
            ) {
                LazyColumn(
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(NuvioTheme.spacing.sm),
                    contentPadding = PaddingValues(vertical = NuvioTheme.spacing.xs)
                ) {
                    itemsIndexed(
                        items = options,
                        key = { index, option -> "$index-${option.value}" }
                    ) { index, option ->
                        val isSelected = selected.contains(option.value)
                        Card(
                            onClick = {
                                if (isSelected) {
                                    selected.remove(option.value)
                                } else if (maxSelections == null || selected.size < maxSelections) {
                                    selected.add(option.value)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(if (index == firstSelectedIndex) Modifier.focusRequester(focusRequester) else Modifier)
                                .semantics {
                                    role = Role.Checkbox
                                    toggleableState = if (isSelected) ToggleableState.On else ToggleableState.Off
                                },
                            colors = CardDefaults.colors(
                                containerColor = if (isSelected) NuvioTheme.colors.FocusBackground else NuvioTheme.colors.BackgroundCard,
                                focusedContainerColor = NuvioTheme.colors.FocusBackground
                            ),
                            shape = CardDefaults.shape(RoundedCornerShape(10.dp)),
                            scale = CardDefaults.scale(focusedScale = 1f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(NuvioTheme.spacing.lg),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = option.title,
                                        color = if (isSelected) NuvioTheme.colors.Primary else NuvioTheme.colors.TextPrimary,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontFamily = option.titleFontFamily
                                    )
                                    if (!option.description.isNullOrBlank()) {
                                        Spacer(modifier = Modifier.height(NuvioTheme.spacing.xs))
                                        Text(
                                            text = option.description,
                                            color = NuvioTheme.colors.TextSecondary,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                                if (isSelected) {
                                    Spacer(modifier = Modifier.width(NuvioTheme.spacing.md))
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = NuvioTheme.colors.Primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            SettingsDialogActionRow {
                SettingsDialogActionButton(
                    text = stringResource(R.string.action_clear),
                    onClick = { selected.clear() }
                )
                SettingsDialogActionButton(
                    text = stringResource(R.string.action_save),
                    onClick = { onValuesSelected(options.map { it.value }.filter { selected.contains(it) }) },
                    primary = true
                )
            }
        }
    }
}

@Composable
internal fun SettingsDialogActionRow(
    horizontalAlignment: Alignment.Horizontal = Alignment.End,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(NuvioTheme.spacing.sm, horizontalAlignment),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

@Composable
internal fun SettingsDialogActionButton(
    text: String,
    onClick: () -> Unit,
    primary: Boolean = false,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    if (isSettingsGlass()) {
        SettingsGlassDialogButton(text = text, onClick = onClick, enabled = enabled, modifier = modifier)
        return
    }
    Button(
        modifier = modifier,
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.colors(
            containerColor = if (primary) NuvioTheme.colors.FocusBackground else NuvioTheme.colors.BackgroundCard,
            contentColor = NuvioTheme.colors.TextPrimary
        )
    ) {
        Text(
            text = text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * A dialog button on the launcher glass: a pill, 12 SemiBold, 7 by 16 inside; white .18 with white
 * text, the launcher's white with dark text when focused.
 */
@Composable
internal fun SettingsGlassDialogButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    var isFocused by remember { mutableStateOf(false) }
    Card(
        onClick = { if (enabled) onClick() },
        modifier = modifier
            .focusProperties { canFocus = enabled }
            .onFocusChanged { isFocused = it.isFocused }
            .semantics {
                role = Role.Button
                if (!enabled) disabled()
            },
        colors = CardDefaults.colors(
            containerColor = SettingsGlassColors.RailSelected,
            contentColor = Color.White,
            focusedContainerColor = LauncherStyle.FocusWhite,
            focusedContentColor = LauncherStyle.FocusText
        ),
        border = CardDefaults.border(border = Border.None, focusedBorder = Border.None),
        shape = CardDefaults.shape(RoundedCornerShape(SettingsPillRadius)),
        scale = CardDefaults.scale(focusedScale = 1f, pressedScale = 1f)
    ) {
        Text(
            text = text,
            style = SettingsGlassType.DialogButton,
            color = (if (isFocused) LauncherStyle.FocusText else Color.White).copy(alpha = if (enabled) 1f else 0.4f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp)
        )
    }
}

@Composable
internal fun SettingsChoiceChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onFocused: () -> Unit = {},
    multiSelect: Boolean = false
) {
    var isFocused by remember { mutableStateOf(false) }
    val glass = isSettingsGlass()
    val zen = isFlatSettingsStyle()

    Card(
        onClick = onClick,
        modifier = modifier.onFocusChanged { state ->
            val nowFocused = state.isFocused
            if (isFocused != nowFocused) {
                isFocused = nowFocused
                if (nowFocused) onFocused()
            }
        }.semantics {
            if (multiSelect) {
                role = Role.Checkbox
                toggleableState = if (selected) ToggleableState.On else ToggleableState.Off
            } else {
                role = Role.RadioButton
                this.selected = selected
            }
        },
        // Launcher look: the sidebar pills (idle white .12, selected white .30, focused white).
        colors = if (glass) CardDefaults.colors(
            containerColor = if (selected) LauncherStyle.PillSelected else LauncherStyle.PillIdle,
            contentColor = if (selected) Color.White else LauncherStyle.Soft,
            focusedContainerColor = LauncherStyle.FocusWhite,
            focusedContentColor = LauncherStyle.FocusText
        ) else CardDefaults.colors(
            containerColor = when {
                zen && selected -> NuvioTheme.colors.Secondary.copy(alpha = 0.18f)
                zen -> Color.Transparent
                selected -> NuvioTheme.colors.FocusRing.copy(alpha = 0.2f)
                else -> NuvioTheme.colors.Background
            },
            focusedContainerColor = when {
                zen -> settingsFocusFillColor()
                selected -> NuvioTheme.colors.FocusRing.copy(alpha = 0.2f)
                else -> NuvioTheme.colors.Background
            }
        ),
        border = if (glass) {
            CardDefaults.border(border = Border.None, focusedBorder = Border.None)
        } else if (zen) {
            CardDefaults.border(
                border = if (selected) Border(
                    border = BorderStroke(NuvioTheme.spacing.hairline, NuvioTheme.colors.Secondary.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(SettingsPillRadius)
                ) else Border.None,
                focusedBorder = if (selected) Border(
                    border = BorderStroke(NuvioTheme.spacing.hairline, NuvioTheme.colors.Secondary.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(SettingsPillRadius)
                ) else Border.None
            )
        } else {
            CardDefaults.border(
                border = if (selected) Border(
                    border = NuvioTheme.focusRing.border(NuvioTheme.spacing.hairline),
                    shape = RoundedCornerShape(SettingsPillRadius)
                ) else Border.None,
                focusedBorder = Border(
                    border = NuvioTheme.focusRing.border(NuvioTheme.spacing.hairline),
                    shape = RoundedCornerShape(SettingsPillRadius)
                )
            )
        },
        shape = CardDefaults.shape(RoundedCornerShape(SettingsPillRadius)),
        scale = CardDefaults.scale(focusedScale = 1f, pressedScale = 1f)
    ) {
        Text(
            // Launcher look: a ticked chip reads "✓ label".
            text = if (glass && selected) "✓ $label" else label,
            style = (if (glass) SettingsGlassType.Chip else MaterialTheme.typography.labelMedium).copy(
                textDirection = label.contentTextDirection()
            ),
            color = when {
                glass && isFocused -> LauncherStyle.FocusText
                glass && selected -> Color.White
                glass -> LauncherStyle.Soft
                selected || isFocused -> NuvioTheme.colors.TextPrimary
                else -> NuvioTheme.colors.TextSecondary
            },
            maxLines = 1,
            modifier = if (glass) {
                Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
            } else {
                Modifier.padding(horizontal = NuvioTheme.spacing.lg, vertical = 10.dp)
            }
        )
    }
}

@Composable
private fun SettingsTogglePill(
    checked: Boolean,
    enabled: Boolean,
    focused: Boolean = false
) {
    val alpha = if (enabled) 1f else 0.35f
    val zen = isFlatSettingsStyle()
    val glass = isSettingsGlass()
    val trackColor = when {
        // Launcher look: a white switch on the glass, a dark one on the focused white pill.
        glass && focused && checked -> LauncherStyle.FocusText.copy(alpha = alpha)
        glass && focused -> LauncherStyle.FocusText.copy(alpha = 0.18f * alpha)
        glass && checked -> SettingsGlassColors.TrackOn.copy(alpha = alpha)
        glass -> SettingsGlassColors.TrackOff.copy(alpha = SettingsGlassColors.TrackOff.alpha * alpha)
        zen && checked -> NuvioTheme.colors.Secondary.copy(alpha = 0.9f * alpha)
        checked -> NuvioTheme.colors.Secondary.copy(alpha = 0.35f * alpha)
        else -> NuvioTheme.colors.Border.copy(alpha = alpha)
    }
    val knobColor = if (glass) {
        when {
            focused && checked -> Color.White.copy(alpha = alpha)
            focused -> LauncherStyle.FocusText.copy(alpha = 0.45f * alpha)
            checked -> SettingsGlassColors.KnobOn.copy(alpha = alpha)
            else -> SettingsGlassColors.KnobOff.copy(alpha = SettingsGlassColors.KnobOff.alpha * alpha)
        }
    } else if (zen && checked) {
        NuvioTheme.colors.OnSecondary.copy(alpha = alpha)
    } else {
        Color.White.copy(alpha = alpha)
    }
    Box(
        modifier = Modifier
            // Launcher look: a 34 by 19 switch with a 14 knob, 2.5 in from the edge.
            .width(if (glass) 34.dp else 46.dp)
            .height(if (glass) 19.dp else NuvioTheme.spacing.xl)
            .clip(RoundedCornerShape(SettingsPillRadius))
            .background(trackColor)
            .padding(if (glass) 2.5.dp else NuvioTheme.spacing.xxs),
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .size(if (glass) 14.dp else 20.dp)
                .clip(CircleShape)
                .background(knobColor)
        )
    }
}

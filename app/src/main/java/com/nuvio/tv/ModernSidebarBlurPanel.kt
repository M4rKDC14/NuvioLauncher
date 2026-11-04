package com.nuvio.tv

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.nuvio.tv.ui.components.AutoResizeText
import com.nuvio.tv.ui.components.BrandWordmark
import com.nuvio.tv.ui.components.ProfileAvatarCircle
import com.nuvio.tv.ui.theme.NuvioComponents
import com.nuvio.tv.ui.theme.NuvioMotion
import com.nuvio.tv.ui.theme.NuvioRadii
import com.nuvio.tv.ui.theme.NuvioStrokes
import com.nuvio.tv.ui.theme.NuvioTheme
import com.nuvio.tv.ui.theme.accentBrush
import com.nuvio.tv.ui.util.contentTextDirection
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.draw.drawWithCache
import com.nuvio.tv.ui.util.rememberRawSvgPainter
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeInputScale
import dev.chrisbanes.haze.hazeEffect

private val SidebarLeadingVisualSize = NuvioComponents.tokens.sidebar.leadingVisual
private val SidebarContentGap = NuvioComponents.tokens.sidebar.contentGap
private val SidebarProfileContentGap = NuvioComponents.tokens.sidebar.contentGap + NuvioTheme.spacing.xs

@Composable
internal fun ModernSidebarBlurPanel(
    drawerItems: List<DrawerItem>,
    selectedDrawerRoute: String?,
    keepSidebarFocusDuringCollapse: Boolean,
    sidebarLabelAlpha: Float,
    sidebarIconScale: Float,
    sidebarExpandProgress: Float,
    isSidebarExpanded: Boolean,
    sidebarCollapsePending: Boolean,
    blurEnabled: Boolean,
    sidebarHazeState: HazeState,
    panelShape: RoundedCornerShape,
    drawerItemFocusRequesters: Map<String, FocusRequester>,
    onDrawerItemFocused: (Int) -> Unit,
    onDrawerItemClick: (String) -> Unit,
    activeProfileName: String,
    activeProfileColorHex: String,
    activeProfileAvatarImageUrl: String?,
    showProfileSelector: Boolean,
    onSwitchProfile: () -> Unit,
    clockText: String = ""
) {
    // Nuvio Launcher: Apple TV-style glass. Full blur from the first frame (no late ramp), no grain.
    val showPanelBlur = blurEnabled && (isSidebarExpanded || sidebarCollapsePending)
    val expandedPanelBlurModifier = if (showPanelBlur) {
        Modifier.hazeEffect(state = sidebarHazeState) {
            blurRadius = LauncherGlassBlur
            noiseFactor = 0f
            inputScale = HazeInputScale.Fixed(0.66f)
        }
    } else {
        Modifier
    }
    val colors = NuvioTheme.colors
    val bgElevated = colors.BackgroundElevated
    val bgCard = colors.BackgroundCard
    val borderBase = colors.Border
    val isAmoledBlack = bgElevated == Color.Black
    val panelBackgroundBrush = remember(blurEnabled) {
        if (blurEnabled) launcherGlassBrush() else Brush.verticalGradient(listOf(Color(0xF2161618), Color(0xF2161618)))
    }
    val panelBorderColor = LauncherGlassEdge

    Column(
        modifier = Modifier
            .wrapContentHeight()   // Nuvio Launcher: the panel hugs its items
            .graphicsLayer {
                val p = sidebarExpandProgress
                alpha = p
                val s = 0.97f + (0.03f * p)
                scaleX = s
                scaleY = s
                transformOrigin = TransformOrigin(0f, 0f)
            }
            .clip(panelShape)
            .then(expandedPanelBlurModifier)
            .background(brush = panelBackgroundBrush, shape = panelShape)
            .border(width = 1.dp, color = panelBorderColor, shape = panelShape)
            .padding(horizontal = 7.dp, vertical = 12.dp)
    ) {
        if (showProfileSelector && activeProfileName.isNotEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                SidebarProfileItem(
                    profileName = activeProfileName,
                    profileColorHex = activeProfileColorHex,
                    profileAvatarImageUrl = activeProfileAvatarImageUrl,
                    focusEnabled = keepSidebarFocusDuringCollapse,
                    labelAlpha = sidebarLabelAlpha,
                    onFocusChanged = { focused ->
                        if (focused) onDrawerItemFocused(drawerItems.size)
                    },
                    onClick = onSwitchProfile,
                    clockText = clockText,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 3.dp)
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = NuvioTheme.spacing.md),
                contentAlignment = Alignment.Center
            ) {
                BrandWordmark(
                    contentDescription = stringResource(R.string.app_name),
                    modifier = Modifier
                        .fillMaxWidth(0.72f)
                        .height(36.dp),
                    alpha = sidebarLabelAlpha
                )
            }
        }

        Spacer(modifier = Modifier.height(40.dp))   // Apple TV: clear space under the profile row

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                drawerItems.forEachIndexed { index, item ->
                    key(item.route) {
                        SidebarNavigationItem(
                            label = item.label,
                            iconRes = item.iconRes,
                            icon = item.icon,
                            selected = selectedDrawerRoute == item.route,
                            focusEnabled = keepSidebarFocusDuringCollapse,
                            labelAlpha = sidebarLabelAlpha,
                            iconScale = sidebarIconScale,
                            onFocusChanged = {
                                if (it) {
                                    onDrawerItemFocused(index)
                                }
                            },
                            onClick = { onDrawerItemClick(item.route) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 3.dp)   // the white pill stays inside the glass
                                .focusRequester(drawerItemFocusRequesters.getValue(item.route))
                        )
                    }
                }
            }
        }
    }
}

// Nuvio Launcher: sidebar rows in the 0.23 launcher's style. The current page sits on a translucent pill;
// the focused row is a solid white pill with dark text and a dark icon chip. Nothing scales, so the pill
// never runs into the glass edge.
private val LauncherChipSize = 20.dp
private val LauncherIconSize = 11.dp
private val LauncherLabelSize = 12.sp
private val LauncherFocusPill = Color(0xE0F2F4F8)   // bright, slightly see-through white
private val LauncherText = Color(0xEBFFFFFF)

/** The Apple TV-style glass used by the sidebar and the time pill (blur 11, sheen top-left, faint dark edge). */
internal val LauncherGlassBlur = 11.dp
internal val LauncherGlassEdge = Color(0x470A0C10)
internal fun launcherGlassBrush(): Brush = Brush.linearGradient(
    0f to Color(0x24FFFFFF),
    0.45f to Color(0x08FFFFFF),
    1f to Color(0x2E141820)
)

@Composable
private fun SidebarNavigationItem(
    label: String,
    iconRes: Int?,
    icon: ImageVector?,
    selected: Boolean,
    focusEnabled: Boolean,
    labelAlpha: Float,
    iconScale: Float,
    onFocusChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(NuvioRadii.tokens.full)
    val background = when {
        isFocused -> LauncherFocusPill
        selected -> Color.White.copy(alpha = 0.18f)
        else -> Color.Transparent
    }
    val content = if (isFocused) Color(0xFF16171B) else LauncherText
    Card(
        onClick = onClick,
        modifier = modifier
            .onFocusChanged {
                isFocused = it.hasFocus
                onFocusChanged(it.hasFocus)
            }
            .focusProperties { canFocus = focusEnabled },
        colors = CardDefaults.colors(containerColor = background, focusedContainerColor = background),
        border = CardDefaults.border(border = androidx.tv.material3.Border.None, focusedBorder = androidx.tv.material3.Border.None),
        shape = CardDefaults.shape(shape = shape),
        scale = CardDefaults.scale(focusedScale = 1f, pressedScale = 1f)
    ) {
        Row(
            modifier = Modifier
                .semantics {
                    contentDescription = label
                    role = Role.Tab
                    this.selected = selected
                }
                .fillMaxWidth()
                .padding(start = 8.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(LauncherChipSize)
                    .clip(CircleShape)
                    .background(if (isFocused) Color.Transparent else Color.White.copy(alpha = 0.24f)),
                contentAlignment = Alignment.Center
            ) {
                val tint = if (isFocused) Color(0xFF16171B) else Color.White
                when {
                    icon != null -> Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(LauncherIconSize))
                    iconRes != null -> Icon(painter = rememberRawSvgPainter(iconRes), contentDescription = null, tint = tint, modifier = Modifier.size(LauncherIconSize))
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = label,
                color = content,
                fontSize = LauncherLabelSize,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                modifier = Modifier.weight(1f).graphicsLayer { alpha = labelAlpha }
            )
        }
    }
}

@Composable
private fun SidebarProfileItem(
    profileName: String,
    profileColorHex: String,
    profileAvatarImageUrl: String?,
    focusEnabled: Boolean,
    labelAlpha: Float,
    onFocusChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    clockText: String = "",
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(NuvioRadii.tokens.full)
    val background = if (isFocused) LauncherFocusPill else Color.Transparent
    val content = if (isFocused) Color(0xFF16171B) else LauncherText
    Card(
        onClick = onClick,
        modifier = modifier
            .onFocusChanged {
                isFocused = it.hasFocus
                onFocusChanged(it.hasFocus)
            }
            .focusProperties { canFocus = focusEnabled },
        colors = CardDefaults.colors(containerColor = background, focusedContainerColor = background),
        border = CardDefaults.border(border = androidx.tv.material3.Border.None, focusedBorder = androidx.tv.material3.Border.None),
        shape = CardDefaults.shape(shape = shape),
        scale = CardDefaults.scale(focusedScale = 1f, pressedScale = 1f)
    ) {
        Row(
            modifier = Modifier
                .semantics {
                    contentDescription = profileName
                    role = Role.Button
                }
                .fillMaxWidth()
                .padding(start = 6.dp, end = 10.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProfileAvatarCircle(
                name = profileName,
                colorHex = profileColorHex,
                size = 26.dp,
                avatarImageUrl = profileAvatarImageUrl,
                imageCrossfade = false
            )
            Spacer(modifier = Modifier.width(9.dp))
            Text(
                text = profileName,
                color = content,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                modifier = Modifier.weight(1f).graphicsLayer { alpha = labelAlpha }
            )
            if (clockText.isNotEmpty()) {
                Text(text = clockText, color = content, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            }
        }
    }
}

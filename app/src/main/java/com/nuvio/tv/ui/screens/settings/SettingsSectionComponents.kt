@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.nuvio.tv.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.nuvio.tv.R
import com.nuvio.tv.ui.launcher.LauncherStyle
import com.nuvio.tv.ui.theme.NuvioTheme

internal val SettingsRowContentInset = 18.dp

internal enum class SettingsNoteTone { Info, Warning, Danger }

@Composable
internal fun SettingsCollapsibleSection(
    title: String,
    description: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    focusRequester: FocusRequester? = null,
    onFocused: () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(NuvioTheme.spacing.sm)
    ) {
        SettingsActionRow(
            title = title,
            subtitle = description,
            value = stringResource(if (expanded) R.string.layout_open else R.string.layout_closed),
            onClick = onToggle,
            leadingIcon = icon,
            trailingIcon = if (expanded) Icons.Default.ExpandMore else Icons.AutoMirrored.Filled.KeyboardArrowRight,
            modifier = if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier,
            onFocused = onFocused
        )

        if (expanded) {
            SettingsGroupCard(modifier = Modifier.padding(start = NuvioTheme.spacing.lg)) {
                content()
            }
        }
    }
}

@Composable
internal fun SettingsSectionLabel(
    text: String,
    modifier: Modifier = Modifier,
    description: String? = null
) {
    if (isSettingsGlass()) {
        // Launcher look: 10.5 Bold uppercase, 0.08em apart, white .6, 12 above, 4 in from the rows.
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 4.dp, top = SettingsGlassColors.SectionLabelTop),
            verticalArrangement = Arrangement.spacedBy(NuvioTheme.spacing.xxs)
        ) {
            Text(
                text = text.uppercase(),
                style = SettingsGlassType.SectionLabel,
                color = SettingsGlassColors.TextIdleSecondary
            )
            if (!description.isNullOrBlank()) {
                Text(
                    text = description,
                    style = SettingsGlassType.RowSubtitle,
                    color = SettingsGlassColors.TextIdleSecondary
                )
            }
        }
        return
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = SettingsRowContentInset,
                end = SettingsRowContentInset,
                top = NuvioTheme.spacing.md,
                bottom = NuvioTheme.spacing.xxs
            ),
        verticalArrangement = Arrangement.spacedBy(NuvioTheme.spacing.xxs)
    ) {
        // On the launcher glass the darker grey would sink into the picture: white .6 instead.
        val labelColor = if (isSettingsGlass()) SettingsGlassColors.TextIdleSecondary else NuvioTheme.colors.TextTertiary
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            letterSpacing = 1.2.sp,
            color = labelColor
        )
        if (!description.isNullOrBlank()) {
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = labelColor
            )
        }
    }
}

@Composable
internal fun SettingsNote(
    text: String,
    modifier: Modifier = Modifier,
    tone: SettingsNoteTone = SettingsNoteTone.Info
) {
    val glass = isSettingsGlass()
    Text(
        text = text,
        style = if (glass) SettingsGlassType.RowSubtitle else MaterialTheme.typography.bodySmall,
        color = when (tone) {
            SettingsNoteTone.Info -> settingsSecondaryColor(false)
            SettingsNoteTone.Warning -> NuvioTheme.colors.Warning
            SettingsNoteTone.Danger -> NuvioTheme.colors.Error
        },
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (glass) {
                    // The mockup's text-only row: 4 above and below, 16 in.
                    Modifier.padding(horizontal = SettingsGlassColors.RowPaddingH, vertical = 4.dp)
                } else {
                    Modifier.padding(
                        start = SettingsRowContentInset,
                        end = SettingsRowContentInset,
                        top = NuvioTheme.spacing.xxs,
                        bottom = NuvioTheme.spacing.xs
                    )
                }
            )
    )
}

@Composable
internal fun SettingsRailDivider() {
    // Approved mockup: no dividers in the Settings menu.
    if (isSettingsGlass()) return
    // Launcher look: inset 16 on each side.
    val inset = if (isSettingsGlass()) 16.dp else SettingsRowContentInset
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = inset, end = inset, bottom = 10.dp)
            .height(NuvioTheme.spacing.hairline)
            .background(if (isSettingsGlass()) SettingsGlassColors.Divider else NuvioTheme.colors.Border)
    )
}

@Composable
internal fun SettingsTopBarDivider() {
    Box(
        modifier = Modifier
            .padding(end = NuvioTheme.spacing.sm)
            .width(NuvioTheme.spacing.hairline)
            .height(20.dp)
            .background(if (isSettingsGlass()) SettingsGlassColors.Divider else NuvioTheme.colors.Border)
    )
}

@Composable
internal fun SettingsResetButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onFocused: () -> Unit = {}
) {
    val glass = isSettingsGlass()
    val flat = isFlatSettingsStyle() || glass
    val shape = if (flat) settingsRowShape() else RoundedCornerShape(SettingsPillRadius)
    var isFocused by remember { mutableStateOf(false) }
    Button(
        onClick = onClick,
        modifier = modifier.onFocusChanged {
            isFocused = it.isFocused
            if (it.isFocused) onFocused()
        },
        shape = ButtonDefaults.shape(shape = shape),
        colors = if (glass) ButtonDefaults.colors(
            containerColor = SettingsGlassColors.RowIdle,
            contentColor = Color.White,
            focusedContainerColor = LauncherStyle.FocusWhite,
            focusedContentColor = LauncherStyle.FocusText
        ) else ButtonDefaults.colors(
            containerColor = NuvioTheme.colors.Background,
            focusedContainerColor = if (flat) settingsFocusFillColor() else NuvioTheme.colors.Background
        ),
        border = if (flat) {
            ButtonDefaults.border(border = Border.None, focusedBorder = Border.None)
        } else {
            ButtonDefaults.border(
                focusedBorder = Border(
                    border = NuvioTheme.focusRing.border(NuvioTheme.spacing.xxs),
                    shape = shape
                )
            )
        },
        scale = ButtonDefaults.scale(focusedScale = 1f, pressedScale = 1f)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.RestartAlt,
                contentDescription = null,
                tint = settingsSecondaryColor(isFocused),
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(NuvioTheme.spacing.sm))
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = settingsTitleColor(isFocused)
            )
        }
    }
}

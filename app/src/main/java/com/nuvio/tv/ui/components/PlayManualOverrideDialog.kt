package com.nuvio.tv.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.nuvio.tv.R
import com.nuvio.tv.ui.launcher.LauncherStyle
import com.nuvio.tv.ui.screens.player.LauncherScaled
import com.nuvio.tv.ui.screens.settings.LocalSettingsGlass

/**
 * The Resume popup (Play manually / Start from beginning). Nuvio Launcher (approved mockup, then 15% smaller): the
 * settings dialogs' glass panel, 282dp wide; 18sp bold white title with a 14sp grey line 4dp under it; full-width 40dp
 * pills 16dp apart, text centred: grey on white 12%, the focused one the white pill with dark bold text. Everything is then drawn at 85%.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PlayManualOverrideDialog(
    title: String,
    subtitle: String?,
    onDismiss: () -> Unit,
    showPlayManually: Boolean = true,
    onPlayManually: () -> Unit,
    showStartFromBeginning: Boolean = false,
    onStartFromBeginning: () -> Unit = {}
) {
    val primaryFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        primaryFocusRequester.requestFocus()
    }

    LauncherScaled(0.85f) {
        CompositionLocalProvider(LocalSettingsGlass provides true) {
            NuvioDialog(
                onDismiss = onDismiss,
                title = "",   // drawn below, so the line under it can sit 4dp away (the dialog spaces its own 18dp)
                width = 282.dp   // owner: 40% narrower than the settings dialogs' 470dp (239.7dp once drawn at 85%)
            ) {
                Column {
                    Text(
                        text = title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            fontSize = 14.sp,
                            color = LauncherStyle.TileTitleIdle,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {   // owner: double the 8dp gap
                    if (showPlayManually) {
                        LauncherDialogPill(
                            text = stringResource(R.string.play_manually),
                            onClick = onPlayManually,
                            modifier = Modifier.focusRequester(primaryFocusRequester)
                        )
                    }
                    if (showStartFromBeginning) {
                        LauncherDialogPill(
                            text = stringResource(R.string.cw_action_start_from_beginning),
                            onClick = onStartFromBeginning,
                            modifier = if (!showPlayManually) Modifier.focusRequester(primaryFocusRequester) else Modifier
                        )
                    }
                }
            }
        }
    }
}

/** A full-width 40dp pill, 15sp: grey on white 12%; focused, the white pill with dark bold text. */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun LauncherDialogPill(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    var focused by remember { mutableStateOf(false) }
    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
            .onFocusChanged { focused = it.isFocused },
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.White.copy(alpha = 0.12f),
            focusedContainerColor = LauncherStyle.FocusWhite
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(999.dp))
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp), contentAlignment = Alignment.Center) {   // owner: text centred
            Text(
                text = text,
                fontSize = 15.sp,
                fontWeight = if (focused) FontWeight.Bold else FontWeight.Normal,
                color = if (focused) LauncherStyle.FocusText else LauncherStyle.TileTitleIdle
            )
        }
    }
}

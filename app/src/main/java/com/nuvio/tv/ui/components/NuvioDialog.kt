package com.nuvio.tv.ui.components

import com.nuvio.tv.ui.theme.NuvioTheme
import com.nuvio.tv.ui.util.contentTextDirection

import android.os.SystemClock
import android.view.KeyEvent as AndroidKeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.nuvio.tv.ui.screens.ambient.AmbientDialogEffect
import com.nuvio.tv.ui.launcher.LauncherStyle
import com.nuvio.tv.ui.screens.settings.SettingsGlassColors
import com.nuvio.tv.ui.screens.settings.SettingsGlassType
import com.nuvio.tv.ui.screens.settings.isSettingsGlass
import androidx.compose.foundation.layout.PaddingValues

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun NuvioDialog(
    onDismiss: () -> Unit,
    title: String,
    subtitle: String? = null,
    width: Dp = 520.dp,
    titleTextAlign: TextAlign = TextAlign.Start,
    suppressFirstKeyUp: Boolean = true,
    usePlatformDefaultWidth: Boolean = true,
    containerBrush: Brush? = null,
    containerBorderColor: Color? = null,
    containerBorderWidth: Dp = NuvioTheme.spacing.hairline,
    containerCornerRadius: Dp = NuvioTheme.radii.xl,
    contentPadding: Dp = NuvioTheme.spacing.xl,
    contentSpacing: Dp = NuvioTheme.spacing.lg,
    backgroundContent: @Composable BoxScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    var isReady by remember { mutableStateOf(!suppressFirstKeyUp) }
    val maxDialogHeight = (LocalConfiguration.current.screenHeightDp.dp - NuvioTheme.spacing.xxxl).coerceAtLeast(320.dp)
    // Nuvio Launcher: opened from Settings (launcher glass), the dialog takes the mockup's look: 470 wide
    // (when no width was asked for), radius 22, 22 by 24 inside, dark see-through glass, title 18 Bold.
    val settingsGlass = isSettingsGlass()
    val dialogWidth = if (settingsGlass && width == 520.dp) 470.dp else width
    val containerShape = RoundedCornerShape(if (settingsGlass) 22.dp else containerCornerRadius)
    val backgroundModifier = if (settingsGlass && containerBrush == null) {
        Modifier
            .background(Color(0xE6141518), containerShape)
            .background(LauncherStyle.glassBrush(), containerShape)
    } else if (containerBrush == null) {
        Modifier.background(NuvioTheme.colors.BackgroundElevated, containerShape)
    } else {
        Modifier.background(containerBrush, containerShape)
    }
    val innerPadding = if (settingsGlass && contentPadding == NuvioTheme.spacing.xl) {
        PaddingValues(horizontal = 24.dp, vertical = 22.dp)
    } else {
        PaddingValues(contentPadding)
    }
    val innerSpacing = if (settingsGlass && contentSpacing == NuvioTheme.spacing.lg) 18.dp else contentSpacing

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = usePlatformDefaultWidth)
    ) {
        AmbientDialogEffect(onDismiss)
        Box(
            modifier = Modifier
                .width(dialogWidth)
                .heightIn(max = maxDialogHeight)
                .clip(containerShape)
                .then(backgroundModifier)
                .border(
                    containerBorderWidth,
                    containerBorderColor ?: if (settingsGlass) LauncherStyle.Edge else NuvioTheme.colors.Border,
                    containerShape
                )
                .onPreviewKeyEvent { event ->
                    val native = event.nativeKeyEvent
                    if (isSelectKey(native.keyCode) || native.keyCode == AndroidKeyEvent.KEYCODE_MENU) {
                        if (native.action == AndroidKeyEvent.ACTION_DOWN && native.repeatCount == 0) {
                            isReady = true
                        }
                        if (!isReady) {
                            return@onPreviewKeyEvent true
                        }
                    }
                    false
                }
        ) {
            backgroundContent()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(innerPadding),
                verticalArrangement = Arrangement.spacedBy(innerSpacing)
            ) {
                if (title.isNotBlank()) {
                    Text(
                        text = title,
                        style = (if (settingsGlass) SettingsGlassType.DialogTitle else MaterialTheme.typography.titleLarge).copy(
                            textDirection = title.contentTextDirection()
                        ),
                        color = if (settingsGlass) Color.White else NuvioTheme.colors.TextPrimary,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = titleTextAlign,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = if (settingsGlass) SettingsGlassType.PageSubtitle else MaterialTheme.typography.bodyMedium,
                        color = if (settingsGlass) SettingsGlassColors.TextIdleSecondary else NuvioTheme.colors.TextSecondary
                    )
                }

                content()
            }
        }
    }
}

private fun isSelectKey(keyCode: Int): Boolean {
    return keyCode == AndroidKeyEvent.KEYCODE_DPAD_CENTER ||
        keyCode == AndroidKeyEvent.KEYCODE_ENTER ||
        keyCode == AndroidKeyEvent.KEYCODE_NUMPAD_ENTER
}

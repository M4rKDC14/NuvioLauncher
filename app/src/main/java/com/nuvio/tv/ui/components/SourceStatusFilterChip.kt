package com.nuvio.tv.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import com.nuvio.tv.ui.theme.NuvioTheme
import com.nuvio.tv.ui.util.contentTextDirection

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.FilterChip
import androidx.tv.material3.FilterChipDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text

enum class SourceChipStatus {
    LOADING,
    SUCCESS,
    ERROR
}

data class SourceChipItem(
    val name: String,
    val status: SourceChipStatus
)

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SourceStatusFilterChip(
    name: String,
    isSelected: Boolean,
    status: SourceChipStatus = SourceChipStatus.SUCCESS,
    isSelectable: Boolean = true,
    onClick: () -> Unit,
    onFocusSelect: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    val isError = status == SourceChipStatus.ERROR
    val isLoading = status == SourceChipStatus.LOADING
    val shouldUseNormalColors = !isError
    val shakeOffsetPx = remember { Animatable(0f) }
    val alphaPulse = remember { Animatable(1f) }

    LaunchedEffect(isError) {
        shakeOffsetPx.snapTo(0f)
        alphaPulse.snapTo(1f)
        if (!isError) return@LaunchedEffect

        alphaPulse.animateTo(
            targetValue = 0.9f,
            animationSpec = tween(durationMillis = 120, easing = FastOutSlowInEasing)
        )
        alphaPulse.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 160, easing = FastOutSlowInEasing)
        )

        // Two gentle shakes (less abrupt than the previous infinite shake/blink loop).
        shakeOffsetPx.animateTo(3f, tween(80, easing = FastOutSlowInEasing))
        shakeOffsetPx.animateTo(-3f, tween(110, easing = FastOutSlowInEasing))
        shakeOffsetPx.animateTo(2f, tween(90, easing = FastOutSlowInEasing))
        shakeOffsetPx.animateTo(-2f, tween(90, easing = FastOutSlowInEasing))
        shakeOffsetPx.animateTo(0f, tween(140, easing = FastOutSlowInEasing))
    }

    // Nuvio Launcher (approved mockup): grey on white 12%; the selected filter white on white 30%; focused the white
    // pill, dark bold; 32dp tall, 14sp, no borders
    val textColor = when {
        isError -> NuvioTheme.colors.Error
        isFocused -> com.nuvio.tv.ui.launcher.LauncherStyle.FocusText
        isSelected -> Color.White
        else -> com.nuvio.tv.ui.launcher.LauncherStyle.TileTitleIdle
    }

    val container = when {
        isError -> NuvioTheme.colors.Error.copy(alpha = if (isSelected) 0.07f else 0.05f)
        isSelected -> Color.White.copy(alpha = 0.30f)
        else -> Color.White.copy(alpha = 0.12f)
    }
    // Nuvio Launcher: a plain clickable surface (the TV FilterChip has a 36dp minimum height; the mockup is 32dp)
    androidx.tv.material3.Surface(
        onClick = { if (isSelectable) onClick() },
        modifier = modifier
            .graphicsLayer {
                alpha = alphaPulse.value
                translationX = shakeOffsetPx.value
            }
            .onFocusChanged {
                val nowFocused = it.isFocused
                isFocused = nowFocused
                if (nowFocused && !isSelected && isSelectable) {
                    onFocusSelect?.invoke()
                }
            },
        colors = androidx.tv.material3.ClickableSurfaceDefaults.colors(
            containerColor = container,
            focusedContainerColor = if (isError) NuvioTheme.colors.Error.copy(alpha = 0.09f) else com.nuvio.tv.ui.launcher.LauncherStyle.FocusWhite,
            contentColor = textColor,
            focusedContentColor = textColor
        ),
        border = androidx.tv.material3.ClickableSurfaceDefaults.border(
            border = if (isError) Border(BorderStroke(NuvioTheme.spacing.hairline, NuvioTheme.colors.Error.copy(alpha = 0.7f)), shape = RoundedCornerShape(20.dp)) else Border.None,
            focusedBorder = if (isError) Border(BorderStroke(NuvioTheme.spacing.xxs, NuvioTheme.colors.Error.copy(alpha = 0.8f)), shape = RoundedCornerShape(20.dp)) else Border.None
        ),
        scale = androidx.tv.material3.ClickableSurfaceDefaults.scale(focusedScale = 1f),
        shape = androidx.tv.material3.ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(20.dp))
    ) {
        Box(modifier = Modifier.height(32.dp).padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
            Text(
                text = name,
                modifier = Modifier.shimmer(isLoading),
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = 14.sp,
                    fontWeight = if (isFocused) FontWeight.Bold else FontWeight.Normal,
                    textDirection = name.contentTextDirection()
                ),
                color = textColor
            )
        }
    }
}

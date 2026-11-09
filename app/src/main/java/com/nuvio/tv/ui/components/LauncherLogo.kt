package com.nuvio.tv.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import com.nuvio.tv.R

/**
 * Nuvio Launcher: the launcher's logo (approved concept #10): "nuvio" with a gradient play symbol as the dot of the
 * i, and "LAUNCHER" under it. [width] sets the size; the height follows the image (691 x 393).
 */
@Composable
fun LauncherLogo(width: Dp, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.app_logo_wordmark),
        contentDescription = "Nuvio Launcher",
        contentScale = ContentScale.Fit,
        modifier = modifier.size(width = width, height = width * (393f / 691f))
    )
}

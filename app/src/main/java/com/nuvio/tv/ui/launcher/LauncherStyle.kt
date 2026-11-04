package com.nuvio.tv.ui.launcher

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Nuvio Launcher's shared look, used by Home, folders, sources, details, person pages and reviews:
 * the soft-white text, the grey ground, the blurred backdrop of whatever has focus, and the sidebar glass.
 */
object LauncherStyle {
    val Soft = Color(0xB8FFFFFF)            // the carousel synopsis's soft white (72%)
    val Dim = Color(0x8CFFFFFF)             // secondary lines (55%)
    val GreyTop = Color(0xFF2A2B30)
    val GreyBottom = Color(0xFF1E1F23)
    val FocusWhite = Color(0xF2F2F4F8)      // focused pill / card
    val FocusText = Color(0xFF16171B)
    val PillIdle = Color(0x1FFFFFFF)
    val PillSelected = Color(0x4DFFFFFF)
    val CardIdle = Color(0x14FFFFFF)
    val Edge = Color(0x2B0A0C10)   // thin outer edge of every glass panel (17%)
    val Corner = RoundedCornerShape(7.dp)
    val Gap = 29.dp
    val Start = 56.dp
    const val Zoom = 1.11f

    fun glassBrush(): Brush = Brush.linearGradient(
        0f to Color(0x24FFFFFF),
        0.45f to Color(0x08FFFFFF),
        1f to Color(0x2E141820)
    )
}

/**
 * The grey ground with [url]'s picture, blurred, over it. A small copy of the picture is loaded
 * (cheap to blur on the TV's chip), and changes wait a quarter second so holding Left/Right
 * doesn't flicker through every title.
 */
@Composable
fun LauncherBlurredBackdrop(url: String?, modifier: Modifier = Modifier, alpha: Float = 0.6f, debounceMs: Long = 250) {
    // base: the picture fully shown. incoming: the next one, drawn over it and faded in only once it has loaded,
    // so a change never dips to black first.
    var base by remember { mutableStateOf<String?>(null) }
    var incoming by remember { mutableStateOf<String?>(null) }
    val fade = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(url) {
        if (base == null) { base = url; return@LaunchedEffect }
        delay(debounceMs)
        if (url == base) { incoming = null; fade.snapTo(0f) } else { fade.snapTo(0f); incoming = url }
    }
    val context = LocalContext.current
    val layer = Modifier
        .fillMaxSize()
        .graphicsLayer { scaleX = 1.12f; scaleY = 1.12f }
        .blur(30.dp, BlurredEdgeTreatment.Rectangle)
    Box(modifier.fillMaxSize().background(Brush.verticalGradient(listOf(LauncherStyle.GreyTop, LauncherStyle.GreyBottom)))) {
        base?.let { u ->
            AsyncImage(
                model = remember(u) { ImageRequest.Builder(context).data(u).size(320, 180).build() },
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = layer.graphicsLayer { this.alpha = alpha }
            )
        }
        incoming?.let { u ->
            AsyncImage(
                model = remember(u) { ImageRequest.Builder(context).data(u).size(320, 180).build() },
                contentDescription = null,
                contentScale = ContentScale.Crop,
                onSuccess = {
                    scope.launch {
                        fade.animateTo(1f, tween(450))
                        if (incoming == u) { base = u; incoming = null; fade.snapTo(0f) }
                    }
                },
                modifier = layer.graphicsLayer { this.alpha = alpha * fade.value }
            )
        }
        // dark wash on the left and bottom so text stays readable
        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(0f to Color(0x8C141518), 0.6f to Color(0x26141518))))
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to Color(0x1A141518), 1f to Color(0x73141518))))
    }
}

/** The sidebar's glass: blur 11 when [hazeState] is given, sheen, a darker frame, thin edge and a soft shadow. */
@Composable
fun LauncherGlass(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    hazeState: HazeState? = null,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier
            .shadow(18.dp, shape, ambientColor = Color.Black, spotColor = Color.Black)   // soft two-layer look of the mockup
            .clip(shape)
            .then(if (hazeState != null) Modifier.hazeEffect(state = hazeState) { blurRadius = 11.dp; noiseFactor = 0f } else Modifier)
            // 1.4: one darkness for every launcher panel (sources, settings, review popup), matched by eye
            // to the review popup over the blurred page. Without blur (below Android 12) a near-solid frame.
            .background(Color(if (hazeState != null) 0x8F121316 else 0xE61C1D21), shape)
            .background(LauncherStyle.glassBrush(), shape)
            .border(1.dp, LauncherStyle.Edge, shape),
        content = content
    )
}

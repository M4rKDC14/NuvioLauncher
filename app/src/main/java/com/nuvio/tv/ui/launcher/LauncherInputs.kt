package com.nuvio.tv.ui.launcher

import android.content.Context
import android.content.Intent
import android.media.tv.TvContract
import android.media.tv.TvInputInfo
import android.media.tv.TvInputManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SettingsInputHdmi
import androidx.compose.material.icons.filled.Tv
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.nuvio.tv.LauncherChrome
import com.nuvio.tv.ui.screens.player.LauncherScaled

/**
 * Nuvio Launcher: the Inputs popup (approved mockup). Opened by the remote's Input button (through the Home-key
 * service) and by "Inputs" in the side panel. Shows the TV's HDMI ports and its tuner, with what is connected to
 * each port (the name the device gives over HDMI-CEC) and whether it is on (green dot) or in standby (grey dot);
 * ports with nothing connected are faded. Choosing one switches the TV to it through the TV's own TV app.
 */

/** Side panel entry route; it opens the popup instead of a page. */
const val LAUNCHER_INPUTS_ROUTE = "launcher_inputs"

/** Intent extra the Home-key service sets when the Input button is pressed. */
const val EXTRA_OPEN_INPUTS = "nuvio_open_inputs"

/** Intent extra the Home-key service sets when the Home button is pressed: go back to the home screen. */
const val EXTRA_GO_HOME = "nuvio_go_home"

/** Intent extra the Home-key service sets when the remote's gear button is pressed: open Quick settings. */
const val EXTRA_OPEN_QUICK_SETTINGS = "nuvio_open_quick_settings"

private data class LauncherInput(
    val id: String,
    val title: String,
    val subtitle: String,
    val isTuner: Boolean,
    val on: Boolean,        // connected device switched on: green dot
    val standby: Boolean,   // connected device in standby: grey dot
    val empty: Boolean      // nothing connected: faded
)

private const val STATE_CONNECTED = 0           // TvInputManager.INPUT_STATE_CONNECTED
private const val STATE_CONNECTED_STANDBY = 1   // TvInputManager.INPUT_STATE_CONNECTED_STANDBY
private const val STATE_DISCONNECTED = 2        // TvInputManager.INPUT_STATE_DISCONNECTED

/** Apps that register as TV inputs but are not sources you plug in (left out of the popup). */
private val NotSourcePackages = setOf("com.google.android.videos", "com.mediatek.AirplayAPK", "com.tcl.channelplus")

private fun loadInputs(context: Context): List<LauncherInput> {
    val tim = context.getSystemService(Context.TV_INPUT_SERVICE) as? TvInputManager ?: return emptyList()
    val all = runCatching { tim.tvInputList }.getOrDefault(emptyList())
    fun label(info: TvInputInfo): String =
        (info.loadCustomLabel(context) ?: info.loadLabel(context))?.toString().orEmpty()
    fun state(id: String): Int = runCatching { tim.getInputState(id) }.getOrDefault(STATE_CONNECTED)

    val hdmi = all.filter { it.type == TvInputInfo.TYPE_HDMI }
    // A port is the hardware input; a device seen over HDMI-CEC is a separate input whose parent is that port.
    val ports = hdmi.filter { it.parentId == null }
    val devices = hdmi.filter { it.parentId != null }.groupBy { it.parentId }

    val portTiles = ports.map { port ->
        val device = devices[port.id]?.firstOrNull()
        val deviceState = device?.let { state(it.id) }
        val portState = state(port.id)
        val s = deviceState ?: portState
        val name = device?.let { label(it) }.orEmpty()
        val empty = device == null && portState == STATE_DISCONNECTED
        LauncherInput(
            id = port.id,
            title = label(port).ifBlank { "HDMI" },
            subtitle = when {
                empty -> "Nothing connected"
                name.isNotBlank() && s == STATE_CONNECTED_STANDBY -> "$name · standby"
                name.isNotBlank() -> name
                else -> "Connected"
            },
            isTuner = false,
            on = !empty && s == STATE_CONNECTED && device != null,
            standby = !empty && s == STATE_CONNECTED_STANDBY,
            empty = empty
        )
    }.sortedBy { it.title }

    val tuner = all.firstOrNull { it.type == TvInputInfo.TYPE_TUNER && it.serviceInfo.packageName !in NotSourcePackages }
    val tunerTile = tuner?.let {
        LauncherInput(it.id, "Live TV", label(it), isTuner = true, on = false, standby = false, empty = false)
    }
    return portTiles + listOfNotNull(tunerTile)
}

private fun openInput(context: Context, input: LauncherInput) {
    val uri = if (input.isTuner) TvContract.buildChannelsUriForInput(input.id)
    else TvContract.buildChannelUriForPassthroughInput(input.id)
    val intent = Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    val ok = runCatching { context.startActivity(intent) }.isSuccess ||
        (input.isTuner && runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, TvContract.Channels.CONTENT_URI).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }.isSuccess)
    if (!ok) Toast.makeText(context, "Could not open ${input.title}", Toast.LENGTH_SHORT).show()
}

/** Put once at the root of the app: shows the popup while [LauncherChrome.inputsOpen] is true. */
@Composable
fun LauncherInputsHost() {
    val open = LauncherChrome.inputsOpen.value
    AnimatedVisibility(visible = open, enter = fadeIn(tween(180)), exit = fadeOut(tween(150))) {
        // the bottom of the screen dims behind the popup
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(0.3f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.55f)))
        )
    }
    AnimatedVisibility(
        visible = open,
        enter = fadeIn(tween(180)) + slideInVertically(tween(220)) { it / 6 },
        exit = fadeOut(tween(150)) + slideOutVertically(tween(180)) { it / 6 },
        modifier = Modifier.fillMaxSize()
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            InputsPopup(onClose = { LauncherChrome.inputsOpen.value = false })
        }
    }
}

/**
 * Sizes are the approved mockup's before its 80% scale (drawn at 80%): panel 960dp wide (768dp = 80% of the screen),
 * 20dp above the bottom (16dp drawn), 24dp corners, 20/24/24dp inside; "Inputs" 18sp bold white, 16dp to the tiles;
 * tiles 166.4x93.6dp, 20dp apart, 7dp corners, zoom 1.11 when focused.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun InputsPopup(onClose: () -> Unit) {
    val context = LocalContext.current
    val inputs = remember { loadInputs(context) }
    val first = remember { FocusRequester() }
    BackHandler(onBack = onClose)
    LaunchedEffect(inputs) { if (inputs.isNotEmpty()) runCatching { first.requestFocus() } }

    LauncherScaled(0.8f) {
        LauncherGlass(
            modifier = Modifier
                .padding(bottom = 20.dp)
                .width(960.dp),
            shape = RoundedCornerShape(24.dp),
            tint = Color(0xD1121316)   // .82, as the mockup
        ) {
            Column(Modifier.padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 24.dp)) {
                Text("Inputs", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(Modifier.height(16.dp))
                if (inputs.isEmpty()) {
                    Text("No inputs found on this TV", fontSize = 12.sp, color = LauncherStyle.TileTitleIdle)
                } else {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                        // Up/Down have nowhere to go inside the popup
                        modifier = Modifier.onPreviewKeyEvent { it.key == Key.DirectionUp || it.key == Key.DirectionDown }
                    ) {
                        inputs.forEachIndexed { i, input ->
                            InputTile(
                                input = input,
                                modifier = if (i == 0) Modifier.focusRequester(first) else Modifier,
                                onClick = {
                                    onClose()
                                    openInput(context, input)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun InputTile(input: LauncherInput, modifier: Modifier, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val zoom by animateFloatAsState(if (focused) LauncherStyle.Zoom else 1f, tween(160), label = "inputZoom")
    val shape = RoundedCornerShape(7.dp)
    Box(
        Modifier
            .graphicsLayer { scaleX = zoom; scaleY = zoom }
            .shadow(if (focused) 9.dp else 4.dp, shape, ambientColor = Color.Black, spotColor = Color.Black)
            .alpha(if (input.empty) 0.55f else 1f)
    ) {
        Surface(
            onClick = onClick,
            modifier = modifier
                .size(width = 166.4.dp, height = 93.6.dp)
                .onFocusChanged { focused = it.isFocused },
            colors = ClickableSurfaceDefaults.colors(containerColor = Color.Transparent, focusedContainerColor = Color.Transparent),
            scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
            shape = ClickableSurfaceDefaults.shape(shape)
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Brush.linearGradient(listOf(Color(0xFF3B3D45), Color(0xFF24252B))))
            ) {
                Icon(
                    imageVector = if (input.isTuner) Icons.Default.Tv else Icons.Default.SettingsInputHdmi,
                    contentDescription = null,
                    tint = Color(0xFFC9CAD0),
                    modifier = Modifier.padding(start = 8.dp, top = 7.dp).size(20.dp)
                )
                if (input.on || input.standby) {
                    Box(
                        Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 9.dp, end = 9.dp)
                            .size(7.dp)
                            .background(if (input.on) Color(0xFF34C759) else LauncherStyle.TileTitleIdle, CircleShape)
                    )
                }
                Column(Modifier.align(Alignment.BottomStart).padding(start = 8.dp, end = 8.dp, bottom = 7.dp)) {
                    Text(input.title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(input.subtitle, fontSize = 9.sp, color = LauncherStyle.TileTitleIdle, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

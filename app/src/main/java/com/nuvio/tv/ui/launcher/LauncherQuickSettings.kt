package com.nuvio.tv.ui.launcher

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.text.format.DateFormat
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
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
import com.nuvio.tv.data.local.LauncherSettingsHolder
import com.nuvio.tv.launcherhome.LauncherNotification
import com.nuvio.tv.launcherhome.LauncherNotificationService
import com.nuvio.tv.ui.screens.player.LauncherScaled
import kotlinx.coroutines.delay
import java.util.Date
import java.util.Locale

/** Side panel entry route; it opens the Quick settings panel instead of a page. */
const val LAUNCHER_QUICK_SETTINGS_ROUTE = "launcher_quick_settings"

private val QsGrey = Color(0xFF8E8E93)
private val QsGreyDark = Color(0xFF6E6E73)
private val QsIdleFill = Color(0x1FFFFFFF)      // white .12
private val QsDivider = Color(0x1AFFFFFF)       // white .10
private val QsFocusFill = Color(0xF2F2F4F8)     // .95, as the mockup

/** One tile of the grid: label, and what pressing it does. */
private class QsTile(val label: String, val onClick: () -> Unit)

/**
 * Nuvio Launcher: the Quick settings panel (side panel → Quick settings), laid out like Google TV's own panel in the
 * launcher's look. Over whatever page is open; Back closes it.
 */
@Composable
fun LauncherQuickSettingsHost(
    profileName: String?,
    profileColorHex: String?,
    onStartAmbient: () -> Unit,
    onSwitchProfile: () -> Unit
) {
    val open = LauncherChrome.quickSettingsOpen.value
    AnimatedVisibility(visible = open, enter = fadeIn(tween(180)), exit = fadeOut(tween(150))) {
        // the right of the screen dims behind the panel
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.horizontalGradient(0.4f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.55f)))
        )
    }
    AnimatedVisibility(
        visible = open,
        enter = fadeIn(tween(180)) + slideInHorizontally(tween(220)) { it / 6 },
        exit = fadeOut(tween(150)) + slideOutHorizontally(tween(180)) { it / 6 },
        modifier = Modifier.fillMaxSize()
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopEnd) {
            QuickSettingsPanel(
                profileName = profileName,
                profileColorHex = profileColorHex,
                onClose = { LauncherChrome.quickSettingsOpen.value = false },
                onStartAmbient = onStartAmbient,
                onSwitchProfile = onSwitchProfile
            )
        }
    }
}

/**
 * Sizes are the approved mockup's before its 80% scale (drawn at 80%): panel 306dp wide (244.8dp drawn), 30dp from
 * the top, right and bottom (24dp drawn), 24dp corners and padding. Date 13sp grey, time 32sp bold; round buttons
 * 40dp with 10sp labels. Tiles in two columns 8.5dp apart, 71.16dp high, 11.9dp corners, label 11.05sp semibold at the
 * top left (8.5dp from the top, 10dp from the side): grey on white .12, dark on white .95 (zoom 1.04) when focused.
 * Notifications below: one row each between thin lines, a grey dot, title 11.05sp and text 9.35sp; the list is
 * 150dp high, scrolls, and fades out at the bottom. "Back to close" 12sp at the bottom right.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun QuickSettingsPanel(
    profileName: String?,
    profileColorHex: String?,
    onClose: () -> Unit,
    onStartAmbient: () -> Unit,
    onSwitchProfile: () -> Unit
) {
    val context = LocalContext.current
    val launcherSettings by LauncherSettingsHolder.settings.collectAsState()
    val notifications by LauncherNotificationService.items.collectAsState()
    val first = remember { FocusRequester() }
    BackHandler(onBack = onClose)
    LaunchedEffect(Unit) { runCatching { first.requestFocus() } }

    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1_000L - now % 1_000L)
        }
    }
    val uses24Hour = launcherSettings.clock24Hour ?: DateFormat.is24HourFormat(context)
    val dateText = remember(now / 60_000L) {
        java.text.SimpleDateFormat("EEE, MMM dd", Locale.getDefault()).format(Date(now))
    }
    val timeText = remember(now / 1_000L, uses24Hour) {
        java.text.SimpleDateFormat(if (uses24Hour) "HH:mm" else "h:mm a", Locale.getDefault()).format(Date(now))
    }

    fun open(action: String) {
        onClose()
        openSystemSettings(context, action)
    }

    val tiles = listOf(
        QsTile("Ambient mode") { onClose(); onStartAmbient() },
        QsTile("Inputs") { onClose(); LauncherChrome.inputsOpen.value = true },
        QsTile("Picture") { open(android.provider.Settings.ACTION_DISPLAY_SETTINGS) },
        QsTile("Sound") { open(android.provider.Settings.ACTION_SOUND_SETTINGS) },
        QsTile("Wi-Fi") { open(android.provider.Settings.ACTION_WIFI_SETTINGS) },
        QsTile("Bluetooth") { open("com.android.tv.settings.ACCESSORIES_CONNECTED_DEVICES") },
        QsTile("Accessibility") { open(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS) },
        QsTile("Energy saving") { open("com.android.tv.settings.action.ENERGY_SAVING") }
    )

    LauncherScaled(0.8f) {
        LauncherGlass(
            modifier = Modifier
                .padding(top = 30.dp, end = 30.dp, bottom = 30.dp)
                .width(306.dp)
                .fillMaxHeight(),
            shape = RoundedCornerShape(24.dp),
            tint = Color(0xD1121316)   // .82, as the mockup
        ) {
            Column(Modifier.fillMaxSize().padding(24.dp)) {
                // ---- date, time, All settings, Profile ----
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f)) {
                        Text(dateText, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = QsGrey, maxLines = 1)
                        Spacer(Modifier.height(3.dp))
                        Text(
                            timeText,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp,
                            color = Color.White,
                            maxLines = 1
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        QsRoundButton(label = "All settings", onClick = { open(android.provider.Settings.ACTION_SETTINGS) }) { focused ->
                            Icon(
                                imageVector = Icons.Outlined.Settings,
                                contentDescription = null,
                                tint = if (focused) LauncherStyle.FocusText else Color(0xFFE6E7EA),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        QsRoundButton(
                            label = "Profile",
                            fill = profileColorHex?.let { hex -> runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrNull() },
                            onClick = { onClose(); onSwitchProfile() }
                        ) { focused ->
                            Text(
                                text = profileName?.trim()?.firstOrNull()?.uppercase() ?: "?",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (focused) LauncherStyle.FocusText else Color.White
                            )
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))

                // ---- the tiles, two per row ----
                Column(verticalArrangement = Arrangement.spacedBy(8.5.dp)) {
                    tiles.chunked(2).forEachIndexed { rowIndex, pair ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.5.dp)) {
                            pair.forEachIndexed { colIndex, tile ->
                                QsTileButton(
                                    tile = tile,
                                    modifier = Modifier
                                        .weight(1f)
                                        .then(if (rowIndex == 0 && colIndex == 0) Modifier.focusRequester(first) else Modifier)
                                )
                            }
                        }
                    }
                }

                // ---- notifications: one row each, scrolls, fades out at the bottom ----
                if (notifications.isNotEmpty()) {
                    Spacer(Modifier.height(17.dp))
                    Box(Modifier.fillMaxWidth().height(0.75.dp).background(QsDivider))
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                            .drawWithContent {
                                drawContent()
                                drawRect(
                                    brush = Brush.verticalGradient(0.72f to Color.Black, 1f to Color.Transparent),
                                    blendMode = BlendMode.DstIn
                                )
                            }
                    ) {
                        items(notifications, key = { it.key }) { item ->
                            QsNotificationRow(item = item, onClick = {
                                onClose()
                                runCatching { item.contentIntent?.send() }
                            })
                        }
                    }
                }

                Spacer(Modifier.weight(1f))
                Text(
                    "Back to close",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = QsGrey,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}

/** A round 40dp button with a 10sp label under it (All settings, Profile). */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun QsRoundButton(
    label: String,
    onClick: () -> Unit,
    fill: Color? = null,
    content: @Composable (focused: Boolean) -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            onClick = onClick,
            modifier = Modifier
                .size(40.dp)
                .onFocusChanged { focused = it.isFocused },
            colors = ClickableSurfaceDefaults.colors(
                containerColor = fill ?: QsIdleFill,
                focusedContainerColor = QsFocusFill
            ),
            scale = ClickableSurfaceDefaults.scale(focusedScale = 1.04f),
            shape = ClickableSurfaceDefaults.shape(CircleShape)
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { content(focused) }
        }
        Spacer(Modifier.height(5.dp))
        Text(label, fontSize = 10.sp, color = QsGrey, maxLines = 1)
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun QsTileButton(tile: QsTile, modifier: Modifier) {
    var focused by remember { mutableStateOf(false) }
    val zoom by animateFloatAsState(if (focused) 1.04f else 1f, tween(160), label = "qsTileZoom")
    val shape = RoundedCornerShape(11.9.dp)
    Box(
        modifier
            .graphicsLayer { scaleX = zoom; scaleY = zoom }
            .then(if (focused) Modifier.shadow(6.dp, shape, ambientColor = Color.Black, spotColor = Color.Black) else Modifier)
    ) {
        Surface(
            onClick = tile.onClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(71.16.dp)
                .onFocusChanged { focused = it.isFocused },
            colors = ClickableSurfaceDefaults.colors(containerColor = QsIdleFill, focusedContainerColor = QsFocusFill),
            scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
            shape = ClickableSurfaceDefaults.shape(shape)
        ) {
            Text(
                text = tile.label,
                fontSize = 11.05.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (focused) LauncherStyle.FocusText else QsGrey,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 10.dp, end = 10.dp, top = 8.5.dp)
            )
        }
    }
}

/** One notification: a grey dot, title and text, with a thin line under it. */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun QsNotificationRow(item: LauncherNotification, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Column {
        Surface(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focused = it.isFocused },
            colors = ClickableSurfaceDefaults.colors(containerColor = Color.Transparent, focusedContainerColor = QsFocusFill),
            scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
            shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp))
        ) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 3.dp, vertical = 10.dp)) {
                Box(
                    Modifier
                        .padding(top = 5.dp)
                        .size(5.dp)
                        .background(if (focused) LauncherStyle.FocusText else QsGrey, CircleShape)
                )
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(
                        item.title,
                        fontSize = 11.05.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (focused) LauncherStyle.FocusText else QsGrey,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (item.text.isNotEmpty()) {
                        Spacer(Modifier.height(2.5.dp))
                        Text(
                            item.text,
                            fontSize = 9.35.sp,
                            color = if (focused) LauncherStyle.FocusText.copy(alpha = 0.65f) else QsGreyDark,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
        Box(Modifier.fillMaxWidth().height(0.75.dp).background(QsDivider))
    }
}

/** Opens a TV settings screen; falls back to the settings home if this TV has no such screen. */
private fun openSystemSettings(context: Context, action: String) {
    val flags = Intent.FLAG_ACTIVITY_NEW_TASK
    try {
        context.startActivity(Intent(action).addFlags(flags))
    } catch (_: ActivityNotFoundException) {
        runCatching { context.startActivity(Intent(android.provider.Settings.ACTION_SETTINGS).addFlags(flags)) }
    } catch (_: SecurityException) {
        runCatching { context.startActivity(Intent(android.provider.Settings.ACTION_SETTINGS).addFlags(flags)) }
    }
}

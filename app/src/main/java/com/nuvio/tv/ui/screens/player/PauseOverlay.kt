package com.nuvio.tv.ui.screens.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.runtime.collectAsState
import com.nuvio.tv.ui.theme.NuvioTheme

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import androidx.compose.ui.platform.LocalContext
import com.nuvio.tv.domain.model.MetaCastMember
import android.text.format.DateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay
import androidx.compose.ui.res.stringResource
import com.nuvio.tv.R
import com.nuvio.tv.ui.util.contentTextDirection
import com.nuvio.tv.ui.util.localizeEpisodeTitle
import com.nuvio.tv.ui.util.toAbsoluteAlignment

@Composable
fun PauseOverlay(
    visible: Boolean,
    onClose: () -> Unit,
    title: String,
    logo: String?,
    episodeTitle: String?,
    season: Int?,
    episode: Int?,
    year: String?,
    type: String?,
    description: String?,
    cast: List<MetaCastMember>,
    modifier: Modifier = Modifier,
    showClock: Boolean = true,
    // Nuvio Launcher: the playback position, for "36:52 of 1:37:11", the progress bar and "Ends at"
    timeline: kotlinx.coroutines.flow.StateFlow<PlaybackTimelineState>? = null
) {
    var selectedCastMember by remember { mutableStateOf<MetaCastMember?>(null) }

    PlayerOverlayScaffold(
        visible = visible,
        onDismiss = onClose,
        modifier = modifier,
        captureKeys = false,
        dismissOnBackgroundClick = true,
        launcherShade = true,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = NuvioTheme.spacing.huge,
            end = NuvioTheme.spacing.huge,
            top = 40.dp,
            bottom = 120.dp
        ),
        topEndContent = {
            if (showClock) {
                // same place as the clock during playback: 32dp from the right, 20dp from the top (this box sits
                // inside the pause screen's 56dp / 40dp padding)
                PauseOverlayClock(modifier = Modifier.offset(x = 24.dp, y = (-20).dp), timeline = timeline)
            }
        }
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Bottom
        ) {
            if (selectedCastMember != null) {
                CastDetailView(
                    member = selectedCastMember!!,
                    onBack = { selectedCastMember = null }
                )
            } else {
                PauseMetadataView(
                    title = title,
                    logo = logo,
                    episodeTitle = episodeTitle,
                    season = season,
                    episode = episode,
                    year = year,
                    type = type,
                    description = description,
                    cast = cast,
                    onCastSelected = { selectedCastMember = it },
                    timeline = timeline
                )
            }
        }
    }
}

@Composable
private fun PauseOverlayClock(
    modifier: Modifier = Modifier,
    timeline: kotlinx.coroutines.flow.StateFlow<PlaybackTimelineState>? = null
) {
    var nowMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    val context = LocalContext.current
    val formatter = remember(context) { DateFormat.getTimeFormat(context) }

    LaunchedEffect(Unit) {
        while (true) {
            val current = System.currentTimeMillis()
            nowMillis = current
            val delayMs = (60_000L - (current % 60_000L)).coerceAtLeast(1_000L)
            delay(delayMs)
        }
    }

    // Nuvio Launcher: same as the clock during playback: 13sp semibold time, 10sp grey "Ends at" under it
    val t = timeline?.collectAsState()?.value
    Column(modifier = modifier, horizontalAlignment = Alignment.End) {
        Text(
            text = formatter.format(Date(nowMillis)),
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
            color = Color.White.copy(alpha = 0.96f)
        )
        if (t != null && !t.isLive && t.duration > 0L) {
            Text(
                text = stringResource(R.string.player_ends_at, formatter.format(Date(nowMillis + (t.duration - t.currentPosition).coerceAtLeast(0L)))),
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 10.sp),
                color = com.nuvio.tv.ui.launcher.LauncherStyle.TileTitleIdle
            )
        }
    }
}

@Composable
private fun PauseMetadataView(
    title: String,
    logo: String?,
    episodeTitle: String?,
    season: Int?,
    episode: Int?,
    year: String?,
    type: String?,
    description: String?,
    cast: List<MetaCastMember>,
    onCastSelected: (MetaCastMember) -> Unit,
    timeline: kotlinx.coroutines.flow.StateFlow<PlaybackTimelineState>? = null
) {
    val t = timeline?.collectAsState()?.value
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Bottom
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Nuvio Launcher (approved mockup): a small grey "PAUSED" label
            Text(
                text = "PAUSED",
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 9.35.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.275.sp),   // all of this block: -15%
                color = com.nuvio.tv.ui.launcher.LauncherStyle.TileTitleIdle
            )

            Spacer(modifier = Modifier.height(10.2.dp))

            if (!logo.isNullOrBlank()) {
                var logoFailed by remember(logo) { mutableStateOf(false) }
                if (!logoFailed) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(logo)
                            .memoryCacheKey(logo)
                            .crossfade(true)
                            .build(),
                        contentDescription = title,
                        contentScale = ContentScale.Fit,
                        alignment = Alignment.BottomStart,
                        modifier = Modifier.height(59.5.dp),
                        onError = { logoFailed = true }
                    )
                } else {
                    Text(
                        text = title,
                        modifier = Modifier.align(title.contentTextDirection().toAbsoluteAlignment()),
                        style = MaterialTheme.typography.headlineLarge.let { it.copy(fontSize = it.fontSize * 0.85f, lineHeight = it.lineHeight * 0.85f) },
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            } else {
                Text(
                    text = title,
                    modifier = Modifier.align(title.contentTextDirection().toAbsoluteAlignment()),
                    style = MaterialTheme.typography.headlineLarge.let { it.copy(fontSize = it.fontSize * 0.85f, lineHeight = it.lineHeight * 0.85f) },
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Nuvio Launcher: year · Season · Episode · where you are, in grey
            run {
                val parts = buildList {
                    if (!year.isNullOrBlank()) add(year)
                    if (type in listOf("series", "tv") && season != null && episode != null) {
                        add("Season $season"); add("Episode $episode")
                    }
                    if (t != null && !t.isLive && t.duration > 0L) add("${formatTime(t.currentPosition)} of ${formatTime(t.duration)}")
                }
                if (parts.isNotEmpty()) Text(
                    text = parts.joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 10.2.sp),
                    color = com.nuvio.tv.ui.launcher.LauncherStyle.TileTitleIdle,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            if (!episodeTitle.isNullOrBlank()) {
                val context = LocalContext.current
                Text(
                    text = episodeTitle.localizeEpisodeTitle(context),
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 15.3.sp, fontWeight = FontWeight.Bold),
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 9.4.dp)
                )
            }

            if (!description.isNullOrBlank()) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 13.6.sp,
                        lineHeight = 20.4.sp,
                        textDirection = description.contentTextDirection()
                    ),
                    color = com.nuvio.tv.ui.launcher.LauncherStyle.TileTitleIdle,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 7.7.dp).widthIn(max = 408.dp)
                )
            }

            // Nuvio Launcher: a thin progress bar under the synopsis
            if (t != null && !t.isLive && t.duration > 0L) {
                Box(
                    Modifier.padding(top = 12.8.dp).width(306.dp).height(2.6.dp)
                        .clip(RoundedCornerShape(2.dp)).background(Color.White.copy(alpha = 0.25f))
                ) {
                    Box(
                        Modifier.fillMaxWidth((t.currentPosition.toFloat() / t.duration).coerceIn(0f, 1f))
                            .height(2.6.dp).background(Color.White)
                    )
                }
            }

            if (cast.isNotEmpty()) {
                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = stringResource(R.string.pause_cast_label),
                    style = MaterialTheme.typography.titleSmall.copy(fontSize = 11.sp),
                    color = com.nuvio.tv.ui.launcher.LauncherStyle.TileTitleIdle
                )

                Spacer(modifier = Modifier.height(NuvioTheme.spacing.md))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    content = {
                        items(cast.take(8)) { member ->
                            CastChip(member = member, onClick = { onCastSelected(member) })
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun CastChip(
    member: MetaCastMember,
    onClick: () -> Unit
) {
    // Nuvio Launcher: the launcher pill: grey when idle, white with dark bold text and 8% bigger when focused
    var focused by remember { mutableStateOf(false) }
    Card(
        onClick = onClick,
        modifier = Modifier.onFocusChanged { focused = it.isFocused },
        colors = CardDefaults.colors(
            containerColor = Color.White.copy(alpha = 0.12f),
            focusedContainerColor = com.nuvio.tv.ui.launcher.LauncherStyle.FocusWhite
        ),
        scale = CardDefaults.scale(focusedScale = 1.08f),
        shape = CardDefaults.shape(shape = RoundedCornerShape(50))
    ) {
        Text(
            text = member.name,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp, fontWeight = if (focused) FontWeight.Bold else FontWeight.Normal),
            color = if (focused) com.nuvio.tv.ui.launcher.LauncherStyle.FocusText else com.nuvio.tv.ui.launcher.LauncherStyle.TileTitleIdle,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun CastDetailView(
    member: MetaCastMember,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onBack)
                .semantics { role = Role.Button }
                .padding(bottom = 28.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null,
                tint = NuvioTheme.colors.TextSecondary,
                modifier = Modifier.size(NuvioTheme.spacing.xl)
            )
            Spacer(modifier = Modifier.width(NuvioTheme.spacing.md))
            Text(
                text = stringResource(R.string.pause_back_to_details),
                style = MaterialTheme.typography.bodyMedium,
                color = NuvioTheme.colors.TextSecondary
            )
        }

        Row(
            verticalAlignment = Alignment.Top
        ) {
            if (!member.photo.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(member.photo)
                        .crossfade(true)
                        .build(),
                    contentDescription = member.name,
                    modifier = Modifier
                        .size(width = 160.dp, height = 240.dp)
                        .clip(RoundedCornerShape(NuvioTheme.radii.xl)),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.width(28.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = member.name,
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (!member.character.isNullOrBlank()) {
                    Text(
                        text = stringResource(R.string.pause_as_character, member.character ?: ""),
                        style = MaterialTheme.typography.bodyLarge,
                        color = NuvioTheme.colors.TextSecondary,
                        modifier = Modifier.padding(top = NuvioTheme.spacing.sm),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

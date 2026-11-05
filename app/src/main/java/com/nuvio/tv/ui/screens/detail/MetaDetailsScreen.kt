package com.nuvio.tv.ui.screens.detail

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.nuvio.tv.ui.launcher.LauncherBlurredBackdrop
import com.nuvio.tv.ui.launcher.LauncherStyle
import com.nuvio.tv.ui.components.LocalPlaybackAvailability
import com.nuvio.tv.ui.navigation.LocalDetailChildClosedEpoch
import com.nuvio.tv.ui.navigation.LocalDetailChildOverlayVisible
import android.widget.Toast

import com.nuvio.tv.ui.theme.NuvioTheme
import com.nuvio.tv.ui.theme.NuvioMotion

import android.view.KeyEvent
import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.relocation.BringIntoViewResponder
import androidx.compose.foundation.relocation.bringIntoViewResponder
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.Stable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.runtime.State
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import com.nuvio.tv.R
import com.nuvio.tv.ui.util.dpadVerticalFastScroll
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import com.nuvio.tv.ui.util.localizedGenreLabel
import com.nuvio.tv.ui.util.recompositionHighlighter
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListPrefetchStrategy
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.draw.clip
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import coil3.imageLoader
import coil3.memory.MemoryCache
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.request.allowHardware
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import com.nuvio.tv.domain.model.ContentType
import com.nuvio.tv.domain.model.DetailImdbRatingsVisibility
import com.nuvio.tv.domain.model.EpisodeOptionsOverlayStyle
import com.nuvio.tv.domain.model.HomeImdbRatingsVisibility
import com.nuvio.tv.domain.model.LibraryListTab
import com.nuvio.tv.domain.model.localizedMembershipTitle
import com.nuvio.tv.domain.model.LibrarySourceMode
import com.nuvio.tv.domain.model.Meta
import com.nuvio.tv.domain.model.MetaCastMember
import com.nuvio.tv.domain.model.MetaPreview
import com.nuvio.tv.domain.model.MetaTrailer
import com.nuvio.tv.domain.model.resolveContentLanguage
import com.nuvio.tv.domain.model.MDBListRatings
import com.nuvio.tv.domain.model.NextToWatch
import com.nuvio.tv.domain.model.TraktCommentReview
import com.nuvio.tv.domain.model.Video
import com.nuvio.tv.domain.model.WatchProgress
import com.nuvio.tv.ui.components.ErrorState
import com.nuvio.tv.ui.components.MetaDetailsSkeleton
import com.nuvio.tv.ui.components.NuvioDialog
import com.nuvio.tv.ui.components.PlayManualOverrideDialog
import com.nuvio.tv.ui.components.SynopsisOverlay
import com.nuvio.tv.ui.components.TrailerPlayer
import com.nuvio.tv.ui.components.posteroptions.TrackingRemovalConfirmationDialog
import com.nuvio.tv.core.tracking.LOCAL_LIBRARY_LIST_KEY
import com.nuvio.tv.core.tracking.supportsMembershipFor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

private enum class RestoreTarget {
    HERO,
    EPISODE,
    CAST_MEMBER,
    MORE_LIKE_THIS,
    TRAILER,
    COLLECTION,
    COMPANY_OR_NETWORK
}

private class DetailTapScroll {
    var rapidDown: Boolean = false
    var lastKey: Int = 0
    var lastDownAt: Long = 0L
}

private const val RAPID_DOWN_TAP_MS = 280L

// Nuvio Launcher: the rows under the episodes, stacked one under another (each with its own title),
// in page order. The values are the LazyColumn item keys.
private const val ROW_CAST = "cast_row"
private const val ROW_RATINGS = "ratings_row"
private const val ROW_MORE_LIKE_THIS = "more_like_this_row"
private const val ROW_TRAILERS = "trailers_row"
private const val ROW_COLLECTION = "collection_section"
private const val ROW_COMMENTS = "trakt_comments"
private const val ROW_NETWORKS = "networks"
private const val ROW_PRODUCTION = "production"

// Titles the owner asked for that have no string resource yet (strings live outside detail/).
private const val CAST_AND_CREW_TITLE = "Cast & Crew"
private const val PRODUCTION_COMPANIES_TITLE = "Production Companies"

private data class DetailReturnEpisodeFocusRequest(
    val season: Int?,
    val episode: Int?
)

private fun resolveDetailReturnEpisodeFocusTarget(
    meta: Meta,
    request: DetailReturnEpisodeFocusRequest?
): Video? {
    val requestedSeason = request?.season ?: return null
    val requestedEpisode = request.episode ?: return null

    val orderedEpisodes = meta.videos
        .filter { it.season != null && it.episode != null }
        .sortedWith(compareBy({ it.season }, { it.episode }))
    if (orderedEpisodes.isEmpty()) return null

    val matchedIndex = orderedEpisodes.indexOfFirst {
        it.season == requestedSeason && it.episode == requestedEpisode
    }
    if (matchedIndex < 0) return null

    return orderedEpisodes[matchedIndex]
}

internal fun resolveVisibleEpisodeRestoreId(
    requestedId: String?,
    episodesForSeason: List<Video>,
    nextVideoId: String?
): String? {
    requestedId?.let { id ->
        if (episodesForSeason.any { it.id == id }) return id
    }
    nextVideoId?.let { id ->
        if (episodesForSeason.any { it.id == id }) return id
    }
    return episodesForSeason.firstOrNull()?.id
}

internal fun resolveReturnFocusSeason(
    playedSeason: Int?,
    selectedSeason: Int,
    nextSeason: Int?,
    availableSeasons: Collection<Int>,
    hasWaitedForSeasonAdvance: Boolean = false
): Int? {
    val next = nextSeason?.takeIf { it in availableSeasons }
    val played = playedSeason?.takeIf { it in availableSeasons }
    val selected = selectedSeason.takeIf { it in availableSeasons }
    return when {
        next != null && played != null && next > played -> next
        hasWaitedForSeasonAdvance &&
            selected != null &&
            played != null &&
            selected > played -> selected
        played != null -> played
        next != null -> next
        else -> selected
    }
}

internal fun isLastEpisodeOfSeason(
    allVideos: List<Video>,
    season: Int?,
    episode: Int?
): Boolean {
    if (season == null || episode == null) return false
    val lastEpisode = allVideos
        .filter { it.season == season }
        .mapNotNull { it.episode }
        .maxOrNull() ?: return false
    return episode >= lastEpisode
}

internal fun hasLaterAvailableSeason(
    playedSeason: Int?,
    availableSeasons: Collection<Int>
): Boolean = playedSeason != null && availableSeasons.any { it > playedSeason }

internal fun shouldWaitForReturnFocusSeasonAdvance(
    playedSeason: Int?,
    playedEpisode: Int?,
    nextSeason: Int?,
    allVideos: List<Video>,
    availableSeasons: Collection<Int>
): Boolean {
    if (!isLastEpisodeOfSeason(allVideos, playedSeason, playedEpisode)) return false
    if (!hasLaterAvailableSeason(playedSeason, availableSeasons)) return false
    return nextSeason == null || (playedSeason != null && nextSeason <= playedSeason)
}

internal sealed class ReturnFocusStep {
    data object WaitForSeasonAdvance : ReturnFocusStep()
    data class SelectSeason(val season: Int) : ReturnFocusStep()
    data class RestoreEpisode(val episodeId: String, val consumeRequest: Boolean) : ReturnFocusStep()
    data object Idle : ReturnFocusStep()
}

internal fun resolveImmediateNextEpisodeId(
    allVideos: List<Video>,
    fromEpisodeId: String?,
    nextVideoId: String?
): String? {
    if (fromEpisodeId == null || nextVideoId == null || fromEpisodeId == nextVideoId) return null
    val ordered = allVideos
        .filter { it.season != null && it.episode != null }
        .sortedWith(compareBy({ it.season }, { it.episode }))
    val index = ordered.indexOfFirst { it.id == fromEpisodeId }
    if (index < 0 || index + 1 >= ordered.size) return null
    return ordered[index + 1].id.takeIf { it == nextVideoId }
}

internal fun resolveReturnFocusStep(
    playedSeason: Int?,
    playedEpisode: Int?,
    selectedSeason: Int,
    nextSeason: Int?,
    availableSeasons: Collection<Int>,
    allVideos: List<Video>,
    requestedEpisodeId: String?,
    episodesForSeason: List<Video>,
    nextVideoId: String?,
    alreadyRestoredId: String?,
    hasWaitedForSeasonAdvance: Boolean
): ReturnFocusStep {
    val advancedNextId = resolveImmediateNextEpisodeId(
        allVideos = allVideos,
        fromEpisodeId = requestedEpisodeId,
        nextVideoId = nextVideoId
    )
    val focusEpisodeId = advancedNextId ?: requestedEpisodeId
    val focusVideo = focusEpisodeId?.let { id -> allVideos.firstOrNull { it.id == id } }
    val focusSeason = focusVideo?.season ?: playedSeason
    val focusEpisode = focusVideo?.episode ?: playedEpisode

    val waitingForAdvance = advancedNextId == null &&
        shouldWaitForReturnFocusSeasonAdvance(
            playedSeason = playedSeason,
            playedEpisode = playedEpisode,
            nextSeason = nextSeason,
            allVideos = allVideos,
            availableSeasons = availableSeasons
        )
    val bingeAheadOfProgress = nextSeason != null &&
        playedSeason != null &&
        nextSeason < playedSeason
    if (waitingForAdvance && !hasWaitedForSeasonAdvance && !bingeAheadOfProgress) {
        return ReturnFocusStep.WaitForSeasonAdvance
    }

    val exitIsFinale = isLastEpisodeOfSeason(allVideos, focusSeason, focusEpisode)
    val seasonToShow = when {
        advancedNextId != null ->
            focusSeason?.takeIf { it in availableSeasons }
        requestedEpisodeId != null && !exitIsFinale ->
            playedSeason?.takeIf { it in availableSeasons }
                ?: resolveReturnFocusSeason(
                    playedSeason = playedSeason,
                    selectedSeason = selectedSeason,
                    nextSeason = nextSeason,
                    availableSeasons = availableSeasons,
                    hasWaitedForSeasonAdvance = hasWaitedForSeasonAdvance
                )
        else -> resolveReturnFocusSeason(
            playedSeason = playedSeason,
            selectedSeason = selectedSeason,
            nextSeason = nextSeason,
            availableSeasons = availableSeasons,
            hasWaitedForSeasonAdvance = hasWaitedForSeasonAdvance
        )
    }
    if (seasonToShow != null && seasonToShow != selectedSeason) {
        return ReturnFocusStep.SelectSeason(seasonToShow)
    }

    val restoreEpisodeId = resolveVisibleEpisodeRestoreId(
        requestedId = focusEpisodeId,
        episodesForSeason = episodesForSeason,
        nextVideoId = nextVideoId
    ) ?: return ReturnFocusStep.Idle

    if (restoreEpisodeId == alreadyRestoredId) {
        return ReturnFocusStep.Idle
    }

    return ReturnFocusStep.RestoreEpisode(
        episodeId = restoreEpisodeId,
        consumeRequest = !waitingForAdvance || bingeAheadOfProgress
    )
}

private const val RETURN_FOCUS_SEASON_ADVANCE_WAIT_MS = 400L

private const val USER_INTERACTION_DISPATCH_DEBOUNCE_MS = 120L


private fun formatDetailYearRange(releaseInfo: String?): String? {
    if (releaseInfo.isNullOrBlank()) return null
    return releaseInfo.trim()
}

private fun applyDither(bmp: android.graphics.Bitmap) {
    val pixels = IntArray(bmp.width * bmp.height)
    bmp.getPixels(pixels, 0, bmp.width, 0, 0, bmp.width, bmp.height)
    val rng = java.util.Random(0)
    for (i in pixels.indices) {
        val p = pixels[i]
        val a = (p ushr 24) and 0xFF
        val r = (p ushr 16) and 0xFF
        val g = (p ushr 8) and 0xFF
        val b = p and 0xFF
        val noise = rng.nextInt(3) - 1
        pixels[i] = ((a shl 24) or
            ((r + noise).coerceIn(0, 255) shl 16) or
            ((g + noise).coerceIn(0, 255) shl 8) or
            (b + noise).coerceIn(0, 255))
    }
    bmp.setPixels(pixels, 0, bmp.width, 0, 0, bmp.width, bmp.height)
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun MetaDetailsScreen(
    viewModel: MetaDetailsViewModel = hiltViewModel(),
    returnFocusSeason: Int? = null,
    returnFocusEpisode: Int? = null,
    heroRestoreToken: Int = 0,
    heroBackdropUrl: String? = null,
    playOnLoad: Boolean = false,
    playOnLoadManually: Boolean = false,
    onBackPress: () -> Unit,
    onReturnFocusConsumed: () -> Unit = {},
    onNavigateToCastDetail: (personId: Int, personName: String, preferCrew: Boolean) -> Unit = { _, _, _ -> },
    onNavigateToTmdbEntityBrowse: (entityKind: String, entityId: Int, entityName: String, sourceType: String) -> Unit = { _, _, _, _ -> },
    onNavigateToDetail: (itemId: String, itemType: String, addonBaseUrl: String?) -> Unit = { _, _, _ -> },
    onPlayClick: (
        videoId: String,
        contentType: String,
        contentId: String,
        title: String,
        poster: String?,
        backdrop: String?,
        logo: String?,
        season: Int?,
        episode: Int?,
        episodeName: String?,
        genres: String?,
        year: String?,
        runtime: Int?,
        contentLanguage: String?
    ) -> Unit = { _, _, _, _, _, _, _, _, _, _, _, _, _, _ -> },
    onPlayManuallyClick: (
        videoId: String,
        contentType: String,
        contentId: String,
        title: String,
        poster: String?,
        backdrop: String?,
        logo: String?,
        season: Int?,
        episode: Int?,
        episodeName: String?,
        genres: String?,
        year: String?,
        runtime: Int?,
        contentLanguage: String?
    ) -> Unit = { _, _, _, _, _, _, _, _, _, _, _, _, _, _ -> },
    onPlayStartFromBeginningClick: (
        videoId: String,
        contentType: String,
        contentId: String,
        title: String,
        poster: String?,
        backdrop: String?,
        logo: String?,
        season: Int?,
        episode: Int?,
        episodeName: String?,
        genres: String?,
        year: String?,
        runtime: Int?,
        contentLanguage: String?
    ) -> Unit = { _, _, _, _, _, _, _, _, _, _, _, _, _, _ -> }
) {
    val playbackAvailability = LocalPlaybackAvailability.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val posterCardCornerRadiusDp by viewModel.posterCardCornerRadiusDp.collectAsStateWithLifecycle()
    val effectiveAutoplayEnabled by viewModel.effectiveAutoplayEnabled.collectAsStateWithLifecycle(
        initialValue = false
    )
    val selectedComment = uiState.selectedComment
    var commentOverlayDirection by remember { mutableIntStateOf(0) }
    var restorePlayFocusAfterTrailerBackToken by rememberSaveable { mutableIntStateOf(0) }
    var restoreSharedTrailerFocusToken by rememberSaveable { mutableIntStateOf(0) }
    var isTrailerPaused by remember { mutableStateOf(false) }
    val playOnLoadConsumed = rememberSaveable { mutableStateOf(false) }
    val playOnLoadHandoffDispatched = rememberSaveable { mutableStateOf(false) }
    val playOnLoadReturnObserved = rememberSaveable { mutableStateOf(false) }
    val suppressInitialPlayOnLoadContent = playOnLoad && !playOnLoadReturnObserved.value
    val playOnLoadReturnContentReady = !uiState.isLoading && uiState.meta != null
    var playOnLoadReturnRevealRequested by remember(playOnLoad) { mutableStateOf(!playOnLoad) }
    var playOnLoadReturnContentRevealed by remember(playOnLoad) { mutableStateOf(!playOnLoad) }

    LaunchedEffect(playOnLoad, playOnLoadReturnObserved.value, playOnLoadReturnContentReady) {
        if (!playOnLoad || (playOnLoadReturnObserved.value && playOnLoadReturnContentReady)) {
            playOnLoadReturnRevealRequested = true
        }
    }
    val playOnLoadReturnContentAlpha by animateFloatAsState(
        targetValue = if (playOnLoadReturnRevealRequested) 1f else 0f,
        animationSpec = NuvioMotion.mediumTween(),
        label = "playOnLoadReturnContentAlpha",
        finishedListener = { playOnLoadReturnContentRevealed = it == 1f }
    )

    val childOverlayVisible = LocalDetailChildOverlayVisible.current
    BackHandler(enabled = !childOverlayVisible) {
        if (selectedComment != null) {
            commentOverlayDirection = 0
            viewModel.onEvent(MetaDetailsEvent.OnDismissCommentOverlay)
        } else if (uiState.isSharedTrailerOverlayVisible) {
            restoreSharedTrailerFocusToken += 1
            viewModel.onEvent(MetaDetailsEvent.OnDismissSharedTrailer)
        } else if (uiState.isTrailerPlaying) {
            restorePlayFocusAfterTrailerBackToken += 1
            isTrailerPaused = false
            viewModel.onEvent(MetaDetailsEvent.OnTrailerBackPress)
        } else {
            onBackPress()
        }
    }

    val currentIsTrailerPlaying by rememberUpdatedState(uiState.isTrailerPlaying)
    val currentShowTrailerControls by rememberUpdatedState(uiState.showTrailerControls)
    val trailerControllerFocusRequester = remember { FocusRequester() }
    var trailerSeekOverlayVisible by remember { mutableStateOf(false) }
    val trailerSeekOverlayState = remember { TrailerSeekOverlayState() }
    var trailerSeekToken by remember { mutableIntStateOf(0) }
    var trailerSeekDeltaMs by remember { mutableLongStateOf(0L) }
    var lastUserInteractionDispatchMs by remember { mutableLongStateOf(0L) }
    val onTrailerProgressChanged = remember(trailerSeekOverlayState) {
        { position: Long, duration: Long ->
            trailerSeekOverlayState.positionMs = position
            trailerSeekOverlayState.durationMs = duration
        }
    }

    LaunchedEffect(uiState.isTrailerPlaying, uiState.showTrailerControls) {
        if (uiState.isTrailerPlaying && uiState.showTrailerControls) {
            trailerControllerFocusRequester.requestFocusAfterFrames()
        }
    }

    LaunchedEffect(uiState.userMessage) {
        if (uiState.userMessage != null) {
            delay(2500)
            viewModel.onEvent(MetaDetailsEvent.OnClearMessage)
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        if (
            playOnLoad &&
            playOnLoadHandoffDispatched.value &&
            lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
        ) {
            playOnLoadReturnObserved.value = true
        }
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    viewModel.onEvent(MetaDetailsEvent.OnLifecyclePause)
                }
                Lifecycle.Event.ON_RESUME -> {
                    if (playOnLoad && playOnLoadHandoffDispatched.value) {
                        playOnLoadReturnObserved.value = true
                    }
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NuvioTheme.colors.Background)
            .onPreviewKeyEvent { keyEvent ->
                if (currentIsTrailerPlaying) {
                    if (currentShowTrailerControls) {
                        if (keyEvent.nativeKeyEvent.action != KeyEvent.ACTION_DOWN) {
                            return@onPreviewKeyEvent false
                        }
                        when (keyEvent.nativeKeyEvent.keyCode) {
                            KeyEvent.KEYCODE_DPAD_CENTER,
                            KeyEvent.KEYCODE_ENTER,
                            KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                                isTrailerPaused = !isTrailerPaused
                                trailerSeekOverlayVisible = true
                                true
                            }
                            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                                isTrailerPaused = !isTrailerPaused
                                true
                            }
                            KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                                isTrailerPaused = true
                                true
                            }
                            KeyEvent.KEYCODE_MEDIA_PLAY -> {
                                isTrailerPaused = false
                                true
                            }
                            KeyEvent.KEYCODE_DPAD_UP -> {
                                trailerSeekOverlayVisible = true
                                true
                            }
                            KeyEvent.KEYCODE_DPAD_DOWN -> {
                                trailerSeekOverlayVisible = false
                                true
                            }
                            KeyEvent.KEYCODE_DPAD_LEFT -> {
                                val repeatCount = keyEvent.nativeKeyEvent.repeatCount
                                val delta = when {
                                    repeatCount >= 12 -> -12_000L
                                    repeatCount >= 6 -> -8_000L
                                    repeatCount >= 2 -> -5_000L
                                    else -> -3_000L
                                }
                                trailerSeekDeltaMs = delta
                                trailerSeekToken += 1
                                trailerSeekOverlayVisible = true
                                true
                            }
                            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                                val repeatCount = keyEvent.nativeKeyEvent.repeatCount
                                val delta = when {
                                    repeatCount >= 12 -> 12_000L
                                    repeatCount >= 6 -> 8_000L
                                    repeatCount >= 2 -> 5_000L
                                    else -> 3_000L
                                }
                                trailerSeekDeltaMs = delta
                                trailerSeekToken += 1
                                trailerSeekOverlayVisible = true
                                true
                            }
                            else -> false
                        }
                    }
                    // During auto trailer preview, consume all keys except back/ESC so content doesn't scroll.
                    val keyCode = keyEvent.nativeKeyEvent.keyCode
                    return@onPreviewKeyEvent keyCode != KeyEvent.KEYCODE_BACK &&
                            keyCode != KeyEvent.KEYCODE_ESCAPE
                }
                if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                    val nativeEvent = keyEvent.nativeKeyEvent
                    val shouldDispatch =
                        nativeEvent.repeatCount == 0 &&
                            (nativeEvent.eventTime - lastUserInteractionDispatchMs) >=
                            USER_INTERACTION_DISPATCH_DEBOUNCE_MS
                    if (shouldDispatch) {
                        lastUserInteractionDispatchMs = nativeEvent.eventTime
                        viewModel.onEvent(MetaDetailsEvent.OnUserInteraction)
                    }
                }
                false
            }
    ) {
        when {
            uiState.isLoading -> {
                if (playOnLoad && !playOnLoadReturnContentRevealed) {
                    PlaybackHandoffBackdrop(backdropUrl = heroBackdropUrl)
                } else {
                    if (!heroBackdropUrl.isNullOrBlank()) {
                        val localContext = LocalContext.current
                        val localDensity = LocalDensity.current
                        val configuration = LocalConfiguration.current
                        val loadingBackdropWidthPx = remember(configuration, localDensity) {
                            with(localDensity) { configuration.screenWidthDp.dp.roundToPx() }
                        }
                        val loadingBackdropHeightPx = remember(configuration, localDensity) {
                            with(localDensity) { configuration.screenHeightDp.dp.roundToPx() }
                        }
                        val loadingBackdropRequest = remember(localContext, heroBackdropUrl, loadingBackdropWidthPx, loadingBackdropHeightPx) {
                            ImageRequest.Builder(localContext)
                                .data(heroBackdropUrl)
                                .crossfade(false)
                                .size(width = loadingBackdropWidthPx, height = loadingBackdropHeightPx)
                                .build()
                        }
                        AsyncImage(
                            model = loadingBackdropRequest,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            alignment = Alignment.TopEnd
                        )
                    }
                    MetaDetailsSkeleton(backdropAware = !heroBackdropUrl.isNullOrBlank())
                }
            }
            uiState.error != null -> {
                ErrorState(
                    message = uiState.error ?: stringResource(R.string.error_generic),
                    onRetry = { viewModel.onEvent(MetaDetailsEvent.OnRetry) }
                )
            }
            uiState.meta != null -> {
                val meta = uiState.meta!!
                val context = LocalContext.current
                val genresString = remember(meta.genres) {
                    meta.genres.takeIf { it.isNotEmpty() }
                        ?.joinToString(" • ") { localizedGenreLabel(context, it) }
                }
                val yearString = remember(meta.releaseInfo) {
                    formatDetailYearRange(meta.releaseInfo)
                }
                val playEpisode: (Video) -> Unit = playEpisode@{ video ->
                    if (!playbackAvailability.canStream(meta.apiType, video.id, meta.id, video)) {
                        Toast.makeText(context, R.string.playback_unavailable_message, Toast.LENGTH_SHORT).show()
                        return@playEpisode
                    }
                    onPlayClick(
                        video.id,
                        meta.apiType,
                        meta.id,
                        meta.name,
                        video.thumbnail ?: meta.poster,
                        meta.backdropUrl,
                        meta.logo,
                        video.season,
                        video.episode,
                        video.title,
                        null,
                        null,
                        video.runtime,
                        meta.resolveContentLanguage()
                    )
                }
                val playEpisodeManually: (Video) -> Unit = playEpisodeManually@{ video ->
                    if (!playbackAvailability.canStream(meta.apiType, video.id, meta.id, video)) {
                        Toast.makeText(context, R.string.playback_unavailable_message, Toast.LENGTH_SHORT).show()
                        return@playEpisodeManually
                    }
                    onPlayManuallyClick(
                        video.id,
                        meta.apiType,
                        meta.id,
                        meta.name,
                        video.thumbnail ?: meta.poster,
                        meta.backdropUrl,
                        meta.logo,
                        video.season,
                        video.episode,
                        video.title,
                        null,
                        null,
                        video.runtime,
                        meta.resolveContentLanguage()
                    )
                }
                val playTitle: (String) -> Unit = playTitle@{ videoId ->
                    if (!playbackAvailability.canStream(meta.apiType, videoId, meta.id)) {
                        Toast.makeText(context, R.string.playback_unavailable_message, Toast.LENGTH_SHORT).show()
                        return@playTitle
                    }
                    onPlayClick(
                        videoId,
                        meta.apiType,
                        meta.id,
                        meta.name,
                        meta.poster,
                        meta.backdropUrl,
                        meta.logo,
                        null,
                        null,
                        null,
                        genresString,
                        yearString,
                        null,
                        meta.resolveContentLanguage()
                    )
                }
                val playTitleManually: (String) -> Unit = playTitleManually@{ videoId ->
                    if (!playbackAvailability.canStream(meta.apiType, videoId, meta.id)) {
                        Toast.makeText(context, R.string.playback_unavailable_message, Toast.LENGTH_SHORT).show()
                        return@playTitleManually
                    }
                    onPlayManuallyClick(
                        videoId,
                        meta.apiType,
                        meta.id,
                        meta.name,
                        meta.poster,
                        meta.backdropUrl,
                        meta.logo,
                        null,
                        null,
                        null,
                        genresString,
                        yearString,
                        null,
                        meta.resolveContentLanguage()
                    )
                }
                val isSeries = remember(meta.type, meta.videos) {
                    meta.type == ContentType.SERIES || meta.videos.isNotEmpty()
                }
                val playOnLoadVideo = remember(meta, uiState.nextToWatch, uiState.episodesForSeason) {
                    resolveHeroPlaybackVideo(
                        meta = meta,
                        nextToWatch = uiState.nextToWatch,
                        episodesForSeason = uiState.episodesForSeason
                    )
                }

                LaunchedEffect(
                    playOnLoad,
                    playOnLoadManually,
                    playOnLoadConsumed.value,
                    isSeries,
                    uiState.nextToWatch,
                    playOnLoadVideo?.id,
                    playbackAvailability
                ) {
                    if (!playOnLoad || playOnLoadConsumed.value || (isSeries && uiState.nextToWatch == null)) {
                        return@LaunchedEffect
                    }
                    if (!playbackAvailability.isLoaded) return@LaunchedEffect
                    playOnLoadConsumed.value = true
                    if (uiState.shufflePoolEmpty) {
                        playOnLoadReturnObserved.value = true
                        return@LaunchedEffect
                    }
                    if (!playbackAvailability.canStream(meta.apiType, playOnLoadVideo?.id ?: meta.id, meta.id, playOnLoadVideo)) {
                        playOnLoadReturnObserved.value = true
                        Toast.makeText(context, R.string.playback_unavailable_message, Toast.LENGTH_SHORT).show()
                        return@LaunchedEffect
                    }
                    playOnLoadHandoffDispatched.value = true
                    if (playOnLoadVideo != null) {
                        if (playOnLoadManually) {
                            playEpisodeManually(playOnLoadVideo)
                        } else {
                            playEpisode(playOnLoadVideo)
                        }
                    } else if (playOnLoadManually) {
                        playTitleManually(meta.id)
                    } else {
                        playTitle(meta.id)
                    }
                }

                if (suppressInitialPlayOnLoadContent) {
                    PlaybackHandoffBackdrop(backdropUrl = heroBackdropUrl ?: meta.backdropUrl)
                    return@Box
                }
                if (playOnLoad && !playOnLoadReturnContentRevealed) {
                    PlaybackHandoffBackdrop(backdropUrl = heroBackdropUrl ?: meta.backdropUrl)
                }
                MetaDetailsContent(
                    modifier = Modifier.graphicsLayer {
                        alpha = playOnLoadReturnContentAlpha
                    },
                    heroBackdropUrl = heroBackdropUrl,
                    meta = meta,
                    detailReturnEpisodeFocusRequest = DetailReturnEpisodeFocusRequest(
                        season = returnFocusSeason,
                        episode = returnFocusEpisode
                    ),
                    onDetailReturnEpisodeFocusConsumed = onReturnFocusConsumed,
                    lastFocusedEpisodeIdBySeason = viewModel.lastFocusedEpisodeIdBySeason,
                    heroRestoreToken = heroRestoreToken,
                    seasons = uiState.seasons,
                    selectedSeason = uiState.selectedSeason,
                    episodesForSeason = uiState.episodesForSeason,
                    isInLibrary = uiState.isInLibrary,
                    librarySourceMode = uiState.librarySourceMode,
                    nextToWatch = uiState.nextToWatch,
                    episodeProgressMap = uiState.episodeProgressMap,
                    watchedEpisodes = uiState.watchedEpisodes,
                    episodeWatchedPendingKeys = uiState.episodeWatchedPendingKeys,
                    blurUnwatchedEpisodes = uiState.blurUnwatchedEpisodes,
                    randomEpisodeEnabled = uiState.randomEpisodeEnabled,
                    episodeShuffle = uiState.episodeShuffle,
                    shufflePoolEmpty = uiState.shufflePoolEmpty,
                    onEpisodeShuffleChange = viewModel::setEpisodeShuffle,
                    episodeOptionsOverlayStyle = uiState.episodeOptionsOverlayStyle,
                    showFullReleaseDate = uiState.showFullReleaseDate,
                    overallRatingsVisibility = uiState.overallRatingsVisibility,
                    detailImdbRatingsVisibility = uiState.detailImdbRatingsVisibility,
                    isMovieWatched = uiState.isMovieWatched,
                    isMovieWatchedPending = uiState.isMovieWatchedPending,
                    moreLikeThis = uiState.moreLikeThis,
                    moreLikeThisSource = uiState.moreLikeThisSource,
                    posterCardCornerRadiusDp = posterCardCornerRadiusDp,
                    collection = uiState.collection,
                    collectionName = uiState.collectionName,
                    relatedWatchedStatus = uiState.relatedWatchedStatus,
                    episodeImdbRatings = uiState.episodeImdbRatings,
                    isEpisodeRatingsLoading = uiState.isEpisodeRatingsLoading,
                    episodeRatingsError = uiState.episodeRatingsError,
                    mdbListRatings = uiState.mdbListRatings,
                    mdbListRatingOrder = uiState.mdbListRatingOrder,
                    isMdbListRatingsActive = uiState.isMdbListRatingsActive,
                    tmdbRating = uiState.tmdbRating,
                    comments = uiState.comments,
                    commentsCurrentPage = uiState.commentsCurrentPage,
                    commentsPageCount = uiState.commentsPageCount,
                    isCommentsLoading = uiState.isCommentsLoading,
                    isCommentsLoadingMore = uiState.isCommentsLoadingMore,
                    commentsError = uiState.commentsError,
                    shouldShowCommentsSection = uiState.shouldShowCommentsSection,
                    commentsMode = uiState.commentsMode,
                    commentsEpisodeTarget = uiState.commentsEpisodeTarget,
                    selectedComment = uiState.selectedComment,
                    onSeasonSelected = { viewModel.onEvent(MetaDetailsEvent.OnSeasonSelected(it)) },
                    onEpisodeClick = playEpisode,
                    onEpisodeManualPlayClick = playEpisodeManually,
                    onPlayClick = playTitle,
                    onPlayManuallyClick = playTitleManually,
                    onEpisodeStartFromBeginningClick = onEpisodeStartFromBeginningClick@{ video ->
                        if (!playbackAvailability.canStream(meta.apiType, video.id, meta.id, video)) {
                            Toast.makeText(context, R.string.playback_unavailable_message, Toast.LENGTH_SHORT).show()
                            return@onEpisodeStartFromBeginningClick
                        }
                        onPlayStartFromBeginningClick(
                            video.id,
                            meta.apiType,
                            meta.id,
                            meta.name,
                            video.thumbnail ?: meta.poster,
                            meta.backdropUrl,
                            meta.logo,
                            video.season,
                            video.episode,
                            video.title,
                            null,
                            null,
                            video.runtime,
                            meta.resolveContentLanguage()
                        )
                    },
                    onPlayStartFromBeginningClick = onPlayStartFromBeginningClick@{ videoId ->
                        if (!playbackAvailability.canStream(meta.apiType, videoId, meta.id)) {
                            Toast.makeText(context, R.string.playback_unavailable_message, Toast.LENGTH_SHORT).show()
                            return@onPlayStartFromBeginningClick
                        }
                        onPlayStartFromBeginningClick(
                            videoId,
                            meta.apiType,
                            meta.id,
                            meta.name,
                            meta.poster,
                            meta.backdropUrl,
                            meta.logo,
                            null,
                            null,
                            null,
                            genresString,
                            yearString,
                            null,
                            meta.resolveContentLanguage()
                        )
                    },
                    showManualPlayOption = effectiveAutoplayEnabled,
                    onPlayButtonFocused = { viewModel.onEvent(MetaDetailsEvent.OnPlayButtonFocused) },
                    onToggleLibrary = { viewModel.onEvent(MetaDetailsEvent.OnToggleLibrary) },
                    onLibraryLongPress = { viewModel.onEvent(MetaDetailsEvent.OnLibraryLongPress) },
                    onToggleMovieWatched = { viewModel.onEvent(MetaDetailsEvent.OnToggleMovieWatched) },
                    onToggleEpisodeWatched = { video ->
                        viewModel.onEvent(MetaDetailsEvent.OnToggleEpisodeWatched(video))
                    },
                    onMarkSeasonWatched = { season ->
                        viewModel.onEvent(MetaDetailsEvent.OnMarkSeasonWatched(season))
                    },
                    onMarkSeasonUnwatched = { season ->
                        viewModel.onEvent(MetaDetailsEvent.OnMarkSeasonUnwatched(season))
                    },
                    onMarkPreviousEpisodesWatched = { video ->
                        viewModel.onEvent(MetaDetailsEvent.OnMarkPreviousEpisodesWatched(video))
                    },
                    onMarkPreviousSeasonsWatched = { season ->
                        viewModel.onEvent(MetaDetailsEvent.OnMarkPreviousSeasonsWatched(season))
                    },
                    isSeasonFullyWatched = { season ->
                        viewModel.isSeasonFullyWatched(season)
                    },
                    trailerUrl = uiState.trailerUrl,
                    trailerAudioUrl = uiState.trailerAudioUrl,
                    isTrailerPlaying = uiState.isTrailerPlaying,
                    isBackgroundTrailerPlaying = uiState.isBackgroundTrailerPlaying,
                    pauseBackgroundTrailerOnScroll = uiState.pauseBackgroundTrailerOnScroll,
                    isTrailerPaused = isTrailerPaused,
                    showTrailerControls = uiState.showTrailerControls,
                    hideLogoDuringTrailer = uiState.hideLogoDuringTrailer,
                    trailerButtonEnabled = uiState.trailerButtonEnabled,
                    isSharedTrailerOverlayVisible = uiState.isSharedTrailerOverlayVisible,
                    isSharedTrailerLoading = uiState.isSharedTrailerLoading,
                    sharedTrailerUrl = uiState.sharedTrailerUrl,
                    sharedTrailerAudioUrl = uiState.sharedTrailerAudioUrl,
                    sharedTrailerErrorMessage = uiState.sharedTrailerErrorMessage,
                    selectedSharedTrailer = uiState.selectedSharedTrailer,
                    trailerSeekToken = trailerSeekToken,
                    trailerSeekDeltaMs = trailerSeekDeltaMs,
                    onTrailerControlKey = { keyCode, action, repeatCount ->
                        if (!uiState.showTrailerControls || !uiState.isTrailerPlaying) {
                            false
                        } else if (action != KeyEvent.ACTION_DOWN) {
                            false
                        } else {
                            val seekStepMs = when {
                                repeatCount >= 12 -> 12_000L
                                repeatCount >= 6 -> 8_000L
                                repeatCount >= 2 -> 5_000L
                                else -> 3_000L
                            }
                            when (keyCode) {
                                KeyEvent.KEYCODE_DPAD_CENTER,
                                KeyEvent.KEYCODE_ENTER,
                                KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                                    isTrailerPaused = !isTrailerPaused
                                    trailerSeekOverlayVisible = true
                                    true
                                }
                                KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                                    isTrailerPaused = !isTrailerPaused
                                    true
                                }
                                KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                                    isTrailerPaused = true
                                    true
                                }
                                KeyEvent.KEYCODE_MEDIA_PLAY -> {
                                    isTrailerPaused = false
                                    true
                                }
                                KeyEvent.KEYCODE_DPAD_UP -> {
                                    trailerSeekOverlayVisible = true
                                    true
                                }
                                KeyEvent.KEYCODE_DPAD_DOWN -> {
                                    trailerSeekOverlayVisible = false
                                    true
                                }
                                KeyEvent.KEYCODE_DPAD_LEFT -> {
                                    trailerSeekDeltaMs = -seekStepMs
                                    trailerSeekToken += 1
                                    trailerSeekOverlayVisible = true
                                    true
                                }
                                KeyEvent.KEYCODE_DPAD_RIGHT -> {
                                    trailerSeekDeltaMs = seekStepMs
                                    trailerSeekToken += 1
                                    trailerSeekOverlayVisible = true
                                    true
                                }
                                else -> false
                            }
                        }
                    },
                    onTrailerProgressChanged = onTrailerProgressChanged,
                    onTrailerEnded = {
                        restorePlayFocusAfterTrailerBackToken += 1
                        isTrailerPaused = false
                        viewModel.onEvent(MetaDetailsEvent.OnTrailerEnded)
                    },
                    onTrailerButtonClick = { viewModel.onEvent(MetaDetailsEvent.OnTrailerButtonClick) },
                    onSharedTrailerSelected = { viewModel.onEvent(MetaDetailsEvent.OnSharedTrailerSelected(it)) },
                    onDismissSharedTrailer = { viewModel.onEvent(MetaDetailsEvent.OnDismissSharedTrailer) },
                    onRetrySharedTrailer = { viewModel.onEvent(MetaDetailsEvent.OnRetrySharedTrailer) },
                    onRetryComments = { viewModel.onEvent(MetaDetailsEvent.OnRetryComments) },
                    onLoadMoreComments = { viewModel.onEvent(MetaDetailsEvent.OnLoadMoreComments) },
                    onCommentsModeSelected = { viewModel.onEvent(MetaDetailsEvent.OnCommentsModeSelected(it)) },
                    onCommentsEpisodeSelected = { viewModel.onEvent(MetaDetailsEvent.OnCommentsEpisodeSelected(it)) },
                    onCommentClick = {
                        commentOverlayDirection = 0
                        viewModel.onEvent(MetaDetailsEvent.OnCommentSelected(it))
                    },
                    onShowPreviousComment = {
                        commentOverlayDirection = -1
                        viewModel.onEvent(MetaDetailsEvent.OnAdvanceCommentOverlay(direction = -1))
                    },
                    onShowNextComment = {
                        commentOverlayDirection = 1
                        viewModel.onEvent(MetaDetailsEvent.OnAdvanceCommentOverlay(direction = 1))
                    },
                    onDismissCommentOverlay = {
                        commentOverlayDirection = 0
                        viewModel.onEvent(MetaDetailsEvent.OnDismissCommentOverlay)
                    },
                    commentOverlayDirection = commentOverlayDirection,
                    restorePlayFocusAfterTrailerBackToken = restorePlayFocusAfterTrailerBackToken,
                    restoreSharedTrailerFocusToken = restoreSharedTrailerFocusToken,
                    onSharedTrailerFocusRestored = { restoreSharedTrailerFocusToken = 0 },
                    onNavigateToCastDetail = onNavigateToCastDetail,
                    onNavigateToTmdbEntityBrowse = onNavigateToTmdbEntityBrowse,
                    onNavigateToDetail = onNavigateToDetail,
                    onPosterLongPress = { item -> viewModel.posterOptions.show(item, null) }
                )
            }
        }

        if (uiState.showListPicker) {
            val nuvioListTab = LibraryListTab(
                key = LOCAL_LIBRARY_LIST_KEY,
                title = stringResource(R.string.trakt_library_source_nuvio),
                type = LibraryListTab.Type.WATCHLIST
            )
            val contentType = uiState.meta?.apiType?.lowercase().orEmpty()
            val providerTabs = uiState.libraryListTabs.filter { tab ->
                tab.supportsMembershipFor(contentType)
            }
            val combinedTabs = listOf(nuvioListTab) + providerTabs
            LibraryListPickerDialog(
                title = uiState.meta?.name ?: stringResource(R.string.detail_lists_fallback),
                tabs = combinedTabs,
                membership = uiState.pickerMembership,
                isPending = uiState.pickerPending,
                error = uiState.pickerError,
                onToggle = { key ->
                    viewModel.onEvent(MetaDetailsEvent.OnPickerMembershipToggled(key))
                },
                onSave = { viewModel.onEvent(MetaDetailsEvent.OnPickerSave) },
                onDismiss = { viewModel.onEvent(MetaDetailsEvent.OnPickerDismiss) }
            )
        }

        if (uiState.removalConfirmations.isNotEmpty()) {
            TrackingRemovalConfirmationDialog(
                itemTitle = uiState.meta?.name.orEmpty(),
                confirmations = uiState.removalConfirmations,
                isPending = uiState.pickerPending || uiState.defaultLibraryTogglePending,
                onConfirm = { viewModel.onEvent(MetaDetailsEvent.OnRemovalConfirmed) },
                onDismiss = { viewModel.onEvent(MetaDetailsEvent.OnRemovalCancelled) }
            )
        }

        val message = uiState.userMessage
        if (!message.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = NuvioTheme.spacing.xl)
                    .background(
                        color = if (uiState.userMessageIsError) {
                            Color(0xFF5A1C1C)
                        } else {
                            NuvioTheme.colors.BackgroundElevated
                        },
                        shape = RoundedCornerShape(10.dp)
                    )
                    .padding(horizontal = 18.dp, vertical = 10.dp)
            ) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = NuvioTheme.colors.TextPrimary
                )
            }
        }

        if (uiState.isTrailerPlaying && uiState.showTrailerControls) {
            // This surface exists only to capture remote keys (back/play/pause/seek) while the
            // trailer is playing, so it has no visible label of its own to announce.
            val trailerControlsDescription = stringResource(R.string.cd_trailer_controls)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .focusRequester(trailerControllerFocusRequester)
                    .focusable()
                    .semantics { contentDescription = trailerControlsDescription }
            )
        }

        TrailerSeekOverlayHost(
            visible = uiState.isTrailerPlaying && uiState.showTrailerControls && trailerSeekOverlayVisible,
            overlayState = trailerSeekOverlayState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    LaunchedEffect(trailerSeekOverlayVisible, uiState.isTrailerPlaying, uiState.showTrailerControls, trailerSeekToken) {
        if (trailerSeekOverlayVisible && uiState.isTrailerPlaying && uiState.showTrailerControls) {
            delay(3000)
            trailerSeekOverlayVisible = false
        }
    }

    LaunchedEffect(uiState.isTrailerPlaying, uiState.showTrailerControls) {
        if (!uiState.isTrailerPlaying || !uiState.showTrailerControls) {
            trailerSeekOverlayVisible = false
        }
    }

    val posterOptionsState by viewModel.posterOptions.state.collectAsStateWithLifecycle()
    com.nuvio.tv.ui.components.posteroptions.PosterOptionsHost(
        state = posterOptionsState,
        controller = viewModel.posterOptions,
        onNavigateToDetail = { id, type, addonBaseUrl ->
            onNavigateToDetail(id, type, addonBaseUrl.takeIf { it.isNotBlank() })
        }
    )
}

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
private fun MetaDetailsContent(
    modifier: Modifier = Modifier,
    heroBackdropUrl: String? = null,
    meta: Meta,
    detailReturnEpisodeFocusRequest: DetailReturnEpisodeFocusRequest? = null,
    onDetailReturnEpisodeFocusConsumed: () -> Unit,
    lastFocusedEpisodeIdBySeason: MutableMap<Int, String>,
    heroRestoreToken: Int = 0,
    seasons: List<Int>,
    selectedSeason: Int,
    episodesForSeason: List<Video>,
    isInLibrary: Boolean,
    librarySourceMode: LibrarySourceMode,
    nextToWatch: NextToWatch?,
    episodeProgressMap: Map<Pair<Int, Int>, WatchProgress>,
    watchedEpisodes: Set<Pair<Int, Int>>,
    episodeWatchedPendingKeys: Set<String>,
    blurUnwatchedEpisodes: Boolean,
    randomEpisodeEnabled: Boolean,
    episodeShuffle: com.nuvio.tv.domain.model.EpisodeShuffleSettings,
    shufflePoolEmpty: Boolean,
    onEpisodeShuffleChange: suspend (com.nuvio.tv.domain.model.EpisodeShuffleSettings) -> Boolean,
    episodeOptionsOverlayStyle: EpisodeOptionsOverlayStyle,
    showFullReleaseDate: Boolean,
    overallRatingsVisibility: HomeImdbRatingsVisibility,
    detailImdbRatingsVisibility: DetailImdbRatingsVisibility,
    isMovieWatched: Boolean,
    isMovieWatchedPending: Boolean,
    moreLikeThis: List<MetaPreview>,
    moreLikeThisSource: MoreLikeThisSource?,
    posterCardCornerRadiusDp: Int = 12,
    collection: List<MetaPreview>,
    collectionName: String?,
    relatedWatchedStatus: Map<String, Boolean> = emptyMap(),
    episodeImdbRatings: Map<Pair<Int, Int>, Double>,
    isEpisodeRatingsLoading: Boolean,
    episodeRatingsError: String?,
    mdbListRatings: MDBListRatings?,
    mdbListRatingOrder: List<String> = com.nuvio.tv.domain.model.MDBListSettings.DEFAULT_RATING_ORDER,
    isMdbListRatingsActive: Boolean,
    tmdbRating: Float?,
    comments: List<TraktCommentReview>,
    commentsCurrentPage: Int,
    commentsPageCount: Int,
    isCommentsLoading: Boolean,
    isCommentsLoadingMore: Boolean,
    commentsError: String?,
    shouldShowCommentsSection: Boolean,
    commentsMode: CommentsMode,
    commentsEpisodeTarget: Video?,
    selectedComment: TraktCommentReview?,
    onSeasonSelected: (Int) -> Unit,
    onEpisodeClick: (Video) -> Unit,
    onEpisodeManualPlayClick: (Video) -> Unit,
    onEpisodeStartFromBeginningClick: (Video) -> Unit = {},
    onPlayClick: (String) -> Unit,
    onPlayManuallyClick: (String) -> Unit,
    onPlayStartFromBeginningClick: (String) -> Unit = {},
    showManualPlayOption: Boolean,
    onPlayButtonFocused: () -> Unit,
    onToggleLibrary: () -> Unit,
    onLibraryLongPress: () -> Unit,
    onToggleMovieWatched: () -> Unit,
    onToggleEpisodeWatched: (Video) -> Unit,
    onMarkSeasonWatched: (Int) -> Unit,
    onMarkSeasonUnwatched: (Int) -> Unit,
    onMarkPreviousEpisodesWatched: (Video) -> Unit,
    onMarkPreviousSeasonsWatched: (Int) -> Unit,
    isSeasonFullyWatched: (Int) -> Boolean,
    trailerUrl: String?,
    trailerAudioUrl: String?,
    isTrailerPlaying: Boolean,
    isBackgroundTrailerPlaying: Boolean,
    pauseBackgroundTrailerOnScroll: Boolean,
    isTrailerPaused: Boolean = false,
    showTrailerControls: Boolean,
    hideLogoDuringTrailer: Boolean,
    trailerButtonEnabled: Boolean,
    isSharedTrailerOverlayVisible: Boolean,
    isSharedTrailerLoading: Boolean,
    sharedTrailerUrl: String?,
    sharedTrailerAudioUrl: String?,
    sharedTrailerErrorMessage: String?,
    selectedSharedTrailer: MetaTrailer?,
    trailerSeekToken: Int,
    trailerSeekDeltaMs: Long,
    onTrailerControlKey: (keyCode: Int, action: Int, repeatCount: Int) -> Boolean,
    onTrailerProgressChanged: (Long, Long) -> Unit,
    onTrailerEnded: () -> Unit,
    onTrailerButtonClick: () -> Unit,
    onSharedTrailerSelected: (MetaTrailer) -> Unit,
    onDismissSharedTrailer: () -> Unit,
    onRetrySharedTrailer: () -> Unit,
    onRetryComments: () -> Unit,
    onLoadMoreComments: () -> Unit,
    onCommentsModeSelected: (CommentsMode) -> Unit,
    onCommentsEpisodeSelected: (Video) -> Unit,
    onCommentClick: (TraktCommentReview) -> Unit,
    onShowPreviousComment: () -> Unit,
    onShowNextComment: () -> Unit,
    onDismissCommentOverlay: () -> Unit,
    commentOverlayDirection: Int,
    restorePlayFocusAfterTrailerBackToken: Int,
    restoreSharedTrailerFocusToken: Int,
    onSharedTrailerFocusRestored: () -> Unit,
    onNavigateToCastDetail: (personId: Int, personName: String, preferCrew: Boolean) -> Unit = { _, _, _ -> },
    onNavigateToTmdbEntityBrowse: (entityKind: String, entityId: Int, entityName: String, sourceType: String) -> Unit = { _, _, _, _ -> },
    onNavigateToDetail: (itemId: String, itemType: String, addonBaseUrl: String?) -> Unit = { _, _, _ -> },
    onPosterLongPress: (MetaPreview) -> Unit = {}
) {
    val playbackAvailability = LocalPlaybackAvailability.current
    val canLoadMoreComments = commentsCurrentPage in 1 until commentsPageCount
    val selectedCommentIndex = remember(comments, selectedComment?.id) {
        selectedComment?.let { review -> comments.indexOfFirst { it.id == review.id } } ?: -1
    }
    val isSeries = remember(meta.type, meta.videos) {
        meta.type == ContentType.SERIES || meta.videos.isNotEmpty()
    }
    val defaultSeriesVideo = remember(meta.behaviorHints?.defaultVideoId, meta.videos) {
        val defaultVideoId = meta.behaviorHints?.defaultVideoId
        meta.videos.firstOrNull { it.id == defaultVideoId && it.available != false }
    }
    val nextEpisode = remember(episodesForSeason) { episodesForSeason.firstOrNull() }
    val heroVideo = remember(meta, nextToWatch, episodesForSeason) {
        resolveHeroPlaybackVideo(
            meta = meta,
            nextToWatch = nextToWatch,
            episodesForSeason = episodesForSeason
        )
    }
    val isPlayEnabled = playbackAvailability.canStream(meta.apiType, heroVideo?.id ?: meta.id, meta.id, heroVideo)
    val canPlayEpisode = remember(playbackAvailability, meta.apiType, meta.id) {
        { video: Video -> playbackAvailability.canStream(meta.apiType, video.id, meta.id, video) }
    }
    val nestedPrefetchStrategy = remember { LazyListPrefetchStrategy(nestedPrefetchItemCount = 2) }
    val listState = rememberLazyListState(prefetchStrategy = nestedPrefetchStrategy)
    val castRowListState = rememberLazyListState(prefetchStrategy = nestedPrefetchStrategy)
    val moreLikeThisListState = rememberLazyListState(prefetchStrategy = nestedPrefetchStrategy)
    val trailerListState = rememberLazyListState(prefetchStrategy = nestedPrefetchStrategy)
    val collectionListState = rememberLazyListState(prefetchStrategy = nestedPrefetchStrategy)
    val commentsListState = rememberLazyListState(prefetchStrategy = nestedPrefetchStrategy)
    val networkLogosListState = rememberLazyListState(prefetchStrategy = nestedPrefetchStrategy)
    val productionLogosListState = rememberLazyListState(prefetchStrategy = nestedPrefetchStrategy)
    var lastFocusedCastKey by rememberSaveable(meta.id) { mutableStateOf<String?>(null) }
    var lastFocusedMoreLikeItemId by rememberSaveable(meta.id) { mutableStateOf<String?>(null) }
    var lastFocusedTrailerId by rememberSaveable(meta.id) { mutableStateOf<String?>(null) }
    var lastFocusedCollectionItemId by rememberSaveable(meta.id) { mutableStateOf<String?>(null) }
    var lastFocusedNetworkCompanyId by rememberSaveable(meta.id) { mutableStateOf<Int?>(null) }
    var lastFocusedProductionCompanyId by rememberSaveable(meta.id) { mutableStateOf<Int?>(null) }
    var savedRestoreScrollIndex by rememberSaveable(meta.id) { mutableIntStateOf(-1) }
    var savedRestoreScrollOffset by rememberSaveable(meta.id) { mutableIntStateOf(0) }
    var pinnedPageIndex by remember { mutableIntStateOf(-1) }
    var pinnedPageOffset by remember { mutableIntStateOf(0) }
    var detailFastScrolling by remember { mutableStateOf(false) }
    var detailFastScrollToken by remember { mutableIntStateOf(0) }
    val detailFastScrollingState = rememberUpdatedState(detailFastScrolling)
    // Suppress auto-scroll when hero buttons get focus
    val heroNoScrollResponder = remember {
        object : BringIntoViewResponder {
            override fun calculateRectForParent(localRect: Rect): Rect = Rect.Zero
            override suspend fun bringChildIntoView(localRect: () -> Rect?) { }
        }
    }
    val selectedSeasonFocusRequester = remember { FocusRequester() }
    val heroPlayButtonFocusRequester = remember { FocusRequester() }
    var synopsisTruncated by remember(meta.id, meta.description) { mutableStateOf(false) }
    val heroPlayFocusRequester = if (synopsisTruncated) null else heroPlayButtonFocusRequester
    val randomEpisodeFocusRequester = remember { FocusRequester() }
    val ratingsContentFocusRequester = remember { FocusRequester() }
    val ratingsGridFocusRequester = remember { FocusRequester() }
    val castSectionFocusRequester = remember { FocusRequester() }
    val moreLikeSectionFocusRequester = remember { FocusRequester() }
    val trailerSectionFocusRequester = remember { FocusRequester() }
    val collectionSectionFocusRequester = remember { FocusRequester() }
    val commentsTitleModeFocusRequester = remember { FocusRequester() }
    val commentsEpisodeModeFocusRequester = remember { FocusRequester() }
    val commentsRowEntryFocusRequester = remember { FocusRequester() }
    val networkSectionFocusRequester = remember { FocusRequester() }
    val productionSectionFocusRequester = remember { FocusRequester() }
    var pendingRestoreType by rememberSaveable { mutableStateOf<RestoreTarget?>(null) }
    var pendingRestoreEpisodeId by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingRestoreCastPersonId by rememberSaveable { mutableStateOf<Int?>(null) }
    var pendingRestoreMoreLikeItemId by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingRestoreCollectionItemId by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingRestoreCompanyId by rememberSaveable { mutableStateOf<Int?>(null) }
    var restoreFocusToken by rememberSaveable { mutableIntStateOf(0) }
    var commentsEntryFocusToken by rememberSaveable { mutableIntStateOf(0) }
    var companyRestoreToken by rememberSaveable { mutableIntStateOf(0) }
    var restoreOnNextResume by rememberSaveable { mutableStateOf(false) }
    var consumeReturnEpisodeFocusOnClear by rememberSaveable(meta.id) { mutableStateOf(false) }
    var initialHeroFocusRequested by rememberSaveable(meta.id) { mutableStateOf(false) }
    var showHeroPlayOptionsDialog by rememberSaveable(meta.id) { mutableStateOf(false) }
    var showSynopsisOverlay by rememberSaveable(meta.id) { mutableStateOf(false) }
    var showRandomEpisodeOverlay by rememberSaveable(meta.id) { mutableStateOf(false) }
    var randomEpisodePlaybackPending by rememberSaveable(meta.id) { mutableStateOf(false) }
    var stoppingShuffle by remember(meta.id) { mutableStateOf(false) }
    val showRandomEpisodeButton = remember(randomEpisodeEnabled, isSeries, meta.videos, episodeShuffle.enabled) {
        randomEpisodeEnabled && isSeries && (episodeShuffle.enabled || meta.videos.any {
            (it.season ?: 0) > 0 && (it.episode ?: 0) > 0
        })
    }
    LaunchedEffect(showRandomEpisodeButton) {
        if (!showRandomEpisodeButton) showRandomEpisodeOverlay = false
    }
    var lastReturnFocusRestoreId by rememberSaveable(
        meta.id,
        detailReturnEpisodeFocusRequest?.season,
        detailReturnEpisodeFocusRequest?.episode
    ) {
        mutableStateOf<String?>(null)
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    val childClosedEpoch = LocalDetailChildClosedEpoch.current
    val suppressDetailRowRelocation = pendingRestoreType == RestoreTarget.EPISODE
    val suppressRestoreBringIntoView =
        pendingRestoreType == RestoreTarget.COMPANY_OR_NETWORK ||
            pendingRestoreType == RestoreTarget.CAST_MEMBER ||
            pendingRestoreType == RestoreTarget.MORE_LIKE_THIS ||
            pendingRestoreType == RestoreTarget.COLLECTION
    val defaultBringIntoViewSpec = LocalBringIntoViewSpec.current
    val restoreNoScrollBringIntoViewSpec = remember {
        object : BringIntoViewSpec {
            override fun calculateScrollDistance(
                offset: Float,
                size: Float,
                containerSize: Float
            ): Float = 0f
        }
    }
    val detailPageBringIntoViewSpec = remember(defaultBringIntoViewSpec) {
        @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
        object : BringIntoViewSpec {
            override val scrollAnimationSpec = defaultBringIntoViewSpec.scrollAnimationSpec

            override fun calculateScrollDistance(
                offset: Float,
                size: Float,
                containerSize: Float
            ): Float {
                if (detailFastScrollingState.value) return 0f
                // Nuvio Launcher: a request exactly one viewport tall is the episodes block's anchor
                // window (see episodesBlockAnchorRect); align its top with the top of the screen
                // instead of centring it.
                if (containerSize > 0f && kotlin.math.abs(size - containerSize) < 1f) return offset
                return defaultBringIntoViewSpec.calculateScrollDistance(offset, size, containerSize)
            }
        }
    }
    var lastDetailDpadKey by rememberSaveable(meta.id) { mutableIntStateOf(0) }
    val tapScroll = remember { DetailTapScroll() }
    // Nuvio Launcher: when the episodes block (Episodes title, season pills, episode tiles) takes
    // focus, the page scrolls so the "Episodes" title sits EpisodesTitleScreenTop from the top of
    // the screen (rather than centring the focused row). Without season tabs the tiles land where
    // they would sit under the pills.
    val hasSeasonTabsItem = isSeries && seasons.isNotEmpty() &&
        !(seasons.size == 1 && meta.apiType.equals("other", ignoreCase = true))
    val episodesAnchorDensity = LocalDensity.current
    val seasonTabsAnchorPx = with(episodesAnchorDensity) { (EpisodesTitleScreenTop - SeasonTabsTitleTopInset).toPx() }
    val episodesOnlyAnchorPx = with(episodesAnchorDensity) { EpisodesRowScreenTopWithoutTabs.toPx() }
    var seasonTabsHeightPx by remember { mutableIntStateOf(0) }
    // Scroll offset (for LazyListState.scrollToItem on list index 1) that puts the episodes block
    // at its anchored position: negative, i.e. item 1 starts below the top of the screen.
    fun episodesBlockScrollOffsetPx(): Int =
        -(if (hasSeasonTabsItem) seasonTabsAnchorPx else episodesOnlyAnchorPx).roundToInt()
    // A viewport-tall rect, in the coordinates of an item of the episodes block, whose top is the
    // point that must end up at the top of the screen. detailPageBringIntoViewSpec aligns such a
    // rect's top with the top of the viewport.
    fun episodesBlockAnchorRect(localRect: Rect, isSeasonTabsItem: Boolean): Rect {
        val viewportHeight = listState.layoutInfo.viewportSize.height.toFloat()
        if (viewportHeight <= 0f) return localRect
        val top = when {
            isSeasonTabsItem -> -seasonTabsAnchorPx
            hasSeasonTabsItem -> -(seasonTabsHeightPx.toFloat() + seasonTabsAnchorPx)
            else -> -episodesOnlyAnchorPx
        }
        return Rect(localRect.left, top, localRect.right, top + viewportHeight)
    }
    val seasonTabsAnchorResponder = remember(suppressDetailRowRelocation, hasSeasonTabsItem, seasonTabsAnchorPx) {
        object : BringIntoViewResponder {
            override fun calculateRectForParent(localRect: Rect): Rect {
                return if (suppressDetailRowRelocation) {
                    Rect.Zero
                } else {
                    episodesBlockAnchorRect(localRect, isSeasonTabsItem = true)
                }
            }

            override suspend fun bringChildIntoView(localRect: () -> Rect?) { }
        }
    }
    val episodeRowStayVerticalResponder = remember(
        lastDetailDpadKey,
        pendingRestoreType,
        hasSeasonTabsItem,
        seasonTabsAnchorPx,
        episodesOnlyAnchorPx
    ) {
        object : BringIntoViewResponder {
            override fun calculateRectForParent(localRect: Rect): Rect {
                val stayVertical = pendingRestoreType == RestoreTarget.EPISODE ||
                    lastDetailDpadKey == KeyEvent.KEYCODE_DPAD_LEFT ||
                    lastDetailDpadKey == KeyEvent.KEYCODE_DPAD_RIGHT
                return if (stayVertical) {
                    Rect(localRect.left, 0f, localRect.right, 0f)
                } else {
                    episodesBlockAnchorRect(localRect, isSeasonTabsItem = false)
                }
            }

            override suspend fun bringChildIntoView(localRect: () -> Rect?) { }
        }
    }

    // Nuvio Launcher: the stacked rows under the episodes. When focus moves into a row (Up/Down) the
    // page scrolls so the whole row - title, tiles and captions - sits in the middle of the screen
    // (fully visible; a row taller than the screen is aligned to the top). A sideways press never
    // scrolls the page vertically: the request is answered with an empty rect that is already on screen.
    val stackedRowHeightsPx = remember { mutableMapOf<String, Int>() }
    val stackedRowResponders = remember { mutableMapOf<String, BringIntoViewResponder>() }
    fun stackedRowResponder(rowKey: String): BringIntoViewResponder = stackedRowResponders.getOrPut(rowKey) {
        object : BringIntoViewResponder {
            override fun calculateRectForParent(localRect: Rect): Rect {
                val sideways = lastDetailDpadKey == KeyEvent.KEYCODE_DPAD_LEFT ||
                    lastDetailDpadKey == KeyEvent.KEYCODE_DPAD_RIGHT
                val layoutInfo = listState.layoutInfo
                if (sideways) {
                    val rowOffset = layoutInfo.visibleItemsInfo.firstOrNull { it.key == rowKey }?.offset ?: 0
                    val visibleTop = (-rowOffset).coerceAtLeast(0).toFloat()
                    return Rect(localRect.left, visibleTop, localRect.right, visibleTop)
                }
                val viewportHeight = layoutInfo.viewportSize.height.toFloat()
                val rowHeight = (stackedRowHeightsPx[rowKey] ?: 0).toFloat()
                if (viewportHeight <= 0f || rowHeight <= 0f) return localRect
                // A rect exactly one viewport tall is aligned with the top of the viewport by
                // detailPageBringIntoViewSpec, so this one centres the row.
                val top = if (rowHeight >= viewportHeight) 0f else (rowHeight - viewportHeight) / 2f
                return Rect(localRect.left, top, localRect.right, top + viewportHeight)
            }

            override suspend fun bringChildIntoView(localRect: () -> Rect?) { }
        }
    }

    fun capturePageScroll() {
        savedRestoreScrollIndex = listState.firstVisibleItemIndex
        savedRestoreScrollOffset = listState.firstVisibleItemScrollOffset
    }

    fun pinDetailPageScroll() {
        if (pinnedPageIndex >= 0) return
        pinnedPageIndex = listState.firstVisibleItemIndex
        pinnedPageOffset = listState.firstVisibleItemScrollOffset
    }

    suspend fun animateDetailScrollTo(index: Int, offset: Int) {
        if (index < 0) return
        val currentIndex = listState.firstVisibleItemIndex
        val currentOffset = listState.firstVisibleItemScrollOffset
        if (currentIndex == index) {
            val delta = (offset - currentOffset).toFloat()
            if (kotlin.math.abs(delta) > 1f) {
                listState.animateScrollBy(delta, NuvioMotion.slowTween())
            }
            return
        }
        listState.animateScrollToItem(index, offset)
    }

    fun remainingDetailScrollPx(): Int {
        val info = listState.layoutInfo
        val lastVisible = info.visibleItemsInfo.lastOrNull() ?: return 0
        val tail = (lastVisible.offset + lastVisible.size - info.viewportEndOffset).coerceAtLeast(0)
        return if (lastVisible.index < info.totalItemsCount - 1) Int.MAX_VALUE else tail
    }

    fun onCompanyRowFocused(revealOverflowPx: Float) {
        if (tapScroll.rapidDown && tapScroll.lastKey == KeyEvent.KEYCODE_DPAD_DOWN) return
        pinDetailPageScroll()
        if (pendingRestoreType == RestoreTarget.COMPANY_OR_NETWORK) return
        val remaining = remainingDetailScrollPx()
        val distance = when {
            revealOverflowPx > 1f -> {
                if (remaining == Int.MAX_VALUE) revealOverflowPx else minOf(revealOverflowPx, remaining.toFloat())
            }
            remaining in 1..200 -> remaining.toFloat()
            else -> 0f
        }
        if (distance <= 1f) return
        coroutineScope.launch {
            listState.animateScrollBy(distance, NuvioMotion.slowTween())
        }
    }

    fun allowCompanyPageScroll(): Boolean {
        val key = tapScroll.lastKey
        val vertical = key == KeyEvent.KEYCODE_DPAD_UP || key == KeyEvent.KEYCODE_DPAD_DOWN
        if (!vertical) return false
        if (key == KeyEvent.KEYCODE_DPAD_DOWN && tapScroll.rapidDown) return true
        return remainingDetailScrollPx() !in 1..200
    }

    fun restorePinnedDetailPageIfNudge() {
        val index = pinnedPageIndex
        val offset = pinnedPageOffset
        if (index < 0) return
        pinnedPageIndex = -1
        val currentIndex = listState.firstVisibleItemIndex
        val currentOffset = listState.firstVisibleItemScrollOffset
        val offsetDelta = kotlin.math.abs(currentOffset - offset)
        if (currentIndex != index || offsetDelta !in 1..200) return
        coroutineScope.launch {
            animateDetailScrollTo(index, offset)
        }
    }

    fun clearPendingRestore() {
        savedRestoreScrollIndex = -1
        savedRestoreScrollOffset = 0
        val shouldConsumeReturnFocus = consumeReturnEpisodeFocusOnClear
        pendingRestoreType = null
        pendingRestoreEpisodeId = null
        pendingRestoreCastPersonId = null
        pendingRestoreMoreLikeItemId = null
        pendingRestoreCollectionItemId = null
        pendingRestoreCompanyId = null
        restoreFocusToken = 0
        companyRestoreToken = 0
        restoreOnNextResume = false
        consumeReturnEpisodeFocusOnClear = false
        if (shouldConsumeReturnFocus) {
            onDetailReturnEpisodeFocusConsumed()
        }
    }

    fun markHeroRestore() {
        restoreOnNextResume = lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
        pendingRestoreType = RestoreTarget.HERO
        pendingRestoreEpisodeId = null
        pendingRestoreCastPersonId = null
        pendingRestoreMoreLikeItemId = null
        pendingRestoreCollectionItemId = null
        pendingRestoreCompanyId = null
    }

    fun markEpisodeRestore(episodeId: String, restoreOnResume: Boolean = true) {
        restoreOnNextResume = restoreOnResume &&
            lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
        restoreFocusToken = 0
        pendingRestoreType = RestoreTarget.EPISODE
        pendingRestoreEpisodeId = episodeId
        pendingRestoreCastPersonId = null
        pendingRestoreMoreLikeItemId = null
        pendingRestoreCollectionItemId = null
        pendingRestoreCompanyId = null
    }

    fun markCastMemberRestore(personId: Int) {
        capturePageScroll()
        restoreOnNextResume = lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
        restoreFocusToken = 0
        pendingRestoreType = RestoreTarget.CAST_MEMBER
        pendingRestoreEpisodeId = null
        pendingRestoreCastPersonId = personId
        pendingRestoreMoreLikeItemId = null
        pendingRestoreCollectionItemId = null
        pendingRestoreCompanyId = null
    }

    fun markMoreLikeThisRestore(itemId: String) {
        capturePageScroll()
        restoreOnNextResume = lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
        restoreFocusToken = 0
        pendingRestoreType = RestoreTarget.MORE_LIKE_THIS
        pendingRestoreEpisodeId = null
        pendingRestoreCastPersonId = null
        pendingRestoreMoreLikeItemId = itemId
        pendingRestoreCollectionItemId = null
        pendingRestoreCompanyId = null
    }

    fun markCollectionRestore(itemId: String) {
        capturePageScroll()
        restoreOnNextResume = lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
        restoreFocusToken = 0
        pendingRestoreType = RestoreTarget.COLLECTION
        pendingRestoreEpisodeId = null
        pendingRestoreCastPersonId = null
        pendingRestoreMoreLikeItemId = null
        pendingRestoreCollectionItemId = itemId
        pendingRestoreCompanyId = null
    }

    fun markCompanyRestore(companyId: Int) {
        capturePageScroll()
        restoreOnNextResume = lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
        pendingRestoreType = RestoreTarget.COMPANY_OR_NETWORK
        pendingRestoreEpisodeId = null
        pendingRestoreCastPersonId = null
        pendingRestoreMoreLikeItemId = null
        pendingRestoreCollectionItemId = null
        pendingRestoreCompanyId = companyId
    }

    LaunchedEffect(heroRestoreToken) {
        if (heroRestoreToken > 0) {
            markHeroRestore()
            restoreFocusToken += 1
        }
    }

    val childOverlayVisible = LocalDetailChildOverlayVisible.current
    fun launchPendingFocusRestore() {
        if (childOverlayVisible) return
        if (!restoreOnNextResume || pendingRestoreType == null) return
        restoreOnNextResume = false
        val index = savedRestoreScrollIndex
        val offset = savedRestoreScrollOffset
        val bumpCompany = pendingRestoreType == RestoreTarget.COMPANY_OR_NETWORK
        coroutineScope.launch {
            if (index >= 0) {
                animateDetailScrollTo(index, offset)
            }
            if (bumpCompany) {
                companyRestoreToken += 1
            }
            restoreFocusToken += 1
        }
    }

    LaunchedEffect(childClosedEpoch, childOverlayVisible) {
        if (childClosedEpoch > 0 && !childOverlayVisible) {
            launchPendingFocusRestore()
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                randomEpisodePlaybackPending = false
                launchPendingFocusRestore()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    var lastCastRowMetaId by remember { mutableStateOf(meta.id) }
    LaunchedEffect(meta.id) {
        if (lastCastRowMetaId == meta.id) return@LaunchedEffect
        lastCastRowMetaId = meta.id
        if (castRowListState.firstVisibleItemIndex != 0 ||
            castRowListState.firstVisibleItemScrollOffset != 0
        ) {
            castRowListState.scrollToItem(0)
        }
        if (moreLikeThisListState.firstVisibleItemIndex != 0 ||
            moreLikeThisListState.firstVisibleItemScrollOffset != 0
        ) {
            moreLikeThisListState.scrollToItem(0)
        }
        if (collectionListState.firstVisibleItemIndex != 0 ||
            collectionListState.firstVisibleItemScrollOffset != 0
        ) {
            collectionListState.scrollToItem(0)
        }
        if (commentsListState.firstVisibleItemIndex != 0 ||
            commentsListState.firstVisibleItemScrollOffset != 0
        ) {
            commentsListState.scrollToItem(0)
        }
    }

    LaunchedEffect(
        pendingRestoreType,
        pendingRestoreEpisodeId,
        selectedSeason,
        seasons
    ) {
        if (pendingRestoreType != RestoreTarget.EPISODE) return@LaunchedEffect
        val episodeId = pendingRestoreEpisodeId ?: return@LaunchedEffect
        val targetSeason = meta.videos
            .firstOrNull { it.id == episodeId }
            ?.season
            ?.takeIf { it in seasons }
            ?: return@LaunchedEffect
        if (targetSeason != selectedSeason) {
            onSeasonSelected(targetSeason)
        }
    }

    LaunchedEffect(
        meta.id,
        detailReturnEpisodeFocusRequest?.season,
        detailReturnEpisodeFocusRequest?.episode,
        selectedSeason,
        nextToWatch?.nextSeason,
        nextToWatch?.nextVideoId,
        episodesForSeason.size,
        episodesForSeason.firstOrNull()?.id,
        meta.videos.size
    ) {
        if (!isSeries) {
            return@LaunchedEffect
        }
        val request = detailReturnEpisodeFocusRequest
        if (request?.season == null || request.episode == null) {
            return@LaunchedEffect
        }
        val targetEpisode = resolveDetailReturnEpisodeFocusTarget(
            meta = meta,
            request = request
        )
        if (targetEpisode == null) {
            initialHeroFocusRequested = true
            return@LaunchedEffect
        }
        if (episodesForSeason.isEmpty()) {
            initialHeroFocusRequested = true
            return@LaunchedEffect
        }

        suspend fun applyReturnFocusStep(step: ReturnFocusStep) {
            when (step) {
                ReturnFocusStep.WaitForSeasonAdvance -> {
                    delay(RETURN_FOCUS_SEASON_ADVANCE_WAIT_MS)
                    // If nextToWatch advanced, this effect is cancelled and restarted.
                    // Otherwise fall through with the same snapshot as a fallback restore.
                    applyReturnFocusStep(
                        resolveReturnFocusStep(
                            playedSeason = targetEpisode.season,
                            playedEpisode = targetEpisode.episode,
                            selectedSeason = selectedSeason,
                            nextSeason = nextToWatch?.nextSeason,
                            availableSeasons = seasons,
                            allVideos = meta.videos,
                            requestedEpisodeId = targetEpisode.id,
                            episodesForSeason = episodesForSeason,
                            nextVideoId = nextToWatch?.nextVideoId,
                            alreadyRestoredId = lastReturnFocusRestoreId,
                            hasWaitedForSeasonAdvance = true
                        )
                    )
                }
                is ReturnFocusStep.SelectSeason -> {
                    onSeasonSelected(step.season)
                }
                is ReturnFocusStep.RestoreEpisode -> {
                    lastReturnFocusRestoreId = step.episodeId
                    // Prevent the default hero autofocus from stealing focus after the episode restore completes.
                    initialHeroFocusRequested = true
                    consumeReturnEpisodeFocusOnClear = step.consumeRequest
                    markEpisodeRestore(step.episodeId, restoreOnResume = false)
                    restoreFocusToken += 1
                    if (seasons.isNotEmpty()) {
                        // Ensure the episodes row is composed before requesting focus on a card,
                        // with the episodes block at its anchored position.
                        listState.scrollToItem(1, episodesBlockScrollOffsetPx())
                    }
                }
                ReturnFocusStep.Idle -> Unit
            }
        }

        applyReturnFocusStep(
            resolveReturnFocusStep(
                playedSeason = targetEpisode.season,
                playedEpisode = targetEpisode.episode,
                selectedSeason = selectedSeason,
                nextSeason = nextToWatch?.nextSeason,
                availableSeasons = seasons,
                allVideos = meta.videos,
                requestedEpisodeId = targetEpisode.id,
                episodesForSeason = episodesForSeason,
                nextVideoId = nextToWatch?.nextVideoId,
                alreadyRestoredId = lastReturnFocusRestoreId,
                hasWaitedForSeasonAdvance = false
            )
        )
    }

    // Track if scrolled past hero (first item)
    val isScrolledPastHero by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 ||
            (listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset > 200)
        }
    }

    val belowHeroAlpha = animateFloatAsState(
        targetValue = if (isTrailerPlaying) 0f else 1f,
        animationSpec = tween(NuvioMotion.tokens.durations.overlay),
        label = "belowHeroFade"
    )

    // Pre-compute cast members to avoid recomputation in lazy scope
    val castMembersToShow = remember(meta.castMembers, meta.cast) {
        if (meta.castMembers.isNotEmpty()) {
            meta.castMembers
        } else {
            meta.cast.map { name -> MetaCastMember(name = name) }
        }
    }

    fun isLeadCreditRole(role: String?): Boolean {
        val r = role?.trim().orEmpty()
        return r.equals("Creator", ignoreCase = true) ||
            r.equals("Director", ignoreCase = true) ||
            r.equals("Writer", ignoreCase = true)
    }

    val directorWriterMembers = remember(castMembersToShow) {
        val creators = castMembersToShow.filter { it.character.equals("Creator", ignoreCase = true) }
        val directors = castMembersToShow.filter { it.character.equals("Director", ignoreCase = true) }
        val writers = castMembersToShow.filter { it.character.equals("Writer", ignoreCase = true) }
        when {
            creators.isNotEmpty() -> creators
            directors.isNotEmpty() -> directors
            else -> writers
        }
    }

    val normalCastMembers = remember(castMembersToShow, directorWriterMembers) {
        val leadingKeys = directorWriterMembers.map {
            listOf(
                it.tmdbId?.toString().orEmpty(),
                it.name.trim().lowercase(),
                it.character.orEmpty().trim().lowercase()
            ).joinToString("|")
        }.toSet()
        castMembersToShow.filterNot {
            isLeadCreditRole(it.character) && listOf(
                it.tmdbId?.toString().orEmpty(),
                it.name.trim().lowercase(),
                it.character.orEmpty().trim().lowercase()
            ).joinToString("|") in leadingKeys
        }
    }
    val isTvShow = remember(meta.type, meta.apiType) {
        meta.type == ContentType.SERIES ||
            meta.type == ContentType.TV ||
            meta.apiType in listOf("series", "tv")
    }
    val hasCastSection = directorWriterMembers.isNotEmpty() || normalCastMembers.isNotEmpty()
    val hasMoreLikeThisSection = moreLikeThis.isNotEmpty()
    val hasTrailerSection = remember(meta.trailers) { meta.trailers.any { !it.ytId.isNullOrBlank() } }
    val showEpisodeImdbRatings = detailImdbRatingsVisibility.showEpisodeRatings
    val visibleEpisodeImdbRatings = remember(
        episodeImdbRatings,
        detailImdbRatingsVisibility,
        episodeProgressMap,
        watchedEpisodes
    ) {
        episodeImdbRatings.filterKeys { episodeKey ->
            val isWatched = episodeProgressMap[episodeKey]?.isCompleted() == true ||
                watchedEpisodes.contains(episodeKey)
            detailImdbRatingsVisibility.showEpisodeRating(isWatched)
        }
    }
    val showStandardOverallRatings = overallRatingsVisibility
        .showStandardDetailRatings(isMdbListRatingsActive)
    val visibleMdbListRatings = mdbListRatings.takeIf { isMdbListRatingsActive }
    val hasRatingsSection = isTvShow && showEpisodeImdbRatings
    val strTabRatings = stringResource(R.string.detail_tab_ratings)
    val strTabMoreLikeThis = stringResource(R.string.detail_tab_more_like_this)
    val strTrailers = stringResource(R.string.tmdb_trailers_title)
    val strTabCollection = stringResource(R.string.tmdb_collections_title)
    val moreLikeThisSourceLabel = when (moreLikeThisSource) {
        MoreLikeThisSource.TMDB -> stringResource(R.string.detail_more_like_this_powered_by_tmdb)
        MoreLikeThisSource.TRAKT -> stringResource(R.string.detail_more_like_this_powered_by_trakt)
        MoreLikeThisSource.SIMKL -> stringResource(R.string.detail_more_like_this_powered_by_simkl)
        null -> null
    }
    val hasCollectionSection = collection.isNotEmpty()
    // Nuvio Launcher: the rows under the episodes, in page order; only rows with data are listed.
    val stackedRowKeys = remember(
        hasCastSection,
        hasRatingsSection,
        hasMoreLikeThisSection,
        hasTrailerSection,
        hasCollectionSection,
        shouldShowCommentsSection,
        isTvShow,
        meta.networks.isNotEmpty(),
        meta.productionCompanies.isNotEmpty()
    ) {
        buildList {
            if (hasCastSection) add(ROW_CAST)
            if (hasRatingsSection) add(ROW_RATINGS)
            if (hasMoreLikeThisSection) add(ROW_MORE_LIKE_THIS)
            if (hasTrailerSection) add(ROW_TRAILERS)
            if (hasCollectionSection) add(ROW_COLLECTION)
            if (shouldShowCommentsSection) add(ROW_COMMENTS)
            if (isTvShow) {
                if (meta.networks.isNotEmpty()) add(ROW_NETWORKS)
                if (meta.productionCompanies.isNotEmpty()) add(ROW_PRODUCTION)
            } else {
                if (meta.productionCompanies.isNotEmpty()) add(ROW_PRODUCTION)
                if (meta.networks.isNotEmpty()) add(ROW_NETWORKS)
            }
        }
    }
    val showSeasonTabsItem = isSeries && seasons.isNotEmpty() &&
        !(seasons.size == 1 && meta.apiType.equals("other", ignoreCase = true))
    val commentsItemIndex = remember(isSeries, seasons, showSeasonTabsItem, stackedRowKeys) {
        // hero, then the season tabs and the episodes, then the stacked rows above the comments
        var index = 1
        if (showSeasonTabsItem) index += 1
        if (isSeries && seasons.isNotEmpty()) index += 1
        index + stackedRowKeys.indexOf(ROW_COMMENTS).coerceAtLeast(0)
    }
    var seasonOptionsDialogSeason by remember { mutableStateOf<Int?>(null) }
    // Tracks whether the initial auto-scroll to the "next to play" episode has fired.
    // Once it fires, no more auto-scrolls happen for the lifetime of this detail screen.
    var initialEpisodeScrollDone by remember(meta.id) { mutableStateOf(false) }
    val episodeFocusRequestersBySeason = remember(meta.id) { mutableMapOf<Int, MutableMap<String, FocusRequester>>() }
    val seasonEpisodeFocusRequesters = remember(selectedSeason, episodesForSeason) {
        val byEpisodeId = episodeFocusRequestersBySeason.getOrPut(selectedSeason) { mutableMapOf() }
        episodesForSeason.forEach { episode ->
            if (!byEpisodeId.containsKey(episode.id)) {
                byEpisodeId[episode.id] = FocusRequester()
            }
        }
        byEpisodeId.keys.retainAll(episodesForSeason.map { it.id }.toSet())
        byEpisodeId
    }
    val seasonDownFocusRequester = remember(selectedSeason, episodesForSeason, seasonEpisodeFocusRequesters, lastFocusedEpisodeIdBySeason[selectedSeason], nextToWatch, defaultSeriesVideo, pendingRestoreType, pendingRestoreEpisodeId) {
        val nextEpisodeId = if (pendingRestoreType == RestoreTarget.EPISODE) {
            null
        } else {
            nextToWatch?.nextVideoId
                ?: nextToWatch?.let { ntw -> episodesForSeason.firstOrNull { it.season == ntw.nextSeason && it.episode == ntw.nextEpisode }?.id }
                ?: defaultSeriesVideo?.id?.takeIf { defaultId -> episodesForSeason.any { it.id == defaultId } }
        }
        val preferredEpisodeId = lastFocusedEpisodeIdBySeason[selectedSeason]
            ?: nextEpisodeId?.takeIf { episodesForSeason.any { ep -> ep.id == it } }
        (preferredEpisodeId?.let { seasonEpisodeFocusRequesters[it] })
            ?: episodesForSeason.firstOrNull()?.id?.let { seasonEpisodeFocusRequesters[it] }
    }

    val canToggleEpisodeComments = isSeries && episodesForSeason.isNotEmpty()
    val commentsSelectedModeFocusRequester =
        if (commentsMode == CommentsMode.EPISODE) commentsEpisodeModeFocusRequester else commentsTitleModeFocusRequester
    // Nuvio Launcher: Up/Down between the stacked rows is plain D-pad focus search; every row's tiles
    // sit in a focus group with a focusRestorer, so entering a row lands on the tile it last had
    // (the first tile if it never had focus). Only two moves are pinned:
    //  - Up from the first stacked row goes back to the episode last focused (or Play on a movie);
    //  - Down into the reviews lands on the review mode buttons, when there are any.
    val firstStackedRowUpFocusRequester = seasonDownFocusRequester ?: heroPlayFocusRequester
    fun stackedRowUpFocusRequester(rowKey: String): FocusRequester? =
        if (stackedRowKeys.firstOrNull() == rowKey) firstStackedRowUpFocusRequester else null
    fun stackedRowDownFocusRequester(rowKey: String): FocusRequester? {
        val index = stackedRowKeys.indexOf(rowKey)
        val next = if (index < 0) stackedRowKeys.firstOrNull() else stackedRowKeys.getOrNull(index + 1)
        return if (next == ROW_COMMENTS && canToggleEpisodeComments) commentsSelectedModeFocusRequester else null
    }
    // the episode tiles' Down: the first stacked row (see stackedRowDownFocusRequester)
    val episodesDownFocusRequester = stackedRowDownFocusRequester("")

    // Backdrop alpha for crossfade
    // Nuvio Launcher: the page's ground is the launcher grey; the backdrop's left side fades into it.
    val backgroundColor = LauncherStyle.GreyBottom

    // Pre-compute gradient brushes once

    // Stable hero play callback
    val heroPlayClick = remember(heroVideo, meta.id, onEpisodeClick, onPlayClick, isPlayEnabled) {
        {
            if (isPlayEnabled) markHeroRestore()
            if (heroVideo != null) {
                onEpisodeClick(heroVideo)
            } else {
                onPlayClick(meta.id)
            }
        }
    }
    val heroPlayManualClick = remember(heroVideo, meta.id, onEpisodeManualPlayClick, onPlayManuallyClick, isPlayEnabled) {
        {
            if (isPlayEnabled) markHeroRestore()
            if (heroVideo != null) {
                onEpisodeManualPlayClick(heroVideo)
            } else {
                onPlayManuallyClick(meta.id)
            }
        }
    }
    val heroPlayStartFromBeginningClick = remember(heroVideo, meta.id, onEpisodeStartFromBeginningClick, onPlayStartFromBeginningClick, isPlayEnabled) {
        {
            if (isPlayEnabled) markHeroRestore()
            if (heroVideo != null) {
                onEpisodeStartFromBeginningClick(heroVideo)
            } else {
                onPlayStartFromBeginningClick(meta.id)
            }
        }
    }
    val episodeClick = remember(onEpisodeClick, canPlayEpisode) {
        { video: Video ->
            if (canPlayEpisode(video)) markEpisodeRestore(video.id)
            onEpisodeClick(video)
        }
    }
    val episodeManualClick = remember(onEpisodeManualPlayClick, canPlayEpisode) {
        { video: Video ->
            if (canPlayEpisode(video)) markEpisodeRestore(video.id)
            onEpisodeManualPlayClick(video)
        }
    }
    val episodeCommentsClick = remember(
        onCommentsEpisodeSelected,
        shouldShowCommentsSection
    ) {
        { video: Video ->
            if (shouldShowCommentsSection) {
                onCommentsEpisodeSelected(video)
                commentsEntryFocusToken += 1
            }
            Unit
        }
    }

    LaunchedEffect(commentsEntryFocusToken, shouldShowCommentsSection, commentsItemIndex) {
        if (commentsEntryFocusToken <= 0 || !shouldShowCommentsSection) return@LaunchedEffect
        listState.animateScrollToItem(commentsItemIndex)
    }

    LaunchedEffect(
        pendingRestoreType,
        pendingRestoreEpisodeId,
        initialHeroFocusRequested,
        isTrailerPlaying,
        detailReturnEpisodeFocusRequest?.season,
        detailReturnEpisodeFocusRequest?.episode
    ) {
        if (
            !initialHeroFocusRequested &&
            pendingRestoreType == null &&
            pendingRestoreEpisodeId == null &&
            detailReturnEpisodeFocusRequest?.season == null &&
            detailReturnEpisodeFocusRequest?.episode == null &&
            !isTrailerPlaying
        ) {
            repeat(3) {
                if (initialHeroFocusRequested) return@repeat
                heroPlayButtonFocusRequester.requestFocusAfterFrames()
                delay(80)
            }
        }
    }

    // Pre-compute screen dimensions to avoid BoxWithConstraints subcomposition overhead
    val configuration = LocalConfiguration.current
    val localContext = LocalContext.current
    val localDensity = LocalDensity.current
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val screenWidthDp = remember(configuration) { configuration.screenWidthDp.dp }
    val screenHeightDp = remember(configuration) { configuration.screenHeightDp.dp }
    val backdropWidthPx = remember(screenWidthDp, localDensity) {
        with(localDensity) { screenWidthDp.roundToPx() }
    }
    val backdropHeightPx = remember(screenHeightDp, localDensity) {
        with(localDensity) { screenHeightDp.roundToPx() }
    }
    val hasHeroBackdrop = !heroBackdropUrl.isNullOrBlank()
    val seedBackdropUrl = heroBackdropUrl?.takeIf { it.isNotBlank() }
    val backdropDataUrl = meta.backdropUrl ?: meta.poster
    // Nuvio Launcher: the left and bottom shade take the title's own colour (its strongest hue, kept dark so
    // white text stays readable) instead of always the launcher grey. Worked out once per title from a 24x14
    // copy of the backdrop; grey while it loads or when the picture has no real colour.
    var shadeColor by remember(backdropDataUrl) { mutableStateOf(backgroundColor) }
    // Nuvio Launcher (approved mockup): true when the picture behind the details block (left 60%, lower 65%) is
    // bright; a dark scrim then goes behind the text so it and the white title logo stay readable.
    var backdropBehindTextIsBright by remember(backdropDataUrl) { mutableStateOf(false) }
    LaunchedEffect(backdropDataUrl) {
        val u = backdropDataUrl ?: return@LaunchedEffect
        val result = runCatching {
            localContext.imageLoader.execute(
                ImageRequest.Builder(localContext).data(u).size(24, 14)
                    .allowHardware(false)   // the pixels are read, so a software bitmap
                    .memoryCacheKey("launcher_shade_" + u)
                    .build()
            )
        }.getOrNull()
        val bmp = (result as? coil3.request.SuccessResult)?.image?.let { (it as? coil3.BitmapImage)?.bitmap }
        if (bmp != null) {
            backdropBehindTextIsBright = launcherBrightBehindText(bmp, rtl = isRtl)
            if (DetailsHeroShade) launcherDominantShade(bmp)?.let { shadeColor = it }
        }
    }
    val shouldReuseSeedBackdrop = seedBackdropUrl != null && seedBackdropUrl == backdropDataUrl
    val shouldShowSeedBackdropUnderlay = seedBackdropUrl != null && !shouldReuseSeedBackdrop
    val heroBackdropRequest = remember(
        localContext,
        seedBackdropUrl,
        backdropWidthPx,
        backdropHeightPx
    ) {
        seedBackdropUrl?.let {
            ImageRequest.Builder(localContext)
                .data(it)
                .crossfade(false)
                .size(width = backdropWidthPx, height = backdropHeightPx)
                .build()
        }
    }
    val backdropRequest = remember(
        localContext,
        backdropDataUrl,
        shouldReuseSeedBackdrop,
        hasHeroBackdrop,
        heroBackdropRequest,
        backdropWidthPx,
        backdropHeightPx
    ) {
        if (shouldReuseSeedBackdrop && heroBackdropRequest != null) {
            heroBackdropRequest
        } else {
            ImageRequest.Builder(localContext)
                .data(backdropDataUrl)
                .apply { if (shouldShowSeedBackdropUnderlay) crossfade(400) else if (hasHeroBackdrop) crossfade(false) else crossfade(400) }
                .size(width = backdropWidthPx, height = backdropHeightPx)
                .build()
        }
    }

    val leftGradientBitmap = remember(shadeColor, backdropWidthPx, backdropHeightPx, isRtl) {
        val w = backdropWidthPx.coerceAtLeast(1)
        val h = backdropHeightPx.coerceAtLeast(1)
        val transparent = shadeColor.copy(alpha = 0f * LeftShadeOpacity).toArgb()
        val bmp = android.graphics.Bitmap.createBitmap(w, 2, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bmp)
        val fadeWidth = w * 0.62f
        val shader = if (isRtl) {
            android.graphics.LinearGradient(
                w.toFloat(), 0f, w - fadeWidth, 0f,
                intArrayOf(
                    shadeColor.copy(alpha = 1f * LeftShadeOpacity).toArgb(),
                    shadeColor.copy(alpha = 1f * LeftShadeOpacity).toArgb(),
                    shadeColor.copy(alpha = 0.92f * LeftShadeOpacity).toArgb(),
                    shadeColor.copy(alpha = 0.72f * LeftShadeOpacity).toArgb(),
                    shadeColor.copy(alpha = 0.52f * LeftShadeOpacity).toArgb(),
                    shadeColor.copy(alpha = 0.32f * LeftShadeOpacity).toArgb(),
                    shadeColor.copy(alpha = 0.16f * LeftShadeOpacity).toArgb(),
                    shadeColor.copy(alpha = 0.06f * LeftShadeOpacity).toArgb(),
                    transparent
                ),
                floatArrayOf(0f, 0.36f, 0.46f, 0.56f, 0.66f, 0.76f, 0.85f, 0.93f, 1f),
                android.graphics.Shader.TileMode.CLAMP
            )
        } else {
            android.graphics.LinearGradient(
                0f, 0f, fadeWidth, 0f,
                intArrayOf(
                    shadeColor.copy(alpha = 1f * LeftShadeOpacity).toArgb(),
                    shadeColor.copy(alpha = 1f * LeftShadeOpacity).toArgb(),
                    shadeColor.copy(alpha = 0.92f * LeftShadeOpacity).toArgb(),
                    shadeColor.copy(alpha = 0.72f * LeftShadeOpacity).toArgb(),
                    shadeColor.copy(alpha = 0.52f * LeftShadeOpacity).toArgb(),
                    shadeColor.copy(alpha = 0.32f * LeftShadeOpacity).toArgb(),
                    shadeColor.copy(alpha = 0.16f * LeftShadeOpacity).toArgb(),
                    shadeColor.copy(alpha = 0.06f * LeftShadeOpacity).toArgb(),
                    transparent
                ),
                floatArrayOf(0f, 0.36f, 0.46f, 0.56f, 0.66f, 0.76f, 0.85f, 0.93f, 1f),
                android.graphics.Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, w.toFloat(), 2f, android.graphics.Paint().apply {
            this.shader = shader
        })
        bmp.asImageBitmap()
    }
    val bottomGradientBitmap = remember(shadeColor, backdropWidthPx, backdropHeightPx) {
        val w = backdropWidthPx.coerceAtLeast(1)
        val h = backdropHeightPx.coerceAtLeast(1)
        val transparent = shadeColor.copy(alpha = 0f).toArgb()
        val bmp = android.graphics.Bitmap.createBitmap(2, h, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bmp)
        val startY = h * 0.38f
        val shader = android.graphics.LinearGradient(
            0f, startY, 0f, h.toFloat(),
            intArrayOf(
                transparent,
                shadeColor.copy(alpha = 0.05f).toArgb(),
                shadeColor.copy(alpha = 0.18f).toArgb(),
                shadeColor.copy(alpha = 0.38f).toArgb(),
                shadeColor.copy(alpha = 0.60f).toArgb(),
                shadeColor.copy(alpha = 0.78f).toArgb(),
                shadeColor.copy(alpha = 0.91f).toArgb(),
                shadeColor.copy(alpha = 0.97f).toArgb(),
                shadeColor.copy(alpha = 1f).toArgb()
            ),
            floatArrayOf(0f, 0.10f, 0.22f, 0.36f, 0.52f, 0.66f, 0.78f, 0.90f, 1f),
            android.graphics.Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, 2f, h.toFloat(), android.graphics.Paint().apply {
            this.shader = shader
        })
        bmp.asImageBitmap()
    }

    // Animated gradient alpha (moved outside subcomposition scope)

    // Always-composed bottom gradient alpha (avoids add/remove during scroll)

    // Nuvio Launcher: once focus is in the rows, the title's backdrop, blurred, is the page's background.
    val blurredBackdropAlpha by animateFloatAsState(
        targetValue = if (isScrolledPastHero && !isTrailerPlaying) 1f else 0f,
        animationSpec = tween(durationMillis = 400),
        label = "blurredBackdropFade"
    )
    // The "Episodes" title above the season pills stays white while the episode tiles have focus.
    var episodesRowFocused by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
            .onPreviewKeyEvent { randomEpisodePlaybackPending }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = blurredBackdropAlpha }
        ) {
            LaunchedEffect(backdropDataUrl) { if (backdropDataUrl != null) com.nuvio.tv.LauncherChrome.lastBackdrop.value = backdropDataUrl }
            LauncherBlurredBackdrop(url = backdropDataUrl)
        }
        // Sticky background — backdrop or trailer
        BackdropLayer(
            backdropRequest = backdropRequest,
            heroBackdropRequest = if (shouldShowSeedBackdropUnderlay) heroBackdropRequest else null,
            trailerUrl = trailerUrl,
            trailerAudioUrl = trailerAudioUrl,
            isTrailerPlaying = isTrailerPlaying,
            isBackgroundTrailerPlaying = isBackgroundTrailerPlaying,
            pauseBackgroundTrailerOnScroll = pauseBackgroundTrailerOnScroll,
            isTrailerPaused = isTrailerPaused,
            showTrailerControls = showTrailerControls,
            trailerSeekToken = trailerSeekToken,
            trailerSeekDeltaMs = trailerSeekDeltaMs,
            onTrailerControlKey = onTrailerControlKey,
            onTrailerProgressChanged = onTrailerProgressChanged,
            onTrailerEnded = onTrailerEnded,
            isScrolledPastHero = isScrolledPastHero,
            leftGradient = leftGradientBitmap,
            bottomGradient = bottomGradientBitmap,
        )

        // Nuvio Launcher (approved mockup): on a bright picture, a dark scrim behind the details block: black 70% at
        // the text side's edge, 60% a third of the way across, 30% at 58%, gone by 75%; plus a light shade along the
        // bottom (25% at the edge). Only over the hero, and not while a trailer plays.
        val brightScrimAlpha by animateFloatAsState(
            targetValue = if (backdropBehindTextIsBright && !isScrolledPastHero && !isTrailerPlaying) 1f else 0f,
            animationSpec = tween(durationMillis = 400),
            label = "brightBackdropScrim"
        )
        if (brightScrimAlpha > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = brightScrimAlpha }
                    .drawWithCache {
                        val stops = arrayOf(
                            0f to Color.Black.copy(alpha = 0.70f),
                            0.35f to Color.Black.copy(alpha = 0.60f),
                            0.58f to Color.Black.copy(alpha = 0.30f),
                            0.75f to Color.Transparent
                        )
                        val across = if (isRtl) {
                            Brush.horizontalGradient(*stops, startX = size.width, endX = 0f)
                        } else {
                            Brush.horizontalGradient(*stops, startX = 0f, endX = size.width)
                        }
                        val down = Brush.verticalGradient(
                            0.30f to Color.Transparent,
                            1f to Color.Black.copy(alpha = 0.25f),
                            startY = 0f,
                            endY = size.height
                        )
                        onDrawBehind {
                            drawRect(brush = across)
                            drawRect(brush = down)
                        }
                    }
            )
        }

        // Single scrollable column with hero + content
        CompositionLocalProvider(
            // The hero grows to full height during a trailer; keep the list from
            // scrolling to follow the focused hero while it resizes.
            LocalBringIntoViewSpec provides if (suppressRestoreBringIntoView || isTrailerPlaying) {
                restoreNoScrollBringIntoViewSpec
            } else {
                detailPageBringIntoViewSpec
            }
        ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .recompositionHighlighter()
                .fadeBelowHero(listState, belowHeroAlpha)
                .onPreviewKeyEvent { event ->
                    val native = event.nativeKeyEvent
                    if (native.action == KeyEvent.ACTION_DOWN) {
                        when (native.keyCode) {
                            KeyEvent.KEYCODE_DPAD_UP,
                            KeyEvent.KEYCODE_DPAD_DOWN,
                            KeyEvent.KEYCODE_DPAD_LEFT,
                            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                                lastDetailDpadKey = native.keyCode
                                tapScroll.lastKey = native.keyCode
                            }
                        }
                        if (native.keyCode == KeyEvent.KEYCODE_DPAD_DOWN && native.repeatCount == 0) {
                            val now = SystemClock.uptimeMillis()
                            tapScroll.rapidDown = now - tapScroll.lastDownAt <= RAPID_DOWN_TAP_MS
                            tapScroll.lastDownAt = now
                            if (tapScroll.rapidDown) {
                                coroutineScope.launch {
                                    val beforeIndex = listState.firstVisibleItemIndex
                                    val beforeOffset = listState.firstVisibleItemScrollOffset
                                    repeat(2) { withFrameNanos { } }
                                    val moved = listState.firstVisibleItemIndex != beforeIndex ||
                                        kotlin.math.abs(listState.firstVisibleItemScrollOffset - beforeOffset) > 2
                                    if (moved || listState.isScrollInProgress || !listState.canScrollForward) {
                                        return@launch
                                    }
                                    val remaining = remainingDetailScrollPx()
                                    val step = when {
                                        remaining <= 0 -> return@launch
                                        remaining == Int.MAX_VALUE -> with(localDensity) { 240.dp.toPx() }
                                        else -> remaining.toFloat()
                                    }
                                    listState.scroll { scrollBy(step) }
                                }
                            }
                        } else if (native.keyCode != KeyEvent.KEYCODE_DPAD_DOWN) {
                            tapScroll.rapidDown = false
                        }
                    }
                    false
                }
                .dpadVerticalFastScroll(
                    scrollableState = listState,
                    throttleHorizontalRepeats = false,
                    onFastScrollingChanged = { active ->
                        if (active) {
                            detailFastScrollToken += 1
                            detailFastScrolling = true
                            pinnedPageIndex = -1
                            pinnedPageOffset = 0
                        }
                    },
                    shouldHaltForward = {
                        val info = listState.layoutInfo
                        val lastIdx = info.totalItemsCount - 1
                        val lastVisible = info.visibleItemsInfo.lastOrNull { it.index == lastIdx }
                        lastIdx >= 0 && lastVisible != null &&
                            lastVisible.offset + lastVisible.size <= info.viewportEndOffset
                    },
                    resolveVerticalLanding = { sign ->
                        val layoutInfo = listState.layoutInfo
                        val visibleItems = layoutInfo.visibleItemsInfo
                        val lastIdx = layoutInfo.totalItemsCount - 1
                        val viewportEnd = layoutInfo.viewportEndOffset
                        val lastItemAtBottom = lastIdx >= 0 &&
                            visibleItems.lastOrNull { it.index == lastIdx }?.let {
                                it.offset + it.size <= viewportEnd
                            } == true
                        val upwardTopItem = if (sign < 0) {
                            visibleItems.firstOrNull()?.takeIf { it.offset > -it.size / 2 }
                        } else {
                            null
                        }
                        val target = when {
                            lastItemAtBottom -> visibleItems.lastOrNull { it.index == lastIdx }
                            upwardTopItem != null -> upwardTopItem
                            else -> visibleItems.firstOrNull { it.offset >= 0 }
                                ?: visibleItems.firstOrNull()
                        }
                        fun requesterForKey(key: Any?): FocusRequester? {
                            val name = key as? String ?: return null
                            return when {
                                name == "hero" -> heroPlayButtonFocusRequester
                                name == "season_tabs" -> selectedSeasonFocusRequester
                                name.startsWith("episodes_") ->
                                    seasonDownFocusRequester ?: selectedSeasonFocusRequester
                                name == ROW_CAST -> castSectionFocusRequester
                                name == ROW_RATINGS -> ratingsContentFocusRequester
                                name == ROW_MORE_LIKE_THIS -> moreLikeSectionFocusRequester
                                name == ROW_TRAILERS -> trailerSectionFocusRequester
                                name == ROW_COLLECTION -> collectionSectionFocusRequester
                                name == ROW_COMMENTS -> when {
                                    comments.isNotEmpty() -> commentsRowEntryFocusRequester
                                    canToggleEpisodeComments -> commentsSelectedModeFocusRequester
                                    else -> commentsRowEntryFocusRequester
                                }
                                name == "networks" -> networkSectionFocusRequester
                                name == "production" -> productionSectionFocusRequester
                                else -> null
                            }
                        }
                        val primary = requesterForKey(target?.key)
                        val fallback = visibleItems.firstNotNullOfOrNull { item ->
                            requesterForKey(item.key)?.takeIf { it != primary }
                        }
                        val landingToken = detailFastScrollToken
                        coroutineScope.launch {
                            try {
                                val candidates = listOfNotNull(primary, fallback)
                                repeat(6) {
                                    for (requester in candidates) {
                                        val focused = runCatching {
                                            requester.requestFocus(FocusDirection.Enter)
                                        }.getOrDefault(false)
                                        if (focused) return@launch
                                    }
                                    withFrameNanos { }
                                }
                            } finally {
                                if (detailFastScrollToken == landingToken) {
                                    detailFastScrolling = false
                                }
                            }
                        }
                        null
                    }
                ),
            state = listState
        ) {
            // Hero as first item in the lazy column
            item(key = "hero", contentType = "hero") {
                Box(modifier = Modifier.bringIntoViewResponder(heroNoScrollResponder)) {
                    HeroContentSection(
                        meta = meta,
                        nextEpisode = nextEpisode,
                        nextToWatch = nextToWatch,
                        onPlayClick = { if (shufflePoolEmpty) showRandomEpisodeOverlay = true else heroPlayClick() },
                        isPlayEnabled = shufflePoolEmpty || isPlayEnabled,
                        onPlayLongPress = if (!shufflePoolEmpty && isPlayEnabled && (showManualPlayOption || nextToWatch?.isResume == true)) {
                            { showHeroPlayOptionsDialog = true }
                        } else {
                            null
                        },
                        isInLibrary = isInLibrary,
                        onToggleLibrary = onToggleLibrary,
                        onLibraryLongPress = onLibraryLongPress,
                        isMovieWatched = isMovieWatched,
                        isMovieWatchedPending = isMovieWatchedPending,
                        onToggleMovieWatched = onToggleMovieWatched,
                        mdbListRatings = visibleMdbListRatings,
                        mdbListRatingOrder = mdbListRatingOrder,
                        hideMetaInfoImdb = !showStandardOverallRatings,
                        tmdbRating = tmdbRating.takeIf { showStandardOverallRatings },
                        showFullReleaseDate = showFullReleaseDate,
                        trailerAvailable = trailerButtonEnabled && !trailerUrl.isNullOrBlank(),
                        onTrailerClick = onTrailerButtonClick,
                        showRandomEpisodeButton = showRandomEpisodeButton,
                        onRandomEpisodeClick = {
                            if (!episodeShuffle.enabled) {
                                showRandomEpisodeOverlay = true
                            } else if (!stoppingShuffle) {
                                stoppingShuffle = true
                                coroutineScope.launch {
                                    try {
                                        onEpisodeShuffleChange(episodeShuffle.copy(enabled = false))
                                    } finally {
                                        stoppingShuffle = false
                                    }
                                }
                            }
                        },
                        episodeShuffle = episodeShuffle,
                        shuffleActionPending = stoppingShuffle,
                        randomEpisodeFocusRequester = randomEpisodeFocusRequester,
                        hideLogoDuringTrailer = hideLogoDuringTrailer,
                        isTrailerPlaying = isTrailerPlaying,
                        playButtonFocusRequester = heroPlayButtonFocusRequester,
                        onHeroActionFocused = {
                            if (listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0) {
                                coroutineScope.launch {
                                    listState.animateScrollToItem(0)
                                }
                            }
                            initialHeroFocusRequested = true
                            if (pendingRestoreType != RestoreTarget.HERO) {
                                clearPendingRestore()
                            }
                        },
                        restorePlayFocusToken = (if (pendingRestoreType == RestoreTarget.HERO) restoreFocusToken else 0) +
                                restorePlayFocusAfterTrailerBackToken,
                        onPlayFocusRestored = {
                            onPlayButtonFocused()
                            initialHeroFocusRequested = true
                            clearPendingRestore()
                        },
                        onShowFullDescription = { showSynopsisOverlay = true },
                        onTruncationChanged = { synopsisTruncated = it }
                    )
                }
            }

            // Season tabs and episodes for series
            val showSeasonTabs = isSeries && seasons.isNotEmpty() && !(seasons.size == 1 && meta.apiType.equals("other", ignoreCase = true))
            val showEpisodesRow = isSeries && seasons.isNotEmpty()
            if (showSeasonTabs) {
                item(key = "season_tabs", contentType = "season_tabs") {
                    Box(
                        modifier = Modifier
                            .bringIntoViewResponder(seasonTabsAnchorResponder)
                            .onSizeChanged { seasonTabsHeightPx = it.height }
                    ) {
                        SeasonTabs(
                            seasons = seasons,
                            selectedSeason = selectedSeason,
                            onSeasonSelected = onSeasonSelected,
                            onSeasonLongPress = { seasonOptionsDialogSeason = it },
                            selectedTabFocusRequester = selectedSeasonFocusRequester,
                            upFocusRequester = heroPlayFocusRequester,
                            downFocusRequester = seasonDownFocusRequester,
                            isFocusEnabled = pendingRestoreType != RestoreTarget.EPISODE,
                            titleHighlighted = episodesRowFocused
                        )
                    }
                }
            }
            if (showEpisodesRow) {
                item(key = "episodes_$selectedSeason", contentType = "episodes") {
                    val visibleEpisodeRestoreId = if (pendingRestoreType == RestoreTarget.EPISODE) {
                        resolveVisibleEpisodeRestoreId(
                            requestedId = pendingRestoreEpisodeId,
                            episodesForSeason = episodesForSeason,
                            nextVideoId = nextToWatch?.nextVideoId
                        )
                    } else {
                        null
                    }
                    Box(
                        modifier = Modifier
                            .bringIntoViewResponder(episodeRowStayVerticalResponder)
                            .onFocusChanged { episodesRowFocused = it.hasFocus }
                    ) {
                        EpisodesRow(
                            episodes = episodesForSeason,
                            episodeProgressMap = episodeProgressMap,
                            episodeRatings = visibleEpisodeImdbRatings,
                            watchedEpisodes = watchedEpisodes,
                            episodeWatchedPendingKeys = episodeWatchedPendingKeys,
                            blurUnwatchedEpisodes = blurUnwatchedEpisodes,
                            episodeOptionsOverlayStyle = episodeOptionsOverlayStyle,
                            posterCardCornerRadiusDp = posterCardCornerRadiusDp,
                            onEpisodeClick = episodeClick,
                            canPlayEpisode = canPlayEpisode,
                            onEpisodeManualPlayClick = episodeManualClick,
                            onEpisodeStartFromBeginningClick = { video ->
                                if (canPlayEpisode(video)) markEpisodeRestore(video.id)
                                onEpisodeStartFromBeginningClick(video)
                            },
                            showManualPlayOption = showManualPlayOption,
                            onToggleEpisodeWatched = onToggleEpisodeWatched,
                            onMarkSeasonWatched = onMarkSeasonWatched,
                            onMarkSeasonUnwatched = onMarkSeasonUnwatched,
                            isSeasonFullyWatched = isSeasonFullyWatched(selectedSeason),
                            selectedSeason = selectedSeason,
                            onOpenEpisodeComments = episodeCommentsClick,
                            showOpenEpisodeComments = shouldShowCommentsSection,
                            onMarkPreviousEpisodesWatched = onMarkPreviousEpisodesWatched,
                            upFocusRequester = if (showSeasonTabs) selectedSeasonFocusRequester else (heroPlayFocusRequester ?: heroPlayButtonFocusRequester),
                            downFocusRequester = episodesDownFocusRequester,
                            episodeFocusRequesters = seasonEpisodeFocusRequesters,
                            restoreEpisodeId = visibleEpisodeRestoreId,
                            restoreFocusToken = if (pendingRestoreType == RestoreTarget.EPISODE) restoreFocusToken else 0,
                            onRestoreFocusHandled = {
                                val isStaleEpisodeRestore =
                                    pendingRestoreType == RestoreTarget.EPISODE &&
                                        pendingRestoreEpisodeId != null &&
                                        visibleEpisodeRestoreId != null &&
                                        pendingRestoreEpisodeId != visibleEpisodeRestoreId
                                if (!isStaleEpisodeRestore) {
                                    clearPendingRestore()
                                }
                            },
                            onEpisodeFocused = { episodeId ->
                                lastFocusedEpisodeIdBySeason[selectedSeason] = episodeId
                                if (lastDetailDpadKey == KeyEvent.KEYCODE_DPAD_UP) {
                                    val episodeListItemIndex = 1 + if (showSeasonTabs) 1 else 0
                                    val episodeItem = listState.layoutInfo.visibleItemsInfo
                                        .firstOrNull { it.index == episodeListItemIndex }
                                    if (episodeItem == null || episodeItem.offset < 0) {
                                        coroutineScope.launch {
                                            listState.animateScrollToItem(1, episodesBlockScrollOffsetPx())
                                        }
                                    }
                                }
                            },
                            scrollToEpisodeId = if (lastFocusedEpisodeIdBySeason[selectedSeason] != null) {
                                null
                            } else if (!initialEpisodeScrollDone && pendingRestoreType != RestoreTarget.EPISODE) {
                                val ntwId = nextToWatch?.nextVideoId
                                    ?: nextToWatch?.let { ntw -> episodesForSeason.firstOrNull { it.season == ntw.nextSeason && it.episode == ntw.nextEpisode }?.id }
                                if (ntwId != null) {
                                    ntwId
                                } else if (nextToWatch != null) {
                                    // nextToWatch resolved but target is in a different season — mark done and fall through.
                                    initialEpisodeScrollDone = true
                                    defaultSeriesVideo?.id?.takeIf { defaultId -> episodesForSeason.any { it.id == defaultId } }
                                } else {
                                    // nextToWatch not yet calculated — emit null so LaunchedEffect waits.
                                    null
                                }
                            } else if (lastFocusedEpisodeIdBySeason[selectedSeason] == null && !initialEpisodeScrollDone && pendingRestoreType != RestoreTarget.EPISODE) {
                                // Initial scroll not yet done; fall back to default only if user hasn't focused anything yet.
                                defaultSeriesVideo?.id?.takeIf { defaultId -> episodesForSeason.any { it.id == defaultId } }
                            } else null,
                            onScrollToEpisodeHandled = {
                                initialEpisodeScrollDone = true
                            },
                            anchorEpisodeId = lastFocusedEpisodeIdBySeason[selectedSeason],
                            windowResetKey = meta.id
                        )
                    }
            }
        }

        // Nuvio Launcher: the rows under the episodes, stacked one under another, each with its own
        // title (white while the row has focus). Up/Down moves row to row, each row coming back to
        // the tile it last had; Left/Right stays in the row; Right on a row's last tile does nothing.
        fun stackedRowModifier(rowKey: String): Modifier = Modifier
            .onSizeChanged { stackedRowHeightsPx[rowKey] = it.height }
            .bringIntoViewResponder(stackedRowResponder(rowKey))

        if (hasCastSection) {
            item(key = ROW_CAST, contentType = "horizontal_row") {
                Box(modifier = stackedRowModifier(ROW_CAST)) {
                    CastSection(
                        cast = normalCastMembers,
                        listState = castRowListState,
                        title = CAST_AND_CREW_TITLE,
                        leadingCast = directorWriterMembers,
                        upFocusRequester = stackedRowUpFocusRequester(ROW_CAST),
                        downFocusRequester = stackedRowDownFocusRequester(ROW_CAST),
                        sectionFocusRequester = castSectionFocusRequester,
                        restorePersonId = if (!childOverlayVisible && pendingRestoreType == RestoreTarget.CAST_MEMBER) pendingRestoreCastPersonId else null,
                        restoreFocusToken = if (pendingRestoreType == RestoreTarget.CAST_MEMBER) restoreFocusToken else 0,
                        blockDefaultRestore = pendingRestoreType != null && pendingRestoreType != RestoreTarget.CAST_MEMBER,
                        lastFocusedPersonKey = lastFocusedCastKey,
                        onLastFocusedPersonKeyChange = { lastFocusedCastKey = it },
                        onRestoreFocusHandled = {
                            clearPendingRestore()
                        },
                        onCastMemberFocused = {
                            restorePinnedDetailPageIfNudge()
                        },
                        windowResetKey = meta.id,
                        onCastMemberClick = { member ->
                            member.tmdbId?.let { id ->
                                markCastMemberRestore(id)
                                val preferCrew = member.character.equals("Creator", ignoreCase = true) ||
                                    member.character.equals("Director", ignoreCase = true) ||
                                    member.character.equals("Writer", ignoreCase = true)
                                onNavigateToCastDetail(id, member.name, preferCrew)
                            }
                        }
                    )
                }
            }
        }

        if (hasRatingsSection) {
            item(key = ROW_RATINGS, contentType = "ratings_row") {
                Box(modifier = stackedRowModifier(ROW_RATINGS)) {
                    EpisodeRatingsSection(
                        episodes = meta.videos,
                        ratings = visibleEpisodeImdbRatings,
                        isLoading = isEpisodeRatingsLoading,
                        error = episodeRatingsError,
                        title = strTabRatings,
                        upFocusRequester = stackedRowUpFocusRequester(ROW_RATINGS),
                        downFocusRequester = stackedRowDownFocusRequester(ROW_RATINGS),
                        firstItemFocusRequester = ratingsContentFocusRequester,
                        ratingsGridFocusRequester = ratingsGridFocusRequester
                    )
                }
            }
        }

        if (hasMoreLikeThisSection) {
            item(key = ROW_MORE_LIKE_THIS, contentType = "horizontal_row") {
                Box(modifier = stackedRowModifier(ROW_MORE_LIKE_THIS)) {
                    MoreLikeThisSection(
                        items = moreLikeThis,
                        listState = moreLikeThisListState,
                        title = strTabMoreLikeThis,
                        sourceLabel = moreLikeThisSourceLabel,
                        posterCardCornerRadius = posterCardCornerRadiusDp.dp,
                        upFocusRequester = stackedRowUpFocusRequester(ROW_MORE_LIKE_THIS),
                        downFocusRequester = stackedRowDownFocusRequester(ROW_MORE_LIKE_THIS),
                        sectionFocusRequester = moreLikeSectionFocusRequester,
                        restoreItemId = if (!childOverlayVisible && pendingRestoreType == RestoreTarget.MORE_LIKE_THIS) pendingRestoreMoreLikeItemId else null,
                        restoreFocusToken = if (pendingRestoreType == RestoreTarget.MORE_LIKE_THIS) restoreFocusToken else 0,
                        blockDefaultRestore = pendingRestoreType != null && pendingRestoreType != RestoreTarget.MORE_LIKE_THIS,
                        lastFocusedItemId = lastFocusedMoreLikeItemId,
                        onLastFocusedItemIdChange = { lastFocusedMoreLikeItemId = it },
                        onRestoreFocusHandled = {
                            clearPendingRestore()
                        },
                        isItemWatched = { item -> relatedWatchedStatus["${item.id}|${item.apiType}"] == true },
                        onItemFocused = {
                            restorePinnedDetailPageIfNudge()
                        },
                        onItemClick = { item ->
                            markMoreLikeThisRestore(item.id)
                            onNavigateToDetail(item.id, item.apiType, null)
                        },
                        windowResetKey = meta.id,
                        onItemLongPress = { item ->
                            onPosterLongPress(item)
                        }
                    )
                }
            }
        }

        if (hasTrailerSection) {
            item(key = ROW_TRAILERS, contentType = "horizontal_row") {
                Box(modifier = stackedRowModifier(ROW_TRAILERS)) {
                    TrailerSection(
                        trailers = meta.trailers,
                        listState = trailerListState,
                        title = strTrailers,
                        posterCardCornerRadius = posterCardCornerRadiusDp.dp,
                        upFocusRequester = stackedRowUpFocusRequester(ROW_TRAILERS),
                        downFocusRequester = stackedRowDownFocusRequester(ROW_TRAILERS),
                        sectionFocusRequester = trailerSectionFocusRequester,
                        restoreTrailerId = if (restoreSharedTrailerFocusToken > 0) selectedSharedTrailer?.ytId else null,
                        restoreFocusToken = restoreSharedTrailerFocusToken,
                        lastFocusedTrailerId = lastFocusedTrailerId,
                        onLastFocusedTrailerIdChange = { lastFocusedTrailerId = it },
                        onRestoreFocusHandled = onSharedTrailerFocusRestored,
                        windowResetKey = meta.id,
                        onTrailerClick = { trailer ->
                            onSharedTrailerSelected(trailer)
                        }
                    )
                }
            }
        }

        if (hasCollectionSection) {
            item(key = ROW_COLLECTION, contentType = "horizontal_row") {
                Box(modifier = stackedRowModifier(ROW_COLLECTION)) {
                    CollectionSection(
                        items = collection,
                        listState = collectionListState,
                        title = collectionName ?: strTabCollection,
                        posterCardCornerRadius = posterCardCornerRadiusDp.dp,
                        upFocusRequester = stackedRowUpFocusRequester(ROW_COLLECTION),
                        downFocusRequester = stackedRowDownFocusRequester(ROW_COLLECTION),
                        sectionFocusRequester = collectionSectionFocusRequester,
                        restoreItemId = if (!childOverlayVisible && pendingRestoreType == RestoreTarget.COLLECTION) pendingRestoreCollectionItemId else null,
                        restoreFocusToken = if (pendingRestoreType == RestoreTarget.COLLECTION) restoreFocusToken else 0,
                        blockDefaultRestore = pendingRestoreType != null && pendingRestoreType != RestoreTarget.COLLECTION,
                        lastFocusedItemId = lastFocusedCollectionItemId,
                        onLastFocusedItemIdChange = { lastFocusedCollectionItemId = it },
                        onRestoreFocusHandled = {
                            clearPendingRestore()
                        },
                        onItemFocused = {
                            restorePinnedDetailPageIfNudge()
                        },
                        onItemClick = { item ->
                            markCollectionRestore(item.id)
                            onNavigateToDetail(item.id, item.apiType, null)
                        },
                        isItemWatched = { item -> relatedWatchedStatus["${item.id}|${item.apiType}"] == true },
                        windowResetKey = meta.id,
                        onItemLongPress = { item ->
                            onPosterLongPress(item)
                        }
                    )
                }
            }
        }

        if (shouldShowCommentsSection) {
            item(key = ROW_COMMENTS, contentType = "horizontal_row") {
                Box(modifier = stackedRowModifier(ROW_COMMENTS)) {
                    CommentsSection(
                        comments = comments,
                        commentsMode = commentsMode,
                        canToggleEpisodeComments = canToggleEpisodeComments,
                        titleModeFocusRequester = commentsTitleModeFocusRequester,
                        episodeModeFocusRequester = commentsEpisodeModeFocusRequester,
                        selectedEpisode = commentsEpisodeTarget,
                        allEpisodes = meta.videos.filter { it.season != null && it.episode != null },
                        selectedSeason = selectedSeason,
                        availableSeasons = seasons,
                        isLoading = isCommentsLoading,
                        isLoadingMore = isCommentsLoadingMore,
                        canLoadMore = canLoadMoreComments,
                        error = commentsError,
                        upFocusRequester = stackedRowUpFocusRequester(ROW_COMMENTS),
                        entryFocusToken = commentsEntryFocusToken,
                        onEntryFocusHandled = {
                            commentsEntryFocusToken = 0
                        },
                        onRetry = onRetryComments,
                        onLoadMore = onLoadMoreComments,
                        onCommentsModeSelected = onCommentsModeSelected,
                        onEpisodeSelected = onCommentsEpisodeSelected,
                        onCommentClick = onCommentClick,
                        listState = commentsListState,
                        windowResetKey = meta.id,
                        rowEntryFocusRequester = commentsRowEntryFocusRequester,
                        modifier = Modifier
                    )
                }
            }
        }

        // The company rows keep their own scrolling (they are the last rows of the page).
        val companyRowKeys = stackedRowKeys.filter { it == ROW_NETWORKS || it == ROW_PRODUCTION }
        companyRowKeys.forEach { rowKey ->
            val isNetworkRow = rowKey == ROW_NETWORKS
            item(key = rowKey, contentType = "horizontal_row") {
                CompanyLogosSection(
                    title = if (isNetworkRow) {
                        stringResource(R.string.detail_section_network)
                    } else {
                        PRODUCTION_COMPANIES_TITLE
                    },
                    companies = if (isNetworkRow) meta.networks else meta.productionCompanies,
                    listState = if (isNetworkRow) networkLogosListState else productionLogosListState,
                    restoreCompanyId = if (!childOverlayVisible && pendingRestoreType == RestoreTarget.COMPANY_OR_NETWORK) pendingRestoreCompanyId else null,
                    restoreFocusToken = if (pendingRestoreType == RestoreTarget.COMPANY_OR_NETWORK) restoreFocusToken else 0,
                    lastFocusedCompanyId = if (isNetworkRow) lastFocusedNetworkCompanyId else lastFocusedProductionCompanyId,
                    onLastFocusedCompanyIdChange = { id ->
                        if (isNetworkRow) lastFocusedNetworkCompanyId = id else lastFocusedProductionCompanyId = id
                    },
                    onRestoreFocusHandled = { clearPendingRestore() },
                    onCompanyFocused = { overflow -> onCompanyRowFocused(overflow) },
                    upFocusRequester = stackedRowUpFocusRequester(rowKey),
                    sectionFocusRequester = if (isNetworkRow) networkSectionFocusRequester else productionSectionFocusRequester,
                    allowPageScroll = ::allowCompanyPageScroll,
                    windowResetKey = meta.id,
                    onCompanyClick = { company ->
                        company.tmdbId?.let { entityId ->
                            markCompanyRestore(entityId)
                            onNavigateToTmdbEntityBrowse(
                                if (isNetworkRow) "network" else "company",
                                entityId,
                                company.name,
                                meta.apiType
                            )
                        }
                    }
                )
            }
        }
        }
        }

        seasonOptionsDialogSeason?.let { season ->
            val hasPreviousSeasons = remember(season, seasons) {
                seasons.any { it != 0 && it < season }
            }
            SeasonOptionsDialog(
                season = season,
                isFullyWatched = isSeasonFullyWatched(season),
                hasPreviousSeasons = hasPreviousSeasons,
                onDismiss = { seasonOptionsDialogSeason = null },
                onMarkSeasonWatched = {
                    onMarkSeasonWatched(season)
                    seasonOptionsDialogSeason = null
                },
                onMarkSeasonUnwatched = {
                    onMarkSeasonUnwatched(season)
                    seasonOptionsDialogSeason = null
                },
                onMarkPreviousSeasonsWatched = {
                    onMarkPreviousSeasonsWatched(season)
                    seasonOptionsDialogSeason = null
                }
            )
        }

        if (showHeroPlayOptionsDialog && isPlayEnabled) {
            PlayManualOverrideDialog(
                title = meta.name,
                subtitle = nextToWatch?.displayText ?: stringResource(R.string.hero_play),
                onDismiss = { showHeroPlayOptionsDialog = false },
                showPlayManually = showManualPlayOption,
                onPlayManually = {
                    showHeroPlayOptionsDialog = false
                    heroPlayManualClick()
                },
                showStartFromBeginning = nextToWatch?.isResume == true,
                onStartFromBeginning = {
                    showHeroPlayOptionsDialog = false
                    heroPlayStartFromBeginningClick()
                }
            )
        }

        if (showRandomEpisodeOverlay && showRandomEpisodeButton) {
            EpisodeShuffleDialog(
                meta = meta,
                shuffleSettings = episodeShuffle,
                onSaveSettings = onEpisodeShuffleChange,
                watchedEpisodes = watchedEpisodes,
                episodeProgress = episodeProgressMap,
                blurUnwatchedEpisodes = blurUnwatchedEpisodes,
                showManualPlayOption = showManualPlayOption,
                onDismiss = {
                    showRandomEpisodeOverlay = false
                    coroutineScope.launch { randomEpisodeFocusRequester.requestFocusAfterFrames() }
                },
                onPlay = { video ->
                    if (canPlayEpisode(video)) {
                        randomEpisodePlaybackPending = true
                        showRandomEpisodeOverlay = false
                        video.season?.let(onSeasonSelected)
                    }
                    episodeClick(video)
                },
                onPlayManually = { video ->
                    if (canPlayEpisode(video)) {
                        randomEpisodePlaybackPending = true
                        showRandomEpisodeOverlay = false
                        video.season?.let(onSeasonSelected)
                    }
                    episodeManualClick(video)
                },
                onStartFromBeginning = { video ->
                    if (canPlayEpisode(video)) {
                        randomEpisodePlaybackPending = true
                        showRandomEpisodeOverlay = false
                        video.season?.let(onSeasonSelected)
                        markEpisodeRestore(video.id)
                    }
                    onEpisodeStartFromBeginningClick(video)
                }
            )
        }

        selectedComment?.let { review ->
            CommentOverlay(
                review = review,
                episode = if (commentsMode == CommentsMode.EPISODE) commentsEpisodeTarget else null,
                canNavigatePrevious = selectedCommentIndex > 0,
                canNavigateNext = selectedCommentIndex >= 0 && (
                    selectedCommentIndex < comments.lastIndex || canLoadMoreComments || isCommentsLoadingMore
                ),
                isLoadingNext = isCommentsLoadingMore,
                transitionDirection = commentOverlayDirection,
                onPrevious = onShowPreviousComment,
                onNext = onShowNextComment,
                onDismiss = onDismissCommentOverlay
            )
        }

        if (isSharedTrailerOverlayVisible) {
            SharedTrailerOverlay(
                title = selectedSharedTrailer?.name?.takeIf { it.isNotBlank() }
                    ?: selectedSharedTrailer?.type?.takeIf { it.isNotBlank() }
                    ?: stringResource(R.string.detail_tab_trailer),
                trailerUrl = sharedTrailerUrl,
                trailerAudioUrl = sharedTrailerAudioUrl,
                isLoading = isSharedTrailerLoading,
                errorMessage = sharedTrailerErrorMessage,
                onDismiss = onDismissSharedTrailer,
                onRetry = onRetrySharedTrailer
            )
        }

        meta.description?.takeIf { showSynopsisOverlay && it.isNotBlank() }?.let { synopsis ->
            SynopsisOverlay(
                title = meta.name,
                description = synopsis,
                onDismiss = { showSynopsisOverlay = false }
            )
        }
        if (randomEpisodePlaybackPending) {
            PlaybackHandoffBackdrop(backdropUrl = heroBackdropUrl ?: meta.backdropUrl)
        }
    }
}

@Composable
private fun PlaybackHandoffBackdrop(backdropUrl: String?) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (!backdropUrl.isNullOrBlank()) {
            val context = LocalContext.current
            val density = LocalDensity.current
            val configuration = LocalConfiguration.current
            val widthPx = remember(configuration, density) {
                with(density) { configuration.screenWidthDp.dp.roundToPx() }
            }
            val heightPx = remember(configuration, density) {
                with(density) { configuration.screenHeightDp.dp.roundToPx() }
            }
            val request = remember(context, backdropUrl, widthPx, heightPx) {
                ImageRequest.Builder(context)
                    .data(backdropUrl)
                    .crossfade(false)
                    .size(width = widthPx, height = heightPx)
                    .build()
            }
            AsyncImage(
                model = request,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alignment = Alignment.TopEnd
            )
        }
    }
}

/**
 * Fades the rows below the hero item while a full-screen trailer plays. On screens
 * taller than the hero (lower-density TV boxes) the next row would otherwise sit over
 * the trailer.
 */
// Nuvio Launcher: where the "Episodes" title sits on screen while the episodes block has focus,
// and where the episode row's top goes when a series has no season tabs.
private val EpisodesTitleScreenTop = 63.dp
private val EpisodesRowScreenTopWithoutTabs = 120.dp

private fun Modifier.fadeBelowHero(
    listState: LazyListState,
    alpha: State<Float>
): Modifier = drawWithCache {
    val layerPaint = Paint()
    onDrawWithContent {
        val belowHeroAlpha = alpha.value
        if (belowHeroAlpha >= 1f) {
            drawContent()
            return@onDrawWithContent
        }
        val heroBottom = listState.layoutInfo.visibleItemsInfo
            .firstOrNull { it.key == "hero" }
            ?.let { (it.offset + it.size).toFloat() }
            ?: 0f
        clipRect(bottom = heroBottom) { this@onDrawWithContent.drawContent() }
        if (belowHeroAlpha > 0f) {
            layerPaint.alpha = belowHeroAlpha
            drawContext.canvas.saveLayer(Rect(0f, heroBottom, size.width, size.height), layerPaint)
            clipRect(top = heroBottom) { this@onDrawWithContent.drawContent() }
            drawContext.canvas.restore()
        }
    }
}

@Composable
private fun BackdropLayer(
    backdropRequest: ImageRequest,
    heroBackdropRequest: ImageRequest? = null,
    trailerUrl: String?,
    trailerAudioUrl: String?,
    isTrailerPlaying: Boolean,
    isBackgroundTrailerPlaying: Boolean,
    pauseBackgroundTrailerOnScroll: Boolean,
    isTrailerPaused: Boolean = false,
    showTrailerControls: Boolean,
    trailerSeekToken: Int,
    trailerSeekDeltaMs: Long,
    onTrailerControlKey: (keyCode: Int, action: Int, repeatCount: Int) -> Boolean,
    onTrailerProgressChanged: (Long, Long) -> Unit,
    onTrailerEnded: () -> Unit,
    isScrolledPastHero: Boolean,
    leftGradient: ImageBitmap,
    bottomGradient: ImageBitmap,
) {
    var showHeroBackdropUnderlay by remember(heroBackdropRequest, backdropRequest) {
        mutableStateOf(heroBackdropRequest != null)
    }
    val isBackgroundTrailerPaused =
        isBackgroundTrailerPlaying && pauseBackgroundTrailerOnScroll && isScrolledPastHero
    val isBackgroundTrailerVisible = isBackgroundTrailerPlaying && !isBackgroundTrailerPaused
    // Keep the backdrop until the trailer has a frame to show; the request alone can be many
    // seconds ahead of it while the stream is fetched and buffered.
    // Keyed on "any trailer playing" rather than the individual modes: switching between the
    // background and full-screen trailer keeps the same player running, so no new first frame
    // arrives and resetting here would leave the gradient and backdrop stuck on screen.
    val isAnyTrailerPlaying = isTrailerPlaying || isBackgroundTrailerPlaying
    var isTrailerRendered by remember(isAnyTrailerPlaying, trailerUrl) { mutableStateOf(false) }
    val backdropAlphaState = animateFloatAsState(
        targetValue = when {
            isTrailerPlaying && isTrailerRendered -> 0f
            isBackgroundTrailerVisible && isTrailerRendered -> 0f
            isScrolledPastHero -> 0f
            else -> 1f
        },
        animationSpec = tween(durationMillis = if (isScrolledPastHero) 400 else 800),
        label = "backdropFade"
    )
    val trailerAlphaState = animateFloatAsState(
        targetValue = when {
            isTrailerPlaying -> 1f
            !isBackgroundTrailerVisible -> 0f
            isScrolledPastHero -> 0.15f
            else -> 1f
        },
        animationSpec = tween(durationMillis = if (isScrolledPastHero) 300 else 800),
        label = "trailerFade"
    )
    val gradientAlphaState = animateFloatAsState(
        targetValue = if ((isTrailerPlaying && isTrailerRendered) || isScrolledPastHero) 0f else 1f,
        animationSpec = tween(durationMillis = if (isScrolledPastHero) 300 else 800),
        label = "gradientFade"
    )
    Box(modifier = Modifier.fillMaxSize()) {
        // Show hero backdrop from previous screen as persistent underlay
        // to prevent flash/re-render during navigation transition
        if (showHeroBackdropUnderlay && heroBackdropRequest != null) {
            AsyncImage(
                model = heroBackdropRequest,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                alpha = backdropAlphaState.value,
                contentScale = ContentScale.Crop,
                alignment = Alignment.TopEnd
            )
        }
        AsyncImage(
            model = backdropRequest,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            alpha = backdropAlphaState.value,
            onSuccess = { showHeroBackdropUnderlay = false },
            contentScale = ContentScale.Crop,
            alignment = Alignment.TopEnd
        )
        TrailerPlayer(
            trailerUrl = trailerUrl,
            trailerAudioUrl = trailerAudioUrl,
            isPlaying = isTrailerPlaying || isBackgroundTrailerPlaying,
            isPaused = isTrailerPaused || isBackgroundTrailerPaused,
            seekRequestToken = if (showTrailerControls) trailerSeekToken else 0,
            seekDeltaMs = if (showTrailerControls) trailerSeekDeltaMs else 0L,
            onRemoteKey = onTrailerControlKey,
            onProgressChanged = onTrailerProgressChanged,
            onEnded = onTrailerEnded,
            onError = onTrailerEnded,
            onFirstFrameRendered = { isTrailerRendered = true },
            cropToFill = isBackgroundTrailerPlaying,
            autoCropLetterbox = isBackgroundTrailerPlaying,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = if (isAnyTrailerPlaying) trailerAlphaState.value else 1f
                }
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawWithCache {
                    onDrawBehind {
                        if (DetailsHeroShade && gradientAlphaState.value > 0f) {
                            drawImage(
                                leftGradient,
                                dstSize = androidx.compose.ui.unit.IntSize(size.width.toInt(), size.height.toInt()),
                                alpha = gradientAlphaState.value,
                                filterQuality = androidx.compose.ui.graphics.FilterQuality.Low
                            )
                            // the bottom of the picture blends into the grey below the hero
                            drawImage(
                                bottomGradient,
                                dstSize = androidx.compose.ui.unit.IntSize(size.width.toInt(), size.height.toInt()),
                                alpha = gradientAlphaState.value,
                                filterQuality = androidx.compose.ui.graphics.FilterQuality.Low
                            )
                        }
                    }
                }
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun LibraryListPickerDialog(
    title: String,
    tabs: List<LibraryListTab>,
    membership: Map<String, Boolean>,
    isPending: Boolean,
    error: String?,
    onToggle: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    val primaryFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        primaryFocusRequester.requestFocus()
    }

    NuvioDialog(
        onDismiss = onDismiss,
        title = title,
        subtitle = stringResource(R.string.detail_lists_subtitle),
        width = 500.dp
    ) {
        if (!error.isNullOrBlank()) {
            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFFFB6B6)
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 300.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(tabs, key = { it.key }) { tab ->
                val selected = membership[tab.key] == true
                // The membership state is exposed through semantics below instead of a leading
                // glyph in the label, so screen readers hear one clean name plus the state.
                val titleText = tab.localizedMembershipTitle()
                val rowModifier = if (tab.key == tabs.firstOrNull()?.key) {
                    Modifier
                        .fillMaxWidth()
                        .focusRequester(primaryFocusRequester)
                } else {
                    Modifier.fillMaxWidth()
                }
                Button(
                    onClick = { onToggle(tab.key) },
                    enabled = !isPending,
                    modifier = rowModifier.semantics {
                        role = Role.Checkbox
                        toggleableState = if (selected) ToggleableState.On else ToggleableState.Off
                    },
                    colors = ButtonDefaults.colors(
                        containerColor = if (selected) NuvioTheme.colors.FocusBackground else NuvioTheme.colors.BackgroundCard,
                        contentColor = NuvioTheme.colors.TextPrimary
                    )
                ) {
                    Text(
                        text = titleText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        HorizontalDivider(color = NuvioTheme.colors.Border, thickness = NuvioTheme.spacing.hairline)

        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
            Button(
                onClick = onSave,
                enabled = !isPending,
                colors = ButtonDefaults.colors(
                    containerColor = NuvioTheme.colors.BackgroundCard,
                    contentColor = NuvioTheme.colors.TextPrimary
                )
            ) {
                Text(if (isPending) stringResource(R.string.action_saving) else stringResource(R.string.action_save))
            }
        }
    }
}

/**
 * Nuvio Launcher: the shade over the title's picture (left and bottom) is switched off at the owner's request:
 * the picture is shown uncovered. Set true to bring it back (tinted with the title's colour, as before).
 */
private const val DetailsHeroShade = false

/** Nuvio Launcher: the left shade is 30% see-through (its strongest point 70% opaque instead of 100%). */
private const val LeftShadeOpacity = 0.7f

/**
 * Nuvio Launcher: the title's shade colour. The tiny backdrop's hues, weighted by how colourful and bright each
 * pixel is (near-black and near-grey pixels ignored); the strongest hue at saturation .35 and brightness .22
 * (30% lighter than the first proposal's .17). Null when the picture has no real colour.
 */
/**
 * Nuvio Launcher: whether the picture is bright where the details text sits: the text side's 60% of the width, lower
 * 65% of the height. Average luminance (Rec. 709) of that part of a small copy of the backdrop, above 0.45.
 */
internal fun launcherBrightBehindText(bitmap: android.graphics.Bitmap, rtl: Boolean): Boolean {
    val w = bitmap.width
    val h = bitmap.height
    if (w <= 0 || h <= 0) return false
    val px = IntArray(w * h)
    runCatching { bitmap.getPixels(px, 0, w, 0, 0, w, h) }.getOrElse { return false }
    val x0 = if (rtl) (w * 0.4f).toInt() else 0
    val x1 = if (rtl) w else (w * 0.6f).toInt().coerceAtLeast(1)
    val y0 = (h * 0.35f).toInt()
    var sum = 0.0
    var n = 0
    for (y in y0 until h) for (x in x0 until x1) {
        val c = px[y * w + x]
        val r = ((c shr 16) and 0xFF) / 255.0
        val g = ((c shr 8) and 0xFF) / 255.0
        val b = (c and 0xFF) / 255.0
        sum += 0.2126 * r + 0.7152 * g + 0.0722 * b
        n++
    }
    return n > 0 && sum / n > BrightBehindTextThreshold
}

private const val BrightBehindTextThreshold = 0.45

internal fun launcherDominantShade(bitmap: android.graphics.Bitmap): Color? {
    val w = bitmap.width
    val h = bitmap.height
    if (w <= 0 || h <= 0) return null
    val px = IntArray(w * h)
    runCatching { bitmap.getPixels(px, 0, w, 0, 0, w, h) }.getOrElse { return null }
    val bins = FloatArray(36)
    val hsv = FloatArray(3)
    for (c in px) {
        android.graphics.Color.colorToHSV(c, hsv)
        if (hsv[2] < 0.12f || hsv[1] < 0.15f) continue
        bins[((hsv[0] / 10f).toInt()).coerceIn(0, 35)] += hsv[1] * hsv[2]
    }
    var best = 0
    for (i in 1 until 36) if (bins[i] > bins[best]) best = i
    if (bins[best] <= 0f) return null
    return Color.hsv(best * 10f + 5f, 0.35f, 0.22f)
}

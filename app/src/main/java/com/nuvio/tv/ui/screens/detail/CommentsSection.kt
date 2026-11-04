package com.nuvio.tv.ui.screens.detail

import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.ui.platform.LocalView
import androidx.compose.runtime.SideEffect
import androidx.compose.foundation.layout.heightIn
import com.nuvio.tv.ui.launcher.LauncherGlass
import com.nuvio.tv.ui.launcher.LauncherStyle
import com.nuvio.tv.ui.theme.NuvioTheme

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.nuvio.tv.R
import com.nuvio.tv.domain.model.TraktCommentReview
import com.nuvio.tv.domain.model.Video
import com.nuvio.tv.ui.components.NuvioDialog
import com.nuvio.tv.ui.util.contentTextDirection
import com.nuvio.tv.ui.util.localizeEpisodeTitle
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlin.math.max
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import com.nuvio.tv.ui.screens.ambient.AmbientDialogEffect

@OptIn(ExperimentalComposeUiApi::class, ExperimentalTvMaterial3Api::class)
@Composable
fun CommentsSection(
    comments: List<TraktCommentReview>,
    commentsMode: CommentsMode,
    canToggleEpisodeComments: Boolean,
    titleModeFocusRequester: FocusRequester? = null,
    episodeModeFocusRequester: FocusRequester? = null,
    selectedEpisode: Video?,
    allEpisodes: List<Video>,
    selectedSeason: Int?,
    availableSeasons: List<Int>,
    isLoading: Boolean,
    isLoadingMore: Boolean,
    canLoadMore: Boolean,
    error: String?,
    upFocusRequester: FocusRequester? = null,
    entryFocusToken: Int = 0,
    onEntryFocusHandled: () -> Unit = {},
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    onCommentsModeSelected: (CommentsMode) -> Unit,
    onEpisodeSelected: (Video) -> Unit,
    onCommentClick: (TraktCommentReview) -> Unit,
    listState: LazyListState,
    rowEntryFocusRequester: FocusRequester? = null,
    modifier: Modifier = Modifier,
    windowResetKey: String? = null
) {
    val cardShape = RoundedCornerShape(12.dp)
    var sectionHasFocus by remember { mutableStateOf(false) }
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val firstItemFocusRequester = remember { FocusRequester() }
    val internalTitleModeFocusRequester = remember { FocusRequester() }
    val internalEpisodeModeFocusRequester = remember { FocusRequester() }
    val resolvedTitleModeFocusRequester = titleModeFocusRequester ?: internalTitleModeFocusRequester
    val resolvedEpisodeModeFocusRequester = episodeModeFocusRequester ?: internalEpisodeModeFocusRequester
    val commentFocusRequesters = remember(comments) { mutableMapOf<Long, FocusRequester>() }
    var showEpisodePicker by remember { mutableStateOf(false) }
    var pickerSeason by rememberSaveable { mutableStateOf<Int?>(null) }
    var lastFocusedCommentId by rememberSaveable { mutableStateOf<Long?>(null) }
    var lockCommentFocusId by remember { mutableStateOf(false) }
    val controlsFocusRequester = if (commentsMode == CommentsMode.EPISODE) {
        resolvedEpisodeModeFocusRequester
    } else {
        resolvedTitleModeFocusRequester
    }
    val commentsScrollResetKey = "${commentsMode.name}:${selectedEpisode?.id.orEmpty()}"
    val commentWindowIds = remember(comments) { comments.map { it.id.toString() } }
    val anchoredCommentId = listState.keepDetailRowWindow(
        itemIds = commentWindowIds,
        lazyKeyAt = { index -> comments.getOrNull(index)?.id },
        resetKey = "${windowResetKey.orEmpty()}:$commentsScrollResetKey"
    )
    val visibleFirstCommentId = anchoredCommentId?.toLongOrNull()
        ?: comments.getOrNull(max(listState.firstVisibleItemIndex, 0))?.id
    val hasFocusableCommentContent = isLoading || !error.isNullOrBlank() || comments.isNotEmpty()
    val commentsFocusTargetId = if (!hasFocusableCommentContent) {
        null
    } else {
        lastFocusedCommentId?.takeIf { id -> comments.any { it.id == id } }
            ?: visibleFirstCommentId
            ?: comments.firstOrNull()?.id
    }
    val commentsTargetFocusRequester = remember(
        comments,
        commentsFocusTargetId,
        hasFocusableCommentContent
    ) {
        if (!hasFocusableCommentContent) return@remember null
        commentsFocusTargetId?.let { commentFocusRequesters.getOrPut(it) { FocusRequester() } }
            ?: firstItemFocusRequester
    }
    val pickerDefaultSeason = selectedEpisode?.season
        ?: selectedSeason
        ?: availableSeasons.firstOrNull()
    val pickerEpisodes = remember(allEpisodes, pickerSeason) {
        val season = pickerSeason
        if (season == null) {
            emptyList()
        } else {
            allEpisodes
                .filter { it.season == season }
                .sortedBy { it.episode }
        }
    }
    val upFocusModifier = if (upFocusRequester != null) {
        Modifier.focusProperties { up = upFocusRequester }
    } else {
        Modifier
    }
    val subtitleText = if (commentsMode == CommentsMode.EPISODE && selectedEpisode != null) {
        stringResource(
            R.string.detail_comments_subtitle_episode,
            selectedEpisode.season ?: 0,
            selectedEpisode.episode ?: 0
        )
    } else {
        stringResource(R.string.detail_comments_subtitle)
    }

    LaunchedEffect(listState, comments.size, canLoadMore, isLoadingMore, isLoading, error) {
        if (isLoading || !error.isNullOrBlank()) return@LaunchedEffect
        snapshotFlow {
            val lastVisibleIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            val totalItems = listState.layoutInfo.totalItemsCount
            canLoadMore && !isLoadingMore && totalItems > 0 && lastVisibleIndex >= totalItems - 3
        }
            .distinctUntilChanged()
            .collect { shouldLoadMore ->
                if (shouldLoadMore) onLoadMore()
            }
    }

    LaunchedEffect(showEpisodePicker, pickerDefaultSeason) {
        if (showEpisodePicker) {
            pickerSeason = pickerDefaultSeason
        }
    }

    var appliedCommentsScrollResetKey by rememberSaveable { mutableStateOf(commentsScrollResetKey) }
    LaunchedEffect(commentsScrollResetKey) {
        if (appliedCommentsScrollResetKey == commentsScrollResetKey) return@LaunchedEffect
        appliedCommentsScrollResetKey = commentsScrollResetKey
        lastFocusedCommentId = null
        if (listState.firstVisibleItemIndex != 0 || listState.firstVisibleItemScrollOffset != 0) {
            listState.scrollToItem(0)
        }
    }

    LaunchedEffect(entryFocusToken) {
        if (entryFocusToken > 0) {
            controlsFocusRequester.requestFocusAfterFrames()
            onEntryFocusHandled()
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged { sectionHasFocus = it.hasFocus }
            .padding(top = 26.dp, bottom = NuvioTheme.spacing.sm)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = LauncherStyle.Start),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(NuvioTheme.spacing.sm)
        ) {
            LauncherRowTitle(
                text = "Reviews",   // approved mockup title
                highlighted = sectionHasFocus
            )
            Image(
                painter = painterResource(id = R.drawable.trakt_logo_wordmark),
                contentDescription = stringResource(R.string.cd_trakt_logo),
                modifier = Modifier
                    .width(38.dp)
                    .height(16.dp),
                colorFilter = ColorFilter.tint(LauncherStyle.Dim)
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = subtitleText,
            fontSize = 11.sp,
            color = LauncherStyle.Dim,
            modifier = Modifier.padding(horizontal = LauncherStyle.Start)
        )
        if (canToggleEpisodeComments) {
            Spacer(modifier = Modifier.height(NuvioTheme.spacing.md))
            Row(
                modifier = Modifier
                    .padding(horizontal = LauncherStyle.Start)
                    .focusRestorer(controlsFocusRequester),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CommentModeButton(
                    text = stringResource(R.string.detail_comments_mode_show),
                    selected = commentsMode == CommentsMode.TITLE,
                    focusRequester = resolvedTitleModeFocusRequester,
                    upFocusRequester = upFocusRequester,
                    downFocusRequester = commentsTargetFocusRequester,
                    leftFocusRequester = if (isRtl) resolvedEpisodeModeFocusRequester else FocusRequester.Cancel,
                    rightFocusRequester = if (isRtl) FocusRequester.Cancel else resolvedEpisodeModeFocusRequester,
                    onClick = { onCommentsModeSelected(CommentsMode.TITLE) }
                )
                CommentModeButton(
                    text = if (commentsMode == CommentsMode.EPISODE && selectedEpisode != null) {
                        stringResource(
                            R.string.detail_comments_mode_episode_change,
                            selectedEpisodeLabel(selectedEpisode)
                        )
                    } else {
                        stringResource(R.string.detail_comments_mode_episode)
                    },
                    selected = commentsMode == CommentsMode.EPISODE,
                    focusRequester = resolvedEpisodeModeFocusRequester,
                    upFocusRequester = upFocusRequester,
                    downFocusRequester = commentsTargetFocusRequester,
                    leftFocusRequester = if (isRtl) FocusRequester.Cancel else resolvedTitleModeFocusRequester,
                    rightFocusRequester = if (isRtl) resolvedTitleModeFocusRequester else FocusRequester.Cancel,
                    onClick = {
                        if (commentsMode == CommentsMode.EPISODE && allEpisodes.isNotEmpty()) {
                            showEpisodePicker = true
                        } else {
                            onCommentsModeSelected(CommentsMode.EPISODE)
                        }
                    }
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))

        when {
            isLoading -> {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            commentsTargetFocusRequester?.let { Modifier.focusRestorer(it) } ?: Modifier
                        ),
                    contentPadding = PaddingValues(horizontal = LauncherStyle.Start, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(LauncherStyle.Gap)
                ) {
                    items(3) { index ->
                        LoadingCommentCard(
                            shape = cardShape,
                            modifier = Modifier.then(
                                if (index == 0) {
                                    Modifier
                                        .focusRequester(firstItemFocusRequester)
                                        .then(
                                            if (canToggleEpisodeComments) {
                                                Modifier.focusProperties {
                                                    up = controlsFocusRequester
                                                }
                                            } else {
                                                upFocusModifier
                                            }
                                        )
                                } else {
                                    Modifier.then(upFocusModifier)
                                }
                            )
                        )
                    }
                }
            }

            !error.isNullOrBlank() -> {
                Column(
                    modifier = Modifier.padding(horizontal = LauncherStyle.Start),
                    verticalArrangement = Arrangement.spacedBy(NuvioTheme.spacing.md)
                ) {
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            textDirection = error.contentTextDirection()
                        ),
                        color = LauncherStyle.Soft
                    )
                    Button(
                        onClick = onRetry,
                        modifier = Modifier
                            .focusRequester(firstItemFocusRequester)
                            .then(
                                if (canToggleEpisodeComments) {
                                    Modifier.focusProperties {
                                        up = controlsFocusRequester
                                    }
                                } else {
                                    upFocusModifier
                                }
                            ),
                        colors = ButtonDefaults.colors(
                            containerColor = LauncherStyle.PillIdle,
                            contentColor = Color.White,
                            focusedContainerColor = LauncherStyle.FocusWhite,
                            focusedContentColor = LauncherStyle.FocusText
                        ),
                        shape = ButtonDefaults.shape(RoundedCornerShape(15.dp)),
                        scale = ButtonDefaults.scale(focusedScale = 1.06f)
                    ) {
                        Text(stringResource(R.string.action_retry))
                    }
                }
            }

            comments.isEmpty() -> {
                Text(
                    text = stringResource(R.string.detail_comments_empty),
                    fontSize = 12.sp,
                    color = LauncherStyle.Soft,
                    modifier = Modifier.padding(horizontal = LauncherStyle.Start, vertical = 10.dp)
                )
            }

            else -> {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onPreviewKeyEvent { event ->
                            if (
                                event.type == KeyEventType.KeyDown &&
                                (event.key == Key.DirectionUp || event.key == Key.DirectionDown)
                            ) {
                                lockCommentFocusId = true
                            }
                            false
                        }
                        .onFocusChanged { state ->
                            if (!state.hasFocus) lockCommentFocusId = false
                        }
                        .focusProperties {
                            onEnter = {
                                commentsTargetFocusRequester?.requestFocus()
                            }
                        }
                        .focusGroup(),
                    state = listState,
                    contentPadding = PaddingValues(horizontal = LauncherStyle.Start, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(LauncherStyle.Gap)
                ) {
                    items(comments, key = { it.id }) { review ->
                        val commentFocusRequester = commentFocusRequesters.getOrPut(review.id) { FocusRequester() }
                        CommentCard(
                            review = review,
                            shape = cardShape,
                            modifier = Modifier
                                .then(
                                    if (rowEntryFocusRequester != null && review.id == commentsFocusTargetId) {
                                        Modifier.focusRequester(rowEntryFocusRequester)
                                    } else {
                                        Modifier
                                    }
                                )
                                .focusRequester(commentFocusRequester)
                                .then(
                                    if (canToggleEpisodeComments) {
                                        Modifier.focusProperties { up = controlsFocusRequester }
                                    } else {
                                        upFocusModifier
                                    }
                                )
                                .onFocusChanged { focusState ->
                                    if (focusState.isFocused && !lockCommentFocusId) {
                                        lastFocusedCommentId = review.id
                                    }
                                }
                                .stopFocusAtRowEnd(!isLoadingMore && review.id == comments.lastOrNull()?.id),
                            onClick = { onCommentClick(review) }
                        )
                    }
                    if (isLoadingMore) {
                        item(key = "loading_more_comments") {
                            LoadingCommentCard(shape = cardShape)
                        }
                    }
                }
            }
        }
    }

    if (showEpisodePicker && pickerEpisodes.isNotEmpty()) {
        EpisodeCommentPickerDialog(
            seasons = availableSeasons,
            episodes = pickerEpisodes,
            season = pickerSeason ?: pickerDefaultSeason,
            selectedEpisodeId = selectedEpisode?.id,
            onDismiss = { showEpisodePicker = false },
            onSeasonSelected = { pickerSeason = it },
            onEpisodeSelected = {
                showEpisodePicker = false
                onEpisodeSelected(it)
            }
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun CommentModeButton(
    text: String,
    selected: Boolean,
    focusRequester: FocusRequester,
    upFocusRequester: FocusRequester? = null,
    downFocusRequester: FocusRequester? = null,
    leftFocusRequester: FocusRequester? = null,
    rightFocusRequester: FocusRequester? = null,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .focusRequester(focusRequester)
            .focusProperties {
                if (upFocusRequester != null) {
                    up = upFocusRequester
                }
                if (downFocusRequester != null) {
                    down = downFocusRequester
                }
                if (leftFocusRequester != null) {
                    left = leftFocusRequester
                }
                if (rightFocusRequester != null) {
                    right = rightFocusRequester
                }
            }
            .semantics {
                role = Role.Tab
                this.selected = selected
            },
        // Launcher look: sidebar-style pills
        colors = ButtonDefaults.colors(
            containerColor = if (selected) LauncherStyle.PillSelected else LauncherStyle.PillIdle,
            contentColor = if (selected) Color.White else LauncherStyle.Soft,
            focusedContainerColor = LauncherStyle.FocusWhite,
            focusedContentColor = LauncherStyle.FocusText
        ),
        shape = ButtonDefaults.shape(RoundedCornerShape(14.dp)),
        scale = ButtonDefaults.scale(focusedScale = 1f),
        contentPadding = PaddingValues(horizontal = 13.dp, vertical = 5.dp)
    ) {
        Text(text, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun CommentCard(
    review: TraktCommentReview,
    shape: RoundedCornerShape,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bodyText = if (review.hasSpoilerContent) {
        stringResource(R.string.detail_comments_spoiler_hidden)
    } else {
        review.comment
    }

    // Launcher look (source-list cards): white .08 with soft text, focused near-white with dark text.
    var isFocused by remember { mutableStateOf(false) }
    val nameColor = if (isFocused) LauncherStyle.FocusText else Color.White
    val metaColor = if (isFocused) Color(0xB316171B) else LauncherStyle.Dim
    val bodyColor = when {
        review.hasSpoilerContent -> if (isFocused) Color(0xFF8A5A00) else NuvioTheme.colors.Warning
        isFocused -> LauncherStyle.FocusText
        else -> LauncherStyle.Soft
    }
    val metaLine = buildList {
        review.rating?.let { add(stringResource(R.string.detail_comments_badge_rating, it)) }
        if (review.review) add(stringResource(R.string.detail_comments_badge_review))
        add(stringResource(R.string.detail_comments_likes, review.likes))
    }.joinToString(" · ")

    Card(
        onClick = onClick,
        modifier = modifier
            .width(300.dp)
            .height(150.dp)
            .onFocusChanged { isFocused = it.isFocused },
        colors = CardDefaults.colors(
            containerColor = Color(0x14FFFFFF),
            focusedContainerColor = Color(0xEBF2F4F8)
        ),
        border = CardDefaults.border(border = Border.None, focusedBorder = Border.None),
        shape = CardDefaults.shape(shape),
        scale = CardDefaults.scale(focusedScale = 1.06f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Text(
                text = review.authorDisplayName,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = nameColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = metaLine,
                fontSize = 10.5.sp,
                color = metaColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = bodyText,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    textDirection = bodyText.contentTextDirection()
                ),
                color = bodyColor,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun CommentChip(text: String) {
    val shape = RoundedCornerShape(999.dp)
    Box(
        modifier = Modifier
            .background(
                color = NuvioTheme.colors.BackgroundElevated,
                shape = shape
            )
            .padding(horizontal = NuvioTheme.spacing.sm, vertical = NuvioTheme.spacing.xs)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = NuvioTheme.colors.TextPrimary,
            maxLines = 1
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun EpisodeCommentPickerDialog(
    seasons: List<Int>,
    episodes: List<Video>,
    season: Int?,
    selectedEpisodeId: String?,
    onDismiss: () -> Unit,
    onSeasonSelected: (Int) -> Unit,
    onEpisodeSelected: (Video) -> Unit
) {
    val primaryFocusRequester = remember { FocusRequester() }
    val selectedSeasonFocusRequester = remember { FocusRequester() }
    val selectedEpisodeFocusRequester = remember { FocusRequester() }
    val seasonListState = rememberLazyListState()
    val episodeListState = rememberLazyListState()
    val sortedSeasons = remember(seasons) {
        seasons.filter { it > 0 }.sorted() + seasons.filter { it == 0 }
    }

    LaunchedEffect(season, selectedEpisodeId, episodes, sortedSeasons) {
        season?.let { activeSeason ->
            val selectedSeasonIndex = sortedSeasons.indexOf(activeSeason)
            if (selectedSeasonIndex >= 0) {
                seasonListState.scrollToItem(selectedSeasonIndex)
            }
        }
        selectedEpisodeId?.let { activeEpisodeId ->
            val selectedEpisodeIndex = episodes.indexOfFirst { it.id == activeEpisodeId }
            if (selectedEpisodeIndex >= 0) {
                episodeListState.scrollToItem(selectedEpisodeIndex)
            }
        }
        withFrameNanos { }
        if (selectedEpisodeId != null && episodes.any { it.id == selectedEpisodeId }) {
            selectedEpisodeFocusRequester.requestFocus()
        } else {
            primaryFocusRequester.requestFocus()
        }
    }

    NuvioDialog(
        onDismiss = onDismiss,
        title = stringResource(R.string.detail_comments_episode_picker_title),
        subtitle = stringResource(R.string.detail_comments_episode_picker_subtitle),
        width = 560.dp,
        suppressFirstKeyUp = false
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(NuvioTheme.spacing.md)
        ) {
            if (seasons.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    state = seasonListState,
                    contentPadding = PaddingValues(horizontal = NuvioTheme.spacing.md, vertical = NuvioTheme.spacing.xs),
                    horizontalArrangement = Arrangement.spacedBy(NuvioTheme.spacing.sm)
                ) {
                    items(sortedSeasons, key = { it }) { seasonNumber ->
                        val seasonModifier = if (seasonNumber == season) {
                            Modifier.focusRequester(selectedSeasonFocusRequester)
                        } else {
                            Modifier
                        }
                        Button(
                            onClick = { onSeasonSelected(seasonNumber) },
                            modifier = seasonModifier.semantics {
                                role = Role.Tab
                                selected = seasonNumber == season
                            },
                            colors = ButtonDefaults.colors(
                                containerColor = if (seasonNumber == season) {
                                    NuvioTheme.colors.Secondary
                                } else {
                                    NuvioTheme.colors.BackgroundCard
                                },
                                contentColor = if (seasonNumber == season) {
                                    NuvioTheme.colors.OnSecondary
                                } else {
                                    NuvioTheme.colors.TextPrimary
                                }
                            )
                        ) {
                            Text(
                                text = if (seasonNumber == 0) {
                                    stringResource(R.string.episodes_specials)
                                } else {
                                    stringResource(R.string.episodes_season, seasonNumber)
                                }
                            )
                        }
                    }
                }
            }
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp),
                state = episodeListState,
                contentPadding = PaddingValues(vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(episodes, key = { it.id }) { episode ->
                    val episodeModifier = when {
                        episode.id == selectedEpisodeId -> Modifier
                            .fillMaxWidth()
                            .focusRequester(selectedEpisodeFocusRequester)
                        episode.id == episodes.firstOrNull()?.id -> Modifier
                            .fillMaxWidth()
                            .focusRequester(primaryFocusRequester)
                        else -> Modifier.fillMaxWidth()
                    }
                    Button(
                        onClick = { onEpisodeSelected(episode) },
                        modifier = episodeModifier,
                        colors = ButtonDefaults.colors(
                            containerColor = if (episode.id == selectedEpisodeId) {
                                NuvioTheme.colors.FocusBackground
                            } else {
                                NuvioTheme.colors.BackgroundCard
                            },
                            contentColor = NuvioTheme.colors.TextPrimary
                        )
                    ) {
                        Text(
                            text = "${selectedEpisodeLabel(episode)}  ${episode.title.localizeEpisodeTitle(androidx.compose.ui.platform.LocalContext.current)}",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalTvMaterial3Api::class)
@Composable
fun CommentOverlay(
    review: TraktCommentReview,
    episode: Video?,
    canNavigatePrevious: Boolean,
    canNavigateNext: Boolean,
    isLoadingNext: Boolean,
    transitionDirection: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onDismiss: () -> Unit
) {
    val primaryFocusRequester = remember { FocusRequester() }
    val mainContentFocusRequester = remember { FocusRequester() }
    val closeHint = stringResource(R.string.detail_comments_back_hint)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        AmbientDialogEffect(onDismiss)
        // Launcher look: the page stays visible behind, dimmed (black .3), with the review in a
        // panel in the sidebar's glass. Our own scrim replaces the platform's dialog dim.
        val dialogWindow = (LocalView.current.parent as? DialogWindowProvider)?.window
        SideEffect { dialogWindow?.setDimAmount(0f) }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x4D000000))
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) {
                        false
                    } else {
                        when (event.key) {
                            Key.DirectionLeft -> {
                                if (canNavigatePrevious) {
                                    onPrevious()
                                    true
                                } else {
                                    false
                                }
                            }
                            Key.DirectionRight -> {
                                if (canNavigateNext) {
                                    if (!isLoadingNext) onNext()
                                    true
                                } else {
                                    false
                                }
                            }
                            else -> false
                        }
                    }
                }
        ) {
            LauncherGlass(
                modifier = Modifier
                    .align(Alignment.Center)
                    .width(560.dp)
                    .heightIn(max = 430.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
            AnimatedContent(
                targetState = review,
                transitionSpec = {
                    when {
                        transitionDirection > 0 -> {
                            slideInHorizontally(initialOffsetX = { it / 5 }) + fadeIn() togetherWith
                                slideOutHorizontally(targetOffsetX = { -it / 5 }) + fadeOut()
                        }
                        transitionDirection < 0 -> {
                            slideInHorizontally(initialOffsetX = { -it / 5 }) + fadeIn() togetherWith
                                slideOutHorizontally(targetOffsetX = { it / 5 }) + fadeOut()
                        }
                        else -> fadeIn() togetherWith fadeOut()
                    }
                },
                label = "comment_overlay_transition",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 30.dp, vertical = 26.dp)
            ) {
                currentReview ->
                CommentOverlayContent(
                    review = currentReview,
                    episode = episode,
                    primaryFocusRequester = primaryFocusRequester,
                    mainContentFocusRequester = mainContentFocusRequester,
                    isLoadingNext = isLoadingNext
                )
            }

            // Round close button, top right of the panel (Back closes too).
            var closeFocused by remember { mutableStateOf(false) }
            Card(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 24.dp, end = 26.dp)
                    .size(32.dp)
                    .focusRequester(primaryFocusRequester)
                    .onFocusChanged { closeFocused = it.isFocused }
                    .semantics { contentDescription = closeHint }
                    .focusProperties {
                        down = mainContentFocusRequester
                    },
                shape = CardDefaults.shape(CircleShape),
                colors = CardDefaults.colors(
                    containerColor = Color(0x2EFFFFFF),
                    focusedContainerColor = LauncherStyle.FocusWhite
                ),
                border = CardDefaults.border(border = Border.None, focusedBorder = Border.None),
                scale = CardDefaults.scale(focusedScale = 1.06f)
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "\u2715",
                        fontSize = 14.sp,
                        color = if (closeFocused) LauncherStyle.FocusText else Color.White
                    )
                }
            }
            // Trakt attribution, bottom right of the panel.
            Image(
                painter = painterResource(id = R.drawable.trakt_logo_wordmark),
                contentDescription = stringResource(R.string.cd_trakt_logo),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 30.dp, bottom = 22.dp)
                    .width(38.dp)
                    .height(16.dp),
                colorFilter = ColorFilter.tint(LauncherStyle.Dim)
            )
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun CommentOverlayContent(
    review: TraktCommentReview,
    episode: Video?,
    primaryFocusRequester: FocusRequester,
    mainContentFocusRequester: FocusRequester,
    isLoadingNext: Boolean
) {
    val commentScrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    var isSpoilerRevealed by rememberSaveable(review.id) { mutableStateOf(!review.hasSpoilerContent) }
    val commentText = if (review.hasSpoilerContent && !isSpoilerRevealed) {
        stringResource(R.string.detail_comments_spoiler_hidden)
    } else {
        review.comment
    }
    val commentStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 13.5.sp, lineHeight = 20.sp)
    val formattedCommentDate = remember(review.createdAt, review.updatedAt) {
        formatCommentTimestamp(review.createdAt, review.updatedAt)
    }
    val overlayLabels = buildList {
        episode?.let { add(selectedEpisodeLabel(it)) }
        if (review.review) add(stringResource(R.string.detail_comments_badge_review))
        if (review.hasSpoilerContent) add(stringResource(R.string.detail_comments_badge_spoiler))
        review.rating?.let { add(stringResource(R.string.detail_comments_badge_rating, it)) }
        formattedCommentDate?.let { add(it) }
    }

    LaunchedEffect(review.id) {
        mainContentFocusRequester.requestFocus()
        withFrameNanos { }
        commentScrollState.scrollTo(0)
        withFrameNanos { }
        commentScrollState.scrollTo(0)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Column(
                modifier = Modifier.padding(end = 44.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = review.authorDisplayName,
                    fontSize = 20.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val softLine = buildList {
                    review.authorUsername?.takeIf { it.isNotBlank() }?.let { add("@$it") }
                    addAll(overlayLabels)
                }
                if (softLine.isNotEmpty()) {
                    OverlayMetaRow(labels = softLine)
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(commentScrollState)
                    .focusRequester(mainContentFocusRequester)
                    .focusable()
                    // The scrollable body captures D-pad scrolling, so it needs the comment text
                    // as its name rather than being an unnamed focus stop.
                    .semantics { contentDescription = commentText }
                    .focusProperties {
                        up = primaryFocusRequester
                    }
                    .onPreviewKeyEvent { event ->
                        when {
                            event.type != KeyEventType.KeyDown -> false
                            event.key == Key.DirectionDown && commentScrollState.value < commentScrollState.maxValue -> {
                                coroutineScope.launch {
                                    commentScrollState.animateScrollTo(
                                        (commentScrollState.value + 260).coerceAtMost(commentScrollState.maxValue)
                                    )
                                }
                                true
                            }
                            event.key == Key.DirectionUp && commentScrollState.value > 0 -> {
                                coroutineScope.launch {
                                    commentScrollState.animateScrollTo(
                                        (commentScrollState.value - 260).coerceAtLeast(0)
                                    )
                                }
                                true
                            }
                            !isSpoilerRevealed && (
                                event.key == Key.DirectionCenter ||
                                    event.key == Key.Enter ||
                                    event.key == Key.NumPadEnter
                                ) -> {
                                isSpoilerRevealed = true
                                true
                            }
                            else -> false
                        }
                    },
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = commentText,
                    style = commentStyle.copy(textDirection = commentText.contentTextDirection()),
                    color = Color(0xE0FFFFFF),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = stringResource(R.string.detail_comments_likes, review.likes),
                    fontSize = 11.sp,
                    color = LauncherStyle.Dim
                )
                if (review.hasSpoilerContent && !isSpoilerRevealed) {
                    Text(
                        text = stringResource(R.string.detail_comments_reveal_spoiler_hint),
                        fontSize = 11.sp,
                        color = LauncherStyle.Soft
                    )
                }
                if (isLoadingNext) {
                    Text(
                        text = "...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.42f)
                    )
                }
            }
        }
    }
}

@Composable
private fun OverlayMetaRow(labels: List<String>) {
    Text(
        text = labels.joinToString(" \u00B7 "),
        fontSize = 12.sp,
        color = LauncherStyle.Soft,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun readerCommentStyle(length: Int): TextStyle {
    val typography = MaterialTheme.typography
    return when {
        length <= 160 -> typography.displaySmall.copy(fontSize = 40.sp, lineHeight = 48.sp)
        length <= 280 -> typography.headlineLarge.copy(fontSize = 30.sp, lineHeight = 38.sp)
        length <= 420 -> typography.headlineMedium.copy(fontSize = 24.sp, lineHeight = 31.sp)
        length <= 650 -> typography.titleLarge.copy(fontSize = 20.sp, lineHeight = 26.sp)
        length <= 900 -> typography.titleMedium.copy(fontSize = 18.sp, lineHeight = 23.sp)
        else -> typography.bodyLarge.copy(fontSize = 16.sp, lineHeight = 21.sp)
    }
}

private fun formatCommentTimestamp(createdAt: String?, updatedAt: String?): String? {
    val rawTimestamp = createdAt?.trim()?.takeIf { it.isNotBlank() }
        ?: updatedAt?.trim()?.takeIf { it.isNotBlank() }
        ?: return null

    val instant = runCatching {
        if (rawTimestamp.all { it.isDigit() }) {
            val epoch = rawTimestamp.toLong()
            val epochMillis = if (epoch < 100_000_000_000L) epoch * 1000L else epoch
            Instant.ofEpochMilli(epochMillis)
        } else {
            runCatching { Instant.parse(rawTimestamp) }.getOrElse {
                runCatching { OffsetDateTime.parse(rawTimestamp).toInstant() }.getOrElse {
                    LocalDateTime.parse(rawTimestamp).atZone(ZoneId.systemDefault()).toInstant()
                }
            }
        }
    }.getOrNull() ?: return null

    val formatter = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault())
    return formatter.format(instant.atZone(ZoneId.systemDefault()))
}

private fun commentMaxLines(length: Int): Int = when {
    length <= 160 -> 7
    length <= 280 -> 10
    length <= 420 -> 13
    length <= 650 -> 17
    length <= 900 -> 22
    else -> 28
}

private fun selectedEpisodeLabel(video: Video): String {
    val season = video.season ?: 0
    val episode = video.episode ?: 0
    return "S${season.toString().padStart(2, '0')}E${episode.toString().padStart(2, '0')}"
}

@Composable
private fun LoadingCommentCard(
    shape: RoundedCornerShape,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(300.dp)
            .height(150.dp)
            .background(
                color = Color(0x14FFFFFF),
                shape = shape
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .width(160.dp)
                .height(18.dp)
                .background(Color(0x14FFFFFF), shape = shape)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(NuvioTheme.spacing.sm)) {
            repeat(3) {
                Box(
                    modifier = Modifier
                        .width(56.dp)
                        .height(12.dp)
                        .background(Color(0x14FFFFFF), shape = shape)
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(Color(0x14FFFFFF), shape = shape)
        )
        Box(
            modifier = Modifier
                .width(120.dp)
                .height(NuvioTheme.spacing.lg)
                .background(Color(0x14FFFFFF), shape = shape)
        )
    }
}

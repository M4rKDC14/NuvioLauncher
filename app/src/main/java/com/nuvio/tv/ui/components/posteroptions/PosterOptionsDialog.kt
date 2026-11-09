package com.nuvio.tv.ui.components.posteroptions

import com.nuvio.tv.ui.theme.NuvioTheme

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Divider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.nuvio.tv.R
import com.nuvio.tv.core.tracking.TrackingMembershipRemovalImpact
import com.nuvio.tv.core.tracking.TrackingMembershipRemovalConfirmation
import com.nuvio.tv.core.tracking.LOCAL_LIBRARY_LIST_KEY
import com.nuvio.tv.core.tracking.supportsMembershipFor
import com.nuvio.tv.domain.model.LibraryListTab
import com.nuvio.tv.domain.model.localizedMembershipTitle
import com.nuvio.tv.domain.model.LibrarySourceMode
import com.nuvio.tv.ui.components.NuvioDialog

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PosterOptionsDialog(
    title: String,
    isInLibrary: Boolean,
    isLibraryPending: Boolean,
    showManageLists: Boolean,
    isMovie: Boolean,
    isSeries: Boolean = false,
    isWatched: Boolean,
    isWatchedPending: Boolean,
    onDismiss: () -> Unit,
    onDetails: () -> Unit,
    onToggleLibrary: () -> Unit,
    onToggleWatched: () -> Unit
) {
    // Nuvio Launcher: the launcher's own options popup (approved mockup), as for apps and Resume:
    // narrow glass, title and "Title actions", centred pills with the focused one white.
    val detailsLabel = stringResource(R.string.cw_action_go_to_details)
    val libraryLabel = when {
        showManageLists -> stringResource(R.string.library_manage_lists)
        isInLibrary -> stringResource(R.string.hero_remove_from_library)
        else -> stringResource(R.string.hero_add_to_library)
    }
    val watchedLabel = if (isWatched) stringResource(R.string.hero_mark_unwatched) else stringResource(R.string.hero_mark_watched)
    com.nuvio.tv.ui.launcher.AppOptionsDialog(
        label = title,
        subtitle = stringResource(R.string.home_poster_dialog_subtitle),
        onDismiss = onDismiss,
        options = buildList {
            add(detailsLabel to onDetails)
            add(libraryLabel to { if (!isLibraryPending) onToggleLibrary() })
            if (isMovie || isSeries) add(watchedLabel to { if (!isWatchedPending) onToggleWatched() })
        }
    )
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PosterListPickerDialog(
    title: String,
    tabs: List<LibraryListTab>,
    membership: Map<String, Boolean>,
    isPending: Boolean,
    error: String?,
    onToggle: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    // Nuvio Launcher: the launcher's popup look (approved mockup), shared by every "lists" popup.
    com.nuvio.tv.ui.launcher.LauncherListPickerDialog(
        title = title,
        subtitle = stringResource(R.string.detail_lists_subtitle),
        lists = tabs.map { it.key to it.localizedMembershipTitle() },
        membership = membership,
        isPending = isPending,
        error = error,
        saveLabel = if (isPending) stringResource(R.string.action_saving) else stringResource(R.string.action_save),
        onToggle = onToggle,
        onSave = onSave,
        onDismiss = onDismiss
    )
}

@Composable
fun PosterOptionsHost(
    state: PosterOptionsState,
    controller: PosterOptionsController,
    onNavigateToDetail: (id: String, type: String, addonBaseUrl: String) -> Unit
) {
    val target = state.target
    if (target != null) {
        val isMovie = target.apiType.equals("movie", ignoreCase = true)
        val isSeries = target.apiType.equals("series", ignoreCase = true) ||
            target.apiType.equals("tv", ignoreCase = true) ||
            target.apiType.equals("anime", ignoreCase = true)
        PosterOptionsDialog(
            title = target.name,
            isInLibrary = state.isInLibrary,
            isLibraryPending = state.isLibraryPending,
            showManageLists = state.librarySourceMode != LibrarySourceMode.LOCAL,
            isMovie = isMovie,
            isSeries = isSeries,
            isWatched = state.isWatched,
            isWatchedPending = state.isWatchedPending,
            onDismiss = { controller.dismiss() },
            onDetails = {
                onNavigateToDetail(target.id, target.apiType, state.addonBaseUrl)
                controller.dismiss()
            },
            onToggleLibrary = {
                if (state.librarySourceMode != LibrarySourceMode.LOCAL) {
                    controller.openListPicker()
                } else {
                    controller.toggleLibrary()
                    controller.dismiss()
                }
            },
            onToggleWatched = {
                if (isMovie) {
                    controller.toggleMovieWatched()
                } else {
                    controller.toggleSeriesWatched()
                }
                controller.dismiss()
            }
        )
    }

    if (state.listPickerActive) {
        val localTab = LibraryListTab(
            key = LOCAL_LIBRARY_LIST_KEY,
            title = stringResource(R.string.trakt_library_source_nuvio),
            type = LibraryListTab.Type.WATCHLIST
        )
        val contentType = state.listPickerContentType.orEmpty()
        val destinationTabs = state.libraryListTabs.filter { tab ->
            tab.supportsMembershipFor(contentType)
        }
        PosterListPickerDialog(
            title = state.listPickerTitle.orEmpty(),
            tabs = listOf(localTab) + destinationTabs,
            membership = state.listPickerMembership,
            isPending = state.listPickerPending,
            error = state.listPickerError,
            onToggle = { key -> controller.toggleListMembership(key) },
            onSave = { controller.saveListPicker() },
            onDismiss = { controller.dismissListPicker() }
        )
    }

    val confirmations = state.removalConfirmations
    if (confirmations.isNotEmpty()) {
        TrackingRemovalConfirmationDialog(
            itemTitle = state.listPickerTitle.orEmpty(),
            confirmations = confirmations,
            isPending = state.listPickerPending,
            onConfirm = controller::confirmDestructiveRemoval,
            onDismiss = controller::cancelDestructiveRemoval
        )
    }
}

@Composable
fun TrackingRemovalConfirmationDialog(
    itemTitle: String,
    confirmations: List<TrackingMembershipRemovalConfirmation>,
    isPending: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val providers = confirmations.joinToString { confirmation ->
        confirmation.providerId.storageId.replaceFirstChar(Char::uppercase)
    }
    val impacts = confirmations.flatMapTo(linkedSetOf()) { confirmation -> confirmation.impacts }
    val impact = when (impacts) {
        setOf(TrackingMembershipRemovalImpact.WATCHED_HISTORY) ->
            stringResource(R.string.tracking_removal_impact_history)
        setOf(TrackingMembershipRemovalImpact.RATING) ->
            stringResource(R.string.tracking_removal_impact_rating)
        else -> stringResource(R.string.tracking_removal_impact_history_rating)
    }
    NuvioDialog(
        onDismiss = onDismiss,
        title = stringResource(R.string.tracking_removal_title, providers),
        subtitle = stringResource(R.string.tracking_removal_message, itemTitle, providers, impact),
        width = 560.dp,
        suppressFirstKeyUp = false
    ) {
        Button(
            onClick = onConfirm,
            enabled = !isPending,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.action_remove_anyway))
        }
        Button(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.colors(
                containerColor = NuvioTheme.colors.BackgroundCard,
                contentColor = NuvioTheme.colors.TextPrimary
            )
        ) {
            Text(stringResource(R.string.action_cancel))
        }
    }
}

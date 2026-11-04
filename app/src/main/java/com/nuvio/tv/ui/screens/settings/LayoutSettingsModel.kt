package com.nuvio.tv.ui.screens.settings

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.automirrored.filled.ViewSidebar
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.RoundedCorner
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.ui.graphics.vector.ImageVector
import com.nuvio.tv.R

internal enum class LayoutSection(
    @param:StringRes val title: Int,
    @param:StringRes val description: Int,
    val icon: ImageVector
) {
    HOME_LAYOUT(R.string.layout_section_home, R.string.layout_section_home_desc, Icons.Default.Dashboard),
    HOME_CONTENT(R.string.layout_section_content, R.string.layout_section_content_desc, Icons.Default.ViewAgenda),
    SIDEBAR(R.string.layout_section_sidebar, R.string.layout_section_sidebar_desc, Icons.AutoMirrored.Filled.ViewSidebar),
    CONTINUE_WATCHING(R.string.layout_section_continue_watching, R.string.layout_section_continue_watching_desc, Icons.Default.History),
    FOCUSED_POSTER(R.string.layout_section_focused, R.string.layout_section_focused_desc, Icons.Default.CenterFocusStrong),
    POSTER_CARD(R.string.layout_section_card_style, R.string.layout_section_card_style_desc, Icons.Default.RoundedCorner),
    CARD_DEPTH(R.string.settings_card_depth_title, R.string.layout_section_card_depth_desc, Icons.Default.ViewInAr),
    CUSTOM_POSTERS(R.string.layout_custom_poster_title, R.string.layout_section_custom_poster_desc, Icons.Default.PhotoLibrary),
    DETAIL_PAGE(R.string.layout_section_detail, R.string.layout_section_detail_desc, Icons.Default.Description),
    STREAMS(R.string.layout_section_streams, R.string.layout_section_streams_desc, Icons.AutoMirrored.Filled.ViewList)
}

/**
 * Nuvio Launcher: sections that only shape Nuvio's own Home layouts. The launcher draws its own
 * Home (its carousel catalogs live on the Launcher settings page), so these are hidden.
 */
internal val LauncherHiddenLayoutSections = setOf(
    LayoutSection.HOME_LAYOUT,
    LayoutSection.FOCUSED_POSTER,
    LayoutSection.POSTER_CARD,
    LayoutSection.CARD_DEPTH
)

internal fun visibleLayoutSections(
    essentialMode: Boolean,
    @Suppress("UNUSED_PARAMETER") focusedPosterHasOptions: Boolean
): List<LayoutSection> =
    if (essentialMode) {
        // Nuvio's essential mode only offered the Home layout choice, which the launcher hides.
        listOf(LayoutSection.HOME_CONTENT)
    } else {
        LayoutSection.entries.filter { section -> section !in LauncherHiddenLayoutSections }
    }

internal object LayoutSettingsTestTags {
    fun section(section: LayoutSection): String = "layout_section_${section.name.lowercase()}"
}

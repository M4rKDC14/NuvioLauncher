package com.nuvio.tv.ui.screens.settings

import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.view.KeyEvent
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.nuvio.tv.R
import com.nuvio.tv.domain.model.AmbientModeSettings
import com.nuvio.tv.domain.model.AmbientSource
import com.nuvio.tv.ui.components.NuvioDialog
import com.nuvio.tv.ui.theme.NuvioTheme

/** Nuvio Launcher: the Source chips, in order, with their labels. */
private val AmbientSourceChoices = listOf(
    AmbientSource.CATALOGS to "Catalogs",
    AmbientSource.WALLPAPER to "r/Wallpaper",
    AmbientSource.EARTHPORN to "r/EarthPorn",
    AmbientSource.CUSTOM to "Custom subreddit"
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AmbientModeSettingsCard(
    firstRowFocusRequester: androidx.compose.ui.focus.FocusRequester? = null,
    viewModel: AmbientModeSettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val settings = uiState.settings
    var showTimeoutDialog by remember { mutableStateOf(false) }
    var showCatalogsDialog by remember { mutableStateOf(false) }
    var showSubredditDialog by remember { mutableStateOf(false) }
    val selectedCatalogs = settings.catalogKeys.filter { key -> uiState.catalogs.any { it.key == key } }

    SettingsGroupCard(
        modifier = Modifier.fillMaxWidth(),
        title = stringResource(R.string.ambient_mode_title),
        subtitle = stringResource(R.string.ambient_mode_subtitle)
    ) {
        SettingsToggleRow(
            title = stringResource(R.string.ambient_mode_title),
            subtitle = stringResource(R.string.ambient_mode_enabled_subtitle),
            checked = settings.enabled,
            onToggle = { viewModel.setEnabled(!settings.enabled) },
            modifier = firstRowFocusRequester?.let { androidx.compose.ui.Modifier.focusRequester(it) } ?: androidx.compose.ui.Modifier
        )
        if (settings.enabled) {
            SettingsActionRow(
                title = stringResource(R.string.ambient_mode_timeout),
                subtitle = stringResource(R.string.ambient_mode_timeout_subtitle),
                value = timeoutLabel(settings.timeoutMinutes),
                onClick = { showTimeoutDialog = true }
            )
            // Nuvio Launcher: where the pictures come from. Catalogs keeps the Catalogs row below; a fixed
            // subreddit needs nothing more; Custom subreddit shows the Subreddit row, which takes a link.
            LauncherRowCard(
                title = "Source",
                subtitle = "Where Ambient Mode takes its pictures from"
            ) {
                val firstSourceFocus = remember { FocusRequester() }
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .settingsOptionRow(firstSourceFocus),
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    AmbientSourceChoices.forEachIndexed { index, (source, label) ->
                        SettingsChoiceChip(
                            label = label,
                            selected = settings.source == source,
                            onClick = {
                                viewModel.setSource(source)
                                // no community chosen yet: ask for the link straight away
                                if (source == AmbientSource.CUSTOM && settings.customSubreddit.isBlank()) {
                                    showSubredditDialog = true
                                }
                            },
                            modifier = if (index == 0) Modifier.focusRequester(firstSourceFocus) else Modifier
                        )
                    }
                }
            }
            if (settings.source == AmbientSource.CUSTOM) {
                SettingsActionRow(
                    title = "Subreddit",
                    subtitle = "Pictures from this Reddit community",
                    value = settings.customSubreddit.takeIf { it.isNotBlank() }?.let { "r/$it" } ?: "Not set",
                    onClick = { showSubredditDialog = true }
                )
            }
            if (settings.source == AmbientSource.CATALOGS) SettingsActionRow(
                title = stringResource(R.string.ambient_mode_catalogs),
                subtitle = stringResource(R.string.ambient_mode_catalogs_subtitle),
                value = if (selectedCatalogs.isEmpty()) {
                    stringResource(R.string.ambient_mode_catalogs_default)
                } else {
                    stringResource(
                        R.string.ambient_mode_catalogs_count,
                        selectedCatalogs.size,
                        AmbientModeSettings.MAX_CATALOGS
                    )
                },
                enabled = uiState.catalogs.isNotEmpty(),
                onClick = { showCatalogsDialog = true }
            )
        }
    }

    if (showTimeoutDialog) {
        SettingsSingleChoiceDialog(
            title = stringResource(R.string.ambient_mode_timeout),
            subtitle = stringResource(R.string.ambient_mode_timeout_subtitle),
            options = AmbientModeSettings.TIMEOUT_OPTIONS_MINUTES.map { minutes ->
                SettingsPickerOption(minutes, timeoutLabel(minutes))
            },
            selectedValue = settings.timeoutMinutes,
            onOptionSelected = { minutes ->
                viewModel.setTimeoutMinutes(minutes)
                showTimeoutDialog = false
            },
            onDismiss = { showTimeoutDialog = false },
            maxHeight = 360.dp
        )
    }

    if (showSubredditDialog) {
        SubredditDialog(
            currentValue = settings.customSubreddit.takeIf { it.isNotBlank() }?.let { "r/$it" }.orEmpty(),
            onSaved = { name ->
                viewModel.setCustomSubreddit(name)
                viewModel.setSource(AmbientSource.CUSTOM)
                showSubredditDialog = false
            },
            onDismiss = { showSubredditDialog = false }
        )
    }

    if (showCatalogsDialog) {
        SettingsMultiChoiceDialog(
            title = stringResource(R.string.ambient_mode_catalogs),
            subtitle = stringResource(R.string.ambient_mode_catalogs_dialog_subtitle),
            options = uiState.catalogs.map { catalog ->
                SettingsPickerOption(catalog.key, catalog.title, description = catalog.source)
            },
            selectedValues = selectedCatalogs,
            onValuesSelected = { keys ->
                viewModel.setCatalogKeys(keys)
                showCatalogsDialog = false
            },
            onDismiss = { showCatalogsDialog = false },
            width = 620.dp,
            maxHeight = 460.dp,
            maxSelections = AmbientModeSettings.MAX_CATALOGS
        )
    }
}

@Composable
private fun timeoutLabel(minutes: Int): String =
    pluralStringResource(R.plurals.ambient_mode_timeout_minutes, minutes, minutes)

/** Nuvio Launcher: Subreddit link entry, built like the TMDB API key dialog. */
@Composable
private fun SubredditDialog(
    currentValue: String,
    onSaved: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var value by remember(currentValue) { mutableStateOf(currentValue) }
    var isInputFocused by remember { mutableStateOf(false) }
    val inputFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val context = LocalContext.current

    NuvioDialog(
        onDismiss = onDismiss,
        title = "Subreddit",
        subtitle = "Paste a Reddit link or type a community name",
        width = 700.dp
    ) {
        Card(
            onClick = { inputFocusRequester.requestFocus() },
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { isInputFocused = it.isFocused || it.hasFocus },
            colors = CardDefaults.colors(
                containerColor = NuvioTheme.colors.BackgroundElevated,
                focusedContainerColor = NuvioTheme.colors.BackgroundElevated
            ),
            border = CardDefaults.border(
                border = Border(
                    border = androidx.compose.foundation.BorderStroke(NuvioTheme.spacing.hairline, NuvioTheme.colors.Border),
                    shape = RoundedCornerShape(10.dp)
                ),
                focusedBorder = Border(
                    border = NuvioTheme.focusRing.border(NuvioTheme.spacing.xxs),
                    shape = RoundedCornerShape(10.dp)
                )
            ),
            shape = CardDefaults.shape(RoundedCornerShape(10.dp)),
            scale = CardDefaults.scale(focusedScale = 1f)
        ) {
            Box(modifier = Modifier.padding(horizontal = 14.dp, vertical = NuvioTheme.spacing.md)) {
                BasicTextField(
                    value = value,
                    onValueChange = { value = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(inputFocusRequester)
                        .onKeyEvent { event ->
                            event.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DPAD_CENTER &&
                                event.nativeKeyEvent.action == KeyEvent.ACTION_DOWN
                        },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = NuvioTheme.colors.TextPrimary),
                    cursorBrush = SolidColor(
                        if (isInputFocused) NuvioTheme.colors.Primary else androidx.compose.ui.graphics.Color.Transparent
                    ),
                    decorationBox = { innerTextField ->
                        if (value.isBlank()) {
                            Text(
                                text = "https://www.reddit.com/r/…",
                                style = MaterialTheme.typography.bodyMedium,
                                color = NuvioTheme.colors.TextTertiary
                            )
                        }
                        innerTextField()
                    }
                )
            }
        }

        Text(
            text = "Only image posts are shown. Landscape pictures only.",
            style = MaterialTheme.typography.bodySmall,
            color = NuvioTheme.colors.TextTertiary
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.colors(containerColor = NuvioTheme.colors.BackgroundElevated, contentColor = NuvioTheme.colors.TextPrimary)
            ) { Text(stringResource(R.string.action_cancel)) }
            Spacer(modifier = Modifier.width(NuvioTheme.spacing.sm))
            Button(
                onClick = {
                    val name = AmbientModeSettings.parseSubreddit(value)
                    if (name == null) {
                        Toast.makeText(context, "That is not a Reddit community link or name.", Toast.LENGTH_LONG).show()
                    } else {
                        onSaved(name)
                    }
                },
                colors = ButtonDefaults.colors(containerColor = NuvioTheme.colors.BackgroundCard, contentColor = NuvioTheme.colors.TextPrimary)
            ) { Text(stringResource(R.string.action_save)) }
        }
    }
}

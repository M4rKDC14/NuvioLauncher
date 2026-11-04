@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.nuvio.tv.ui.screens.settings

import com.nuvio.tv.ui.theme.NuvioTheme

import androidx.activity.compose.BackHandler
import android.view.KeyEvent
import android.widget.Toast
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.rememberCoroutineScope
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
import com.nuvio.tv.core.tmdb.TmdbKey
import com.nuvio.tv.ui.components.NuvioDialog
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.ExperimentalTvMaterial3Api
import com.nuvio.tv.R
import com.nuvio.tv.core.build.AppFeaturePolicy
import com.nuvio.tv.data.local.AVAILABLE_TMDB_LANGUAGES
import com.nuvio.tv.data.local.displayName

@Composable
fun TmdbSettingsScreen(
    viewModel: TmdbSettingsViewModel = hiltViewModel(),
    onBackPress: () -> Unit
) {
    BackHandler { onBackPress() }

    SettingsStandaloneScaffold(
        title = stringResource(R.string.tmdb_title),
        subtitle = stringResource(R.string.tmdb_subtitle)
    ) {
        TmdbSettingsContent(viewModel = viewModel)
    }
}

@Composable
fun TmdbSettingsContent(
    viewModel: TmdbSettingsViewModel = hiltViewModel(),
    initialFocusRequester: FocusRequester? = null
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showLanguageDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var showKeyDialog by remember { mutableStateOf(false) }
    // follows the account: a personal key set in the Nuvio phone app arrives here too
    val savedKey by TmdbKey.userKeyFlow.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        SettingsDetailHeader(
            title = stringResource(R.string.tmdb_title),
            subtitle = stringResource(R.string.tmdb_subtitle)
        )

        SettingsGroupCard(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            val tmdbListState = rememberLazyListState()
            Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = tmdbListState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = NuvioTheme.spacing.sm),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item(key = "tmdb_enabled") {
                    SettingsToggleRow(
                        title = stringResource(R.string.tmdb_enable_title),
                        subtitle = stringResource(R.string.tmdb_enable_subtitle),
                        checked = uiState.enabled,
                        onToggle = { viewModel.onEvent(TmdbSettingsEvent.ToggleEnabled(!uiState.enabled)) },
                        modifier = Modifier
                            .padding(top = NuvioTheme.spacing.xxs)
                            .then(
                                if (initialFocusRequester != null) {
                                    Modifier.focusRequester(initialFocusRequester)
                                } else {
                                    Modifier
                                })
                    )
                }

                // Nuvio Launcher: this build has no built-in TMDB key, so the owner adds theirs here.
                item(key = "tmdb_api_key") {
                    SettingsActionRow(
                        title = "TMDB API key",
                        subtitle = "Your own key from themoviedb.org (API key or Read Access Token). Syncs with the personal TMDB key in the Nuvio phone app. Needed for cast, reviews, companies, episodes and more like this.",
                        value = maskTmdbKey(savedKey),
                        onClick = { showKeyDialog = true }
                    )
                }

                item(key = "tmdb_modern_home_enabled") {
                    SettingsToggleRow(
                        title = stringResource(R.string.tmdb_modern_home_title),
                        subtitle = stringResource(R.string.tmdb_modern_home_subtitle),
                        checked = uiState.modernHomeEnabled,
                        enabled = uiState.enabled,
                        onToggle = {
                            viewModel.onEvent(
                                TmdbSettingsEvent.ToggleModernHomeEnabled(!uiState.modernHomeEnabled)
                            )
                        }
                    )
                }

                item(key = "tmdb_enrich_continue_watching") {
                    SettingsToggleRow(
                        title = stringResource(R.string.tmdb_enrich_continue_watching_title),
                        subtitle = stringResource(R.string.tmdb_enrich_continue_watching_subtitle),
                        checked = uiState.enrichContinueWatching,
                        enabled = uiState.enabled,
                        onToggle = {
                            viewModel.onEvent(
                                TmdbSettingsEvent.ToggleEnrichContinueWatching(!uiState.enrichContinueWatching)
                            )
                        }
                    )
                }

                item(key = "tmdb_language") {
                    val languageName = AVAILABLE_TMDB_LANGUAGES
                        .find { it.code == uiState.language }
                        ?.displayName
                        ?: uiState.language.uppercase()
                    SettingsActionRow(
                        title = stringResource(R.string.tmdb_language_title),
                        subtitle = stringResource(R.string.tmdb_language_subtitle),
                        value = languageName,
                        enabled = uiState.enabled,
                        onClick = { showLanguageDialog = true }
                    )
                }

                item(key = "tmdb_artwork") {
                    SettingsToggleRow(
                        title = stringResource(R.string.tmdb_artwork_title),
                        subtitle = stringResource(R.string.tmdb_artwork_subtitle),
                        checked = uiState.useArtwork,
                        enabled = uiState.enabled,
                        onToggle = { viewModel.onEvent(TmdbSettingsEvent.ToggleArtwork(!uiState.useArtwork)) }
                    )
                }

                item(key = "tmdb_basic_info") {
                    SettingsToggleRow(
                        title = stringResource(R.string.tmdb_basic_info_title),
                        subtitle = stringResource(R.string.tmdb_basic_info_subtitle),
                        checked = uiState.useBasicInfo,
                        enabled = uiState.enabled,
                        onToggle = { viewModel.onEvent(TmdbSettingsEvent.ToggleBasicInfo(!uiState.useBasicInfo)) }
                    )
                }

                item(key = "tmdb_details") {
                    SettingsToggleRow(
                        title = stringResource(R.string.tmdb_details_title),
                        subtitle = stringResource(R.string.tmdb_details_subtitle),
                        checked = uiState.useDetails,
                        enabled = uiState.enabled,
                        onToggle = { viewModel.onEvent(TmdbSettingsEvent.ToggleDetails(!uiState.useDetails)) }
                    )
                }

                item(key = "tmdb_credits") {
                    SettingsToggleRow(
                        title = stringResource(R.string.tmdb_credits_title),
                        subtitle = stringResource(R.string.tmdb_credits_subtitle),
                        checked = uiState.useCredits,
                        enabled = uiState.enabled,
                        onToggle = { viewModel.onEvent(TmdbSettingsEvent.ToggleCredits(!uiState.useCredits)) }
                    )
                }

                item(key = "tmdb_productions") {
                    SettingsToggleRow(
                        title = stringResource(R.string.tmdb_productions_title),
                        subtitle = stringResource(R.string.tmdb_productions_subtitle),
                        checked = uiState.useProductions,
                        enabled = uiState.enabled,
                        onToggle = { viewModel.onEvent(TmdbSettingsEvent.ToggleProductions(!uiState.useProductions)) }
                    )
                }

                item(key = "tmdb_networks") {
                    SettingsToggleRow(
                        title = stringResource(R.string.tmdb_networks_title),
                        subtitle = stringResource(R.string.tmdb_networks_subtitle),
                        checked = uiState.useNetworks,
                        enabled = uiState.enabled,
                        onToggle = { viewModel.onEvent(TmdbSettingsEvent.ToggleNetworks(!uiState.useNetworks)) }
                    )
                }

                item(key = "tmdb_episodes") {
                    SettingsToggleRow(
                        title = stringResource(R.string.tmdb_episodes_title),
                        subtitle = stringResource(R.string.tmdb_episodes_subtitle),
                        checked = uiState.useEpisodes,
                        enabled = uiState.enabled,
                        onToggle = { viewModel.onEvent(TmdbSettingsEvent.ToggleEpisodes(!uiState.useEpisodes)) }
                    )
                }

                if (AppFeaturePolicy.inAppTrailerPlaybackEnabled) {
                    item(key = "tmdb_trailers") {
                        SettingsToggleRow(
                            title = stringResource(R.string.tmdb_trailers_title),
                            subtitle = stringResource(R.string.tmdb_trailers_subtitle),
                            checked = uiState.useTrailers,
                            enabled = uiState.enabled,
                            onToggle = {
                                viewModel.onEvent(
                                    TmdbSettingsEvent.ToggleTrailers(!uiState.useTrailers)
                                )
                            }
                        )
                    }
                }

                item(key = "tmdb_more_like_this") {
                    SettingsToggleRow(
                        title = stringResource(R.string.tmdb_more_like_this_title),
                        subtitle = stringResource(R.string.tmdb_more_like_this_subtitle),
                        checked = uiState.useMoreLikeThis,
                        enabled = uiState.enabled,
                        onToggle = {
                            viewModel.onEvent(
                                TmdbSettingsEvent.ToggleMoreLikeThis(!uiState.useMoreLikeThis)
                            )
                        }
                    )
                }

                item(key = "tmdb_collections") {
                    SettingsToggleRow(
                        title = stringResource(R.string.tmdb_collections_title),
                        subtitle = stringResource(R.string.tmdb_collections_subtitle),
                        checked = uiState.useCollections,
                        enabled = uiState.enabled,
                        onToggle = {
                            viewModel.onEvent(
                                TmdbSettingsEvent.ToggleCollections(!uiState.useCollections)
                            )
                        }
                    )
                }

            }
            SettingsVerticalScrollIndicators(state = tmdbListState)
            }
        }
    }

    if (showKeyDialog) {
        TmdbApiKeyDialog(
            currentValue = savedKey,
            onSaved = { k ->
                TmdbKey.save(context, k); showKeyDialog = false
                // a working key turns TMDB on, so the details appear without a second step
                if (k.isNotBlank() && !uiState.enabled) viewModel.onEvent(TmdbSettingsEvent.ToggleEnabled(true))
            },
            onDismiss = { showKeyDialog = false }
        )
    }

    if (showLanguageDialog) {
        LanguageSelectionDialog(
            title = stringResource(R.string.tmdb_language_dialog_title),
            selectedLanguage = uiState.language,
            showNoneOption = false,
            onLanguageSelected = { language ->
                viewModel.onEvent(TmdbSettingsEvent.SetLanguage(language ?: "en"))
                showLanguageDialog = false
            },
            onDismiss = { showLanguageDialog = false }
        )
    }
}

private fun maskTmdbKey(key: String): String {
    val k = key.trim()
    if (k.isBlank()) return "Not set"
    return if (k.length <= 4) "••••" else "••••••${k.takeLast(4)}"
}

@Composable
private fun TmdbApiKeyDialog(
    currentValue: String,
    onSaved: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var value by remember(currentValue) { mutableStateOf(currentValue) }
    var isInputFocused by remember { mutableStateOf(false) }
    var checking by remember { mutableStateOf(false) }
    val inputFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    NuvioDialog(
        onDismiss = onDismiss,
        title = "TMDB API key",
        subtitle = "Kept on this TV only and sent only to api.themoviedb.org.",
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
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp)
                ),
                focusedBorder = Border(
                    border = NuvioTheme.focusRing.border(NuvioTheme.spacing.xxs),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp)
                )
            ),
            shape = CardDefaults.shape(androidx.compose.foundation.shape.RoundedCornerShape(10.dp)),
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
                                text = "Paste or type your TMDB key",
                                style = MaterialTheme.typography.bodyMedium,
                                color = NuvioTheme.colors.TextTertiary
                            )
                        }
                        innerTextField()
                    }
                )
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.colors(containerColor = NuvioTheme.colors.BackgroundElevated, contentColor = NuvioTheme.colors.TextPrimary)
            ) { Text(stringResource(R.string.action_cancel)) }
            Spacer(modifier = Modifier.width(NuvioTheme.spacing.sm))
            Button(
                onClick = { onSaved("") },
                colors = ButtonDefaults.colors(containerColor = NuvioTheme.colors.BackgroundElevated, contentColor = NuvioTheme.colors.TextPrimary)
            ) { Text(stringResource(R.string.action_clear)) }
            Spacer(modifier = Modifier.width(NuvioTheme.spacing.sm))
            Button(
                onClick = {
                    if (!checking) {
                        checking = true
                        scope.launch {
                            val problem = TmdbKey.check(value)
                            checking = false
                            if (problem == null) {
                                Toast.makeText(context, "TMDB key saved. Reopen a title to see its details.", Toast.LENGTH_LONG).show()
                                onSaved(value)
                            } else {
                                Toast.makeText(context, problem, Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                },
                colors = ButtonDefaults.colors(containerColor = NuvioTheme.colors.BackgroundCard, contentColor = NuvioTheme.colors.TextPrimary)
            ) { Text(if (checking) "Checking…" else stringResource(R.string.action_save)) }
        }
    }
}

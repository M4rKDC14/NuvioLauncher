package com.nuvio.tv.data.local

import androidx.datastore.preferences.core.stringPreferencesKey
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Nuvio Launcher's own options (Settings → Launcher). Kept on this TV only: not tied to a profile
 * and never synced, since they describe this screen.
 */
data class LauncherSettings(
    /** Seconds before the Home carousel moves on; 0 = it does not move on by itself. */
    val rotationSeconds: Int = DEFAULT_ROTATION_SECONDS,
    /** How many titles the Home carousel cycles through. */
    val heroCount: Int = DEFAULT_HERO_COUNT,
    val showTimePill: Boolean = true,
    /** null = follow the TV's own 12/24-hour setting. */
    val clock24Hour: Boolean? = null,
    val catalogTileSizePercent: Int = DEFAULT_CATALOG_SIZE,
    val catalogTileSpacingDp: Int = DEFAULT_CATALOG_SPACING,
    val catalogFocusZoomPercent: Int = DEFAULT_CATALOG_ZOOM,
    val appsTileSizePercent: Int = DEFAULT_APPS_SIZE,
    val appsTileSpacingDp: Int = DEFAULT_APPS_SPACING,
    val appsFocusZoomPercent: Int = DEFAULT_APPS_ZOOM,
    val hiddenPackages: Set<String> = emptySet(),
    /** Nuvio Launcher: the order apps were moved into in the Applications panel (packages, first to last). */
    val appOrder: List<String> = emptyList(),
    /** Nuvio Launcher: the apps in Home's Favourites row (packages, first to last). */
    val favourites: List<String> = emptyList(),
    /** Settings → Appearance → Background: false = the blurred picture of the last title, true = [backgroundColor]. */
    val solidBackground: Boolean = false,
    /** The solid background colour (ARGB), used when [solidBackground] is on. Default: the launcher grey. */
    val backgroundColor: Int = DEFAULT_BACKGROUND_COLOR
) {
    val catalogScale: Float get() = catalogTileSizePercent / 100f
    val catalogZoom: Float get() = catalogFocusZoomPercent / 100f
    val appsScale: Float get() = appsTileSizePercent / 100f
    val appsZoom: Float get() = appsFocusZoomPercent / 100f
    val rotationMs: Long get() = rotationSeconds * 1_000L

    companion object {
        const val DEFAULT_BACKGROUND_COLOR = 0xFF1E1F23.toInt()
        const val DEFAULT_ROTATION_SECONDS = 9
        val ROTATION_CHOICES = listOf(0, 6, 9, 12, 20)
        const val DEFAULT_HERO_COUNT = 8
        const val MIN_HERO_COUNT = 4
        const val MAX_HERO_COUNT = 12

        const val MIN_SIZE = 70
        const val MAX_SIZE = 130
        const val SIZE_STEP = 5
        const val MIN_SPACING = 13
        const val MAX_SPACING = 45
        const val SPACING_STEP = 4
        const val MIN_ZOOM = 100
        const val MAX_ZOOM = 130
        const val ZOOM_STEP = 1

        const val DEFAULT_CATALOG_SIZE = 100
        const val DEFAULT_CATALOG_SPACING = 29
        const val DEFAULT_CATALOG_ZOOM = 111
        const val DEFAULT_APPS_SIZE = 100
        const val DEFAULT_APPS_SPACING = 17
        const val DEFAULT_APPS_ZOOM = 120
    }
}

/**
 * The current launcher settings for composables that are not handed a ViewModel (Home's carousel and
 * rows, folders, Applications, the time pill). Filled by [LauncherSettingsDataStore] as soon as it is
 * created; until then it holds the defaults.
 */
object LauncherSettingsHolder {
    private val _settings = MutableStateFlow(LauncherSettings())
    val settings: StateFlow<LauncherSettings> = _settings.asStateFlow()

    internal fun publish(value: LauncherSettings) {
        _settings.value = value
    }
}

private val Context.launcherSettingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "launcher_settings",
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() }
)

@Singleton
class LauncherSettingsDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.launcherSettingsDataStore

    /** Nuvio Launcher backup: every saved value, and putting a saved set back. */
    internal suspend fun exportPrefs(): org.json.JSONObject = dataStore.data.first().toBackupJson()
    internal suspend fun importPrefs(json: org.json.JSONObject) { dataStore.edit { it.restoreFromBackupJson(json) } }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val rotationKey = intPreferencesKey("rotation_seconds")
    private val heroCountKey = intPreferencesKey("hero_count")
    private val showTimePillKey = booleanPreferencesKey("show_time_pill")
    private val clock24HourKey = booleanPreferencesKey("clock_24_hour")
    private val catalogSizeKey = intPreferencesKey("catalog_tile_size")
    private val catalogSpacingKey = intPreferencesKey("catalog_tile_spacing")
    private val catalogZoomKey = intPreferencesKey("catalog_focus_zoom")
    private val appsSizeKey = intPreferencesKey("apps_tile_size")
    private val appsSpacingKey = intPreferencesKey("apps_tile_spacing")
    private val appsZoomKey = intPreferencesKey("apps_focus_zoom")
    private val hiddenPackagesKey = stringSetPreferencesKey("hidden_packages")
    private val appOrderKey = stringPreferencesKey("app_order")
    private val favouritesKey = stringPreferencesKey("favourite_apps")
    private val solidBackgroundKey = booleanPreferencesKey("solid_background")
    private val backgroundColorKey = intPreferencesKey("background_color")

    val settings: Flow<LauncherSettings> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs ->
            LauncherSettings(
                rotationSeconds = (prefs[rotationKey] ?: LauncherSettings.DEFAULT_ROTATION_SECONDS)
                    .takeIf { it in LauncherSettings.ROTATION_CHOICES } ?: LauncherSettings.DEFAULT_ROTATION_SECONDS,
                heroCount = (prefs[heroCountKey] ?: LauncherSettings.DEFAULT_HERO_COUNT)
                    .coerceIn(LauncherSettings.MIN_HERO_COUNT, LauncherSettings.MAX_HERO_COUNT),
                showTimePill = prefs[showTimePillKey] ?: true,
                clock24Hour = prefs[clock24HourKey],
                catalogTileSizePercent = size(prefs[catalogSizeKey], LauncherSettings.DEFAULT_CATALOG_SIZE),
                catalogTileSpacingDp = spacing(prefs[catalogSpacingKey], LauncherSettings.DEFAULT_CATALOG_SPACING),
                catalogFocusZoomPercent = zoom(prefs[catalogZoomKey], LauncherSettings.DEFAULT_CATALOG_ZOOM),
                appsTileSizePercent = size(prefs[appsSizeKey], LauncherSettings.DEFAULT_APPS_SIZE),
                appsTileSpacingDp = spacing(prefs[appsSpacingKey], LauncherSettings.DEFAULT_APPS_SPACING),
                appsFocusZoomPercent = zoom(prefs[appsZoomKey], LauncherSettings.DEFAULT_APPS_ZOOM),
                hiddenPackages = prefs[hiddenPackagesKey] ?: emptySet(),
                appOrder = prefs[appOrderKey]?.split('\n')?.filter { it.isNotBlank() } ?: emptyList(),
                favourites = prefs[favouritesKey]?.split('\n')?.filter { it.isNotBlank() } ?: emptyList(),
                solidBackground = prefs[solidBackgroundKey] ?: false,
                backgroundColor = prefs[backgroundColorKey] ?: LauncherSettings.DEFAULT_BACKGROUND_COLOR
            )
        }
        .distinctUntilChanged()

    init {
        scope.launch { settings.collect { LauncherSettingsHolder.publish(it) } }
    }

    private fun size(v: Int?, d: Int) = (v ?: d).coerceIn(LauncherSettings.MIN_SIZE, LauncherSettings.MAX_SIZE)
    private fun spacing(v: Int?, d: Int) = (v ?: d).coerceIn(LauncherSettings.MIN_SPACING, LauncherSettings.MAX_SPACING)
    private fun zoom(v: Int?, d: Int) = (v ?: d).coerceIn(LauncherSettings.MIN_ZOOM, LauncherSettings.MAX_ZOOM)

    suspend fun setRotationSeconds(seconds: Int) { dataStore.edit { it[rotationKey] = seconds } }
    suspend fun setHeroCount(count: Int) { dataStore.edit { it[heroCountKey] = count } }
    suspend fun setShowTimePill(show: Boolean) { dataStore.edit { it[showTimePillKey] = show } }
    suspend fun setClock24Hour(use24Hour: Boolean) { dataStore.edit { it[clock24HourKey] = use24Hour } }
    suspend fun setCatalogTileSize(percent: Int) { dataStore.edit { it[catalogSizeKey] = percent } }
    suspend fun setCatalogTileSpacing(dp: Int) { dataStore.edit { it[catalogSpacingKey] = dp } }
    suspend fun setCatalogFocusZoom(percent: Int) { dataStore.edit { it[catalogZoomKey] = percent } }
    suspend fun setAppsTileSize(percent: Int) { dataStore.edit { it[appsSizeKey] = percent } }
    suspend fun setAppsTileSpacing(dp: Int) { dataStore.edit { it[appsSpacingKey] = dp } }
    suspend fun setAppsFocusZoom(percent: Int) { dataStore.edit { it[appsZoomKey] = percent } }
    suspend fun setSolidBackground(solid: Boolean) { dataStore.edit { it[solidBackgroundKey] = solid } }
    suspend fun setBackgroundColor(argb: Int) { dataStore.edit { it[backgroundColorKey] = argb } }

    suspend fun setAppOrder(packages: List<String>) { dataStore.edit { it[appOrderKey] = packages.joinToString("\n") } }

    suspend fun setFavourites(packages: List<String>) { dataStore.edit { it[favouritesKey] = packages.distinct().joinToString("\n") } }

    suspend fun setAppHidden(packageName: String, hidden: Boolean) {
        dataStore.edit { prefs ->
            val current = prefs[hiddenPackagesKey] ?: emptySet()
            prefs[hiddenPackagesKey] = if (hidden) current + packageName else current - packageName
        }
    }

    /** Every launcher setting back to its default and every app shown again. Nuvio's hero catalogs are not touched. */
    suspend fun reset() {
        dataStore.edit { it.clear() }
    }
}

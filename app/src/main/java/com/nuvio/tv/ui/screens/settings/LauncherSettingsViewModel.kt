package com.nuvio.tv.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nuvio.tv.data.local.LauncherSettings
import com.nuvio.tv.data.local.LauncherSettingsDataStore
import com.nuvio.tv.data.local.LauncherSettingsHolder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Settings → Launcher: Nuvio Launcher's own options, kept on this TV. */
@HiltViewModel
class LauncherSettingsViewModel @Inject constructor(
    private val store: LauncherSettingsDataStore,
    private val backupStore: com.nuvio.tv.data.local.LauncherBackup
) : ViewModel() {

    /** Nuvio Launcher backup: when the backup on this TV was made (null: none yet). */
    val lastBackupAt = kotlinx.coroutines.flow.MutableStateFlow(backupStore.lastBackupAt())

    fun backup(onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            val ok = backupStore.backup()
            lastBackupAt.value = backupStore.lastBackupAt()
            onDone(ok)
        }
    }

    /** onDone gets null when there is no backup on this TV, false when it could not be read. */
    fun restore(onDone: (Boolean?) -> Unit) {
        viewModelScope.launch { onDone(backupStore.restore()) }
    }

    val settings: StateFlow<LauncherSettings> = store.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = LauncherSettingsHolder.settings.value
    )

    private fun save(block: suspend (LauncherSettingsDataStore) -> Unit) {
        viewModelScope.launch { block(store) }
    }

    fun setRotationSeconds(seconds: Int) = save { it.setRotationSeconds(seconds) }
    fun setHeroCount(count: Int) = save { it.setHeroCount(count) }
    fun setShowTimePill(show: Boolean) = save { it.setShowTimePill(show) }
    fun setClock24Hour(use24Hour: Boolean) = save { it.setClock24Hour(use24Hour) }
    fun setCatalogTileSize(percent: Int) = save { it.setCatalogTileSize(percent) }
    fun setCatalogTileSpacing(dp: Int) = save { it.setCatalogTileSpacing(dp) }
    fun setCatalogFocusZoom(percent: Int) = save { it.setCatalogFocusZoom(percent) }
    fun setAppsTileSize(percent: Int) = save { it.setAppsTileSize(percent) }
    fun setAppsTileSpacing(dp: Int) = save { it.setAppsTileSpacing(dp) }
    fun setAppsFocusZoom(percent: Int) = save { it.setAppsFocusZoom(percent) }
    fun setAppHidden(packageName: String, hidden: Boolean) = save { it.setAppHidden(packageName, hidden) }
    fun setAppOrder(packages: List<String>) = save { it.setAppOrder(packages) }
    fun setFavourites(packages: List<String>) = save { it.setFavourites(packages) }
    fun setSolidBackground(solid: Boolean) = save { it.setSolidBackground(solid) }
    fun setBackgroundColor(argb: Int) = save { it.setBackgroundColor(argb) }
    fun reset() = save { it.reset() }
}

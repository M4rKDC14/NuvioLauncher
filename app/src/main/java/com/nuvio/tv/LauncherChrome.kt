package com.nuvio.tv

import androidx.compose.runtime.mutableStateOf

/** Nuvio Launcher: small pieces of shared screen state between the shell (sidebar, pill) and its pages. */
object LauncherChrome {
    /** The top-left time pill. The Catalog page turns it off while focus is in its rows. */
    val pillVisible = mutableStateOf(true)

    /** Settings hides the pill while it is open (separate flag, so other pages resetting pillVisible can't undo it). */
    val pillHiddenBySettings = mutableStateOf(false)

    /**
     * The picture of the last title that had focus (Home carousel or rows, a folder, a details page). Settings,
     * Search, Library and Applications draw it blurred behind them, so every page feels like a layer over Home.
     */
    val lastBackdrop = mutableStateOf<String?>(null)

    /** The Inputs popup (remote's Input button, or Inputs in the side panel) is open. */
    val inputsOpen = mutableStateOf(false)

    /** The Applications panel (Applications in the side panel) is open. */
    val appsOpen = mutableStateOf(false)

    /** The Quick settings panel (Quick settings in the side panel) is open. */
    val quickSettingsOpen = mutableStateOf(false)

    /** Counts Home-button presses while the launcher is open; each one sends the shell back to the home screen. */
    val goHomeRequests = mutableStateOf(0)
}

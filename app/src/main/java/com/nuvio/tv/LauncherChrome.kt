package com.nuvio.tv

import androidx.compose.runtime.mutableStateOf

/** Nuvio Launcher: small pieces of shared screen state between the shell (sidebar, pill) and its pages. */
object LauncherChrome {
    /** The top-left time pill. The Catalog page turns it off while focus is in its rows. */
    val pillVisible = mutableStateOf(true)

    /**
     * The picture of the last title that had focus (Home carousel or rows, a folder, a details page). Settings,
     * Search, Library and Applications draw it blurred behind them, so every page feels like a layer over Home.
     */
    val lastBackdrop = mutableStateOf<String?>(null)
}

package com.nuvio.tv

import androidx.compose.runtime.mutableStateOf

/** Nuvio Launcher: small pieces of shared screen state between the shell (sidebar, pill) and its pages. */
object LauncherChrome {
    /** The top-left time pill. The Catalog page turns it off while focus is in its rows. */
    val pillVisible = mutableStateOf(true)
}

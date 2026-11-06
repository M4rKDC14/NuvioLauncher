package com.nuvio.tv.ui.screens.ambient

data class AmbientSlide(
    val id: String,
    val title: String,
    val backdropUrl: String,
    val logoUrl: String?,
    val genres: List<String>,
    /** Nuvio Launcher: skip this picture once loaded if it is taller than wide (Reddit pictures). */
    val landscapeOnly: Boolean = false
)

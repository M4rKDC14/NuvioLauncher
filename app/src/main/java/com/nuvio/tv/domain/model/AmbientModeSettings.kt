package com.nuvio.tv.domain.model

data class AmbientModeSettings(
    val enabled: Boolean = true,
    val timeoutMinutes: Int = DEFAULT_TIMEOUT_MINUTES,
    val catalogKeys: List<String> = emptyList(),
    /** Nuvio Launcher: where the pictures come from (catalogs, or a Reddit community). */
    val source: AmbientSource = AmbientSource.CATALOGS,
    /** Nuvio Launcher: the community name used when [source] is [AmbientSource.CUSTOM], without "r/". */
    val customSubreddit: String = ""
) {
    /** The subreddit this source reads, or null for catalogs (or a custom source with no name yet). */
    val subreddit: String?
        get() = when (source) {
            AmbientSource.CATALOGS -> null
            AmbientSource.WALLPAPER -> "Wallpaper"
            AmbientSource.EARTHPORN -> "EarthPorn"
            AmbientSource.CUSTOM -> customSubreddit.takeIf { it.isNotBlank() }
        }

    companion object {
        const val DEFAULT_TIMEOUT_MINUTES = 5
        const val MAX_CATALOGS = 3
        val TIMEOUT_OPTIONS_MINUTES = listOf(1, 2, 5, 10, 15, 30)

        private val LINK_NAME = Regex("""(?:^|/)r/([A-Za-z0-9_]{2,21})(?:[/?#]|$)""")
        private val BARE_NAME = Regex("""^[A-Za-z0-9_]{2,21}$""")

        /** Reads a community name from a pasted Reddit link, "r/Name" or "Name"; null when there is none. */
        fun parseSubreddit(input: String): String? {
            val text = input.trim()
            LINK_NAME.find(text)?.let { return it.groupValues[1] }
            return text.takeIf { BARE_NAME.matches(it) }
        }
    }
}

/** Nuvio Launcher: Ambient Mode picture sources, in the order the Settings chips show them. */
enum class AmbientSource {
    CATALOGS,
    WALLPAPER,
    EARTHPORN,
    CUSTOM
}

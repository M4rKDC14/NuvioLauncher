package com.nuvio.tv.ui.screens.ambient

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

private const val MAX_REDDIT_SLIDES = 60
private const val USER_AGENT = "android:tv.nuviolauncher:1.0 (personal TV launcher, ambient mode)"

/**
 * Nuvio Launcher: Ambient Mode pictures from a Reddit community.
 *
 * Reads the community's public RSS feed (no sign-in; Reddit's JSON feed refuses most apps without one). Keeps only
 * direct picture links (i.redd.it, i.imgur.com). A post whose title gives its size, as "[3840x2160]" or
 * "(3652x4374)", is dropped when it is taller than wide; the slideshow drops any other portrait picture once loaded.
 */
@Singleton
class RedditSlideSource @Inject constructor(
    private val httpClient: OkHttpClient
) {
    suspend fun load(subreddit: String): List<AmbientSlide> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("https://www.reddit.com/r/$subreddit/hot/.rss?limit=100")
            .header("User-Agent", USER_AGENT)
            .build()
        val body = httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@withContext emptyList()
            response.body?.string().orEmpty()
        }
        ENTRY.findAll(body)
            .mapNotNull { match -> match.groupValues[1].toSlide(subreddit) }
            .distinctBy { it.backdropUrl }
            .take(MAX_REDDIT_SLIDES)
            .toList()
            .shuffled()
    }

    private fun String.toSlide(subreddit: String): AmbientSlide? {
        val id = ID.find(this)?.groupValues?.get(1) ?: return null
        val rawTitle = TITLE.find(this)?.groupValues?.get(1)?.unescape() ?: ""
        val content = CONTENT.find(this)?.groupValues?.get(1)?.unescape() ?: return null
        val url = LINK.find(content)?.groupValues?.get(1)?.unescape() ?: return null
        if (!isDirectPicture(url)) return null
        SIZE.find(rawTitle)?.let { size ->
            val width = size.groupValues[1].toIntOrNull() ?: 0
            val height = size.groupValues[2].toIntOrNull() ?: 0
            if (height > width) return null
        }
        return AmbientSlide(
            id = "reddit:$id",
            title = cleanTitle(rawTitle),
            backdropUrl = url,
            logoUrl = null,
            genres = listOf("r/$subreddit"),
            landscapeOnly = true
        )
    }

    private fun isDirectPicture(url: String): Boolean {
        val lower = url.lowercase()
        val host = lower.removePrefix("https://").removePrefix("http://").substringBefore('/')
        val path = lower.substringBefore('?')
        return (host == "i.redd.it" || host == "i.imgur.com") &&
            (path.endsWith(".jpg") || path.endsWith(".jpeg") || path.endsWith(".png") || path.endsWith(".webp"))
    }

    /** "Misty morning in Washington (3652x4374)(OC)" -> "Misty morning in Washington". */
    private fun cleanTitle(title: String): String =
        title.replace(BRACKETED_NOISE, " ").replace(Regex("""\s+"""), " ").trim().trimEnd('-', ',', '|').trim()

    private fun String.unescape(): String = this
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace("&#32;", " ")
        .replace("&amp;", "&")

    private companion object {
        val ENTRY = Regex("""<entry>([\s\S]*?)</entry>""")
        val ID = Regex("""<id>([^<]+)</id>""")
        val TITLE = Regex("""<title>([\s\S]*?)</title>""")
        val CONTENT = Regex("""<content[^>]*>([\s\S]*?)</content>""")
        val LINK = Regex("""href="([^"]+)">\[link]""")
        val SIZE = Regex("""(\d{3,5})\s*[x×X]\s*(\d{3,5})""")
        val BRACKETED_NOISE = Regex("""[\[(][^\])]*?(\d{3,5}\s*[x×X]\s*\d{3,5}|\bOC\b|\boc\b)[^\])]*?[\])]""")
    }
}

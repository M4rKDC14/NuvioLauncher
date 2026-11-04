package com.nuvio.tv.core.tmdb

import android.content.Context
import com.nuvio.tv.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Nuvio Launcher: the TMDB key. Official Nuvio builds carry their own key; this build doesn't, so the owner types
 * theirs in Settings → TMDB. It's kept on this TV only and sent only to api.themoviedb.org. A v3 API key goes in the
 * query; a v4 Read Access Token (starts "eyJ") is moved into the Authorization header by the TMDB HTTP client.
 */
object TmdbKey {
    private const val PREFS = "nuvio_launcher_tmdb"
    private const val KEY = "api_key"

    @Volatile
    var value: String = BuildConfig.TMDB_API_KEY
        private set

    private val _userKeyFlow = MutableStateFlow("")
    /** The owner's own key ("" when none); synced with the Nuvio account like the phone app's personal TMDB key. */
    val userKeyFlow: StateFlow<String> = _userKeyFlow

    fun init(context: Context) {
        val k = userKey(context)
        _userKeyFlow.value = k
        k.takeIf { it.isNotBlank() }?.let { value = it }
    }

    fun userKey(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "").orEmpty()

    fun save(context: Context, key: String) {
        val k = key.trim()
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, k).apply()
        value = k.ifBlank { BuildConfig.TMDB_API_KEY }
        _userKeyFlow.value = k
    }

    fun isBearer(key: String): Boolean = key.startsWith("eyJ")

    /** null when TMDB accepts [key], otherwise the reason. */
    suspend fun check(key: String): String? = withContext(Dispatchers.IO) {
        val k = key.trim()
        if (k.isEmpty()) return@withContext "Enter a key."
        try {
            val url = "https://api.themoviedb.org/3/configuration" + if (isBearer(k)) "" else "?api_key=" + URLEncoder.encode(k, "UTF-8")
            val c = URL(url).openConnection() as HttpURLConnection
            c.connectTimeout = 10_000; c.readTimeout = 10_000
            if (isBearer(k)) c.setRequestProperty("Authorization", "Bearer $k")
            val code = c.responseCode
            c.disconnect()
            when (code) {
                200 -> null
                401 -> "TMDB says this key isn't valid."
                else -> "TMDB answered $code."
            }
        } catch (e: Exception) {
            "Couldn't reach TMDB: ${e.message ?: e.javaClass.simpleName}"
        }
    }
}

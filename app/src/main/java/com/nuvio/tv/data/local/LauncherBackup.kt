package com.nuvio.tv.data.local

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.nuvio.tv.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Nuvio Launcher: Settings → Launcher → Backup. Saves the launcher's own settings (favourites, app order, hidden
 * apps, tiles, carousel, clock, background) and Ambient Mode's choices to one file, and puts them back.
 *
 * The file is in the app's folder on the TV (/sdcard/Android/data/tv.nuviolauncher/files/), so it survives updates.
 * Android deletes that folder on uninstall or factory reset, so to keep a copy elsewhere it is copied to a PC with
 * adb and pushed back before restoring. Nothing is sent anywhere; account and addon settings are not included.
 */
@Singleton
class LauncherBackup @Inject constructor(
    @ApplicationContext private val context: Context,
    private val launcherStore: LauncherSettingsDataStore,
    private val ambientStore: AmbientModeDataStore
) {
    private val file: File
        get() = File(context.getExternalFilesDir(null) ?: context.filesDir, FILE_NAME)

    /** When the backup on this TV was made, or null if there is none. */
    fun lastBackupAt(): Long? = file.takeIf { it.isFile }?.lastModified()

    suspend fun backup(): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val json = JSONObject()
                .put("format", FORMAT)
                .put("version", 1)
                .put("launcherVersion", BuildConfig.LAUNCHER_VERSION)
                .put("created", System.currentTimeMillis())
                .put("launcher", launcherStore.exportPrefs())
                .put("ambient", ambientStore.exportPrefs())
            val tmp = File(file.parentFile, "$FILE_NAME.tmp")
            tmp.writeText(json.toString(2))
            if (!tmp.renameTo(file)) {
                file.writeText(json.toString(2))
                tmp.delete()
            }
            true
        }.getOrDefault(false)
    }

    /** Puts the saved settings back. Null when there is no backup on this TV; false when the file can't be read. */
    suspend fun restore(): Boolean? = withContext(Dispatchers.IO) {
        if (!file.isFile) return@withContext null
        runCatching {
            val json = JSONObject(file.readText())
            require(json.optString("format") == FORMAT)
            json.optJSONObject("launcher")?.let { launcherStore.importPrefs(it) }
            json.optJSONObject("ambient")?.let { ambientStore.importPrefs(it) }
            true
        }.getOrDefault(false)
    }

    private companion object {
        const val FILE_NAME = "nuvio-launcher-backup.json"
        const val FORMAT = "nuvio-launcher-backup"
    }
}

/** Every stored value with its type, so it can be written back exactly as it was. */
internal fun Preferences.toBackupJson(): JSONObject {
    val out = JSONObject()
    for ((key, value) in asMap()) {
        val entry = when (value) {
            is Boolean -> JSONObject().put("t", "b").put("v", value)
            is Int -> JSONObject().put("t", "i").put("v", value)
            is Long -> JSONObject().put("t", "l").put("v", value)
            is Float -> JSONObject().put("t", "f").put("v", value.toDouble())
            is Double -> JSONObject().put("t", "d").put("v", value)
            is String -> JSONObject().put("t", "s").put("v", value)
            is Set<*> -> JSONObject().put("t", "ss").put("v", JSONArray(value.map { it.toString() }))
            else -> null
        }
        if (entry != null) out.put(key.name, entry)
    }
    return out
}

/** Replaces the stored values with those in [json] (anything not in the backup goes back to its default). */
internal fun MutablePreferences.restoreFromBackupJson(json: JSONObject) {
    clear()
    for (name in json.keys()) {
        val entry = json.optJSONObject(name) ?: continue
        when (entry.optString("t")) {
            "b" -> this[booleanPreferencesKey(name)] = entry.getBoolean("v")
            "i" -> this[intPreferencesKey(name)] = entry.getInt("v")
            "l" -> this[longPreferencesKey(name)] = entry.getLong("v")
            "f" -> this[floatPreferencesKey(name)] = entry.getDouble("v").toFloat()
            "d" -> this[doublePreferencesKey(name)] = entry.getDouble("v")
            "s" -> this[stringPreferencesKey(name)] = entry.getString("v")
            "ss" -> {
                val array = entry.getJSONArray("v")
                this[stringSetPreferencesKey(name)] = (0 until array.length()).map { array.getString(it) }.toSet()
            }
        }
    }
}

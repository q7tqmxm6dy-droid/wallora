package com.wallora.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "wallora_prefs")

/** Persists favourites, preferences, wallpaper history and rotation settings. */
class PreferencesRepository(private val context: Context) {

    private object Keys {
        val FAVORITES = stringSetPreferencesKey("favorites")
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val DARK_MODE_SET = booleanPreferencesKey("dark_mode_set")

        val DAILY_ROTATION = booleanPreferencesKey("daily_rotation")
        val ROTATION_TARGET = stringPreferencesKey("rotation_target")
        val AUTO_THEME = booleanPreferencesKey("auto_theme")
        val CURRENT_WALLPAPER = stringPreferencesKey("current_wallpaper")
        val LAST_DARK = booleanPreferencesKey("last_dark")
        val ROTATION_INDEX = stringPreferencesKey("rotation_index")
        val HISTORY = stringPreferencesKey("history")

        val LIVE_MODE = stringPreferencesKey("live_mode")
        val LIVE_INTERVAL = stringPreferencesKey("live_interval")
        val LIVE_CATEGORIES = stringSetPreferencesKey("live_categories")
        val SETS = stringPreferencesKey("sets_count")
        val LAST_OPEN = stringPreferencesKey("last_open_day")
        val STREAK = stringPreferencesKey("open_streak")
        val OPEN_DAYS = stringPreferencesKey("open_days")
        val COLLECTIONS = stringPreferencesKey("collections_json")
    }

    val favorites: Flow<Set<String>> =
        context.dataStore.data.map { prefs -> prefs[Keys.FAVORITES] ?: emptySet() }

    /** `null` means "follow the system setting". */
    val darkMode: Flow<Boolean?> = context.dataStore.data.map { prefs ->
        if (prefs[Keys.DARK_MODE_SET] == true) prefs[Keys.DARK_MODE] else null
    }

    val dailyRotation: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.DAILY_ROTATION] ?: false }

    /** "HOME", "LOCK" or "BOTH". */
    val rotationTarget: Flow<String> =
        context.dataStore.data.map { it[Keys.ROTATION_TARGET] ?: "HOME" }

    val autoThemeApply: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.AUTO_THEME] ?: false }

    /** Live wallpaper: "slideshow" or "gradient". */
    val liveMode: Flow<String> =
        context.dataStore.data.map { it[Keys.LIVE_MODE] ?: "slideshow" }

    /** Seconds each wallpaper is shown in slideshow mode. */
    val liveInterval: Flow<Int> =
        context.dataStore.data.map { it[Keys.LIVE_INTERVAL]?.toIntOrNull() ?: 10 }

    /** Empty set = every category. */
    val liveCategories: Flow<Set<String>> =
        context.dataStore.data.map { it[Keys.LIVE_CATEGORIES] ?: emptySet() }

    /** How many times a wallpaper has been applied. */
    val sets: Flow<Int> =
        context.dataStore.data.map { it[Keys.SETS]?.toIntOrNull() ?: 0 }

    val streak: Flow<Int> =
        context.dataStore.data.map { it[Keys.STREAK]?.toIntOrNull() ?: 0 }

    val openDays: Flow<Int> =
        context.dataStore.data.map { it[Keys.OPEN_DAYS]?.toIntOrNull() ?: 0 }

    // ---- collections (local albums) ----------------------------------------

    /** name -> wallpaper ids, stored as JSON in DataStore. */
    val collections: Flow<Map<String, List<String>>> =
        context.dataStore.data.map { prefs -> parseCollections(prefs[Keys.COLLECTIONS]) }

    private fun parseCollections(raw: String?): Map<String, List<String>> {
        if (raw.isNullOrBlank()) return emptyMap()
        return runCatching {
            val json = JSONObject(raw)
            buildMap {
                json.keys().forEach { key ->
                    val array = json.optJSONArray(key)
                    val ids = buildList {
                        if (array != null) for (i in 0 until array.length()) add(array.getString(i))
                    }
                    put(key, ids)
                }
            }
        }.getOrDefault(emptyMap())
    }

    private fun encodeCollections(map: Map<String, List<String>>): String {
        val json = JSONObject()
        map.forEach { (name, ids) -> json.put(name, JSONArray(ids)) }
        return json.toString()
    }

    private suspend fun updateCollections(transform: (Map<String, List<String>>) -> Map<String, List<String>>) {
        context.dataStore.edit { prefs ->
            prefs[Keys.COLLECTIONS] = encodeCollections(transform(parseCollections(prefs[Keys.COLLECTIONS])))
        }
    }

    suspend fun createCollection(name: String) =
        updateCollections { it + (name to (it[name] ?: emptyList())) }

    suspend fun deleteCollection(name: String) = updateCollections { it - name }

    suspend fun toggleInCollection(name: String, wallpaperId: String) = updateCollections { current ->
        val ids = current[name] ?: emptyList()
        current + (name to if (wallpaperId in ids) ids - wallpaperId else ids + wallpaperId)
    }

    /** Keeps the consecutive-day streak up to date. Returns the new streak. */
    suspend fun recordDailyOpen(): Int {
        var streak = 1
        context.dataStore.edit { prefs ->
            val today = LocalDate.now()
            val last = prefs[Keys.LAST_OPEN]?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            val current = prefs[Keys.STREAK]?.toIntOrNull() ?: 0
            streak = when {
                last == null -> 1
                last == today -> current.coerceAtLeast(1)
                last == today.minusDays(1) -> current + 1
                else -> 1
            }
            prefs[Keys.STREAK] = streak.toString()
            prefs[Keys.LAST_OPEN] = today.toString()
            if (last != today) {
                prefs[Keys.OPEN_DAYS] = ((prefs[Keys.OPEN_DAYS]?.toIntOrNull() ?: 0) + 1).toString()
            }
        }
        return streak
    }

    val currentWallpaperId: Flow<String?> =
        context.dataStore.data.map { it[Keys.CURRENT_WALLPAPER] }

    /** Most recent first, capped. */
    val history: Flow<List<String>> = context.dataStore.data.map { prefs ->
        prefs[Keys.HISTORY]?.split("|")?.filter { it.isNotBlank() } ?: emptyList()
    }

    suspend fun toggleFavorite(id: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.FAVORITES] ?: emptySet()
            prefs[Keys.FAVORITES] = if (id in current) current - id else current + id
        }
    }

    suspend fun setDarkMode(value: Boolean?) {
        context.dataStore.edit { prefs ->
            if (value == null) {
                prefs.remove(Keys.DARK_MODE)
                prefs[Keys.DARK_MODE_SET] = false
            } else {
                prefs[Keys.DARK_MODE] = value
                prefs[Keys.DARK_MODE_SET] = true
            }
        }
    }

    suspend fun setDailyRotation(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DAILY_ROTATION] = enabled }
    }

    suspend fun setRotationTarget(target: String) {
        context.dataStore.edit { it[Keys.ROTATION_TARGET] = target }
    }

    suspend fun setAutoThemeApply(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_THEME] = enabled }
    }

    suspend fun setLiveMode(mode: String) {
        context.dataStore.edit { it[Keys.LIVE_MODE] = mode }
    }

    suspend fun setLiveInterval(seconds: Int) {
        context.dataStore.edit { it[Keys.LIVE_INTERVAL] = seconds.toString() }
    }

    suspend fun setLiveCategories(categories: Set<String>) {
        context.dataStore.edit { it[Keys.LIVE_CATEGORIES] = categories }
    }

    suspend fun readLiveConfig(): Triple<String, Int, Set<String>> {
        val prefs = context.dataStore.data.first()
        return Triple(
            prefs[Keys.LIVE_MODE] ?: "slideshow",
            prefs[Keys.LIVE_INTERVAL]?.toIntOrNull() ?: 10,
            prefs[Keys.LIVE_CATEGORIES] ?: emptySet(),
        )
    }

    suspend fun recordHistory(id: String, limit: Int = 24) {
        context.dataStore.edit { prefs ->
            val existing = prefs[Keys.HISTORY]?.split("|")?.filter { it.isNotBlank() } ?: emptyList()
            val updated = (listOf(id) + existing.filterNot { it == id }).take(limit)
            prefs[Keys.HISTORY] = updated.joinToString("|")
        }
    }

    suspend fun clearHistory() {
        context.dataStore.edit { it.remove(Keys.HISTORY) }
    }

    // ---- used by the rotation worker / theme applier -------------------------

    suspend fun readRotationConfig(): Triple<Boolean, Int, String> {
        val prefs = context.dataStore.data.first()
        return Triple(
            prefs[Keys.DAILY_ROTATION] ?: false,
            prefs[Keys.ROTATION_INDEX]?.toIntOrNull() ?: 0,
            prefs[Keys.ROTATION_TARGET] ?: "HOME",
        )
    }

    suspend fun writeRotationIndex(index: Int) {
        context.dataStore.edit { it[Keys.ROTATION_INDEX] = index.toString() }
    }

    suspend fun rememberApplied(id: String, dark: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.CURRENT_WALLPAPER] = id
            prefs[Keys.LAST_DARK] = dark
            val current = prefs[Keys.SETS]?.toIntOrNull() ?: 0
            prefs[Keys.SETS] = (current + 1).toString()
        }
    }

    suspend fun readAppliedTheme(): Triple<String?, Boolean?, Boolean> {
        val prefs = context.dataStore.data.first()
        return Triple(
            prefs[Keys.CURRENT_WALLPAPER],
            prefs[Keys.LAST_DARK],
            prefs[Keys.AUTO_THEME] ?: false,
        )
    }
}

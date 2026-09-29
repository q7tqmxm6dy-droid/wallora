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

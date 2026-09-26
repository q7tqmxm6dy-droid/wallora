package com.wallora.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "wallora_prefs")

/** Persists favourites and the dark-mode preference with DataStore. */
class PreferencesRepository(private val context: Context) {

    private object Keys {
        val FAVORITES = stringSetPreferencesKey("favorites")
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val DARK_MODE_SET = booleanPreferencesKey("dark_mode_set")
    }

    val favorites: Flow<Set<String>> =
        context.dataStore.data.map { prefs -> prefs[Keys.FAVORITES] ?: emptySet() }

    /** `null` means "follow the system setting". */
    val darkMode: Flow<Boolean?> = context.dataStore.data.map { prefs ->
        if (prefs[Keys.DARK_MODE_SET] == true) prefs[Keys.DARK_MODE] else null
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
}

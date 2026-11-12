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

/** One saved copy of a wallpaper, kept so the user can find it again. */
data class DownloadRecord(val id: String, val at: Long, val label: String, val width: Int, val height: Int)

/** Persists favourites, preferences, wallpaper history and rotation settings. */
class PreferencesRepository(private val context: Context) {

    private object Keys {
        val FAVORITES = stringSetPreferencesKey("favorites")
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val DARK_MODE_SET = booleanPreferencesKey("dark_mode_set")
        val THEME_CHOSEN = booleanPreferencesKey("theme_chosen")

        val DAILY_ROTATION = booleanPreferencesKey("daily_rotation")
        val ROTATION_TARGET = stringPreferencesKey("rotation_target")
        val AUTO_THEME = booleanPreferencesKey("auto_theme")
        val CURRENT_WALLPAPER = stringPreferencesKey("current_wallpaper")
        val LAST_DARK = booleanPreferencesKey("last_dark")
        val ROTATION_INDEX = stringPreferencesKey("rotation_index")
        val HISTORY = stringPreferencesKey("history")
        val DOWNLOADS = stringPreferencesKey("downloads_json")

        val LIVE_MODE = stringPreferencesKey("live_mode")
        val LIVE_INTERVAL = stringPreferencesKey("live_interval")
        val LIVE_CATEGORIES = stringSetPreferencesKey("live_categories")
        val SETS = stringPreferencesKey("sets_count")
        val LAST_OPEN = stringPreferencesKey("last_open_day")
        val STREAK = stringPreferencesKey("open_streak")
        val OPEN_DAYS = stringPreferencesKey("open_days")
        val COLLECTIONS = stringPreferencesKey("collections_json")
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")

        val INTERSTITIAL_COUNT = stringPreferencesKey("interstitial_count")
        val LAST_INTERSTITIAL = stringPreferencesKey("last_interstitial_ms")

        val NEW_ALERTS = booleanPreferencesKey("new_wallpaper_alerts")
        val KNOWN_WALLPAPERS = stringSetPreferencesKey("known_wallpapers")
        val ALERTS_SEEDED = booleanPreferencesKey("alerts_seeded")
        val IS_PRO = booleanPreferencesKey("is_pro")
        val CURRENT_TARGET = stringPreferencesKey("current_wallpaper_target")

        val ROTATION_FAVORITES = booleanPreferencesKey("rotation_favorites_only")
        val DAILY_DROP_ALERTS = booleanPreferencesKey("daily_drop_alerts")
        val LAST_DROP_DAY = stringPreferencesKey("last_drop_notified_day")
        val REVIEW_ASKED = booleanPreferencesKey("review_asked")
    }

    /**
     * Last known Wallora Pro entitlement. Cached so ads stay off (and Pro features stay on) at
     * startup and offline; [com.wallora.app.billing.ProManager] refreshes it from Google Play.
     */
    val isPro: Flow<Boolean> = context.dataStore.data.map { it[Keys.IS_PRO] ?: false }

    suspend fun isProNow(): Boolean = isPro.first()

    suspend fun setPro(value: Boolean) {
        context.dataStore.edit { it[Keys.IS_PRO] = value }
    }

    val favorites: Flow<Set<String>> =
        context.dataStore.data.map { prefs -> prefs[Keys.FAVORITES] ?: emptySet() }

    /** `null` means "follow the system setting". Until the user picks, Wallora is dark. */
    val darkMode: Flow<Boolean?> = context.dataStore.data.map { prefs ->
        when {
            prefs[Keys.THEME_CHOSEN] != true -> true
            prefs[Keys.DARK_MODE_SET] == true -> prefs[Keys.DARK_MODE]
            else -> null
        }
    }

    val dailyRotation: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.DAILY_ROTATION] ?: false }

    /** "HOME", "LOCK" or "BOTH". */
    val rotationTarget: Flow<String> =
        context.dataStore.data.map { it[Keys.ROTATION_TARGET] ?: "HOME" }

    val autoThemeApply: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.AUTO_THEME] ?: true }

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

    val onboardingDone: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.ONBOARDING_DONE] ?: false }

    suspend fun completeOnboarding() {
        context.dataStore.edit { it[Keys.ONBOARDING_DONE] = true }
    }

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

    // ---- download history ---------------------------------------------------

    /** Most recent first, capped at 300. */
    val downloads: Flow<List<DownloadRecord>> = context.dataStore.data.map { parseDownloads(it[Keys.DOWNLOADS]) }

    private fun parseDownloads(raw: String?): List<DownloadRecord> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            List(array.length()) { i ->
                val o = array.getJSONObject(i)
                DownloadRecord(o.getString("id"), o.getLong("at"), o.optString("label"), o.optInt("w"), o.optInt("h"))
            }
        }.getOrDefault(emptyList())
    }

    suspend fun recordDownload(record: DownloadRecord) {
        context.dataStore.edit { prefs ->
            val updated = (listOf(record) + parseDownloads(prefs[Keys.DOWNLOADS])).take(300)
            prefs[Keys.DOWNLOADS] = JSONArray().also { arr ->
                updated.forEach { r ->
                    arr.put(JSONObject().put("id", r.id).put("at", r.at).put("label", r.label).put("w", r.width).put("h", r.height))
                }
            }.toString()
        }
    }

    suspend fun clearDownloads() {
        context.dataStore.edit { it.remove(Keys.DOWNLOADS) }
    }

    suspend fun toggleFavorite(id: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.FAVORITES] ?: emptySet()
            prefs[Keys.FAVORITES] = if (id in current) current - id else current + id
        }
    }

    suspend fun setDarkMode(value: Boolean?) {
        context.dataStore.edit { prefs ->
            prefs[Keys.THEME_CHOSEN] = true
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

    /** Pro: rotate through the user's favourites instead of the whole catalogue. */
    val rotationFavoritesOnly: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.ROTATION_FAVORITES] ?: false }

    suspend fun setRotationFavoritesOnly(enabled: Boolean) {
        context.dataStore.edit { it[Keys.ROTATION_FAVORITES] = enabled }
    }

    suspend fun setAutoThemeApply(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_THEME] = enabled }
    }

    // ---- interstitial frequency capping ---------------------------------------
    // Persisted rather than held in memory so the cap survives restarts; otherwise force-quitting
    // would reset the counter and every launch would show an ad again.

    suspend fun interstitialCount(): Int =
        context.dataStore.data.first()[Keys.INTERSTITIAL_COUNT]?.toIntOrNull() ?: 0

    suspend fun setInterstitialCount(value: Int) {
        context.dataStore.edit { it[Keys.INTERSTITIAL_COUNT] = value.toString() }
    }

    suspend fun lastInterstitialMs(): Long =
        context.dataStore.data.first()[Keys.LAST_INTERSTITIAL]?.toLongOrNull() ?: 0L

    suspend fun setLastInterstitialMs(value: Long) {
        context.dataStore.edit { it[Keys.LAST_INTERSTITIAL] = value.toString() }
    }

    // ---- new-wallpaper alerts -------------------------------------------------

    /** Defaults to on: this is the feature's opt-in state, the OS still gates it by permission. */
    val newAlerts: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.NEW_ALERTS] ?: true }

    suspend fun newAlertsEnabled(): Boolean = newAlerts.first()

    suspend fun setNewAlerts(enabled: Boolean) {
        context.dataStore.edit { it[Keys.NEW_ALERTS] = enabled }
    }

    // ---- "Today's Drop" morning notification ----------------------------------

    /** Defaults to on, like [newAlerts]; the OS still gates it by the notification permission. */
    val dailyDropAlerts: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.DAILY_DROP_ALERTS] ?: true }

    suspend fun dailyDropAlertsEnabled(): Boolean = dailyDropAlerts.first()

    suspend fun setDailyDropAlerts(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DAILY_DROP_ALERTS] = enabled }
    }

    /** Marks [epochDay] as announced; false when it already was, so a day is never announced twice. */
    suspend fun claimDropDay(epochDay: Long): Boolean {
        var claimed = false
        context.dataStore.edit { prefs ->
            if (prefs[Keys.LAST_DROP_DAY] != epochDay.toString()) {
                prefs[Keys.LAST_DROP_DAY] = epochDay.toString()
                claimed = true
            }
        }
        return claimed
    }

    // ---- Play in-app review -----------------------------------------------------

    suspend fun appliedCount(): Int = sets.first()

    suspend fun reviewAsked(): Boolean = context.dataStore.data.first()[Keys.REVIEW_ASKED] ?: false

    suspend fun setReviewAsked() {
        context.dataStore.edit { it[Keys.REVIEW_ASKED] = true }
    }

    /** Ids we have already told the user about. */
    suspend fun knownWallpaperIds(): Set<String> =
        context.dataStore.data.first()[Keys.KNOWN_WALLPAPERS] ?: emptySet()

    suspend fun alertsSeeded(): Boolean =
        context.dataStore.data.first()[Keys.ALERTS_SEEDED] ?: false

    /** Seeds the baseline silently so a fresh install doesn't announce the whole catalogue. */
    suspend fun seedKnownWallpapers(ids: Set<String>) {
        context.dataStore.edit {
            it[Keys.KNOWN_WALLPAPERS] = ids
            it[Keys.ALERTS_SEEDED] = true
        }
    }

    /**
     * Grows the known set monotonically. Unioning (rather than replacing) means an offline read
     * that only sees the bundled catalogue can never make the cloud wallpapers look "new" later.
     */
    suspend fun rememberKnownWallpapers(ids: Set<String>) {
        context.dataStore.edit { it[Keys.KNOWN_WALLPAPERS] = it[Keys.KNOWN_WALLPAPERS].orEmpty() + ids }
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

    suspend fun readRotationFavoritesOnly(): Boolean = rotationFavoritesOnly.first()

    suspend fun readFavorites(): Set<String> = favorites.first()

    suspend fun writeRotationIndex(index: Int) {
        context.dataStore.edit { it[Keys.ROTATION_INDEX] = index.toString() }
    }

    /** [target] is where the user put it (HOME, LOCK or BOTH), so the day/night switch can use the same place. */
    suspend fun rememberApplied(id: String, dark: Boolean, target: String? = null) {
        context.dataStore.edit { prefs ->
            if (target != null) prefs[Keys.CURRENT_TARGET] = target
            prefs[Keys.CURRENT_WALLPAPER] = id
            prefs[Keys.LAST_DARK] = dark
            val current = prefs[Keys.SETS]?.toIntOrNull() ?: 0
            prefs[Keys.SETS] = (current + 1).toString()
        }
    }

    /** Which version is currently on the phone, without counting it as another wallpaper the user set. */
    suspend fun rememberThemeVariant(dark: Boolean) {
        context.dataStore.edit { it[Keys.LAST_DARK] = dark }
    }

    suspend fun readAppliedTarget(): String? = context.dataStore.data.first()[Keys.CURRENT_TARGET]

    suspend fun readAppliedTheme(): Triple<String?, Boolean?, Boolean> {
        val prefs = context.dataStore.data.first()
        return Triple(
            prefs[Keys.CURRENT_WALLPAPER],
            prefs[Keys.LAST_DARK],
            prefs[Keys.AUTO_THEME] ?: true,
        )
    }
}

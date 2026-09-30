package com.wallora.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.wallora.app.data.AdminRepository
import com.wallora.app.data.Category
import com.wallora.app.data.PreferencesRepository
import com.wallora.app.data.RemoteWallpaperRepository
import com.wallora.app.data.Wallpaper
import com.wallora.app.data.WallpaperCatalog
import com.wallora.app.data.mergeCatalog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class SortOrder(val label: String) {
    DEFAULT("Default"),
    NAME("Name A–Z"),
    CATEGORY("Category"),
}

class WallpaperViewModel(application: Application) : AndroidViewModel(application) {

    private val preferences = PreferencesRepository(application)
    private val remote = RemoteWallpaperRepository()
    private val admin = AdminRepository()

    val favorites: StateFlow<Set<String>> = preferences.favorites
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    val darkMode: StateFlow<Boolean?> = preferences.darkMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Remote wallpapers (Firestore) first, then the bundled ones as offline fallback. */
    val wallpapers: StateFlow<List<Wallpaper>> = remote.wallpapers()
        .map { remoteWallpapers -> mergeCatalog(remoteWallpapers, WallpaperCatalog.wallpapers) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WallpaperCatalog.wallpapers)

    /** Bundled categories, plus any extra categories defined in Firestore. */
    val categories: StateFlow<List<Category>> = remote.categories()
        .map { remoteCategories ->
            val bundledIds = WallpaperCatalog.categories.map { it.id }.toSet()
            WallpaperCatalog.categories + remoteCategories.filter { it.id !in bundledIds }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WallpaperCatalog.categories)

    val isAdminSignedIn: StateFlow<Boolean> = admin.authState()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    // ---- browsing ----------------------------------------------------------

    val searchQuery = MutableStateFlow("")
    val sortOrder = MutableStateFlow(SortOrder.DEFAULT)

    private val historyIds: StateFlow<List<String>> = preferences.history
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val history: StateFlow<List<Wallpaper>> = combine(historyIds, wallpapers) { ids, all ->
        ids.mapNotNull { id -> all.firstOrNull { it.id == id } }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ---- settings ----------------------------------------------------------

    val dailyRotation: StateFlow<Boolean> = preferences.dailyRotation
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val rotationTarget: StateFlow<String> = preferences.rotationTarget
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "HOME")

    val autoThemeApply: StateFlow<Boolean> = preferences.autoThemeApply
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun setDailyRotation(enabled: Boolean) {
        viewModelScope.launch { preferences.setDailyRotation(enabled) }
    }

    fun setRotationTarget(target: String) {
        viewModelScope.launch { preferences.setRotationTarget(target) }
    }

    fun setAutoThemeApply(enabled: Boolean) {
        viewModelScope.launch { preferences.setAutoThemeApply(enabled) }
    }

    val liveMode: StateFlow<String> = preferences.liveMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "slideshow")

    val liveInterval: StateFlow<Int> = preferences.liveInterval
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 10)

    val liveCategories: StateFlow<Set<String>> = preferences.liveCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    /** Number of times a wallpaper has been set. */
    val sets: StateFlow<Int> = preferences.sets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val streak: StateFlow<Int> = preferences.streak
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val openDays: StateFlow<Int> = preferences.openDays
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    // ---- collections & tags ------------------------------------------------

    val collections: StateFlow<Map<String, List<String>>> = preferences.collections
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    fun createCollection(name: String) {
        viewModelScope.launch { preferences.createCollection(name) }
    }

    fun deleteCollection(name: String) {
        viewModelScope.launch { preferences.deleteCollection(name) }
    }

    fun toggleInCollection(name: String, wallpaperId: String) {
        viewModelScope.launch { preferences.toggleInCollection(name, wallpaperId) }
    }

    /** Chosen tag filter (null = any). */
    val selectedTag = MutableStateFlow<String?>(null)

    val tags: StateFlow<List<String>> = wallpapers
        .map { list -> list.flatMap { it.tags }.distinct().sorted() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun wallpaperIdsInCollection(name: String): List<Wallpaper> {
        val ids = collections.value[name] ?: return emptyList()
        return wallpapers.value.filter { it.id in ids }
    }

    init {
        viewModelScope.launch { preferences.recordDailyOpen() }
    }

    /** Deterministic daily pick — same wallpaper for everyone for the whole day. */
    fun wallpaperOfTheDay(): Wallpaper? {
        val list = wallpapers.value
        if (list.isEmpty()) return null
        val epochDay = java.time.LocalDate.now().toEpochDay()
        val index = Math.floorMod(epochDay, list.size.toLong()).toInt()
        return list[index]
    }

    fun setLiveMode(mode: String) {
        viewModelScope.launch { preferences.setLiveMode(mode) }
    }

    fun setLiveInterval(seconds: Int) {
        viewModelScope.launch { preferences.setLiveInterval(seconds) }
    }

    fun toggleLiveCategory(id: String) {
        viewModelScope.launch {
            val current = liveCategories.value
            preferences.setLiveCategories(if (id in current) current - id else current + id)
        }
    }

    fun clearLiveCategories() {
        viewModelScope.launch { preferences.setLiveCategories(emptySet()) }
    }

    fun clearHistory() {
        viewModelScope.launch { preferences.clearHistory() }
    }

    // ---- actions -----------------------------------------------------------

    fun toggleFavorite(id: String) {
        viewModelScope.launch { preferences.toggleFavorite(id) }
    }

    fun setDarkMode(value: Boolean?) {
        viewModelScope.launch { preferences.setDarkMode(value) }
    }

    fun recordViewed(id: String) {
        viewModelScope.launch { preferences.recordHistory(id) }
    }

    fun rememberApplied(id: String, dark: Boolean) {
        viewModelScope.launch { preferences.rememberApplied(id, dark) }
    }

    fun randomWallpaper(): Wallpaper? = wallpapers.value.randomOrNull()

    fun wallpaper(id: String): Wallpaper? =
        wallpapers.value.firstOrNull { it.id == id } ?: WallpaperCatalog.wallpaper(id)

    fun wallpapersInCategory(categoryId: String): List<Wallpaper> =
        wallpapers.value.filter { it.categoryId == categoryId }

    fun categoryTitle(categoryId: String): String =
        categories.value.firstOrNull { it.id == categoryId }?.title ?: categoryId

    fun adminEmail(): String? = admin.currentUserEmail()

    // ---- admin -------------------------------------------------------------

    suspend fun signIn(email: String, password: String): Result<Unit> = admin.signIn(email, password)

    fun signOut() = admin.signOut()

    suspend fun uploadWallpaper(
        title: String,
        category: String,
        bytes: ByteArray,
        darkBytes: ByteArray? = null,
        onProgress: (Float) -> Unit = {},
    ): Result<Unit> = admin.uploadWallpaper(title, category, bytes, darkBytes, onProgress)

    suspend fun deleteWallpaper(id: String): Result<Unit> = admin.deleteWallpaper(id)

    suspend fun addCategory(id: String, title: String): Result<Unit> = admin.addCategory(id, title)

    suspend fun addWallpaperByUrl(
        title: String,
        category: String,
        url: String,
        darkUrl: String? = null,
    ): Result<Unit> = admin.addWallpaperByUrl(title, category, url, darkUrl)
}

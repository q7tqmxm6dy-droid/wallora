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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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
        .map { remoteWallpapers -> remoteWallpapers + WallpaperCatalog.wallpapers }
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

    fun adminEmail(): String? = admin.currentUserEmail()

    fun toggleFavorite(id: String) {
        viewModelScope.launch { preferences.toggleFavorite(id) }
    }

    fun setDarkMode(value: Boolean?) {
        viewModelScope.launch { preferences.setDarkMode(value) }
    }

    fun wallpaper(id: String): Wallpaper? =
        wallpapers.value.firstOrNull { it.id == id } ?: WallpaperCatalog.wallpaper(id)

    fun wallpapersInCategory(categoryId: String): List<Wallpaper> =
        wallpapers.value.filter { it.categoryId == categoryId }

    fun categoryTitle(categoryId: String): String =
        categories.value.firstOrNull { it.id == categoryId }?.title ?: categoryId

    // ---- Admin -------------------------------------------------------------

    suspend fun signIn(email: String, password: String): Result<Unit> = admin.signIn(email, password)

    fun signOut() = admin.signOut()

    suspend fun uploadWallpaper(
        title: String,
        category: String,
        bytes: ByteArray,
        onProgress: (Float) -> Unit,
    ): Result<Unit> = admin.uploadWallpaper(title, category, bytes, onProgress)

    suspend fun deleteWallpaper(id: String): Result<Unit> = admin.deleteWallpaper(id)

    suspend fun addCategory(id: String, title: String): Result<Unit> = admin.addCategory(id, title)
}

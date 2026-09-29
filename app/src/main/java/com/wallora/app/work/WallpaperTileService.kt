package com.wallora.app.work

import android.content.res.Configuration
import android.service.quicksettings.TileService
import com.wallora.app.data.WallpaperCatalog
import com.wallora.app.util.WallpaperImages
import com.wallora.app.util.WallpaperSetter
import com.wallora.app.util.WallpaperTarget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Quick Settings tile: sets a random wallpaper from the bundled catalogue. */
class WallpaperTileService : TileService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onClick() {
        super.onClick()
        val context = applicationContext
        scope.launch {
            val wallpaper = WallpaperCatalog.wallpapers.randomOrNull() ?: return@launch
            val dark = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES
            val bitmap = WallpaperImages.loadBitmap(context, wallpaper, dark) ?: return@launch
            WallpaperSetter.apply(context, bitmap, WallpaperTarget.HOME)
            bitmap.recycle()
        }
    }
}

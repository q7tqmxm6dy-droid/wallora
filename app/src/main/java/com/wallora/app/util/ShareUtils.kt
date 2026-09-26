package com.wallora.app.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.core.content.FileProvider
import java.io.File

/** Shares a wallpaper bitmap through the system share sheet. */
object ShareUtils {

    fun share(context: Context, bitmap: Bitmap, name: String) {
        try {
            val dir = File(context.cacheDir, "shared").apply { mkdirs() }
            val file = File(dir, "$name.jpg")
            file.outputStream().use { out -> bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out) }
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/jpeg"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share wallpaper"))
        } catch (t: Throwable) {
            Analytics.recordError(t)
        }
    }
}

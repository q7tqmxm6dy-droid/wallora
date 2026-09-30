package com.wallora.app.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.wallora.app.MainActivity
import com.wallora.app.data.WallpaperCatalog

/** Home-screen widget showing a random bundled wallpaper. */
class WallpaperWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val wallpaper = WallpaperCatalog.wallpapers.randomOrNull()
        val intent = Intent(context, MainActivity::class.java)
        provideContent { WidgetContent(wallpaper?.resId, intent) }
    }

    @Composable
    private fun WidgetContent(resId: Int?, intent: Intent) {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(Color(0xFF101014)))
                .clickable(actionStartActivity(intent)),
            contentAlignment = Alignment.BottomStart,
        ) {
            if (resId != null) {
                Image(
                    provider = ImageProvider(resId),
                    contentDescription = "Wallpaper",
                    modifier = GlanceModifier.fillMaxSize(),
                )
            }
            Text(
                text = "Wallora",
                style = TextStyle(
                    color = ColorProvider(Color.White),
                    fontWeight = FontWeight.Bold,
                ),
                modifier = GlanceModifier.padding(10.dp),
            )
            Text(
                text = "Shuffle",
                style = TextStyle(
                    color = ColorProvider(Color.White),
                    fontWeight = FontWeight.Bold,
                ),
                modifier = GlanceModifier
                    .padding(10.dp)
                    .clickable(actionRunCallback<ShuffleWallpaperAction>()),
            )
        }
    }
}

/** Tapping the widget shuffles to a different random wallpaper. */
class ShuffleWallpaperAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        WallpaperWidget().update(context, glanceId)
    }
}

class WallpaperWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WallpaperWidget()
}

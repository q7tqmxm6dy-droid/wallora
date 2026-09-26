package com.wallora.app.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wallora.app.R
import com.wallora.app.ui.WallpaperViewModel
import com.wallora.app.ui.components.CategoryChips
import com.wallora.app.ui.components.WallpaperGrid
import com.wallora.app.util.Analytics

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: WallpaperViewModel,
    darkTheme: Boolean,
    onToggleDarkMode: () -> Unit,
    onOpenWallpaper: (String) -> Unit,
    onOpenAdmin: () -> Unit,
) {
    val context = LocalContext.current
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val wallpapers by viewModel.wallpapers.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    var selectedCategory by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Wallora", fontWeight = FontWeight.SemiBold) },
                actions = {
                    IconButton(onClick = onToggleDarkMode) {
                        Icon(
                            painter = painterResource(
                                if (darkTheme) R.drawable.ic_light_mode else R.drawable.ic_dark_mode,
                            ),
                            contentDescription = "Toggle light or dark theme",
                        )
                    }
                    IconButton(onClick = onOpenAdmin) {
                        Icon(
                            painter = painterResource(R.drawable.ic_admin),
                            contentDescription = "Admin",
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        val visible = remember(wallpapers, selectedCategory) {
            selectedCategory?.let { id -> wallpapers.filter { it.categoryId == id } } ?: wallpapers
        }
        WallpaperGrid(
            wallpapers = visible,
            favorites = favorites,
            onOpen = onOpenWallpaper,
            onToggleFavorite = { id ->
                viewModel.toggleFavorite(id)
                Analytics.log(
                    context,
                    Analytics.Events.FAVORITE_TOGGLED,
                    mapOf("id" to id, "favorite" to (id !in favorites)),
                )
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            header = {
                CategoryChips(
                    categories = categories,
                    selected = selectedCategory,
                    onSelect = { category ->
                        selectedCategory = category
                        if (category != null) {
                            Analytics.log(context, Analytics.Events.OPEN_CATEGORY, mapOf("category" to category))
                        }
                    },
                )
            },
        )
    }
}

package com.wallora.app.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wallora.app.ui.WallpaperViewModel
import com.wallora.app.ui.components.WallpaperGrid

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryScreen(
    viewModel: WallpaperViewModel,
    categoryId: String,
    onBack: () -> Unit,
    onOpenWallpaper: (String) -> Unit,
) {
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val wallpapers by viewModel.wallpapers.collectAsStateWithLifecycle()
    val items = remember(wallpapers, categoryId) {
        wallpapers.filter { it.categoryId == categoryId }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(viewModel.categoryTitle(categoryId)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        WallpaperGrid(
            wallpapers = items,
            favorites = favorites,
            onOpen = onOpenWallpaper,
            onToggleFavorite = viewModel::toggleFavorite,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        )
    }
}

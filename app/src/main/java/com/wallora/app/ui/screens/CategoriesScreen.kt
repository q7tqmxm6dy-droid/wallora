package com.wallora.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wallora.app.ui.WallpaperViewModel
import com.wallora.app.ui.components.CategoryCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(
    viewModel: WallpaperViewModel,
    onOpenCategory: (String) -> Unit,
) {
    val wallpapers by viewModel.wallpapers.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Categories") }) },
    ) { innerPadding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            gridItems(items = categories, key = { it.id }) { category ->
                val items = remember(wallpapers, category.id) {
                    wallpapers.filter { it.categoryId == category.id }
                }
                CategoryCard(
                    category = category,
                    cover = items.firstOrNull(),
                    count = items.size,
                    onClick = { onOpenCategory(category.id) },
                )
            }
        }
    }
}

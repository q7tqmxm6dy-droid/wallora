package com.wallora.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wallora.app.R
import com.wallora.app.data.Curation
import com.wallora.app.ui.WallpaperViewModel
import com.wallora.app.ui.components.CategoryTile
import com.wallora.app.ui.components.GlowBackground
import com.wallora.app.ui.components.WallpaperGrid
import java.time.LocalDate

@Composable
fun DiscoverScreen(
    viewModel: WallpaperViewModel,
    dark: Boolean,
    onOpenCategory: (String) -> Unit,
    onOpenWallpaper: (String) -> Unit,
) {
    val wallpapers by viewModel.wallpapers.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val query by viewModel.searchQuery.collectAsStateWithLifecycle()
    var only4k by rememberSaveable { mutableStateOf(false) }
    val day = remember { LocalDate.now().toEpochDay() }

    val results = remember(wallpapers, query, only4k) {
        val q = query.trim()
        wallpapers.filter { w ->
            (!only4k || w.uhd) && (
                q.isEmpty() ||
                    w.title.contains(q, ignoreCase = true) ||
                    viewModel.categoryTitle(w.categoryId).contains(q, ignoreCase = true) ||
                    w.tags.any { it.contains(q, ignoreCase = true) }
                )
        }
    }
    val searching = query.isNotBlank() || only4k

    Box(Modifier.fillMaxSize()) {
        GlowBackground()
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Text(
                stringResource(R.string.nav_discover),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(start = 20.dp, top = 16.dp),
            )
            OutlinedTextField(
                value = query,
                onValueChange = { viewModel.searchQuery.value = it },
                singleLine = true,
                shape = RoundedCornerShape(28.dp),
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                            Icon(Icons.Filled.Clear, contentDescription = stringResource(R.string.clear_search))
                        }
                    }
                },
                placeholder = { Text(stringResource(R.string.discover_search_hint)) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            )
            if (wallpapers.any { it.uhd }) {
                FilterChip(
                    selected = only4k,
                    onClick = { only4k = !only4k },
                    label = { Text(stringResource(R.string.uhd_filter)) },
                    modifier = Modifier.padding(start = 16.dp, bottom = 8.dp),
                )
            }
            if (searching) {
                WallpaperGrid(
                    wallpapers = results,
                    favorites = favorites,
                    dark = dark,
                    onOpen = onOpenWallpaper,
                    onToggleFavorite = viewModel::toggleFavorite,
                    emptyText = stringResource(R.string.no_results),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 160.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            stringResource(R.string.discover_browse),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    items(categories, key = { it.id }) { category ->
                        val inCategory = remember(wallpapers, category.id) { wallpapers.filter { it.categoryId == category.id } }
                        val cover = remember(inCategory) { Curation.trending(inCategory, day, 1).firstOrNull() }
                        CategoryTile(category, cover, inCategory.size, dark, onClick = { onOpenCategory(category.id) })
                    }
                }
            }
        }
    }
}

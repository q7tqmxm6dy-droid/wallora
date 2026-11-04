package com.wallora.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.lazy.items as lazyRowItems
import com.wallora.app.data.WallpaperColors
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
    var color by rememberSaveable { mutableStateOf<String?>(null) }
    var styleId by rememberSaveable { mutableStateOf<String?>(null) }
    val style = Curation.viralStyles.firstOrNull { it.id == styleId }
    val day = remember { LocalDate.now().toEpochDay() }

    val results = remember(wallpapers, query, only4k, color, styleId) {
        val q = query.trim()
        wallpapers.filter { w ->
            (!only4k || w.uhd) && (color == null || color in w.colors) && (style == null || style.matches(w)) && (
                q.isEmpty() ||
                    w.title.contains(q, ignoreCase = true) ||
                    viewModel.categoryTitle(w.categoryId).contains(q, ignoreCase = true) ||
                    w.tags.any { it.contains(q, ignoreCase = true) }
                )
        }
    }
    val searching = query.isNotBlank() || only4k || color != null || style != null

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
            // Colour swatches: tap one to see only wallpapers dominated by it; tap again to clear.
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(bottom = 10.dp),
            ) {
                lazyRowItems(WallpaperColors.all, key = { it.first }) { (name, argb) ->
                    val selected = color == name
                    val label = colorLabel(name)
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(argb))
                            .border(if (selected) 3.dp else 1.dp, if (selected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.25f), CircleShape)
                            .clickable { color = if (selected) null else name }
                            .semantics { contentDescription = label },
                    )
                }
            }
            // Style chips: the same viral looks as the Trending tab.
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 8.dp),
            ) {
                if (wallpapers.any { it.uhd }) {
                    item(key = "4k") {
                        FilterChip(selected = only4k, onClick = { only4k = !only4k }, label = { Text(stringResource(R.string.uhd_filter)) })
                    }
                }
                lazyRowItems(Curation.viralStyles, key = { it.id }) { s ->
                    FilterChip(
                        selected = styleId == s.id,
                        onClick = { styleId = if (styleId == s.id) null else s.id },
                        label = { Text(s.emoji + " " + s.title) },
                    )
                }
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

@Composable
private fun colorLabel(name: String): String = stringResource(
    when (name) {
        "red" -> R.string.color_red; "orange" -> R.string.color_orange; "yellow" -> R.string.color_yellow
        "green" -> R.string.color_green; "teal" -> R.string.color_teal; "blue" -> R.string.color_blue
        "purple" -> R.string.color_purple; "pink" -> R.string.color_pink; "brown" -> R.string.color_brown
        "black" -> R.string.color_black; "gray" -> R.string.color_gray; else -> R.string.color_white
    },
)

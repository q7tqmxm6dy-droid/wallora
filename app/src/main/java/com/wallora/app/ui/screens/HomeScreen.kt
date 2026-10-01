package com.wallora.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.wallora.app.R
import com.wallora.app.data.Wallpaper
import com.wallora.app.ui.SortOrder
import com.wallora.app.ui.WallpaperViewModel
import com.wallora.app.ui.components.CategoryChips
import com.wallora.app.ui.components.WallpaperGrid
import com.wallora.app.util.Analytics

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: WallpaperViewModel,
    darkTheme: Boolean,
    darkModePreference: Boolean?,
    onSetDarkMode: (Boolean?) -> Unit,
    onOpenWallpaper: (String) -> Unit,
    onOpenAdmin: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val context = LocalContext.current
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val wallpapers by viewModel.wallpapers.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val query by viewModel.searchQuery.collectAsStateWithLifecycle()
    val sort by viewModel.sortOrder.collectAsStateWithLifecycle()
    val tags by viewModel.tags.collectAsStateWithLifecycle()
    val selectedTag by viewModel.selectedTag.collectAsStateWithLifecycle()

    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var showThemeMenu by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Wallora", fontWeight = FontWeight.SemiBold) },
                actions = {
                    Box {
                        IconButton(onClick = { showThemeMenu = true }) {
                            Icon(
                                painter = painterResource(
                                    when (darkModePreference) {
                                        null -> R.drawable.ic_theme_auto
                                        true -> R.drawable.ic_dark_mode
                                        false -> R.drawable.ic_light_mode
                                    },
                                ),
                                contentDescription = "Theme: light, dark or auto",
                            )
                        }
                        DropdownMenu(showThemeMenu, onDismissRequest = { showThemeMenu = false }) {
                            ThemeOption("Auto (follow system)", R.drawable.ic_theme_auto, darkModePreference == null) {
                                onSetDarkMode(null); showThemeMenu = false
                            }
                            ThemeOption("Light", R.drawable.ic_light_mode, darkModePreference == false) {
                                onSetDarkMode(false); showThemeMenu = false
                            }
                            ThemeOption("Dark", R.drawable.ic_dark_mode, darkModePreference == true) {
                                onSetDarkMode(true); showThemeMenu = false
                            }
                        }
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                    IconButton(onClick = onOpenAdmin) {
                        Icon(painterResource(R.drawable.ic_admin), contentDescription = "Admin")
                    }
                },
            )
        },
    ) { innerPadding ->
        val visible = remember(wallpapers, selectedCategory, query, sort, selectedTag) {
            wallpapers.asSequence()
                .filter { selectedCategory == null || it.categoryId == selectedCategory }
                .filter { w -> selectedTag == null || selectedTag in w.tags }
                .filter { w ->
                    query.isBlank() ||
                        w.title.contains(query, ignoreCase = true) ||
                        w.categoryId.contains(query, ignoreCase = true)
                }
                .let { seq ->
                    when (sort) {
                        SortOrder.NAME -> seq.sortedBy { it.title.lowercase() }
                        SortOrder.CATEGORY -> seq.sortedWith(
                            compareBy({ it.categoryId }, { it.title.lowercase() }),
                        )
                        SortOrder.DEFAULT -> seq
                    }
                }
                .toList()
        }

        WallpaperGrid(
            wallpapers = visible,
            favorites = favorites,
            dark = darkTheme,
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
            emptyText = stringResource(R.string.no_results),
            header = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = query,
                            onValueChange = { viewModel.searchQuery.value = it },
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                            trailingIcon = {
                                if (query.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                                        Icon(Icons.Filled.Clear, contentDescription = stringResource(R.string.clear_search))
                                    }
                                }
                            },
                            placeholder = { Text(stringResource(R.string.search_hint)) },
                            modifier = Modifier.weight(1f),
                        )
                        Box {
                            IconButton(onClick = { showSortMenu = true }) {
                                Icon(Icons.AutoMirrored.Filled.List, contentDescription = stringResource(R.string.sort))
                            }
                            DropdownMenu(showSortMenu, onDismissRequest = { showSortMenu = false }) {
                                SortOrder.entries.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(stringResource(option.labelRes)) },
                                        trailingIcon = {
                                            if (option == sort) Icon(Icons.Filled.Check, contentDescription = null)
                                        },
                                        onClick = {
                                            viewModel.sortOrder.value = option
                                            showSortMenu = false
                                        },
                                    )
                                }
                            }
                        }
                        IconButton(onClick = {
                            viewModel.randomWallpaper()?.let { onOpenWallpaper(it.id) }
                        }) {
                            Icon(painterResource(R.drawable.ic_shuffle), contentDescription = stringResource(R.string.surprise_me))
                        }
                    }

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

                    if (tags.isNotEmpty()) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item {
                                FilterChip(
                                    selected = selectedTag == null,
                                    onClick = { viewModel.selectedTag.value = null },
                                    label = { Text(stringResource(R.string.any_tag)) },
                                )
                            }
                            items(tags) { tag ->
                                FilterChip(
                                    selected = selectedTag == tag,
                                    onClick = {
                                        viewModel.selectedTag.value = if (selectedTag == tag) null else tag
                                    },
                                    // Leading LRM keeps the "#" attached to the tag in RTL locales,
                                    // otherwise bidi reorders it to the far side ("cute#" instead of "#cute").
                                    label = { Text("\u200E#$tag") },
                                )
                            }
                        }
                    }

                    if (query.isBlank() && history.isNotEmpty()) {
                        RecentRow(history = history, dark = darkTheme, onOpen = onOpenWallpaper)
                    }
                }
            },
            contentPadding = PaddingValues(12.dp),
        )
    }
}

@Composable
private fun RecentRow(
    history: List<Wallpaper>,
    dark: Boolean,
    onOpen: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = stringResource(R.string.recently_viewed),
            style = androidx.compose.material3.MaterialTheme.typography.labelLarge,
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(history.size) { index ->
                val wallpaper = history[index]
                AsyncImage(
                    model = wallpaper.model(dark),
                    contentDescription = wallpaper.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(width = 62.dp, height = 110.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onOpen(wallpaper.id) },
                )
            }
        }
    }
}

@Composable
private fun ThemeOption(
    label: String,
    iconRes: Int,
    selected: Boolean,
    onClick: () -> Unit,
) {
    DropdownMenuItem(
        text = { Text(label) },
        leadingIcon = { Icon(painterResource(iconRes), contentDescription = null) },
        trailingIcon = { if (selected) Icon(Icons.Filled.Check, contentDescription = "Selected") },
        onClick = onClick,
    )
}

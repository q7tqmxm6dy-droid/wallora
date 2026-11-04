package com.wallora.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed as staggeredItemsIndexed
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wallora.app.R
import com.wallora.app.data.Curation
import com.wallora.app.ui.WallpaperViewModel
import com.wallora.app.ui.components.GlowBackground
import com.wallora.app.ui.components.RankedCard
import com.wallora.app.ui.components.SectionHeader
import com.wallora.app.ui.components.WallpaperCard
import com.wallora.app.ui.components.appear
import java.time.LocalDate

@Composable
fun TrendingScreen(
    viewModel: WallpaperViewModel,
    dark: Boolean,
    onOpenWallpaper: (String) -> Unit,
) {
    val wallpapers by viewModel.wallpapers.collectAsStateWithLifecycle()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val downloads by viewModel.downloads.collectAsStateWithLifecycle()
    val day = remember { LocalDate.now().toEpochDay() }
    val topTen = remember(wallpapers) { Curation.trending(wallpapers, day, 10) }
    val styles = Curation.viralStyles
    val topDownloads = remember(wallpapers, downloads) {
        val byId = wallpapers.associateBy { it.id }
        downloads.groupingBy { it.id }.eachCount().entries.sortedByDescending { it.value }.mapNotNull { byId[it.key] }.take(10)
    }
    var selected by rememberSaveable { mutableIntStateOf(0) }
    val style = styles[selected.coerceIn(0, styles.lastIndex)]
    val inStyle = remember(wallpapers, selected) {
        wallpapers.filter(style.matches).sortedByDescending { Curation.trendScore(it, day) }
    }

    Box(Modifier.fillMaxSize()) {
        GlowBackground()
        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Adaptive(minSize = 150.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalItemSpacing = 12.dp,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(span = StaggeredGridItemSpan.FullLine) {
                Column(Modifier.fillMaxWidth().statusBarsPadding().padding(top = 16.dp)) {
                    Text(
                        stringResource(R.string.nav_trending),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                    )
                    Text(
                        stringResource(R.string.trending_sub, Curation.TREND_WINDOW_DAYS),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            item(span = StaggeredGridItemSpan.FullLine) {
                Column {
                SectionHeader(stringResource(R.string.top_ten))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    itemsIndexed(topTen, key = { _, w -> w.id }) { i, w ->
                        RankedCard(i + 1, w, dark, onClick = { onOpenWallpaper(w.id) })
                    }
                }
            }
            }
            if (topDownloads.isNotEmpty()) item(span = StaggeredGridItemSpan.FullLine) {
                Column {
                    SectionHeader(stringResource(R.string.your_top_downloads))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        itemsIndexed(topDownloads, key = { _, w -> "dl-" + w.id }) { i, w ->
                            RankedCard(i + 1, w, dark, onClick = { onOpenWallpaper(w.id) })
                        }
                    }
                }
            }
            item(span = StaggeredGridItemSpan.FullLine) {
                Column {
                SectionHeader(stringResource(R.string.viral_styles))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    itemsIndexed(styles, key = { _, s -> s.id }) { i, s ->
                        FilterChip(
                            selected = i == selected,
                            onClick = { selected = i },
                            label = { Text(s.emoji + " " + s.title) },
                        )
                    }
                }
            }
            }
            item(span = StaggeredGridItemSpan.FullLine) {
                SectionHeader(
                    style.emoji + " " + style.title,
                    subtitle = pluralStringResource(R.plurals.wallpaper_count, inStyle.size, inStyle.size),
                )
            }
            staggeredItemsIndexed(inStyle, key = { _, w -> "style-" + w.id }) { index, w ->
                WallpaperCard(
                    wallpaper = w,
                    isFavorite = w.id in favorites,
                    dark = dark,
                    onOpen = { onOpenWallpaper(w.id) },
                    onToggleFavorite = { viewModel.toggleFavorite(w.id) },
                    aspect = Curation.cardRatio(w.id),
                    modifier = Modifier.animateItem().appear(index),
                )
            }
        }
    }
}

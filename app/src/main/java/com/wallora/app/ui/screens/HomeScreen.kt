package com.wallora.app.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wallora.app.R
import com.wallora.app.data.Curation
import com.wallora.app.data.Taxonomy
import com.wallora.app.ui.WallpaperViewModel
import com.wallora.app.ui.components.GlowBackground
import com.wallora.app.ui.components.HeroCard
import com.wallora.app.ui.components.PosterCard
import com.wallora.app.ui.components.SectionHeader
import com.wallora.app.ui.components.WallpaperCard
import com.wallora.app.ui.components.appear
import com.wallora.app.ui.theme.Pink
import com.wallora.app.ui.theme.TealDark
import com.wallora.app.ui.theme.VioletDark
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.absoluteValue

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    viewModel: WallpaperViewModel,
    darkTheme: Boolean,
    onOpenWallpaper: (String) -> Unit,
    onOpenCategory: (String) -> Unit,
    onOpenTrending: () -> Unit,
    onOpenDiscover: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenAdmin: () -> Unit,
) {
    val wallpapers by viewModel.wallpapers.collectAsStateWithLifecycle()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()

    val today = remember { LocalDate.now() }
    val day = today.toEpochDay()
    val featured = remember(wallpapers) { Curation.featured(wallpapers, day, 6) }
    val trending = remember(wallpapers) { Curation.trending(wallpapers, day, 12) }
    val drop = remember(wallpapers) { Curation.dailyDrop(wallpapers, day, 8) }
    val feed = remember(wallpapers) { Curation.feed(wallpapers, day) }
    val dropDate = remember(today) { today.format(DateTimeFormatter.ofPattern("EEE, MMM d")) }

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
                Row(
                    modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        // The publisher's admin area is not advertised to ordinary users: long-press the title.
                        Text(
                            "Wallora",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Black,
                                brush = Brush.linearGradient(listOf(VioletDark, Pink, TealDark)),
                            ),
                            modifier = Modifier.combinedClickable(onClick = {}, onLongClick = onOpenAdmin),
                        )
                        Text(
                            stringResource(R.string.home_tagline),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = onOpenDiscover) {
                        Icon(Icons.Filled.Search, contentDescription = stringResource(R.string.nav_discover))
                    }
                    IconButton(onClick = onOpenProfile) {
                        Icon(Icons.Filled.Person, contentDescription = stringResource(R.string.nav_profile))
                    }
                }
            }

            if (featured.isNotEmpty()) item(span = StaggeredGridItemSpan.FullLine) {
                HeroCarousel(featured, darkTheme, viewModel, onOpenWallpaper)
            }

            item(span = StaggeredGridItemSpan.FullLine) {
                Column {
                SectionHeader(
                    stringResource(R.string.home_trending_now),
                    action = stringResource(R.string.see_all),
                    onAction = onOpenTrending,
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(trending, key = { it.id }) { w ->
                        PosterCard(w, darkTheme, 138.dp, onClick = { onOpenWallpaper(w.id) })
                    }
                }
            }
            }

            item(span = StaggeredGridItemSpan.FullLine) {
                Column {
                SectionHeader(stringResource(R.string.home_browse))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(categories, key = { it.id }) { c ->
                        FilterChip(
                            selected = false,
                            onClick = { onOpenCategory(c.id) },
                            label = { Text(listOf(c.emoji, c.title).filter { it.isNotBlank() }.joinToString(" ")) },
                        )
                    }
                }
            }
            }

            if (drop.isNotEmpty()) item(span = StaggeredGridItemSpan.FullLine) {
                Column {
                SectionHeader(
                    stringResource(R.string.todays_drop),
                    subtitle = stringResource(R.string.todays_drop_sub, dropDate),
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(drop, key = { it.id }) { w ->
                        PosterCard(w, darkTheme, 176.dp, onClick = { onOpenWallpaper(w.id) }, aspect = 9f / 15f)
                    }
                }
            }
            }

            item(span = StaggeredGridItemSpan.FullLine) {
                SectionHeader(stringResource(R.string.for_you), subtitle = stringResource(R.string.for_you_sub))
            }
            itemsIndexed(feed, key = { _, w -> "feed-" + w.id }) { index, w ->
                WallpaperCard(
                    wallpaper = w,
                    isFavorite = w.id in favorites,
                    dark = darkTheme,
                    onOpen = { onOpenWallpaper(w.id) },
                    onToggleFavorite = { viewModel.toggleFavorite(w.id) },
                    aspect = Curation.cardRatio(w.id),
                    modifier = Modifier.animateItem().appear(index),
                )
            }
        }
    }
}

@Composable
private fun HeroCarousel(
    featured: List<com.wallora.app.data.Wallpaper>,
    dark: Boolean,
    viewModel: WallpaperViewModel,
    onOpen: (String) -> Unit,
) {
    val pager = rememberPagerState(pageCount = { featured.size })
    LaunchedEffect(featured.size) {
        while (true) {
            delay(4500)
            if (!pager.isScrollInProgress) pager.animateScrollToPage((pager.currentPage + 1) % featured.size)
        }
    }
    HorizontalPager(
        state = pager,
        pageSpacing = 14.dp,
        modifier = Modifier.fillMaxWidth().height(470.dp).padding(top = 14.dp),
    ) { page ->
        val w = featured[page]
        HeroCard(
            wallpaper = w,
            categoryLabel = viewModel.categoryTitle(w.categoryId),
            dark = dark,
            onClick = { onOpen(w.id) },
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val offset = ((pager.currentPage - page) + pager.currentPageOffsetFraction).absoluteValue.coerceIn(0f, 1f)
                    val s = lerp(0.9f, 1f, 1f - offset)
                    scaleX = s; scaleY = s
                    alpha = lerp(0.55f, 1f, 1f - offset)
                },
        )
    }
}

package com.wallora.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.wallora.app.ui.WallpaperViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: WallpaperViewModel,
    darkTheme: Boolean,
    darkModePreference: Boolean?,
    onOpenWallpaper: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAdmin: () -> Unit,
    onOpenCollections: () -> Unit,
) {
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val sets by viewModel.sets.collectAsStateWithLifecycle()
    val streak by viewModel.streak.collectAsStateWithLifecycle()
    val openDays by viewModel.openDays.collectAsStateWithLifecycle()
    val wallpapers by viewModel.wallpapers.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val dailyRotation by viewModel.dailyRotation.collectAsStateWithLifecycle()
    val liveMode by viewModel.liveMode.collectAsStateWithLifecycle()
    val autoTheme by viewModel.autoThemeApply.collectAsStateWithLifecycle()
    val signedIn by viewModel.isAdminSignedIn.collectAsStateWithLifecycle()
    val collections by viewModel.collections.collectAsStateWithLifecycle()

    val email = remember(signedIn) { viewModel.adminEmail() }
    val favouriteWallpapers = remember(favorites, wallpapers) {
        wallpapers.filter { it.id in favorites }
    }
    val displayName = email ?: "Wallora Guest"
    val initial = (email?.trim()?.firstOrNull() ?: 'W').uppercaseChar().toString()
    val dailyPick = remember(wallpapers) { viewModel.wallpaperOfTheDay() }
    val badges = listOf(
        "First favourite" to (favorites.size >= 1),
        "Collector" to (favorites.size >= 10),
        "Explorer" to (history.size >= 10),
        "Decorator" to (sets >= 5),
        "On a roll" to (streak >= 3),
        "Devoted" to (streak >= 7),
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile") },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
        ) {
            // ---- header ----------------------------------------------------
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(212.dp)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.tertiary,
                            ),
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.28f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = initial,
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "${favorites.size} favourites · ${wallpapers.size} wallpapers",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.85f),
                    )
                    if (streak > 0) {
                        Text(
                            text = "$streak-day streak · $openDays days opened",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.95f),
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            }

            // ---- stats -----------------------------------------------------
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                StatCard("Favourites", favorites.size.toString(), Modifier.weight(1f))
                StatCard("Viewed", history.size.toString(), Modifier.weight(1f))
                StatCard("Applied", sets.toString(), Modifier.weight(1f))
            }

            // ---- favourites strip ------------------------------------------
            if (favouriteWallpapers.isNotEmpty()) {
                Text(
                    text = "Your favourites",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 8.dp),
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(favouriteWallpapers, key = { it.id }) { wallpaper ->
                        AsyncImage(
                            model = wallpaper.model(darkTheme),
                            contentDescription = wallpaper.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(width = 62.dp, height = 110.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onOpenWallpaper(wallpaper.id) },
                        )
                    }
                }
            }

            // ---- wallpaper of the day --------------------------------------
            if (dailyPick != null) {
                Text(
                    text = "Wallpaper of the day",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 8.dp),
                )
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clickable { onOpenWallpaper(dailyPick.id) },
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(
                            model = dailyPick.model(darkTheme),
                            contentDescription = dailyPick.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(width = 76.dp, height = 116.dp),
                        )
                        Column(Modifier.padding(14.dp)) {
                            Text(
                                text = dailyPick.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = dailyPick.categoryId,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = "Tap to set it",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 6.dp),
                            )
                        }
                    }
                }
            }

            // ---- badges ----------------------------------------------------
            Text(
                text = "Badges",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 8.dp),
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(horizontal = 16.dp),
            ) {
                badges.forEach { (label, earned) ->
                    FilterChip(
                        selected = earned,
                        onClick = { },
                        label = { Text(if (earned) "★ $label" else label) },
                    )
                }
            }

            HorizontalDivider(Modifier.padding(vertical = 20.dp))

            // ---- preference summaries --------------------------------------
            InfoRow(
                label = "Collections",
                value = if (collections.isEmpty()) "None yet" else "${collections.size} collections",
                onClick = onOpenCollections,
            )
            InfoRow(
                label = "Theme",
                value = when (darkModePreference) {
                    null -> "Auto (follow system)"
                    true -> "Dark"
                    false -> "Light"
                },
                onClick = onOpenSettings,
            )
            InfoRow(
                label = "Daily rotation",
                value = if (dailyRotation) "On" else "Off",
                onClick = onOpenSettings,
            )
            InfoRow(
                label = "Live wallpaper",
                value = if (liveMode == "gradient") "Animated gradient" else "Slideshow",
                onClick = onOpenSettings,
            )
            InfoRow(
                label = "Match phone theme",
                value = if (autoTheme) "On" else "Off",
                onClick = onOpenSettings,
            )
            InfoRow(
                label = "Admin",
                value = if (signedIn) (email ?: "Signed in") else "Not signed in",
                onClick = onOpenAdmin,
            )

            Text(
                text = "Wallora · ${categories.size} categories · ${wallpapers.size} wallpapers",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.semantics(mergeDescendants = true) {},
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.titleSmall)
            Text(
                value,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

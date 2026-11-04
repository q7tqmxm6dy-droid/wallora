package com.wallora.app.ui

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.wallora.app.R
import com.wallora.app.ads.AdBanner
import com.wallora.app.ui.screens.AdminScreen
import com.wallora.app.ui.screens.CreateScreen
import com.wallora.app.ui.screens.DiscoverScreen
import com.wallora.app.ui.screens.DownloadsScreen
import com.wallora.app.ui.screens.RoadmapScreen
import com.wallora.app.ui.screens.TrendingScreen
import com.wallora.app.ui.screens.CollectionScreen
import com.wallora.app.ui.screens.CollectionsScreen
import com.wallora.app.ui.screens.CategoryScreen
import com.wallora.app.ui.screens.DetailScreen
import com.wallora.app.ui.screens.FavoritesScreen
import com.wallora.app.ui.screens.HomeScreen
import com.wallora.app.ui.screens.ProScreen
import com.wallora.app.ui.screens.ProfileScreen
import com.wallora.app.ui.screens.SettingsScreen

private object Routes {
    const val HOME = "home"
    const val DISCOVER = "discover"
    const val TRENDING = "trending"
    const val CREATE = "create"
    const val FAVORITES = "favorites"
    const val CATEGORY = "category/{categoryId}"
    const val DETAIL = "detail/{wallpaperId}"
    const val ADMIN = "admin"
    const val SETTINGS = "settings"
    const val PROFILE = "profile"
    const val PRO = "pro"
    const val DOWNLOADS = "downloads"
    const val ROADMAP = "roadmap"
    const val COLLECTIONS = "collections"
    const val COLLECTION = "collection/{collectionName}"

    fun collection(name: String) = "collection/${android.net.Uri.encode(name)}"

    fun category(id: String) = "category/$id"
    fun detail(id: String) = "detail/$id"
}

@Composable
fun WalloraApp(
    viewModel: WallpaperViewModel,
    darkTheme: Boolean,
    darkModePreference: Boolean?,
    onSetDarkMode: (Boolean?) -> Unit,
    startRoute: String? = null,
    onStartRouteHandled: () -> Unit = {},
) {
    val navController = rememberNavController()

    // Launcher shortcuts (long-press the app icon) land on a specific screen.
    LaunchedEffect(startRoute) {
        when (startRoute) {
            null -> Unit
            "random" -> viewModel.randomWallpaper()?.let {
                navController.navigate(Routes.detail(it.id))
            }
            "favorites" -> navController.navigate(Routes.FAVORITES) { launchSingleTop = true }
            "collections" -> navController.navigate(Routes.COLLECTIONS) { launchSingleTop = true }
            "profile" -> navController.navigate(Routes.PROFILE) { launchSingleTop = true }
            "settings" -> navController.navigate(Routes.SETTINGS) { launchSingleTop = true }
            "pro" -> navController.navigate(Routes.PRO) { launchSingleTop = true }
        }
        if (startRoute != null) onStartRouteHandled()
    }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute == Routes.HOME ||
        currentRoute == Routes.DISCOVER ||
        currentRoute == Routes.TRENDING ||
        currentRoute == Routes.CREATE ||
        currentRoute == Routes.FAVORITES

    val navigateToTopLevel: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        // Each screen's own Scaffold/TopAppBar already pads for the status bar; letting this outer
        // Scaffold do it as well pushed every title down by a full extra status-bar height.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            // The banner stays composed on every route and is only hidden off the tab screens, so
            // opening a wallpaper and coming back reuses the same ad instead of requesting a new
            // one. It sits above the tab bar so it never covers content.
            Column {
                AdBanner(visible = showBottomBar)
                if (showBottomBar) {
                    NavigationBar {
                        NavigationBarItem(
                            selected = currentRoute == Routes.HOME,
                            onClick = { navigateToTopLevel(Routes.HOME) },
                            icon = { Icon(Icons.Filled.Home, contentDescription = null) },
                            label = { Text(stringResource(R.string.nav_home)) },
                        )
                        NavigationBarItem(
                            selected = currentRoute == Routes.DISCOVER,
                            onClick = { navigateToTopLevel(Routes.DISCOVER) },
                            icon = { Icon(Icons.Filled.Search, contentDescription = null) },
                            label = { Text(stringResource(R.string.nav_discover)) },
                        )
                        NavigationBarItem(
                            selected = currentRoute == Routes.CREATE,
                            onClick = { navigateToTopLevel(Routes.CREATE) },
                            icon = { Icon(painterResource(R.drawable.ic_create), contentDescription = null) },
                            label = { Text(stringResource(R.string.nav_create)) },
                        )
                        NavigationBarItem(
                            selected = currentRoute == Routes.TRENDING,
                            onClick = { navigateToTopLevel(Routes.TRENDING) },
                            icon = { Icon(painterResource(R.drawable.ic_trending), contentDescription = null) },
                            label = { Text(stringResource(R.string.nav_trending)) },
                        )
                        NavigationBarItem(
                            selected = currentRoute == Routes.FAVORITES,
                            onClick = { navigateToTopLevel(Routes.FAVORITES) },
                            icon = { Icon(Icons.Filled.Favorite, contentDescription = null) },
                            label = { Text(stringResource(R.string.nav_favorites)) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            enterTransition = { fadeIn(tween(260)) + slideInHorizontally(tween(260)) { it / 14 } },
            exitTransition = { fadeOut(tween(160)) },
            popEnterTransition = { fadeIn(tween(240)) },
            popExitTransition = { fadeOut(tween(200)) + slideOutHorizontally(tween(200)) { it / 14 } },
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding),
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    viewModel = viewModel,
                    darkTheme = darkTheme,
                    onOpenWallpaper = { navController.navigate(Routes.detail(it)) },
                    onOpenCategory = { navController.navigate(Routes.category(it)) },
                    onOpenTrending = { navigateToTopLevel(Routes.TRENDING) },
                    onOpenDiscover = { navigateToTopLevel(Routes.DISCOVER) },
                    onOpenProfile = { navController.navigate(Routes.PROFILE) },
                    onOpenAdmin = { navController.navigate(Routes.ADMIN) },
                )
            }
            composable(Routes.DISCOVER) {
                DiscoverScreen(
                    viewModel = viewModel,
                    dark = darkTheme,
                    onOpenCategory = { navController.navigate(Routes.category(it)) },
                    onOpenWallpaper = { navController.navigate(Routes.detail(it)) },
                )
            }
            composable(Routes.CREATE) {
                CreateScreen(
                    viewModel = viewModel,
                    onOpenPro = { navController.navigate(Routes.PRO) },
                    onOpenRoadmap = { navController.navigate(Routes.ROADMAP) },
                )
            }
            composable(Routes.TRENDING) {
                TrendingScreen(
                    viewModel = viewModel,
                    dark = darkTheme,
                    onOpenWallpaper = { navController.navigate(Routes.detail(it)) },
                )
            }
            composable(Routes.FAVORITES) {
                FavoritesScreen(
                    viewModel = viewModel,
                    dark = darkTheme,
                    onOpenWallpaper = { navController.navigate(Routes.detail(it)) },
                )
            }
            composable(Routes.COLLECTIONS) {
                CollectionsScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onOpenCollection = { navController.navigate(Routes.collection(it)) },
                )
            }
            composable(
                route = Routes.COLLECTION,
                arguments = listOf(navArgument("collectionName") { type = NavType.StringType }),
            ) { entry ->
                CollectionScreen(
                    viewModel = viewModel,
                    name = android.net.Uri.decode(entry.arguments?.getString("collectionName").orEmpty()),
                    dark = darkTheme,
                    onBack = { navController.popBackStack() },
                    onOpenWallpaper = { navController.navigate(Routes.detail(it)) },
                )
            }
            composable(Routes.PROFILE) {
                ProfileScreen(
                    viewModel = viewModel,
                    darkTheme = darkTheme,
                    darkModePreference = darkModePreference,
                    onOpenWallpaper = { navController.navigate(Routes.detail(it)) },
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                    onBack = { navController.popBackStack() },
                    onOpenAdmin = { navController.navigate(Routes.ADMIN) },
                    onOpenCollections = { navController.navigate(Routes.COLLECTIONS) },
                    onOpenPro = { navController.navigate(Routes.PRO) },
                    onOpenDownloads = { navController.navigate(Routes.DOWNLOADS) },
                    onOpenRoadmap = { navController.navigate(Routes.ROADMAP) },
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    viewModel = viewModel,
                    darkModePreference = darkModePreference,
                    onSetDarkMode = onSetDarkMode,
                    onOpenPro = { navController.navigate(Routes.PRO) },
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.DOWNLOADS) {
                DownloadsScreen(
                    viewModel = viewModel,
                    dark = darkTheme,
                    onBack = { navController.popBackStack() },
                    onOpenWallpaper = { navController.navigate(Routes.detail(it)) },
                )
            }
            composable(Routes.ROADMAP) { RoadmapScreen(onBack = { navController.popBackStack() }) }
            composable(Routes.PRO) {
                ProScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.ADMIN) {
                AdminScreen(
                    viewModel = viewModel,
                    dark = darkTheme,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(
                route = Routes.CATEGORY,
                arguments = listOf(navArgument("categoryId") { type = NavType.StringType }),
            ) { entry ->
                CategoryScreen(
                    viewModel = viewModel,
                    categoryId = entry.arguments?.getString("categoryId").orEmpty(),
                    dark = darkTheme,
                    onBack = { navController.popBackStack() },
                    onOpenWallpaper = { navController.navigate(Routes.detail(it)) },
                )
            }
            composable(
                route = Routes.DETAIL,
                arguments = listOf(navArgument("wallpaperId") { type = NavType.StringType }),
            ) { entry ->
                DetailScreen(
                    wallpaperId = entry.arguments?.getString("wallpaperId").orEmpty(),
                    viewModel = viewModel,
                    dark = darkTheme,
                    onOpenPro = { navController.navigate(Routes.PRO) },
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}

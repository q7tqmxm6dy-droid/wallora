package com.wallora.app.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.wallora.app.R
import com.wallora.app.ui.screens.AdminScreen
import com.wallora.app.ui.screens.CategoriesScreen
import com.wallora.app.ui.screens.CategoryScreen
import com.wallora.app.ui.screens.DetailScreen
import com.wallora.app.ui.screens.FavoritesScreen
import com.wallora.app.ui.screens.HomeScreen

private object Routes {
    const val HOME = "home"
    const val CATEGORIES = "categories"
    const val FAVORITES = "favorites"
    const val CATEGORY = "category/{categoryId}"
    const val DETAIL = "detail/{wallpaperId}"
    const val ADMIN = "admin"

    fun category(id: String) = "category/$id"
    fun detail(id: String) = "detail/$id"
}

@Composable
fun WalloraApp(
    viewModel: WallpaperViewModel,
    darkTheme: Boolean,
    onToggleDarkMode: () -> Unit,
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute == Routes.HOME ||
        currentRoute == Routes.CATEGORIES ||
        currentRoute == Routes.FAVORITES

    val navigateToTopLevel: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentRoute == Routes.HOME,
                        onClick = { navigateToTopLevel(Routes.HOME) },
                        icon = { Icon(Icons.Filled.Home, contentDescription = null) },
                        label = { Text("Home") },
                    )
                    NavigationBarItem(
                        selected = currentRoute == Routes.CATEGORIES,
                        onClick = { navigateToTopLevel(Routes.CATEGORIES) },
                        icon = { Icon(painterResource(R.drawable.ic_category), contentDescription = null) },
                        label = { Text("Categories") },
                    )
                    NavigationBarItem(
                        selected = currentRoute == Routes.FAVORITES,
                        onClick = { navigateToTopLevel(Routes.FAVORITES) },
                        icon = { Icon(Icons.Filled.Favorite, contentDescription = null) },
                        label = { Text("Favorites") },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    viewModel = viewModel,
                    darkTheme = darkTheme,
                    onToggleDarkMode = onToggleDarkMode,
                    onOpenWallpaper = { navController.navigate(Routes.detail(it)) },
                    onOpenAdmin = { navController.navigate(Routes.ADMIN) },
                )
            }
            composable(Routes.CATEGORIES) {
                CategoriesScreen(
                    viewModel = viewModel,
                    onOpenCategory = { navController.navigate(Routes.category(it)) },
                )
            }
            composable(Routes.FAVORITES) {
                FavoritesScreen(
                    viewModel = viewModel,
                    onOpenWallpaper = { navController.navigate(Routes.detail(it)) },
                )
            }
            composable(Routes.ADMIN) {
                AdminScreen(
                    viewModel = viewModel,
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
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}

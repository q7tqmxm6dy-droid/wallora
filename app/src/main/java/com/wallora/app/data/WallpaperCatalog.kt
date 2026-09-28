package com.wallora.app.data

import com.wallora.app.R

/**
 * The bundled wallpaper catalogue.
 *
 * Every wallpaper ships with two images so the app can match the phone's theme:
 * `wp_XX_light` (used in light mode) and `wp_XX_dark` (used in dark mode).
 * Files live in `res/drawable-nodpi/` and are original, code-generated abstract art.
 */
object WallpaperCatalog {

    val categories: List<Category> = listOf(
        Category("aurora", "Aurora"),
        Category("gradient", "Gradients"),
        Category("waves", "Waves"),
        Category("geometric", "Geometric"),
        Category("minimal", "Minimal"),
        Category("neon", "Neon"),
        Category("cars", "Cars"),
        Category("moto", "Moto"),
    )

    val wallpapers: List<Wallpaper> = listOf(
        // Aurora
        Wallpaper("aurora_01", "Polar Glow", "aurora", R.drawable.wp_01_light, darkResId = R.drawable.wp_01_dark),
        Wallpaper("aurora_02", "Northern Sky", "aurora", R.drawable.wp_02_light, darkResId = R.drawable.wp_02_dark),
        Wallpaper("aurora_03", "Aurora Veil", "aurora", R.drawable.wp_03_light, darkResId = R.drawable.wp_03_dark),
        Wallpaper("aurora_04", "Emerald Drift", "aurora", R.drawable.wp_04_light, darkResId = R.drawable.wp_04_dark),
        // Gradients
        Wallpaper("gradient_01", "Sunset Fade", "gradient", R.drawable.wp_05_light, darkResId = R.drawable.wp_05_dark),
        Wallpaper("gradient_02", "Ocean Blend", "gradient", R.drawable.wp_06_light, darkResId = R.drawable.wp_06_dark),
        Wallpaper("gradient_03", "Golden Hour", "gradient", R.drawable.wp_07_light, darkResId = R.drawable.wp_07_dark),
        Wallpaper("gradient_04", "Violet Dusk", "gradient", R.drawable.wp_08_light, darkResId = R.drawable.wp_08_dark),
        // Waves
        Wallpaper("waves_01", "Tidal Lines", "waves", R.drawable.wp_09_light, darkResId = R.drawable.wp_09_dark),
        Wallpaper("waves_02", "Silk Waves", "waves", R.drawable.wp_10_light, darkResId = R.drawable.wp_10_dark),
        Wallpaper("waves_03", "Ripples", "waves", R.drawable.wp_11_light, darkResId = R.drawable.wp_11_dark),
        Wallpaper("waves_04", "Currents", "waves", R.drawable.wp_12_light, darkResId = R.drawable.wp_12_dark),
        // Geometric
        Wallpaper("geometric_01", "Low Poly", "geometric", R.drawable.wp_13_light, darkResId = R.drawable.wp_13_dark),
        Wallpaper("geometric_02", "Prism", "geometric", R.drawable.wp_14_light, darkResId = R.drawable.wp_14_dark),
        Wallpaper("geometric_03", "Facets", "geometric", R.drawable.wp_15_light, darkResId = R.drawable.wp_15_dark),
        Wallpaper("geometric_04", "Origami", "geometric", R.drawable.wp_16_light, darkResId = R.drawable.wp_16_dark),
        // Minimal
        Wallpaper("minimal_01", "Soft Circle", "minimal", R.drawable.wp_17_light, darkResId = R.drawable.wp_17_dark),
        Wallpaper("minimal_02", "Aqua Ring", "minimal", R.drawable.wp_18_light, darkResId = R.drawable.wp_18_dark),
        Wallpaper("minimal_03", "Pastel Arch", "minimal", R.drawable.wp_19_light, darkResId = R.drawable.wp_19_dark),
        Wallpaper("minimal_04", "Golden Bars", "minimal", R.drawable.wp_20_light, darkResId = R.drawable.wp_20_dark),
        // Neon
        Wallpaper("neon_01", "Neon Dreams", "neon", R.drawable.wp_21_light, darkResId = R.drawable.wp_21_dark),
        Wallpaper("neon_02", "Retro Glow", "neon", R.drawable.wp_22_light, darkResId = R.drawable.wp_22_dark),
        Wallpaper("neon_03", "Matrix Pulse", "neon", R.drawable.wp_23_light, darkResId = R.drawable.wp_23_dark),
        Wallpaper("neon_04", "Laser Arc", "neon", R.drawable.wp_24_light, darkResId = R.drawable.wp_24_dark),
        // Cars
        Wallpaper("cars_01", "Sunset Drive", "cars", R.drawable.cars_01_light, darkResId = R.drawable.cars_01_dark),
        Wallpaper("cars_02", "Night Runner", "cars", R.drawable.cars_02_light, darkResId = R.drawable.cars_02_dark),
        Wallpaper("cars_03", "Tire Tracks", "cars", R.drawable.cars_03_light, darkResId = R.drawable.cars_03_dark),
        Wallpaper("cars_04", "Highway Lines", "cars", R.drawable.cars_04_light, darkResId = R.drawable.cars_04_dark),
        // Moto
        Wallpaper("moto_01", "Night Rider", "moto", R.drawable.moto_01_light, darkResId = R.drawable.moto_01_dark),
        Wallpaper("moto_02", "Cafe Racer", "moto", R.drawable.moto_02_light, darkResId = R.drawable.moto_02_dark),
        Wallpaper("moto_03", "Full Face", "moto", R.drawable.moto_03_light, darkResId = R.drawable.moto_03_dark),
        Wallpaper("moto_04", "Redline", "moto", R.drawable.moto_04_light, darkResId = R.drawable.moto_04_dark),
    )

    private val byId: Map<String, Wallpaper> = wallpapers.associateBy { it.id }

    fun wallpaper(id: String): Wallpaper? = byId[id]

    fun category(id: String): Category? = categories.firstOrNull { it.id == id }

    fun inCategory(id: String): List<Wallpaper> =
        wallpapers.filter { it.categoryId == id }
}

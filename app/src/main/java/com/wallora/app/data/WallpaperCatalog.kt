package com.wallora.app.data

import com.wallora.app.R

/**
 * The bundled wallpaper catalogue. Images live in `res/drawable-nodpi/wp_XX.jpg`
 * and are original, code-generated abstract art (no third-party images).
 */
object WallpaperCatalog {

    val categories: List<Category> = listOf(
        Category("aurora", "Aurora"),
        Category("gradient", "Gradients"),
        Category("waves", "Waves"),
        Category("geometric", "Geometric"),
        Category("minimal", "Minimal"),
        Category("neon", "Neon"),
    )

    val wallpapers: List<Wallpaper> = listOf(
        // Aurora
        Wallpaper("aurora_01", "Polar Glow", "aurora", R.drawable.wp_01),
        Wallpaper("aurora_02", "Northern Sky", "aurora", R.drawable.wp_02),
        Wallpaper("aurora_03", "Aurora Veil", "aurora", R.drawable.wp_03),
        Wallpaper("aurora_04", "Emerald Drift", "aurora", R.drawable.wp_04),
        // Gradients
        Wallpaper("gradient_01", "Sunset Fade", "gradient", R.drawable.wp_05),
        Wallpaper("gradient_02", "Ocean Blend", "gradient", R.drawable.wp_06),
        Wallpaper("gradient_03", "Golden Hour", "gradient", R.drawable.wp_07),
        Wallpaper("gradient_04", "Violet Dusk", "gradient", R.drawable.wp_08),
        // Waves
        Wallpaper("waves_01", "Tidal Lines", "waves", R.drawable.wp_09),
        Wallpaper("waves_02", "Silk Waves", "waves", R.drawable.wp_10),
        Wallpaper("waves_03", "Ripples", "waves", R.drawable.wp_11),
        Wallpaper("waves_04", "Currents", "waves", R.drawable.wp_12),
        // Geometric
        Wallpaper("geometric_01", "Low Poly", "geometric", R.drawable.wp_13),
        Wallpaper("geometric_02", "Prism", "geometric", R.drawable.wp_14),
        Wallpaper("geometric_03", "Facets", "geometric", R.drawable.wp_15),
        Wallpaper("geometric_04", "Origami", "geometric", R.drawable.wp_16),
        // Minimal
        Wallpaper("minimal_01", "Soft Circle", "minimal", R.drawable.wp_17),
        Wallpaper("minimal_02", "Aqua Ring", "minimal", R.drawable.wp_18),
        Wallpaper("minimal_03", "Pastel Arch", "minimal", R.drawable.wp_19),
        Wallpaper("minimal_04", "Golden Bars", "minimal", R.drawable.wp_20),
        // Neon
        Wallpaper("neon_01", "Neon Dreams", "neon", R.drawable.wp_21),
        Wallpaper("neon_02", "Retro Glow", "neon", R.drawable.wp_22),
        Wallpaper("neon_03", "Matrix Pulse", "neon", R.drawable.wp_23),
        Wallpaper("neon_04", "Laser Arc", "neon", R.drawable.wp_24),
    )

    private val byId: Map<String, Wallpaper> = wallpapers.associateBy { it.id }
    private val byCategory: Map<String, List<Wallpaper>> = wallpapers.groupBy { it.categoryId }

    fun wallpaper(id: String): Wallpaper? = byId[id]

    fun category(id: String): Category? = categories.firstOrNull { it.id == id }

    fun inCategory(id: String): List<Wallpaper> = byCategory[id].orEmpty()
}

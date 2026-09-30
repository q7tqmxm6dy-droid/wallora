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
        Category("posters", "Posters"),
        Category("animals", "Animals"),
        Category("space", "Space"),
        Category("food", "Food"),
        Category("patterns", "Patterns"),
        Category("seasonal", "Seasonal"),
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
        // Cars
        Wallpaper("cars_01", "Sunset Drive", "cars", R.drawable.cars_01),
        Wallpaper("cars_02", "Night Runner", "cars", R.drawable.cars_02),
        Wallpaper("cars_03", "Tire Tracks", "cars", R.drawable.cars_03),
        Wallpaper("cars_04", "Highway Lines", "cars", R.drawable.cars_04),
        // Cars — flat-colour posters
        Wallpaper("cars_p01", "Scarlet Coupe", "cars", R.drawable.cars_p01),
        Wallpaper("cars_p02", "Azure Coupe", "cars", R.drawable.cars_p02),
        Wallpaper("cars_p03", "Amber Coupe", "cars", R.drawable.cars_p03),
        Wallpaper("cars_p04", "Emerald Coupe", "cars", R.drawable.cars_p04),
        Wallpaper("cars_p05", "Violet Coupe", "cars", R.drawable.cars_p05),
        Wallpaper("cars_p06", "Graphite Coupe", "cars", R.drawable.cars_p06),
        // Moto — flat-colour posters
        // Posters — colourful flat subjects
        Wallpaper("posters_p01", "Summit", "posters", R.drawable.posters_p01),
        Wallpaper("posters_p02", "Tropic", "posters", R.drawable.posters_p02),
        Wallpaper("posters_p03", "Lift Off", "posters", R.drawable.posters_p03),
        Wallpaper("posters_p04", "Skyline", "posters", R.drawable.posters_p04),
        Wallpaper("posters_p05", "Set Sail", "posters", R.drawable.posters_p05),
        Wallpaper("posters_p06", "Ride", "posters", R.drawable.posters_p06),
        Wallpaper("posters_p07", "Brew", "posters", R.drawable.posters_p07),
        Wallpaper("posters_p08", "Beats", "posters", R.drawable.posters_p08),
        Wallpaper("posters_p09", "Shutter", "posters", R.drawable.posters_p09),
        Wallpaper("posters_p10", "Player One", "posters", R.drawable.posters_p10),
        Wallpaper("posters_p11", "Voltage", "posters", R.drawable.posters_p11),
        Wallpaper("posters_p12", "Orbit", "posters", R.drawable.posters_p12),
        Wallpaper("posters_p13", "Whiskers", "posters", R.drawable.posters_p13),
        Wallpaper("posters_p14", "Desert", "posters", R.drawable.posters_p14),
        Wallpaper("posters_p15", "Rainy", "posters", R.drawable.posters_p15),
        Wallpaper("posters_p16", "Reef", "posters", R.drawable.posters_p16),
        Wallpaper("posters_p17", "Kicks", "posters", R.drawable.posters_p17),
        Wallpaper("posters_p18", "Riff", "posters", R.drawable.posters_p18),
        Wallpaper("posters_p19", "Scoop", "posters", R.drawable.posters_p19),
        Wallpaper("posters_p20", "Harbor", "posters", R.drawable.posters_p20),
        Wallpaper("posters_p21", "Idea", "posters", R.drawable.posters_p21),
        Wallpaper("posters_p22", "Bloom", "posters", R.drawable.posters_p22),
        Wallpaper("posters_p23", "Starlight", "posters", R.drawable.posters_p23),
        Wallpaper("posters_p24", "Love", "posters", R.drawable.posters_p24),
        Wallpaper("animals_01", "Brown Bear", "animals", R.drawable.animals_01),
        Wallpaper("animals_02", "Early Bird", "animals", R.drawable.animals_02),
        Wallpaper("animals_03", "Blue Whale", "animals", R.drawable.animals_03),
        Wallpaper("animals_04", "Butterfly", "animals", R.drawable.animals_04),
        Wallpaper("space_01", "Saturn", "space", R.drawable.space_01),
        Wallpaper("space_02", "Comet", "space", R.drawable.space_02),
        Wallpaper("space_03", "Astronaut", "space", R.drawable.space_03),
        Wallpaper("space_04", "Galaxy", "space", R.drawable.space_04),
        Wallpaper("food_01", "Pizza", "food", R.drawable.food_01),
        Wallpaper("food_02", "Donut", "food", R.drawable.food_02),
        Wallpaper("food_03", "Avocado", "food", R.drawable.food_03),
        Wallpaper("food_04", "Cherries", "food", R.drawable.food_04),
        Wallpaper("patterns_01", "Stripes", "patterns", R.drawable.patterns_01),
        Wallpaper("patterns_02", "Dots", "patterns", R.drawable.patterns_02),
        Wallpaper("patterns_03", "Checks", "patterns", R.drawable.patterns_03),
        Wallpaper("patterns_04", "Diamonds", "patterns", R.drawable.patterns_04),
        Wallpaper("seasonal_01", "First Frost", "seasonal", R.drawable.seasonal_01),
        Wallpaper("seasonal_02", "Falling Leaves", "seasonal", R.drawable.seasonal_02),
        Wallpaper("seasonal_03", "High Summer", "seasonal", R.drawable.seasonal_03),
        Wallpaper("seasonal_04", "Spring Bloom", "seasonal", R.drawable.seasonal_04),
    )

    private val byId: Map<String, Wallpaper> = wallpapers.associateBy { it.id }

    fun wallpaper(id: String): Wallpaper? = byId[id]

    fun category(id: String): Category? = categories.firstOrNull { it.id == id }

    fun inCategory(id: String): List<Wallpaper> =
        wallpapers.filter { it.categoryId == id }
}

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
        Category("city", "City"),
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
        // Cars — flat-colour posters
        Wallpaper("cars_p01", "Scarlet Coupe", "cars", R.drawable.cars_p01_light, darkResId = R.drawable.cars_p01_dark),
        Wallpaper("cars_p02", "Azure Coupe", "cars", R.drawable.cars_p02_light, darkResId = R.drawable.cars_p02_dark),
        Wallpaper("cars_p03", "Amber Coupe", "cars", R.drawable.cars_p03_light, darkResId = R.drawable.cars_p03_dark),
        Wallpaper("cars_p04", "Emerald Coupe", "cars", R.drawable.cars_p04_light, darkResId = R.drawable.cars_p04_dark),
        Wallpaper("cars_p05", "Violet Coupe", "cars", R.drawable.cars_p05_light, darkResId = R.drawable.cars_p05_dark),
        Wallpaper("cars_p06", "Graphite Coupe", "cars", R.drawable.cars_p06_light, darkResId = R.drawable.cars_p06_dark),
        // Moto — flat-colour posters
        // Posters — colourful flat subjects
        Wallpaper("posters_p01", "Summit", "posters", R.drawable.posters_p01_light, darkResId = R.drawable.posters_p01_dark),
        Wallpaper("posters_p02", "Tropic", "posters", R.drawable.posters_p02_light, darkResId = R.drawable.posters_p02_dark),
        Wallpaper("posters_p03", "Lift Off", "posters", R.drawable.posters_p03_light, darkResId = R.drawable.posters_p03_dark),
        Wallpaper("posters_p04", "Skyline", "posters", R.drawable.posters_p04_light, darkResId = R.drawable.posters_p04_dark),
        Wallpaper("posters_p05", "Set Sail", "posters", R.drawable.posters_p05_light, darkResId = R.drawable.posters_p05_dark),
        Wallpaper("posters_p06", "Ride", "posters", R.drawable.posters_p06_light, darkResId = R.drawable.posters_p06_dark),
        Wallpaper("posters_p07", "Brew", "posters", R.drawable.posters_p07_light, darkResId = R.drawable.posters_p07_dark),
        Wallpaper("posters_p08", "Beats", "posters", R.drawable.posters_p08_light, darkResId = R.drawable.posters_p08_dark),
        Wallpaper("posters_p09", "Shutter", "posters", R.drawable.posters_p09_light, darkResId = R.drawable.posters_p09_dark),
        Wallpaper("posters_p10", "Player One", "posters", R.drawable.posters_p10_light, darkResId = R.drawable.posters_p10_dark),
        Wallpaper("posters_p11", "Voltage", "posters", R.drawable.posters_p11_light, darkResId = R.drawable.posters_p11_dark),
        Wallpaper("posters_p12", "Orbit", "posters", R.drawable.posters_p12_light, darkResId = R.drawable.posters_p12_dark),
        Wallpaper("posters_p13", "Whiskers", "posters", R.drawable.posters_p13_light, darkResId = R.drawable.posters_p13_dark),
        Wallpaper("posters_p14", "Desert", "posters", R.drawable.posters_p14_light, darkResId = R.drawable.posters_p14_dark),
        Wallpaper("posters_p15", "Rainy", "posters", R.drawable.posters_p15_light, darkResId = R.drawable.posters_p15_dark),
        Wallpaper("posters_p16", "Reef", "posters", R.drawable.posters_p16_light, darkResId = R.drawable.posters_p16_dark),
        Wallpaper("posters_p17", "Kicks", "posters", R.drawable.posters_p17_light, darkResId = R.drawable.posters_p17_dark),
        Wallpaper("posters_p18", "Riff", "posters", R.drawable.posters_p18_light, darkResId = R.drawable.posters_p18_dark),
        Wallpaper("posters_p19", "Scoop", "posters", R.drawable.posters_p19_light, darkResId = R.drawable.posters_p19_dark),
        Wallpaper("posters_p20", "Harbor", "posters", R.drawable.posters_p20_light, darkResId = R.drawable.posters_p20_dark),
        Wallpaper("posters_p21", "Idea", "posters", R.drawable.posters_p21_light, darkResId = R.drawable.posters_p21_dark),
        Wallpaper("posters_p22", "Bloom", "posters", R.drawable.posters_p22_light, darkResId = R.drawable.posters_p22_dark),
        Wallpaper("posters_p23", "Starlight", "posters", R.drawable.posters_p23_light, darkResId = R.drawable.posters_p23_dark),
        Wallpaper("posters_p24", "Love", "posters", R.drawable.posters_p24_light, darkResId = R.drawable.posters_p24_dark),
        Wallpaper("animals_01", "Brown Bear", "animals", R.drawable.animals_01_light, darkResId = R.drawable.animals_01_dark),
        Wallpaper("animals_02", "Early Bird", "animals", R.drawable.animals_02_light, darkResId = R.drawable.animals_02_dark),
        Wallpaper("animals_03", "Blue Whale", "animals", R.drawable.animals_03_light, darkResId = R.drawable.animals_03_dark),
        Wallpaper("animals_04", "Butterfly", "animals", R.drawable.animals_04_light, darkResId = R.drawable.animals_04_dark),
        Wallpaper("space_01", "Saturn", "space", R.drawable.space_01_light, darkResId = R.drawable.space_01_dark),
        Wallpaper("space_02", "Comet", "space", R.drawable.space_02_light, darkResId = R.drawable.space_02_dark),
        Wallpaper("space_03", "Astronaut", "space", R.drawable.space_03_light, darkResId = R.drawable.space_03_dark),
        Wallpaper("space_04", "Galaxy", "space", R.drawable.space_04_light, darkResId = R.drawable.space_04_dark),
        Wallpaper("food_01", "Pizza", "food", R.drawable.food_01_light, darkResId = R.drawable.food_01_dark),
        Wallpaper("food_02", "Donut", "food", R.drawable.food_02_light, darkResId = R.drawable.food_02_dark),
        Wallpaper("food_03", "Avocado", "food", R.drawable.food_03_light, darkResId = R.drawable.food_03_dark),
        Wallpaper("food_04", "Cherries", "food", R.drawable.food_04_light, darkResId = R.drawable.food_04_dark),
        Wallpaper("patterns_01", "Stripes", "patterns", R.drawable.patterns_01_light, darkResId = R.drawable.patterns_01_dark),
        Wallpaper("patterns_02", "Dots", "patterns", R.drawable.patterns_02_light, darkResId = R.drawable.patterns_02_dark),
        Wallpaper("patterns_03", "Checks", "patterns", R.drawable.patterns_03_light, darkResId = R.drawable.patterns_03_dark),
        Wallpaper("patterns_04", "Diamonds", "patterns", R.drawable.patterns_04_light, darkResId = R.drawable.patterns_04_dark),
        Wallpaper("seasonal_01", "First Frost", "seasonal", R.drawable.seasonal_01_light, darkResId = R.drawable.seasonal_01_dark),
        Wallpaper("seasonal_02", "Falling Leaves", "seasonal", R.drawable.seasonal_02_light, darkResId = R.drawable.seasonal_02_dark),
        Wallpaper("seasonal_03", "High Summer", "seasonal", R.drawable.seasonal_03_light, darkResId = R.drawable.seasonal_03_dark),
        Wallpaper("seasonal_04", "Spring Bloom", "seasonal", R.drawable.seasonal_04_light, darkResId = R.drawable.seasonal_04_dark),
        // City — photographic renders (single image, no theme variant)
        Wallpaper("city_01", "Neo-Kyoto", "city", R.drawable.city_01),
        Wallpaper("city_02", "Lunar Spire", "city", R.drawable.city_02),
    )

    private val byId: Map<String, Wallpaper> = wallpapers.associateBy { it.id }

    fun wallpaper(id: String): Wallpaper? = byId[id]

    fun category(id: String): Category? = categories.firstOrNull { it.id == id }

    fun inCategory(id: String): List<Wallpaper> =
        wallpapers.filter { it.categoryId == id }
}

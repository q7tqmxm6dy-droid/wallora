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

    val categories: List<Category> = Taxonomy.categories

    /** Entries below keep their original category ids; [Taxonomy] files them under the ten browsing categories. */
    private val entries: List<Wallpaper> = listOf(
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
        Wallpaper("geometric_05", "Golden Ridge", "geometric", R.drawable.geometric_05_light, darkResId = R.drawable.geometric_05_dark),
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

        // Trending — original artwork in current social-media styles
        Wallpaper("trend_aura", "Aura Glow", "trending", R.drawable.trend_aura_light, darkResId = R.drawable.trend_aura_dark),
        Wallpaper("trend_chrome_star", "Chrome Star", "trending", R.drawable.trend_chrome_star_light, darkResId = R.drawable.trend_chrome_star_dark),
        Wallpaper("trend_coquette", "Coquette", "trending", R.drawable.trend_coquette_light, darkResId = R.drawable.trend_coquette_dark),
        Wallpaper("trend_arches", "Boho Arches", "trending", R.drawable.trend_arches_light, darkResId = R.drawable.trend_arches_dark),
        Wallpaper("trend_liquid_metal", "Liquid Metal", "trending", R.drawable.trend_liquid_metal_light, darkResId = R.drawable.trend_liquid_metal_dark),
        Wallpaper("trend_cyber_grid", "Cyber Grid", "trending", R.drawable.trend_cyber_grid_light, darkResId = R.drawable.trend_cyber_grid_dark),

        // Trending — real photographs, freely licensed (CC0 / public domain / CC BY). Credits: PHOTO_CREDITS.md
        Wallpaper("photo_milky_way", "Milky Way", "trending", R.drawable.photo_milky_way, credit = "W.carter via Wikimedia Commons (cropped)", license = "CC0"),
        Wallpaper("photo_aurora_hut", "Aurora Hut", "trending", R.drawable.photo_aurora_hut, credit = "Ted.ns via Wikimedia Commons (cropped)", license = "CC BY 4.0"),
        Wallpaper("photo_blossom", "Cherry Blossom", "trending", R.drawable.photo_blossom, credit = "Syced via Wikimedia Commons (cropped)", license = "CC0"),
        Wallpaper("photo_foggy_trail", "Foggy Trail", "trending", R.drawable.photo_foggy_trail, credit = "Mar Mkrtchyan via Wikimedia Commons (cropped)", license = "CC0"),
        Wallpaper("photo_forest_stream", "Forest Stream", "trending", R.drawable.photo_forest_stream, credit = "Tom Hall via Wikimedia Commons (cropped)", license = "CC BY 2.0"),
        Wallpaper("photo_sand_ripples", "Sand Ripples", "trending", R.drawable.photo_sand_ripples, credit = "Vyacheslav Argenberg via Wikimedia Commons (cropped)", license = "CC BY 4.0"),
        Wallpaper("photo_tokyo_night", "Tokyo Night", "trending", R.drawable.photo_tokyo_night, credit = "Zengame via Wikimedia Commons (cropped)", license = "CC BY 2.0"),
        Wallpaper("photo_palm_sunset", "Palm Sunset", "trending", R.drawable.photo_palm_sunset, credit = "John Robert McPherson via Wikimedia Commons (cropped)", license = "CC0"),
        Wallpaper("photo_lake_louise", "Lake Louise", "trending", R.drawable.photo_lake_louise, credit = "Terry D. Ott via Wikimedia Commons (cropped)", license = "CC BY 2.0"),
        Wallpaper("photo_earthrise", "Earthrise", "trending", R.drawable.photo_earthrise, credit = "NASA / LRO via Wikimedia Commons (cropped)", license = "Public domain"),
        Wallpaper("photo_manhattan_night", "Manhattan Night", "trending", R.drawable.photo_manhattan_night, credit = "Tom Bullock via Wikimedia Commons (cropped)", license = "CC BY 2.0"),

        // Dreamscapes — original illustrations drawn for Wallora (paper-cut, space, crystal, ocean, glass)
        Wallpaper("dream_paper_dawn", "Paper Dawn", "dreamscapes", R.drawable.dream_paper_dawn),
        Wallpaper("dream_event_horizon", "Event Horizon", "dreamscapes", R.drawable.dream_event_horizon),
        Wallpaper("dream_ringed_giant", "Ringed Giant", "dreamscapes", R.drawable.dream_ringed_giant),
        Wallpaper("dream_amethyst_geode", "Amethyst Geode", "dreamscapes", R.drawable.dream_amethyst_geode),
        Wallpaper("dream_deep_glow", "Deep Glow", "dreamscapes", R.drawable.dream_deep_glow),
        Wallpaper("dream_glass_orbs", "Glass Orbs", "dreamscapes", R.drawable.dream_glass_orbs),

        // Viral moods — original artwork in the styles people save most: dark curves, cotton clouds, black-gold marble, neon rain, sakura night drive
        Wallpaper("viral_midnight_curves", "Midnight Curves", "dark", R.drawable.viral_midnight_curves),
        Wallpaper("viral_ember_curves", "Ember Curves", "dark", R.drawable.viral_ember_curves),
        Wallpaper("viral_cotton_clouds", "Cotton Clouds", "pinterest", R.drawable.viral_cotton_clouds),
        Wallpaper("viral_black_gold_marble", "Black Gold Marble", "luxury", R.drawable.viral_black_gold_marble),
        Wallpaper("viral_neon_rain", "Neon Rain", "city", R.drawable.viral_neon_rain),
        Wallpaper("viral_sakura_drive", "Sakura Night Drive", "cars", R.drawable.viral_sakura_drive),

        // Night cars — supplied by the app owner from the 'Wallpaper App Prompt_files' folder; see NIGHT_CARS_SOURCE.md
        Wallpaper("nightcar_01", "City Night Sedan", "cars", R.drawable.nightcar_01),
        Wallpaper("nightcar_02", "Smoke Drift", "cars", R.drawable.nightcar_02),
        Wallpaper("nightcar_03", "Red Legend", "cars", R.drawable.nightcar_03),
        Wallpaper("nightcar_04", "Gas Station Noir", "cars", R.drawable.nightcar_04),
        Wallpaper("nightcar_05", "Tunnel Run", "cars", R.drawable.nightcar_05),
        Wallpaper("nightcar_06", "Bridge Fog", "cars", R.drawable.nightcar_06),
        Wallpaper("nightcar_07", "Cherry Blossom Drive", "cars", R.drawable.nightcar_07),
        Wallpaper("nightcar_08", "Violet Skyline", "cars", R.drawable.nightcar_08),
        Wallpaper("nightcar_09", "Snowfall Drive", "cars", R.drawable.nightcar_09),
        Wallpaper("nightcar_10", "Black Muscle", "cars", R.drawable.nightcar_10),
        Wallpaper("nightcar_11", "Pink Neon Skyline", "cars", R.drawable.nightcar_11),
        Wallpaper("nightcar_12", "Silver Streets", "cars", R.drawable.nightcar_12),
        Wallpaper("nightcar_13", "Concrete Silence", "cars", R.drawable.nightcar_13),
        Wallpaper("nightcar_14", "Midnight Blue", "cars", R.drawable.nightcar_14),
        Wallpaper("nightcar_15", "Orange Rain", "cars", R.drawable.nightcar_15),
        Wallpaper("nightcar_16", "Black Mist", "cars", R.drawable.nightcar_16),
        Wallpaper("nightcar_17", "Red Line", "cars", R.drawable.nightcar_17),

        // Mood images — supplied by the app owner from the 'Wallpaper App Prompt_files' folder; see MOOD_IMAGES_SOURCE.md
        Wallpaper("mood_01", "Blue Pedestal", "dark", R.drawable.mood_01),
        Wallpaper("mood_02", "Moonlit Shore", "nature", R.drawable.mood_02),
        Wallpaper("mood_03", "Black Rose", "dark", R.drawable.mood_03),
        Wallpaper("mood_04", "Private Cabin", "luxury", R.drawable.mood_04),
        Wallpaper("mood_05", "Gothic Keep", "dark", R.drawable.mood_05),
        Wallpaper("mood_06", "Misty Peak", "nature", R.drawable.mood_06),
        Wallpaper("mood_08", "Violet Night Sky", "pinterest", R.drawable.mood_08),
        Wallpaper("mood_09", "Dark Wings", "dark", R.drawable.mood_09),
        Wallpaper("mood_13", "Retro Sunset PC", "gaming", R.drawable.mood_13),
        Wallpaper("mood_15", "Neon Avenue", "city", R.drawable.mood_15),
        Wallpaper("mood_16", "Purple Tide", "nature", R.drawable.mood_16),
        Wallpaper("mood_17", "Liquid Chrome", "abstract3d", R.drawable.mood_17),
        Wallpaper("mood_18", "Skyline Penthouse", "luxury", R.drawable.mood_18),
        Wallpaper("mood_20", "Your Own Priority", "pinterest", R.drawable.mood_20),
        Wallpaper("mood_21", "Built in Silence", "luxury", R.drawable.mood_21),
        Wallpaper("mood_22", "Stoic Power", "dark", R.drawable.mood_22),
        Wallpaper("mood_23", "Dark Silk", "abstract3d", R.drawable.mood_23),
        Wallpaper("mood_24", "Forge", "dark", R.drawable.mood_24),
        Wallpaper("mood_25", "Violet Nebula", "space", R.drawable.mood_25),
        Wallpaper("mood_26", "Sunset Run", "nature", R.drawable.mood_26),
        Wallpaper("mood_27", "Discipline", "dark", R.drawable.mood_27),
        Wallpaper("mood_28", "Mirror Lake Galaxy", "space", R.drawable.mood_28),
        Wallpaper("mood_29", "Neon Facets", "abstract3d", R.drawable.mood_29),
        Wallpaper("mood_30", "Heavy Lift", "dark", R.drawable.mood_30),
        Wallpaper("mood_32", "Chrome Flow", "abstract3d", R.drawable.mood_32),
        Wallpaper("mood_33", "Stop When Done", "dark", R.drawable.mood_33),
        Wallpaper("mood_34", "Empty Gym", "dark", R.drawable.mood_34),
        Wallpaper("mood_35", "Teal Peaks", "nature", R.drawable.mood_35),
        Wallpaper("mood_36", "Pastel Haze", "pinterest", R.drawable.mood_36),
        Wallpaper("mood_37", "Day One", "dark", R.drawable.mood_37),

        // Luxury — original embossed-monogram patterns drawn for Wallora (no third-party marks)
        Wallpaper("lux_midnight_gold", "Midnight Gold", "luxury", R.drawable.lux_midnight_gold),
        Wallpaper("lux_noir_gold", "Noir Gold", "luxury", R.drawable.lux_noir_gold),
        Wallpaper("lux_burgundy_crest", "Burgundy Crest", "luxury", R.drawable.lux_burgundy_crest),
        Wallpaper("lux_emerald_royale", "Emerald Royale", "luxury", R.drawable.lux_emerald_royale),
        Wallpaper("lux_ivory_cocoa", "Ivory Cocoa", "luxury", R.drawable.lux_ivory_cocoa),
        Wallpaper("lux_silver_storm", "Silver Storm", "luxury", R.drawable.lux_silver_storm),

        // Supercars and Sports — real photographs, freely licensed. Credits: PHOTO_CREDITS.md
        Wallpaper("photo_supercar_dusk", "White Supercar at Dusk", "supercars", R.drawable.photo_supercar_dusk, credit = "Emran Kassim via Wikimedia Commons (cropped)", license = "CC BY 2.0"),
        Wallpaper("photo_supercar_green", "Green Supercar", "supercars", R.drawable.photo_supercar_green, credit = "Retired electrician via Wikimedia Commons (cropped)", license = "CC0"),
        Wallpaper("photo_supercar_black", "Black Supercar", "supercars", R.drawable.photo_supercar_black, credit = "Syced via Wikimedia Commons (cropped)", license = "CC0"),
        Wallpaper("photo_supercar_garage", "Neon Garage", "supercars", R.drawable.photo_supercar_garage, credit = "Iamjosemom via Wikimedia Commons (cropped)", license = "CC0"),
        Wallpaper("photo_sport_ball", "Match Ball", "sports", R.drawable.photo_sport_ball, credit = "C MA chancema via Wikimedia Commons (cropped)", license = "CC0"),
        Wallpaper("photo_sport_floodlit_fog", "Floodlit Fog", "sports", R.drawable.photo_sport_floodlit_fog, credit = "W.carter via Wikimedia Commons (cropped)", license = "CC0"),
        Wallpaper("photo_sport_stadium_haze", "Stadium Haze", "sports", R.drawable.photo_sport_stadium_haze, credit = "W.carter via Wikimedia Commons (cropped)", license = "CC0"),
        Wallpaper("photo_sport_court_sunset", "Court at Sunset", "sports", R.drawable.photo_sport_court_sunset, credit = "Alyssiui via Wikimedia Commons (cropped)", license = "CC0"),
        Wallpaper("photo_sport_night_hoop", "Night Hoop", "sports", R.drawable.photo_sport_night_hoop, credit = "Ximonic (Simo Räsänen) via Wikimedia Commons (cropped)", license = "CC BY 3.0"),
    )

    val wallpapers: List<Wallpaper> = (entries + PersonalPack.entries).map {
        it.copy(
            categoryId = Taxonomy.categoryFor(it.categoryId, it.title),
            colors = ColorIndex.byId[it.id].orEmpty(),
        )
    }

    private val byId: Map<String, Wallpaper> = wallpapers.associateBy { it.id }

    fun wallpaper(id: String): Wallpaper? = byId[id]

    fun category(id: String): Category? = categories.firstOrNull { it.id == id }

    fun inCategory(id: String): List<Wallpaper> =
        wallpapers.filter { it.categoryId == id }
}

package com.wallora.app.data

/**
 * The ten browsing categories, and the rules that file every wallpaper under one of them.
 *
 * The bundled catalogue and the cloud catalogue were built with older, finer-grained category ids
 * (aurora, posters, neon ...). Rather than rewrite hundreds of entries, [categoryFor] maps the old
 * id (and, for the mixed buckets, the title) onto the new one. Cloud wallpapers go through the same
 * function, so wallpapers uploaded before the change land in the right place too.
 */
object Taxonomy {

    val categories: List<Category> = listOf(
        Category("dark", "Dark Aesthetic", "🖤"),
        Category("space", "Space & Galaxy", "🌌"),
        Category("cars", "Cars", "🚘"),
        Category("luxury", "Luxury Lifestyle", "💸"),
        Category("pinterest", "Pinterest Aesthetic", "🌸"),
        Category("gaming", "Gaming", "🎮"),
        Category("city", "City Nights", "🏙️"),
        Category("nature", "Nature", "🦋"),
        Category("cute", "Cute & Soft", "💗"),
        Category("abstract3d", "3D & Abstract", "🧊"),
    )

    val ids: Set<String> = categories.map { it.id }.toSet()

    private val byOldId = mapOf(
        "aurora" to "nature", "seasonal" to "nature",
        "gradient" to "pinterest", "minimal" to "pinterest",
        "neon" to "gaming",
        "cars" to "cars", "supercars" to "cars",
        "luxury" to "luxury",
        "animals" to "cute", "food" to "cute",
        "waves" to "abstract3d", "geometric" to "abstract3d", "patterns" to "abstract3d",
        "space" to "space",
        "city" to "city",
        "posters" to "pinterest", "trending" to "pinterest", "dreamscapes" to "abstract3d", "sports" to "nature",
    )

    /** Titles that belong somewhere other than their old bucket's default. */
    private val byTitle = mapOf(
        // old "posters"
        "summit" to "nature", "tropic" to "nature", "set sail" to "nature", "desert" to "nature", "reef" to "nature",
        "lift off" to "space", "orbit" to "space", "starlight" to "space",
        "skyline" to "city", "rainy" to "city", "harbor" to "city",
        "ride" to "cars",
        "brew" to "cute", "whiskers" to "cute", "scoop" to "cute", "love" to "cute",
        "player one" to "gaming", "voltage" to "gaming",
        "riff" to "dark", "idea" to "abstract3d",
        // old "trending"
        "chrome star" to "abstract3d", "liquid metal" to "abstract3d", "cyber grid" to "gaming",
        "milky way" to "space", "earthrise" to "space",
        "aurora hut" to "nature", "foggy trail" to "nature", "forest stream" to "nature", "sand ripples" to "nature",
        "palm sunset" to "nature", "lake louise" to "nature",
        "tokyo night" to "city", "manhattan night" to "city",
        // old "dreamscapes"
        "paper dawn" to "nature", "event horizon" to "space", "ringed giant" to "space", "deep glow" to "dark",
        // old "sports"
        "match ball" to "dark", "floodlit fog" to "dark", "night hoop" to "city",
    )

    /**
     * Wallpapers taken out of the app. The cloud catalogue still holds copies under the same ids
     * (they were seeded from the bundled set), so they are filtered out when the cloud list is read.
     * Delete them from the admin page to retire them for good.
     */
    val retiredIds: Set<String> = setOf(
        // flat white-car-on-colour illustrations ("Scarlet Coupe" ... "Graphite Coupe")
        "cars_p01", "cars_p02", "cars_p03", "cars_p04", "cars_p05", "cars_p06",
        // supplied pictures of anime characters, a real person and brand logos: kept out of the Play release
        // (they live in the private debug-only pack instead, see scripts/personal_pack.py)
        "mood_07", "mood_10", "mood_11", "mood_12", "mood_14", "mood_19", "mood_31",
    )

    /** Old ids that must not show up as extra categories when the cloud still lists them. */
    val legacyIds: Set<String> = byOldId.keys

    fun categoryFor(oldCategoryId: String, title: String): String {
        if (oldCategoryId in ids) return oldCategoryId
        return byTitle[title.trim().lowercase()] ?: byOldId[oldCategoryId] ?: "pinterest"
    }

    fun category(id: String): Category? = categories.firstOrNull { it.id == id }
}

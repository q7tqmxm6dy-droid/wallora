package com.wallora.app.data

import kotlin.random.Random

/** A named look people are posting right now, backed by a rule over the catalogue. */
data class ViralStyle(
    val id: String,
    val title: String,
    val emoji: String,
    val matches: (Wallpaper) -> Boolean,
)

/**
 * Editorial curation: what the Home, Trending and Daily Drop surfaces show.
 *
 * There is no usage backend, so "trending" is an editorial rotation rather than measured popularity:
 * a stable pseudo-random score per wallpaper that is re-rolled every [TREND_WINDOW_DAYS] days, with a
 * boost for 4K, featured and freshly uploaded cloud wallpapers. Everything is a pure function of the
 * catalogue and the day, so it is deterministic, testable, and the same for every user on a given day.
 */
object Curation {

    const val TREND_WINDOW_DAYS = 3L

    private fun mix(a: Int, b: Long): Int {
        var h = a * -1640531535 + (b * 40503L).toInt()
        h = h xor (h ushr 15); h *= 0x2c1b3c6d; h = h xor (h ushr 12); h *= 0x297a2d39; h = h xor (h ushr 15)
        return h
    }

    /** Photographs, dreamscapes, luxury and the trend set are the flagship art; the simple posters are not. */
    private val FLAGSHIP_PREFIXES = listOf("photo_", "dream_", "lux_", "trend_", "viral_")
    fun isFlagship(w: Wallpaper): Boolean = w.uhd || FLAGSHIP_PREFIXES.any { w.id.startsWith(it) }

    fun trendScore(w: Wallpaper, epochDay: Long): Int {
        val rolled = (mix(w.id.hashCode(), epochDay / TREND_WINDOW_DAYS) ushr 1) % 1000
        return rolled + (if (w.featured) 400 else 0) + (if (w.uhd) 150 else 0) + (if (w.remote) 100 else 0) +
            (if (isFlagship(w)) 350 else 0)
    }

    fun trending(all: List<Wallpaper>, epochDay: Long, n: Int): List<Wallpaper> =
        all.sortedByDescending { trendScore(it, epochDay) }.take(n)

    /** The hero carousel: the day's strongest pick from each of several categories, so it has variety. */
    fun featured(all: List<Wallpaper>, epochDay: Long, n: Int): List<Wallpaper> {
        val pool = all.filter(::isFlagship).ifEmpty { all }
        val best = pool.groupBy { it.categoryId }.values
            .map { group -> group.maxBy { trendScore(it, epochDay + 1) } }
            .sortedByDescending { trendScore(it, epochDay + 1) }
        return best.take(n)
    }

    /** A fresh set for the day. Same seed, same set, for everyone. */
    fun dailyDrop(all: List<Wallpaper>, epochDay: Long, n: Int): List<Wallpaper> {
        val pool = all.filter(::isFlagship).takeIf { it.size >= n } ?: all
        return pool.shuffled(Random(epochDay * 7919L)).take(n)
    }

    /** The endless "for you" feed: everything, in an order that changes daily. */
    fun feed(all: List<Wallpaper>, epochDay: Long): List<Wallpaper> =
        all.shuffled(Random(epochDay * 104729L + 17))

    /** Varied card heights give the grid its Pinterest rhythm even though every image is 9:16. */
    private val RATIOS = floatArrayOf(0.5625f, 0.66f, 0.75f, 0.62f, 0.5625f, 0.7f)
    fun cardRatio(id: String): Float = RATIOS[(id.hashCode() and Int.MAX_VALUE) % RATIOS.size]

    private fun titled(vararg names: String): (Wallpaper) -> Boolean {
        val set = names.map { it.lowercase() }.toSet()
        return { it.title.trim().lowercase() in set }
    }

    val viralStyles: List<ViralStyle> = listOf(
        ViralStyle("aura", "Aura & Dreamy", "🌈", { w ->
            titled("Aura Glow", "Coquette", "Boho Arches", "Soft Circle", "Aqua Ring", "Pastel Arch", "Sunset Fade",
                "Violet Dusk", "Cherry Blossom", "Glass Orbs", "Paper Dawn")(w)
        }),
        ViralStyle("y2k", "Y2K Chrome", "💿", titled("Chrome Star", "Liquid Metal", "Retro Glow", "Laser Arc",
            "Neon Dreams", "Prism", "Low Poly", "Facets")),
        ViralStyle("moody", "Dark & Moody", "🕯️", { w ->
            w.categoryId == "dark" || titled("Noir Gold", "Foggy Trail", "Night Runner", "Graphite Coupe")(w)
        }),
        ViralStyle("neon", "Neon Nights", "🌃", { it.categoryId == "city" || it.categoryId == "gaming" }),
        ViralStyle("cosmic", "Cosmic", "🪐", { it.categoryId == "space" }),
        ViralStyle("luxe", "Golden Luxe", "✨", { w ->
            w.categoryId == "luxury" || titled("Golden Hour", "Golden Ridge", "Golden Bars")(w)
        }),
        ViralStyle("soft", "Soft Girl", "🎀", { w -> w.categoryId == "cute" || titled("Coquette", "Whiskers")(w) }),
    )
}

package com.wallora.app.data

/**
 * "Picked for you": a small on-device recommender, so nothing about the user's taste leaves the phone.
 *
 * Signals are weighted by how deliberate they are: a favourite counts most, a download next, a view least.
 * Each wallpaper is scored by how much the user likes its category and its dominant colours, plus a little of
 * the day's trend score so the list is not frozen. Wallpapers already favourited are left out. With no signals
 * at all (a new user) it falls back to the trending list.
 */
object Recommender {

    private const val FAVOURITE = 4
    private const val DOWNLOAD = 3
    private const val VIEW = 1

    fun pick(
        all: List<Wallpaper>,
        favouriteIds: Set<String>,
        viewedIds: List<String>,
        downloadedIds: List<String>,
        epochDay: Long,
        n: Int,
    ): List<Wallpaper> {
        val byId = all.associateBy { it.id }
        val categoryAffinity = HashMap<String, Int>(); val colourAffinity = HashMap<String, Int>()
        fun learn(ids: Collection<String>, weight: Int) = ids.forEach { id ->
            val w = byId[id] ?: return@forEach
            categoryAffinity.merge(w.categoryId, weight, Int::plus)
            w.colors.forEach { colourAffinity.merge(it, weight, Int::plus) }
        }
        learn(favouriteIds, FAVOURITE); learn(downloadedIds, DOWNLOAD); learn(viewedIds, VIEW)
        if (categoryAffinity.isEmpty()) return Curation.trending(all, epochDay, n)

        val maxCategory = categoryAffinity.values.max().toFloat(); val maxColour = (colourAffinity.values.maxOrNull() ?: 1).toFloat()
        return all.asSequence()
            .filter { it.id !in favouriteIds }
            .sortedByDescending { w ->
                val c = (categoryAffinity[w.categoryId] ?: 0) / maxCategory
                val col = w.colors.sumOf { colourAffinity[it] ?: 0 } / maxColour
                (c * 700 + col * 250 + Curation.trendScore(w, epochDay) * 0.12f).toInt()
            }
            .take(n).toList()
    }
}

package com.wallora.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecommenderTest {

    private val catalogue = WallpaperCatalog.wallpapers

    @Test
    fun `new users get the trending list`() {
        val picks = Recommender.pick(catalogue, emptySet(), emptyList(), emptyList(), 20000, 8)
        assertEquals(Curation.trending(catalogue, 20000, 8), picks)
    }

    @Test
    fun `a favourite category dominates the picks`() {
        val space = catalogue.filter { it.categoryId == "space" }.take(2).map { it.id }.toSet()
        val picks = Recommender.pick(catalogue, space, space.toList(), emptyList(), 20000, 6)
        assertTrue("expected mostly space, got ${picks.map { it.categoryId }}", picks.count { it.categoryId == "space" } >= 4)
    }

    @Test
    fun `favourited wallpapers are not recommended again`() {
        val favs = catalogue.take(3).map { it.id }.toSet()
        assertTrue(Recommender.pick(catalogue, favs, emptyList(), emptyList(), 1, 20).none { it.id in favs })
    }

    @Test
    fun `downloads count as a taste signal`() {
        val cute = catalogue.filter { it.categoryId == "cute" }.take(2).map { it.id }
        val picks = Recommender.pick(catalogue, emptySet(), emptyList(), cute, 5, 6)
        assertTrue(picks.any { it.categoryId == "cute" })
    }

    @Test
    fun `colour index covers the bundled catalogue`() {
        val missing = catalogue.filter { it.colors.isEmpty() }.map { it.id }
        assertTrue("wallpapers without colours (run scripts/gen_color_index.py): $missing", missing.isEmpty())
        assertFalse(catalogue.any { it.colors.size > 2 })
    }
}

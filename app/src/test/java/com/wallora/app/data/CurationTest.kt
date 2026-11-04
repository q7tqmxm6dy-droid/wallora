package com.wallora.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CurationTest {

    private val catalogue = WallpaperCatalog.wallpapers

    @Test
    fun `every wallpaper lands in one of the ten categories`() {
        val ids = Taxonomy.ids
        val stray = catalogue.filter { it.categoryId !in ids }.map { it.title }
        assertTrue("wallpapers outside the ten categories: $stray", stray.isEmpty())
    }

    @Test
    fun `every category has wallpapers`() {
        val used = catalogue.groupBy { it.categoryId }
        val empty = Taxonomy.categories.filter { used[it.id].isNullOrEmpty() }.map { it.id }
        assertTrue("empty categories: $empty", empty.isEmpty())
    }

    @Test
    fun `curation is deterministic for a given day`() {
        assertEquals(Curation.trending(catalogue, 20000, 10), Curation.trending(catalogue, 20000, 10))
        assertEquals(Curation.dailyDrop(catalogue, 20000, 8), Curation.dailyDrop(catalogue, 20000, 8))
    }

    @Test
    fun `daily drop and trending change over time`() {
        assertNotEquals(Curation.dailyDrop(catalogue, 20000, 8), Curation.dailyDrop(catalogue, 20001, 8))
        assertNotEquals(Curation.trending(catalogue, 20000, 10), Curation.trending(catalogue, 20000 + 3 * Curation.TREND_WINDOW_DAYS, 10))
    }

    @Test
    fun `trending is stable inside a window`() {
        val start = 20001L - (20001L % Curation.TREND_WINDOW_DAYS)
        assertEquals(Curation.trending(catalogue, start, 10), Curation.trending(catalogue, start + 1, 10))
    }

    @Test
    fun `featured spans several categories`() {
        val featured = Curation.featured(catalogue, 20000, 6)
        assertEquals(6, featured.size)
        assertEquals(6, featured.map { it.categoryId }.toSet().size)
    }

    @Test
    fun `fourK and cloud wallpapers rank above equal peers`() {
        val a = Wallpaper("x", "A", "dark", resId = 1)
        val b = a.copy(uhd = true)
        assertTrue(Curation.trendScore(b, 5) > Curation.trendScore(a, 5))
    }

    @Test
    fun `every viral style has something in it`() {
        val empty = Curation.viralStyles.filter { style -> catalogue.none(style.matches) }.map { it.id }
        assertTrue("styles with no wallpapers: $empty", empty.isEmpty())
    }

    @Test
    fun `card ratios vary and are stable`() {
        val ratios = catalogue.map { Curation.cardRatio(it.id) }.toSet()
        assertTrue(ratios.size > 2)
        assertEquals(Curation.cardRatio("abc"), Curation.cardRatio("abc"))
    }

    @Test
    fun `retired wallpapers are not bundled`() {
        val ids = WallpaperCatalog.wallpapers.map { it.id }.toSet()
        assertTrue(Taxonomy.retiredIds.none { it in ids })
    }
}

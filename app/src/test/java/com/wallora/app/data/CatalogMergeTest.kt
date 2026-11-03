package com.wallora.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogMergeTest {

    private fun remote(id: String) = Wallpaper(id, "cloud $id", "aurora", imageUrl = "https://x/$id.jpg", remote = true)
    private fun bundled(id: String) = Wallpaper(id, "bundled $id", "aurora", resId = 1)

    @Test
    fun `cloud replaces bundled when ids match`() {
        val merged = mergeCatalog(listOf(remote("a"), remote("b")), listOf(bundled("a"), bundled("b")))

        assertEquals(2, merged.size)
        assertTrue("cloud should win", merged.all { it.remote })
        assertEquals(listOf("a", "b"), merged.map { it.id })
    }

    @Test
    fun `bundled entries without a cloud counterpart are kept`() {
        val merged = mergeCatalog(listOf(remote("a")), listOf(bundled("a"), bundled("b")))

        assertEquals(2, merged.size)
        assertEquals(listOf("a", "b"), merged.map { it.id })
        assertTrue(merged.first { it.id == "a" }.remote)
        assertTrue(!merged.first { it.id == "b" }.remote)
    }

    @Test
    fun `empty cloud falls back entirely to bundled`() {
        val bundledList = listOf(bundled("a"), bundled("b"), bundled("c"))
        val merged = mergeCatalog(emptyList(), bundledList)

        assertEquals(bundledList, merged)
    }

    @Test
    fun `cloud only ids are included`() {
        val merged = mergeCatalog(listOf(remote("cloud-only")), emptyList())

        assertEquals(1, merged.size)
        assertEquals("cloud-only", merged.first().id)
    }

    @Test
    fun `no duplicates are produced`() {
        val ids = List(24) { "w$it" }
        val merged = mergeCatalog(ids.map(::remote), ids.map(::bundled))

        assertEquals(24, merged.size)
        assertEquals(merged.size, merged.map { it.id }.toSet().size)
    }

    @Test
    fun `cloud entry with the same title and category replaces the bundled one`() {
        val bundledPhoto = Wallpaper(
            "photo_lake", "Lake Louise", "trending", resId = 1,
            credit = "T. Ott via Wikimedia Commons", license = "CC BY 2.0",
        )
        val cloud4k = Wallpaper(
            "auto-id-123", " lake louise ", "trending", imageUrl = "https://x/4k.jpg",
            remote = true, uhd = true,
        )

        val merged = mergeCatalog(listOf(cloud4k), listOf(bundledPhoto))

        assertEquals(listOf("auto-id-123"), merged.map { it.id })
        assertTrue(merged.single().uhd)
    }

    @Test
    fun `replacement keeps the bundled credit and license`() {
        val bundledPhoto = Wallpaper(
            "photo_lake", "Lake Louise", "trending", resId = 1,
            credit = "T. Ott via Wikimedia Commons", license = "CC BY 2.0",
        )
        val cloud = Wallpaper("n1", "Lake Louise", "trending", imageUrl = "u", remote = true)

        val result = mergeCatalog(listOf(cloud), listOf(bundledPhoto)).single()

        assertEquals("T. Ott via Wikimedia Commons", result.credit)
        assertEquals("CC BY 2.0", result.license)
    }

    @Test
    fun `same title in a different category is not treated as a duplicate`() {
        val bundledItem = Wallpaper("a", "Sunset", "gradient", resId = 1)
        val cloud = Wallpaper("b", "Sunset", "seasonal", imageUrl = "u", remote = true)

        assertEquals(2, mergeCatalog(listOf(cloud), listOf(bundledItem)).size)
    }
}

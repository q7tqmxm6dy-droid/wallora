package com.wallora.app.data

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the bundled catalogue against silent drift.
 *
 * This test exists because of a real regression: an automated edit rewrote
 * `WallpaperCatalog.kt` with a search-and-replace whose anchor did not match, so the
 * two `geometric_05` images shipped inside the APK but nothing referenced them. The
 * build stayed green and the app still showed the wallpaper (the cloud catalogue
 * supplied it) so the mistake went unnoticed.
 */
class CatalogConsistencyTest {

    /** Unit tests run with the module directory as the working dir; allow for either. */
    private val moduleDir: File = listOf(File("."), File("app")).first { it.resolve(CATALOG).exists() }

    private fun catalogueSource(): String = moduleDir.resolve(CATALOG).readText()

    private fun bundledDrawables(): Set<String> =
        moduleDir.resolve("src/main/res/drawable-nodpi")
            .listFiles()
            .orEmpty()
            .filter { it.isFile }
            .map { it.name.substringBeforeLast('.') }
            .toSet()

    @Test
    fun `every bundled image is referenced by the catalogue`() {
        val referenced = REFERENCE.findAll(catalogueSource()).map { it.groupValues[1] }.toSet()
        val unreferenced = bundledDrawables() - referenced

        assertTrue(
            "these images ship in the APK but no wallpaper uses them: $unreferenced",
            unreferenced.isEmpty(),
        )
    }

    @Test
    fun `every referenced drawable exists on disk`() {
        val missing = REFERENCE.findAll(catalogueSource()).map { it.groupValues[1] }.toSet() - bundledDrawables()

        assertTrue("catalogue references drawables with no file: $missing", missing.isEmpty())
    }

    @Test
    fun `wallpaper ids are unique`() {
        val ids = WallpaperCatalog.wallpapers.map { it.id }

        assertEquals("duplicate wallpaper ids", ids.size, ids.toSet().size)
    }

    @Test
    fun `every wallpaper has a title and a known category`() {
        val categoryIds = WallpaperCatalog.categories.map { it.id }.toSet()

        WallpaperCatalog.wallpapers.forEach { wallpaper ->
            assertTrue("blank title for ${wallpaper.id}", wallpaper.title.isNotBlank())
            assertTrue(
                "${wallpaper.id} uses unknown category '${wallpaper.categoryId}'",
                wallpaper.categoryId in categoryIds,
            )
        }
    }

    @Test
    fun `every category has at least one wallpaper`() {
        val used = WallpaperCatalog.wallpapers.map { it.categoryId }.toSet()
        val empty = WallpaperCatalog.categories.map { it.id }.filterNot { it in used }

        assertTrue("categories with no wallpapers: $empty", empty.isEmpty())
    }

    private companion object {
        const val CATALOG = "src/main/java/com/wallora/app/data/WallpaperCatalog.kt"
        val REFERENCE = Regex("""R\.drawable\.([A-Za-z0-9_]+)""")
    }
}

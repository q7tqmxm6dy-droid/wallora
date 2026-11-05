package com.wallora.app.data

import com.wallora.app.R

/**
 * The owner's private, debug-only wallpaper pack. Every drawable named `personal_*` becomes a wallpaper, so the pack
 * needs no code of its own: drop the pictures in with scripts/personal_pack.py. The release source set has an empty
 * copy of this object, so none of it can reach a Play bundle.
 */
object PersonalPack {
    val entries: List<Wallpaper> = R.drawable::class.java.fields
        .filter { it.name.startsWith("personal_") }
        .sortedBy { it.name }
        .map { field ->
            val slug = field.name.removePrefix("personal_")
            Wallpaper(
                id = "personal_$slug",
                title = slug.split('_').joinToString(" ") { part -> part.replaceFirstChar { it.uppercase() } }
                    .replace("Spider Man", "Spider-Man").replace("Spider Verse", "Spider-Verse").replace("X Men", "X-Men"),
                categoryId = "gaming",
                resId = field.getInt(null),
            )
        }
}

package com.wallora.app.data

/**
 * Merges the cloud catalogue with the bundled one.
 *
 * A cloud wallpaper replaces a bundled one when they share an id or a title. The title rule is what
 * lets a 4K file uploaded from the admin page (which gets an automatic id) take the place of its
 * 1080p twin that ships in the APK instead of showing up next to it. The replacement keeps the
 * bundled wallpaper's category, so the admin only has to type the title.
 *
 * Attribution travels with the image: the admin forms have no credit field, so a replacement
 * inherits the bundled entry's credit and license rather than silently dropping the CC BY notice.
 * Bundled entries with no cloud counterpart are appended, so the app still has content offline.
 */
internal fun mergeCatalog(remote: List<Wallpaper>, bundled: List<Wallpaper>): List<Wallpaper> {
    val bundledById = bundled.associateBy { it.id }
    val bundledByTitle = bundled.associateBy { titleKey(it) }
    val replaced = HashSet<String>()

    val merged = LinkedHashMap<String, Wallpaper>(remote.size + bundled.size)
    remote.forEach { cloud ->
        val twin = bundledById[cloud.id] ?: bundledByTitle[titleKey(cloud)]
        if (twin != null) replaced += twin.id
        merged[cloud.id] = if (twin == null) cloud else cloud.copy(
            categoryId = twin.categoryId,
            credit = cloud.credit ?: twin.credit,
            license = cloud.license ?: twin.license,
        )
    }
    bundled.forEach { local -> if (local.id !in replaced) merged.putIfAbsent(local.id, local) }
    return merged.values.toList()
}

private fun titleKey(w: Wallpaper) = w.title.trim().lowercase()

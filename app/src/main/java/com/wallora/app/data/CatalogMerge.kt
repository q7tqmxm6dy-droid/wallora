package com.wallora.app.data

/**
 * Merges the cloud catalogue with the bundled one.
 *
 * A cloud wallpaper replaces a bundled one when they share an id, or when they share a title and
 * category. The second rule is what lets a 4K file uploaded from the admin page (which gets an
 * automatic id) take the place of its 1080p twin that ships in the APK instead of showing up next
 * to it. Seeding the cloud with the built-in set therefore never produces duplicates, and editing a
 * cloud entry replaces what ships in the app. Bundled entries with no cloud counterpart are
 * appended, so the app still has content offline.
 *
 * Attribution travels with the image: the admin forms have no credit field, so a replacement
 * inherits the bundled entry's credit and license rather than silently dropping the CC BY notice.
 */
internal fun mergeCatalog(remote: List<Wallpaper>, bundled: List<Wallpaper>): List<Wallpaper> {
    val bundledById = bundled.associateBy { it.id }
    val bundledByName = bundled.associateBy { nameKey(it) }
    val replaced = HashSet<String>()

    val merged = LinkedHashMap<String, Wallpaper>(remote.size + bundled.size)
    remote.forEach { cloud ->
        val twin = bundledById[cloud.id] ?: bundledByName[nameKey(cloud)]
        if (twin != null) replaced += twin.id
        merged[cloud.id] = if (twin == null) cloud else cloud.copy(
            credit = cloud.credit ?: twin.credit,
            license = cloud.license ?: twin.license,
        )
    }
    bundled.forEach { local -> if (local.id !in replaced) merged.putIfAbsent(local.id, local) }
    return merged.values.toList()
}

private fun nameKey(w: Wallpaper) = w.title.trim().lowercase() + "|" + w.categoryId

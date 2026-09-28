package com.wallora.app.data

/**
 * Merges the cloud catalogue with the bundled one.
 *
 * Cloud entries win when they share an id with a bundled wallpaper, so seeding the cloud
 * with the built-in set doesn't produce duplicates — and editing a cloud entry replaces
 * what ships in the APK. Bundled entries that have no cloud counterpart are appended, so
 * the app still has content offline.
 */
internal fun mergeCatalog(remote: List<Wallpaper>, bundled: List<Wallpaper>): List<Wallpaper> {
    val merged = LinkedHashMap<String, Wallpaper>(remote.size + bundled.size)
    remote.forEach { merged[it.id] = it }
    bundled.forEach { merged.putIfAbsent(it.id, it) }
    return merged.values.toList()
}

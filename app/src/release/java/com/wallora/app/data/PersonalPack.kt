package com.wallora.app.data

/** Release builds never carry the private wallpaper pack; the debug source set provides it (see scripts/personal_pack.py). */
object PersonalPack {
    val entries: List<Wallpaper> = emptyList()
}

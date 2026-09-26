package com.wallora.app.data

import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * Live reads of the wallpaper catalogue from Firestore.
 *
 * If Firebase isn't configured yet (or the device is offline) every flow simply
 * emits an empty list, and the app keeps working with the bundled wallpapers.
 */
class RemoteWallpaperRepository {

    private val db: FirebaseFirestore? = runCatching { FirebaseFirestore.getInstance() }.getOrNull()

    fun wallpapers(): Flow<List<Wallpaper>> {
        val reference = db?.collection(COLLECTION) ?: return flowOf(emptyList())
        return reference.snapshotFlow()
            .map { snapshot -> snapshot.documents.mapNotNull { it.toWallpaper() } }
            .catch { emit(emptyList()) }
    }

    fun categories(): Flow<List<Category>> {
        val reference = db?.collection(CATEGORIES) ?: return flowOf(emptyList())
        return reference.snapshotFlow()
            .map { snapshot ->
                snapshot.documents.map { Category(it.id, it.getString("title") ?: it.id) }
            }
            .catch { emit(emptyList()) }
    }

    private fun CollectionReference.snapshotFlow(): Flow<QuerySnapshot> = callbackFlow {
        val registration = addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
            } else if (snapshot != null) {
                trySend(snapshot)
            }
        }
        awaitClose { registration.remove() }
    }

    private fun DocumentSnapshot.toWallpaper(): Wallpaper? {
        val url = getString("url")?.takeIf { it.isNotBlank() } ?: return null
        return Wallpaper(
            id = id,
            title = getString("title")?.takeIf { it.isNotBlank() } ?: "Untitled",
            categoryId = getString("category")?.takeIf { it.isNotBlank() } ?: "remote",
            imageUrl = url,
            remote = true,
        )
    }

    companion object {
        const val COLLECTION = "wallpapers"
        const val CATEGORIES = "categories"
    }
}

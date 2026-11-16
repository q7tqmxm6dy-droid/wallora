package com.wallora.app.account

import com.google.firebase.firestore.FirebaseFirestore
import com.wallora.app.data.PreferencesRepository
import com.wallora.app.util.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Keeps one small document per member, users/{uid}, holding favourite ids and album contents. Pair-maker pictures
 * stay on the phone (they are the member's own photos and can be large), as do all settings.
 *
 * Conflict rule, deliberately simple: the first time a phone signs in, local and cloud are merged (nothing is
 * lost). After that the newest change wins, so un-favouriting on one phone is not undone by another.
 */
class CloudSync(private val prefs: PreferencesRepository) {

    private val db: FirebaseFirestore? = runCatching { FirebaseFirestore.getInstance() }.getOrNull()

    /** What the cloud is known to hold, so the change observer does not upload what was just downloaded. */
    @Volatile private var known: Pair<Set<String>, Map<String, List<String>>>? = null

    fun isKnown(favorites: Set<String>, collections: Map<String, List<String>>) = known == (favorites to collections)

    fun forget() { known = null }

    private fun doc(uid: String) = db?.collection("users")?.document(uid)

    /** Called right after sign-in and on app start. Safe to call repeatedly; returns false if offline. */
    suspend fun pull(uid: String): Boolean = withContext(Dispatchers.IO) {
        val ref = doc(uid) ?: return@withContext false
        runCatching {
            val snap = ref.get().await()
            val localTs = prefs.syncTimestamp()
            val localFavs = prefs.readFavorites()
            val localCols = prefs.readCollections()
            if (!snap.exists()) {
                push(uid, localFavs, localCols)
                return@runCatching
            }
            val remoteTs = snap.getLong("updatedAt") ?: 0L
            @Suppress("UNCHECKED_CAST")
            val remoteFavs = (snap.get("favorites") as? List<String>).orEmpty().toSet()
            val remoteCols = parseCollections(snap.getString("collections"))
            when {
                localTs == 0L -> { // first time on this phone: keep everything from both sides
                    val merged = localFavs + remoteFavs
                    val cols = mergeCollections(localCols, remoteCols)
                    prefs.replaceSynced(merged, cols)
                    push(uid, merged, cols)
                }
                remoteTs > localTs -> {
                    prefs.replaceSynced(remoteFavs, remoteCols)
                    prefs.setSyncTimestamp(remoteTs)
                    known = remoteFavs to remoteCols
                }
                localFavs == remoteFavs && localCols == remoteCols -> known = localFavs to localCols
            }
        }.isSuccess
    }

    /** Uploads the current favourites and albums as the newest copy. */
    suspend fun push(uid: String, favorites: Set<String>, collections: Map<String, List<String>>): Boolean =
        withContext(Dispatchers.IO) {
            val ref = doc(uid) ?: return@withContext false
            val now = System.currentTimeMillis()
            runCatching {
                ref.set(
                    mapOf(
                        "favorites" to favorites.sorted(),
                        "collections" to encodeCollections(collections),
                        "updatedAt" to now,
                    ),
                ).await()
                prefs.setSyncTimestamp(now)
                known = favorites to collections
            }.isSuccess
        }

    companion object {
        fun mergeCollections(a: Map<String, List<String>>, b: Map<String, List<String>>): Map<String, List<String>> =
            (a.keys + b.keys).associateWith { ((a[it].orEmpty()) + (b[it].orEmpty())).distinct() }

        fun encodeCollections(map: Map<String, List<String>>): String {
            val json = org.json.JSONObject()
            map.forEach { (name, ids) -> json.put(name, org.json.JSONArray(ids)) }
            return json.toString()
        }

        fun parseCollections(raw: String?): Map<String, List<String>> {
            if (raw.isNullOrBlank()) return emptyMap()
            return runCatching {
                val json = org.json.JSONObject(raw)
                buildMap {
                    json.keys().forEach { key ->
                        val array = json.optJSONArray(key)
                        put(key, buildList { if (array != null) for (i in 0 until array.length()) add(array.getString(i)) })
                    }
                }
            }.getOrDefault(emptyMap())
        }
    }
}

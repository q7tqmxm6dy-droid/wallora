package com.wallora.app.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.wallora.app.util.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Admin operations against Firebase: signing in, uploading a wallpaper to Cloud
 * Storage + Firestore, deleting wallpapers and adding categories.
 *
 * Every call fails gracefully for the caller via [Result] when Firebase is not set up.
 */
class AdminRepository {

    private val auth: FirebaseAuth? = runCatching { FirebaseAuth.getInstance() }.getOrNull()
    private val storage: FirebaseStorage? = runCatching { FirebaseStorage.getInstance() }.getOrNull()
    private val db: FirebaseFirestore? = runCatching { FirebaseFirestore.getInstance() }.getOrNull()

    fun authState(): Flow<Boolean> = callbackFlow {
        val instance = auth
        if (instance == null) {
            trySend(false)
            close()
            return@callbackFlow
        }
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser != null) }
        instance.addAuthStateListener(listener)
        awaitClose { instance.removeAuthStateListener(listener) }
    }

    fun currentUserEmail(): String? = auth?.currentUser?.email

    suspend fun signIn(email: String, password: String): Result<Unit> = withContext(Dispatchers.IO) {
        val instance = auth
            ?: return@withContext Result.failure(IllegalStateException("Firebase is not configured"))
        runCatching { instance.signInWithEmailAndPassword(email, password).await() }
            .map { }
    }

    fun signOut() {
        runCatching { auth?.signOut() }
    }

    suspend fun uploadWallpaper(
        title: String,
        category: String,
        bytes: ByteArray,
        onProgress: (Float) -> Unit = {},
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val storageRef = storage
        val firestore = db
        if (storageRef == null || firestore == null) {
            return@withContext Result.failure(IllegalStateException("Firebase is not configured"))
        }
        runCatching {
            val fileRef = storageRef.reference.child("wallpapers/${UUID.randomUUID()}.jpg")
            val upload = fileRef.putBytes(bytes)
            upload.addOnProgressListener { snapshot ->
                if (snapshot.totalByteCount > 0) {
                    onProgress(snapshot.bytesTransferred.toFloat() / snapshot.totalByteCount)
                }
            }
            upload.await()
            val url = fileRef.downloadUrl.await().toString()
            firestore.collection(RemoteWallpaperRepository.COLLECTION).add(
                mapOf(
                    "title" to title,
                    "category" to category,
                    "url" to url,
                    "path" to fileRef.path,
                    "createdAt" to FieldValue.serverTimestamp(),
                ),
            ).await()
        }.map { }
    }

    suspend fun deleteWallpaper(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        val firestore = db
            ?: return@withContext Result.failure(IllegalStateException("Firebase is not configured"))
        runCatching {
            val document = firestore.collection(RemoteWallpaperRepository.COLLECTION).document(id)
            val snapshot = document.get().await()
            val path = snapshot.getString("path")
            if (!path.isNullOrBlank()) {
                runCatching { storage?.reference?.child(path)?.delete()?.await() }
            }
            document.delete().await()
        }.map { }
    }

    suspend fun addCategory(id: String, title: String): Result<Unit> = withContext(Dispatchers.IO) {
        val firestore = db
            ?: return@withContext Result.failure(IllegalStateException("Firebase is not configured"))
        runCatching {
            firestore.collection(RemoteWallpaperRepository.CATEGORIES)
                .document(id)
                .set(mapOf("title" to title))
                .await()
        }.map { }
    }
}

package com.wallora.app.account

import android.app.Activity
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.wallora.app.R
import com.wallora.app.util.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext

/** The signed-in person, or null while the app is used as a guest. */
data class AccountUser(val uid: String, val email: String?, val name: String?, val photoUrl: String?)

/** What a failed sign-in or account action should tell the user. */
enum class AccountError { CANCELLED, NO_ACCOUNT, NETWORK, RECENT_LOGIN, OTHER }

/**
 * Optional Google sign-in. Wallora works fully as a guest; signing in only adds a cloud copy of favourites and
 * albums (see [CloudSync]). The email/password sign-in used by the owner's admin screen is separate: such users
 * are admins, Google users are not (see AdminRepository).
 */
class AccountManager {

    private val auth: FirebaseAuth? = runCatching { FirebaseAuth.getInstance() }.getOrNull()
    private val db: FirebaseFirestore? = runCatching { FirebaseFirestore.getInstance() }.getOrNull()

    private fun toUser(u: com.google.firebase.auth.FirebaseUser?): AccountUser? {
        if (u == null) return null
        // Admin accounts (email/password) are not shown as a member account in the Profile.
        if (u.providerData.none { it.providerId == GoogleAuthProvider.PROVIDER_ID }) return null
        return AccountUser(u.uid, u.email, u.displayName, u.photoUrl?.toString())
    }

    fun current(): AccountUser? = toUser(auth?.currentUser)

    fun state(): Flow<AccountUser?> = callbackFlow {
        val instance = auth
        if (instance == null) {
            trySend(null)
            close()
            return@callbackFlow
        }
        val listener = FirebaseAuth.AuthStateListener { trySend(toUser(it.currentUser)) }
        instance.addAuthStateListener(listener)
        awaitClose { instance.removeAuthStateListener(listener) }
    }

    /** Shows Google's account chooser and signs in to Firebase with the result. */
    suspend fun signInWithGoogle(activity: Activity): Result<AccountUser> {
        val instance = auth ?: return Result.failure(AccountFailure(AccountError.OTHER))
        return try {
            val option = GetGoogleIdOption.Builder()
                .setServerClientId(activity.getString(R.string.default_web_client_id))
                .setFilterByAuthorizedAccounts(false)
                .setAutoSelectEnabled(false)
                .build()
            val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
            val credential = CredentialManager.create(activity).getCredential(activity, request).credential
            if (credential !is CustomCredential ||
                credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                return Result.failure(AccountFailure(AccountError.OTHER))
            }
            val idToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
            val result = withContext(Dispatchers.IO) {
                instance.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null)).await()
            }
            val user = toUser(result.user) ?: return Result.failure(AccountFailure(AccountError.OTHER))
            Result.success(user)
        } catch (e: GetCredentialCancellationException) {
            Result.failure(AccountFailure(AccountError.CANCELLED))
        } catch (e: NoCredentialException) {
            Result.failure(AccountFailure(AccountError.NO_ACCOUNT))
        } catch (e: GetCredentialException) {
            Result.failure(AccountFailure(AccountError.OTHER))
        } catch (e: java.io.IOException) {
            Result.failure(AccountFailure(AccountError.NETWORK))
        } catch (e: Exception) {
            Result.failure(AccountFailure(if (e.cause is java.io.IOException) AccountError.NETWORK else AccountError.OTHER))
        }
    }

    suspend fun signOut(activity: Activity) {
        runCatching { auth?.signOut() }
        runCatching { CredentialManager.create(activity).clearCredentialState(ClearCredentialStateRequest()) }
    }

    /**
     * Deletes the cloud copy and the account itself. Local favourites on this phone stay (the user is back to being
     * a guest). Firebase refuses deletion when the sign-in is old; the caller then asks the user to sign in again.
     */
    suspend fun deleteAccount(activity: Activity): Result<Unit> {
        val user = auth?.currentUser ?: return Result.failure(AccountFailure(AccountError.OTHER))
        return try {
            withContext(Dispatchers.IO) {
                db?.collection("users")?.document(user.uid)?.delete()?.await()
                user.delete().await()
            }
            runCatching { CredentialManager.create(activity).clearCredentialState(ClearCredentialStateRequest()) }
            Result.success(Unit)
        } catch (e: FirebaseAuthRecentLoginRequiredException) {
            Result.failure(AccountFailure(AccountError.RECENT_LOGIN))
        } catch (e: Exception) {
            Result.failure(AccountFailure(if (e is java.io.IOException) AccountError.NETWORK else AccountError.OTHER))
        }
    }
}

class AccountFailure(val error: AccountError) : Exception(error.name)

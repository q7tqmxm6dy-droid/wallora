package com.wallora.app.util

import com.google.android.gms.tasks.Task
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Awaits a Google Play Services [Task] without needing kotlinx-coroutines-play-services. */
suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { continuation ->
    addOnCompleteListener { task ->
        if (task.isSuccessful) {
            continuation.resume(task.result as T)
        } else {
            continuation.resumeWithException(task.exception ?: IllegalStateException("Task failed"))
        }
    }
}

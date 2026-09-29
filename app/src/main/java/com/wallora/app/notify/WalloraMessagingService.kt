package com.wallora.app.notify

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.wallora.app.util.Notifications

/**
 * Receives push messages about newly published wallpapers and shows a notification.
 * (Requires a device with Google Play services; it is a no-op without them.)
 */
class WalloraMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.notification?.title
            ?: message.data["title"]
            ?: "New wallpapers"
        val body = message.notification?.body
            ?: message.data["body"]
            ?: "Fresh wallpapers are available in Wallora"
        Notifications.show(this, title, body)
    }

    override fun onNewToken(token: String) {
        // A real deployment would persist this to Firestore under the device's user.
    }
}

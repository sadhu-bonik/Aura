package com.aura.app.firebase

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * AuraMessagingService — handles FCM token refreshes and incoming data messages.
 *
 * Token storage: When Firebase issues a new token (first install, token rotation),
 * this service saves it to `users/{uid}.fcmToken` so a future Cloud Function can
 * send targeted push notifications.
 *
 * Note: Push notifications won't appear in the system tray without a server/Cloud
 * Function sending them. The Firestore notification inbox (NotificationRepository)
 * is the primary in-app alerting mechanism for this project.
 */
class AuraMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "FCM token refreshed")
        saveTokenToFirestore(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        // Data messages from a future Cloud Function would be handled here.
        Log.d(TAG, "FCM message received from ${message.from}")
    }

    private fun saveTokenToFirestore(token: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .update("fcmToken", token)
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to save FCM token: ${e.message}")
            }
    }

    private companion object {
        const val TAG = "AuraMessagingService"
    }
}

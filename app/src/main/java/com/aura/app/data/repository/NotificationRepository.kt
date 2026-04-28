package com.aura.app.data.repository

import com.aura.app.data.model.Notification
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

/**
 * NotificationRepository — all Firestore I/O for the `notifications` collection.
 *
 * Writes happen client-side at deal lifecycle transitions (send / accept / reject).
 * Reads drive the badge count and inbox sheet in the UI.
 */
class NotificationRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    private val col = firestore.collection(COLLECTION)

    /** Write a new notification for another user. */
    suspend fun createNotification(notif: Notification): Result<Unit> = runCatching {
        val ref = col.document()
        val withId = notif.copy(
            notifId = ref.id,
            createdAt = Timestamp.now(),
            read = false,
        )
        ref.set(withId).await()
    }

    /**
     * Real-time stream of unread notifications count for [userId].
     * Powers the badge on the Deals tab.
     */
    fun getUnreadCount(userId: String): Flow<Int> =
        col.whereEqualTo("recipientId", userId)
            .whereEqualTo("read", false)
            .snapshots()
            .map { it.size() }

    /**
     * Real-time stream of the 50 most recent notifications for [userId],
     * newest first. Powers the notification inbox sheet.
     */
    fun getNotifications(userId: String): Flow<List<Notification>> =
        col.whereEqualTo("recipientId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(50)
            .snapshots()
            .map { snap ->
                snap.documents.mapNotNull {
                    it.toObject(Notification::class.java)?.copy(notifId = it.id)
                }
            }

    /**
     * Create or overwrite a coalesced notification using a stable document ID.
     * Used for chat messages so the inbox shows one entry per deal, not one per message.
     * Each new message resets the timestamp and read flag on the same document.
     */
    suspend fun upsertMessageNotification(notif: Notification, dealId: String, recipientId: String): Result<Unit> = runCatching {
        val stableId = "msg_${recipientId}_$dealId"
        val withId = notif.copy(
            notifId = stableId,
            createdAt = Timestamp.now(),
            read = false,
        )
        col.document(stableId).set(withId).await()
    }

    /** Mark a single notification as read. */
    suspend fun markRead(notifId: String): Result<Unit> = runCatching {
        col.document(notifId).update("read", true).await()
    }

    /**
     * Mark every unread notification for [userId] as read.
     * Called when the user opens the notification inbox.
     */
    suspend fun markAllRead(userId: String): Result<Unit> = runCatching {
        val unread = col.whereEqualTo("recipientId", userId)
            .whereEqualTo("read", false)
            .get()
            .await()

        if (unread.isEmpty) return@runCatching

        firestore.runBatch { batch ->
            unread.documents.forEach { doc ->
                batch.update(doc.reference, "read", true)
            }
        }.await()
    }

    private companion object {
        const val COLLECTION = "notifications"
    }
}

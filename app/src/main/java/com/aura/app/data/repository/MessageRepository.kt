package com.aura.app.data.repository

import com.aura.app.data.model.Message
import com.aura.app.utils.Constants
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class MessageRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    private val messages = firestore.collection(Constants.COLLECTION_MESSAGES)
    private val deals = firestore.collection(Constants.COLLECTION_DEALS)

    fun streamMessages(dealId: String): Flow<List<Message>> =
        messages.whereEqualTo("dealId", dealId)
            .snapshots()
            .map { snap ->
                snap.documents
                    .mapNotNull { it.toObject(Message::class.java)?.copy(messageId = it.id) }
                    .sortedBy { it.sentAt }
            }

    fun streamLastMessage(dealId: String): Flow<Message?> =
        messages.whereEqualTo("dealId", dealId)
            .snapshots()
            .map { snap ->
                snap.documents
                    .mapNotNull { it.toObject(Message::class.java)?.copy(messageId = it.id) }
                    .maxByOrNull { it.sentAt?.seconds ?: 0L }
            }

    fun streamUnreadCount(dealId: String, receiverId: String): Flow<Int> =
        messages.whereEqualTo("dealId", dealId)
            .whereEqualTo("receiverId", receiverId)
            .whereEqualTo("isRead", false)
            .snapshots()
            .map { it.size() }

    suspend fun sendMessage(
        dealId: String,
        senderId: String,
        receiverId: String,
        content: String,
        mediaUrl: String = "",
    ): Result<Unit> = runCatching {
        val dealSnap = deals.document(dealId).get().await()
        val chatUnlocked = dealSnap.getBoolean("chatUnlocked") ?: false
        check(chatUnlocked) { "Chat is locked — deal must be accepted first" }

        val ref = messages.document()
        ref.set(
            Message(
                messageId = ref.id,
                dealId = dealId,
                senderId = senderId,
                receiverId = receiverId,
                content = content,
                mediaUrl = mediaUrl,
                mediaType = "text",
                isRead = false,
                sentAt = Timestamp.now(),
            )
        ).await()
    }

    suspend fun sendMessageDirect(
        dealId: String,
        senderId: String,
        receiverId: String,
        content: String,
    ): Result<Unit> = runCatching {
        val ref = messages.document()
        ref.set(
            Message(
                messageId = ref.id,
                dealId = dealId,
                senderId = senderId,
                receiverId = receiverId,
                content = content,
                mediaType = "text",
                isRead = false,
                sentAt = Timestamp.now(),
            )
        ).await()
    }

    suspend fun sendMediaMessage(
        dealId: String,
        senderId: String,
        receiverId: String,
        downloadUrl: String,
        mediaType: String,
        fileName: String = "",
    ): Result<Unit> = runCatching {
        val ref = messages.document()
        ref.set(
            Message(
                messageId = ref.id,
                dealId = dealId,
                senderId = senderId,
                receiverId = receiverId,
                content = "",
                mediaUrl = downloadUrl,
                mediaType = mediaType,
                fileName = fileName,
                isRead = false,
                sentAt = Timestamp.now(),
            )
        ).await()
    }

    suspend fun getSharedMedia(dealId: String): List<Message> = runCatching {
        messages.whereEqualTo("dealId", dealId)
            .get()
            .await()
            .documents
            .mapNotNull { it.toObject(Message::class.java)?.copy(messageId = it.id) }
            .filter { it.mediaUrl.isNotEmpty() }
            .sortedBy { it.sentAt }
    }.getOrDefault(emptyList())

    suspend fun markMessagesAsRead(dealId: String, currentUserId: String): Result<Unit> = runCatching {
        val unread = messages
            .whereEqualTo("dealId", dealId)
            .whereEqualTo("receiverId", currentUserId)
            .whereEqualTo("isRead", false)
            .get()
            .await()

        val batch = firestore.batch()
        unread.documents.forEach { batch.update(it.reference, "isRead", true) }
        batch.commit().await()
    }
}

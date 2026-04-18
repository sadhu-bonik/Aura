package com.aura.app.data.model

import com.google.firebase.Timestamp

data class Message(
    val messageId: String = "",
    val dealId: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val content: String = "",
    val mediaUrl: String = "",
    val mediaType: String = "",  // "", "image", "video", "file"
    val fileName: String = "",   // populated for mediaType == "file"
    val isRead: Boolean = false,
    val sentAt: Timestamp? = null,
)

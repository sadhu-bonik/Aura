package com.aura.app.data.model

import com.google.firebase.Timestamp

data class Deal(
    val dealId: String = "",
    val brandId: String = "",
    val creatorId: String = "",
    val campaignId: String = "",
    val title: String = "",
    val description: String = "",
    val budget: Long = 0L,
    val status: String = "",
    val chatUnlocked: Boolean = false,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
    val completedAt: Timestamp? = null,
    val lastMessageText: String = "",
    val lastMessageTime: Timestamp? = null,
    val unreadCounts: Map<String, Long> = emptyMap(),
    val completionRequestedBy: String = "",
    val cancelRequestedBy: String = "",
    val cancelledBy: String = "",
    val cancelReason: String = "",
    val creatorReviewedAt: com.google.firebase.Timestamp? = null,
    val brandReviewedAt: com.google.firebase.Timestamp? = null,
) {
    fun isClosureReviewPending(): Boolean =
        status == com.aura.app.utils.Constants.STATUS_ACCEPTED &&
                (completionRequestedBy.isNotBlank() || cancelRequestedBy.isNotBlank())

    fun hasUserReviewed(userId: String): Boolean =
        if (userId == creatorId) creatorReviewedAt != null else brandReviewedAt != null

    fun areBothPartiesReviewed(): Boolean =
        creatorReviewedAt != null && brandReviewedAt != null
}

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
    val brandCloseConfirmed: Boolean = false,
    val creatorCloseConfirmed: Boolean = false,
    val creatorReviewedAt: com.google.firebase.Timestamp? = null,
    val brandReviewedAt: com.google.firebase.Timestamp? = null,
) {
    /** True when both parties have independently confirmed completion and we're waiting on reviews. */
    fun bothCloseConfirmed(): Boolean = brandCloseConfirmed && creatorCloseConfirmed

    /** True when at least one party has confirmed close but the other hasn't yet. */
    fun closurePartiallyRequested(): Boolean =
        (brandCloseConfirmed || creatorCloseConfirmed) && !bothCloseConfirmed()

    fun userHasConfirmedClose(userId: String): Boolean =
        if (userId == creatorId) creatorCloseConfirmed else brandCloseConfirmed

    /** Active deal in the "both confirmed close, waiting on reviews" phase — keeps it on Active list. */
    fun isClosureReviewPending(): Boolean =
        status == com.aura.app.utils.Constants.STATUS_ACCEPTED && bothCloseConfirmed()

    fun hasUserReviewed(userId: String): Boolean =
        if (userId == creatorId) creatorReviewedAt != null else brandReviewedAt != null

    fun areBothPartiesReviewed(): Boolean =
        creatorReviewedAt != null && brandReviewedAt != null
}

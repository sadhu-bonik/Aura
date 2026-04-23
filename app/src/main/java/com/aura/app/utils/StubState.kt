package com.aura.app.utils

import com.aura.app.data.model.Deal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Date

object StubState {
    private val _deals = MutableStateFlow(StubData.deals)
    val dealsFlow: StateFlow<List<Deal>> = _deals.asStateFlow()

    fun currentDeals(): List<Deal> = _deals.value

    fun updateDealStatus(dealId: String, status: String, chatUnlocked: Boolean) {
        _deals.value = _deals.value.map { deal ->
            if (deal.dealId == dealId) {
                deal.copy(
                    status = status,
                    chatUnlocked = chatUnlocked,
                    completionRequestedBy = "",
                )
            } else deal
        }
    }

    fun requestCompletion(dealId: String, initiatorId: String) {
        _deals.value = _deals.value.map { deal ->
            if (deal.dealId == dealId) deal.copy(completionRequestedBy = initiatorId) else deal
        }
    }

    fun clearCompletionRequest(dealId: String) {
        _deals.value = _deals.value.map { deal ->
            if (deal.dealId == dealId) deal.copy(completionRequestedBy = "") else deal
        }
    }

    fun cancelDeal(dealId: String, cancelledBy: String, reason: String) {
        _deals.value = _deals.value.map { deal ->
            if (deal.dealId == dealId) {
                deal.copy(
                    status = Constants.STATUS_CANCELLED,
                    chatUnlocked = true,
                    cancelledBy = cancelledBy,
                    cancelReason = reason,
                    completionRequestedBy = "",
                )
            } else deal
        }
    }

    fun updateLastMessage(dealId: String, text: String, time: com.google.firebase.Timestamp) {
        _deals.value = _deals.value.map { deal ->
            if (deal.dealId == dealId) {
                deal.copy(
                    lastMessageText = text,
                    lastMessageTime = time,
                    updatedAt = time,
                )
            } else deal
        }
    }

    fun clearUnreadCount(dealId: String, currentUserId: String) {
        _deals.value = _deals.value.map { deal ->
            if (deal.dealId == dealId) {
                val newCounts = deal.unreadCounts.toMutableMap()
                newCounts[currentUserId] = 0
                deal.copy(unreadCounts = newCounts)
            } else deal
        }
    }

    val stubReviews = MutableStateFlow<List<com.aura.app.data.model.Review>>(emptyList())

    fun addReview(review: com.aura.app.data.model.Review) {
        stubReviews.value = stubReviews.value + review

        // Immediately update the stub creator's aggregate fields
        val allForReviewee = stubReviews.value.filter { it.revieweeId == review.revieweeId }
        val newAvg = allForReviewee.map { it.rating }.average()
        val newCount = allForReviewee.size.toLong()
        StubData.updateCreatorRating(review.revieweeId, newAvg, newCount)
    }

    fun markUserReviewed(dealId: String, userId: String) {
        _deals.value = _deals.value.map { deal ->
            if (deal.dealId == dealId) {
                val now = com.google.firebase.Timestamp.now()
                if (deal.creatorId == userId) deal.copy(creatorReviewedAt = now)
                else deal.copy(brandReviewedAt = now)
            } else deal
        }
    }

    fun expireStaleDeals() {
        val sevenDaysMs = 7L * 24 * 60 * 60 * 1000
        _deals.value = _deals.value.map { deal ->
            if (deal.status != Constants.STATUS_PENDING) return@map deal
            val createdAt = deal.createdAt?.toDate() ?: return@map deal
            if (Date().time - createdAt.time >= sevenDaysMs) deal.copy(status = Constants.STATUS_EXPIRED)
            else deal
        }
    }

    fun updateReviewComment(reviewId: String, comment: String) {
        stubReviews.value = stubReviews.value.map { r ->
            if (r.reviewId == reviewId) r.copy(comment = comment) else r
        }
    }
}

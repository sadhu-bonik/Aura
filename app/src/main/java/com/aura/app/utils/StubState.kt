package com.aura.app.utils

import com.aura.app.data.model.Deal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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
}

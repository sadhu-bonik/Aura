package com.aura.app.ui.chat

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aura.app.data.model.Deal
import com.aura.app.data.model.Review
import com.aura.app.data.repository.DealRepository
import com.aura.app.data.repository.ReviewRepository
import com.aura.app.utils.Constants
import com.aura.app.utils.StubSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ReviewViewModel(application: Application) : AndroidViewModel(application) {
    private val reviewRepo = ReviewRepository(application)
    private val dealRepo = DealRepository()

    private val _reviewsByDealId = MutableStateFlow<Map<String, Review>>(emptyMap())
    val reviewsByDealId: StateFlow<Map<String, Review>> = _reviewsByDealId.asStateFlow()

    private val _pendingReviewDeal = MutableStateFlow<Deal?>(null)
    val pendingReviewDeal: StateFlow<Deal?> = _pendingReviewDeal.asStateFlow()

    private val shownDealIds = mutableSetOf<String>()

    init {
        viewModelScope.launch {
            val currentUserId = StubSession.userId()
            if (currentUserId.isNotEmpty()) {
                reviewRepo.streamMyReviews(currentUserId).collect { map ->
                    _reviewsByDealId.value = map
                    checkPendingDeals(currentUserId, map)
                }
            }
        }
    }

    private fun checkPendingDeals(currentUserId: String, reviewsMap: Map<String, Review>) {
        viewModelScope.launch {
            val isCreator = StubSession.role() == Constants.ROLE_CREATOR
            val dealsFlow = if (isCreator) {
                dealRepo.getDealsForCreator(currentUserId)
            } else {
                dealRepo.getDealsForBrand(currentUserId)
            }

            dealsFlow.collectLatest { deals ->
                val pendingDeal = deals.firstOrNull { deal ->
                    deal.status == Constants.STATUS_COMPLETED &&
                            !reviewsMap.containsKey(deal.dealId) &&
                            !shownDealIds.contains(deal.dealId)
                }
                _pendingReviewDeal.value = pendingDeal
            }
        }
    }

    fun submitRating(dealId: String, revieweeId: String, rating: Double): StateFlow<Result<String>?> {
        val resultFlow = MutableStateFlow<Result<String>?>(null)
        viewModelScope.launch {
            val currentUserId = StubSession.userId()
            if (currentUserId.isNotEmpty()) {
                val review = Review(
                    dealId = dealId,
                    reviewerId = currentUserId,
                    revieweeId = revieweeId,
                    rating = rating,
                    comment = "",
                    createdAt = com.google.firebase.Timestamp.now()
                )
                resultFlow.value = reviewRepo.createReview(review)
            } else {
                resultFlow.value = Result.failure(Exception("User not found"))
            }
        }
        return resultFlow
    }

    fun submitComment(reviewId: String, dealId: String, comment: String): StateFlow<Result<Unit>?> {
        val resultFlow = MutableStateFlow<Result<Unit>?>(null)
        viewModelScope.launch {
            val result = if (comment.isNotBlank()) {
                reviewRepo.updateReviewComment(reviewId, comment)
            } else {
                Result.success(Unit)
            }
            if (result.isSuccess) {
                val userId = StubSession.userId()
                if (Constants.USE_STUBS) {
                    com.aura.app.utils.StubState.markUserReviewed(dealId, userId)
                } else {
                    dealRepo.markUserReviewed(dealId, userId)
                }
            }
            resultFlow.value = result
        }
        return resultFlow
    }

    fun markReviewPromptShown(dealId: String) {
        shownDealIds.add(dealId)
        if (_pendingReviewDeal.value?.dealId == dealId) {
            _pendingReviewDeal.value = null
        }
    }
}

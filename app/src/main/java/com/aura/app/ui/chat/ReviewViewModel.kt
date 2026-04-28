package com.aura.app.ui.chat

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aura.app.data.model.Deal
import com.aura.app.data.model.Review
import com.aura.app.data.repository.AuthRepository
import com.aura.app.data.repository.DealRepository
import com.aura.app.data.repository.ReviewRepository
import com.aura.app.data.repository.UserRepository
import com.aura.app.utils.Constants
import com.aura.app.utils.StubSession
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ReviewViewModel(application: Application) : AndroidViewModel(application) {
    private val reviewRepo = ReviewRepository(application)
    private val dealRepo = DealRepository()
    private val userRepo = UserRepository()
    private val authRepo = AuthRepository()
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    // Real auth uid in non-stub mode; falls back to the stub session id otherwise.
    private fun currentUserId(): String =
        if (Constants.USE_STUBS) StubSession.userId()
        else authRepo.currentUser?.uid.orEmpty()

    private val _reviewsByDealId = MutableStateFlow<Map<String, Review>>(emptyMap())
    val reviewsByDealId: StateFlow<Map<String, Review>> = _reviewsByDealId.asStateFlow()

    private val _pendingReviewDeal = MutableStateFlow<Deal?>(null)
    val pendingReviewDeal: StateFlow<Deal?> = _pendingReviewDeal.asStateFlow()

    private val shownDealIds = mutableSetOf<String>()

    private var boundUid: String? = null
    private var reviewsJob: Job? = null
    private var pendingJob: Job? = null

    private val authListener = FirebaseAuth.AuthStateListener { fa ->
        rebind(fa.currentUser?.uid.orEmpty().ifEmpty { if (Constants.USE_STUBS) StubSession.userId() else "" })
    }

    init {
        auth.addAuthStateListener(authListener)
        rebind(currentUserId())
    }

    /**
     * Re-attach Firestore listeners to the current user. Cancels any prior collectors
     * so a previous user's reviews never leak into the new session.
     */
    private fun rebind(uid: String) {
        if (uid == boundUid) return
        boundUid = uid

        reviewsJob?.cancel()
        pendingJob?.cancel()
        _reviewsByDealId.value = emptyMap()
        _pendingReviewDeal.value = null
        shownDealIds.clear()

        if (uid.isBlank()) return

        reviewsJob = viewModelScope.launch {
            reviewRepo.streamMyReviews(uid).collect { map ->
                _reviewsByDealId.value = map
                checkPendingDeals(uid, map)
            }
        }
    }

    private fun checkPendingDeals(currentUserId: String, reviewsMap: Map<String, Review>) {
        pendingJob?.cancel()
        pendingJob = viewModelScope.launch {
            val role = if (Constants.USE_STUBS) {
                StubSession.role()
            } else {
                userRepo.getUserProfile(currentUserId)?.role.orEmpty()
            }
            val isCreator = role == Constants.ROLE_CREATOR
            val dealsFlow = if (isCreator) {
                dealRepo.getDealsForCreator(currentUserId)
            } else {
                dealRepo.getDealsForBrand(currentUserId)
            }

            dealsFlow.collectLatest { deals ->
                val pendingDeal = deals.firstOrNull { deal ->
                    isReviewRequired(deal) &&
                            !reviewsMap.containsKey(deal.dealId) &&
                            (deal.isClosureReviewPending() || deal.status == Constants.STATUS_CANCELLED || !shownDealIds.contains(deal.dealId))
                }
                _pendingReviewDeal.value = pendingDeal
            }
        }
    }

    private fun isReviewRequired(deal: Deal): Boolean =
        deal.status in setOf(Constants.STATUS_COMPLETED, Constants.STATUS_CANCELLED) ||
                deal.isClosureReviewPending()

    override fun onCleared() {
        auth.removeAuthStateListener(authListener)
        super.onCleared()
    }

    fun submitRating(dealId: String, _revieweeId: String, rating: Double): StateFlow<Result<String>?> {
        val resultFlow = MutableStateFlow<Result<String>?>(null)
        viewModelScope.launch {
            val currentUserId = currentUserId()
            if (currentUserId.isEmpty()) {
                resultFlow.value = Result.failure(Exception("User not found"))
                return@launch
            }

            // Look up role + display-name metadata so the review is self-contained
            // (the reviews list screen can render without an extra profile fetch per row).
            val reviewer = userRepo.getUserProfile(currentUserId)
            val deal = dealRepo.getDeal(dealId).getOrNull()
            if (deal == null) {
                resultFlow.value = Result.failure(Exception("Deal not found"))
                return@launch
            }
            if (!isReviewRequired(deal)) {
                resultFlow.value = Result.failure(Exception("Reviews can only be submitted when completion or cancellation is pending"))
                return@launch
            }

            val actualRevieweeId = when (currentUserId) {
                deal.brandId -> deal.creatorId
                deal.creatorId -> deal.brandId
                else -> {
                    resultFlow.value = Result.failure(Exception("User is not part of this deal"))
                    return@launch
                }
            }
            val actualRevieweeRole = if (actualRevieweeId == deal.creatorId) {
                Constants.ROLE_CREATOR
            } else {
                Constants.ROLE_BRAND
            }
            val actualReviewerRole = if (currentUserId == deal.creatorId) {
                Constants.ROLE_CREATOR
            } else {
                Constants.ROLE_BRAND
            }
            val reviewee = userRepo.getUserProfile(actualRevieweeId)

            val review = Review(
                dealId = dealId,
                dealTitle = deal.title,
                reviewerId = currentUserId,
                reviewerRole = reviewer?.role.orEmpty().ifBlank { actualReviewerRole },
                reviewerDisplayName = reviewer?.displayName.orEmpty(),
                reviewerPhotoUrl = reviewer?.profileImageUrl.orEmpty(),
                revieweeId = actualRevieweeId,
                revieweeRole = reviewee?.role.orEmpty().ifBlank { actualRevieweeRole },
                revieweeDisplayName = reviewee?.displayName.orEmpty(),
                rating = rating,
                comment = "",
                createdAt = com.google.firebase.Timestamp.now(),
            )
            val result = reviewRepo.createReview(review)
            if (result.isSuccess) {
                if (Constants.USE_STUBS) {
                    com.aura.app.utils.StubState.markUserReviewed(dealId, currentUserId)
                } else {
                    dealRepo.markUserReviewed(dealId, currentUserId)
                }
            }
            resultFlow.value = result
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
            resultFlow.value = result
        }
        return resultFlow
    }

    fun markReviewPromptShown(dealId: String) {
        val pendingDeal = _pendingReviewDeal.value
        if (pendingDeal?.dealId == dealId &&
            (pendingDeal.isClosureReviewPending() || pendingDeal.status == Constants.STATUS_CANCELLED)
        ) {
            return
        }
        shownDealIds.add(dealId)
        if (pendingDeal?.dealId == dealId) {
            _pendingReviewDeal.value = null
        }
    }
}

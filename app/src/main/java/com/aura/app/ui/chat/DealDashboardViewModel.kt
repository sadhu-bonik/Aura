package com.aura.app.ui.chat

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.aura.app.data.model.Deal
import com.aura.app.data.model.UserLite
import com.aura.app.data.repository.AuthRepository
import com.aura.app.data.repository.DealRepository
import com.aura.app.data.repository.UserRepository
import com.aura.app.utils.Constants
import com.aura.app.utils.SessionManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

data class DealOfferItem(
    val deal: Deal,
    val otherUser: UserLite?,
)

class DealDashboardViewModel(
    private val dealRepository: DealRepository = DealRepository(),
    private val userRepository: UserRepository = UserRepository(),
    private val authRepository: AuthRepository = AuthRepository(),
    private val sessionManager: SessionManager? = null
) : ViewModel() {

    private val _activeDeals = MutableLiveData<List<ActiveDealItem>>(emptyList())
    val activeDeals: LiveData<List<ActiveDealItem>> = _activeDeals

    private val _newDeals = MutableLiveData<List<DealOfferItem>>(emptyList())
    val newDeals: LiveData<List<DealOfferItem>> = _newDeals

    private val _completedDeals = MutableLiveData<List<DealOfferItem>>(emptyList())
    val completedDeals: LiveData<List<DealOfferItem>> = _completedDeals

    private val _pastDeals = MutableLiveData<List<DealOfferItem>>(emptyList())
    val pastDeals: LiveData<List<DealOfferItem>> = _pastDeals

    private val _hasNewPendingForCreator = MutableLiveData(false)
    val hasNewPendingForCreator: LiveData<Boolean> = _hasNewPendingForCreator

    private val _userRole = MutableLiveData<String?>(null)
    val userRole: LiveData<String?> = _userRole

    private val _isLoading = MutableLiveData(true)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    private val _acceptEvent = MutableLiveData<String?>()
    val acceptEvent: LiveData<String?> = _acceptEvent

    private var loadJob: Job? = null

    init {
        load()
    }

    fun load() {
        loadJob?.cancel()
        val userId = resolveUserId()
        if (userId == null) {
            _isLoading.value = false
            _error.value = "No signed-in user found."
            return
        }
        _isLoading.value = true
        _error.value = null

        loadJob = viewModelScope.launch {
            // Get user profile to determine role
            val user = userRepository.getUserProfile(userId)
            val role = user?.role ?: Constants.ROLE_CREATOR
            _userRole.value = role

            val flow = if (role == Constants.ROLE_CREATOR) {
                dealRepository.getDealsForCreator(userId)
            } else {
                dealRepository.getDealsForBrand(userId)
            }

            flow.catch { 
                android.util.Log.e("DealDashboardVM", "Error loading deals", it)
                _error.value = it.message ?: "Failed to load deals."
                _isLoading.value = false 
            }.collect { deals ->
                partition(deals, userId, role)
                _isLoading.value = false
            }
        }
    }

    private fun resolveUserId(): String? =
        authRepository.currentUser?.uid ?: sessionManager?.getUserId()

    fun acceptDeal(dealId: String) {
        viewModelScope.launch {
            dealRepository.acceptDeal(dealId)
                .onSuccess { _acceptEvent.value = dealId }
        }
    }

    fun rejectDeal(dealId: String) {
        viewModelScope.launch {
            dealRepository.rejectDeal(dealId)
        }
    }

    fun consumeAcceptEvent() {
        _acceptEvent.value = null
    }

    private suspend fun partition(deals: List<Deal>, userId: String, role: String) {
        val active = mutableListOf<ActiveDealItem>()
        val new = mutableListOf<DealOfferItem>()
        val completed = mutableListOf<DealOfferItem>()
        val past = mutableListOf<DealOfferItem>()

        deals.forEach { deal ->
            val otherUserId = if (role == Constants.ROLE_CREATOR) deal.brandId else deal.creatorId
            val otherUser = userRepository.getUserLite(otherUserId)
            when {
                Constants.canSendChatMessage(deal) -> {
                    val unread = (deal.unreadCounts[userId] ?: 0L).toInt()
                    active.add(
                        ActiveDealItem(
                            deal = deal,
                            otherUser = otherUser,
                            unreadCount = unread,
                            needsReview = deal.isClosureReviewPending() && !deal.hasUserReviewed(userId),
                        )
                    )
                }
                deal.status == Constants.STATUS_PENDING ->
                    new.add(DealOfferItem(deal, otherUser))
                // Completed deals always belong in the History/Completed bucket — they no longer
                // appear under Active, even if the current user hasn't submitted their review yet.
                // The review prompt lives on the chat thread's closed-state card and on the
                // REVIEW_REQUESTED notification.
                deal.status == Constants.STATUS_COMPLETED ->
                    completed.add(DealOfferItem(deal, otherUser))
                deal.status in listOf(
                    Constants.STATUS_REJECTED,
                    Constants.STATUS_CANCELLED,
                    Constants.STATUS_EXPIRED
                ) -> past.add(DealOfferItem(deal, otherUser))
            }
        }

        _activeDeals.value = active.sortedByDescending {
            it.deal.lastMessageTime?.seconds ?: it.deal.updatedAt?.seconds ?: 0L
        }
        _newDeals.value = new.sortedByDescending { it.deal.createdAt?.seconds ?: 0L }
        _completedDeals.value = completed.sortedByDescending { it.deal.completedAt?.seconds ?: 0L }
        _pastDeals.value = past.sortedByDescending { it.deal.updatedAt?.seconds ?: 0L }
        _hasNewPendingForCreator.value = new.isNotEmpty() && role == Constants.ROLE_CREATOR
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            DealDashboardViewModel(
                sessionManager = SessionManager(context.applicationContext)
            ) as T
    }
}

package com.aura.app.ui.chat

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.aura.app.data.model.Deal
import com.aura.app.data.model.UserLite
import com.aura.app.data.repository.DealRepository
import com.aura.app.data.repository.UserRepository
import com.aura.app.utils.Constants
import com.aura.app.utils.SessionManager
import com.aura.app.utils.StubSession
import com.aura.app.utils.StubState
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
    private val sessionManager: SessionManager? = null,
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

    private val _isLoading = MutableLiveData(true)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _acceptEvent = MutableLiveData<String?>()
    val acceptEvent: LiveData<String?> = _acceptEvent

    private var loadJob: Job? = null

    init {
        load()
    }

    fun load() {
        loadJob?.cancel()
        val userId = StubSession.userId()
        val role = StubSession.role()
        _isLoading.value = true

        if (Constants.USE_STUBS) {
            StubState.expireStaleDeals()
            loadJob = viewModelScope.launch {
                StubState.dealsFlow.collect { deals ->
                    val filtered = deals.filter { it.creatorId == userId || it.brandId == userId }
                    partition(filtered, userId, role)
                    _isLoading.value = false
                }
            }
            return
        }

        loadJob = viewModelScope.launch {
            val flow = if (role == Constants.ROLE_CREATOR) {
                dealRepository.getDealsForCreator(userId)
            } else {
                dealRepository.getDealsForBrand(userId)
            }

            flow.catch { _isLoading.value = false }
                .collect { deals ->
                    partition(deals, userId, role)
                    _isLoading.value = false
                }
        }
    }

    fun acceptDeal(dealId: String) {
        viewModelScope.launch {
            if (Constants.USE_STUBS) {
                StubState.updateDealStatus(dealId, Constants.STATUS_ACCEPTED, chatUnlocked = true)
                _acceptEvent.value = dealId
            } else {
                dealRepository.acceptDeal(dealId)
                    .onSuccess { _acceptEvent.value = dealId }
            }
        }
    }

    fun rejectDeal(dealId: String) {
        viewModelScope.launch {
            if (Constants.USE_STUBS) {
                StubState.updateDealStatus(dealId, Constants.STATUS_REJECTED, chatUnlocked = false)
            } else {
                dealRepository.rejectDeal(dealId)
            }
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
                deal.status == Constants.STATUS_ACCEPTED && deal.chatUnlocked -> {
                    val unread = (deal.unreadCounts[userId] ?: 0L).toInt()
                    active.add(ActiveDealItem(deal, otherUser, unread))
                }
                deal.status == Constants.STATUS_PENDING ->
                    new.add(DealOfferItem(deal, otherUser))
                deal.status == Constants.STATUS_COMPLETED -> {
                    val hasReviewed = if (userId == deal.creatorId) deal.creatorReviewedAt != null
                                      else deal.brandReviewedAt != null
                    if (hasReviewed) completed.add(DealOfferItem(deal, otherUser))
                    else {
                        val unread = (deal.unreadCounts[userId] ?: 0L).toInt()
                        active.add(ActiveDealItem(deal, otherUser, unread))
                    }
                }
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
}

package com.aura.app.ui.chat

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aura.app.data.model.Deal
import com.aura.app.data.repository.DealRepository
import com.aura.app.data.repository.UserRepository
import com.aura.app.utils.Constants
import com.aura.app.utils.StubSession
import com.aura.app.utils.StubState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class DealHistoryViewModel(
    private val dealRepository: DealRepository = DealRepository(),
    private val userRepository: UserRepository = UserRepository(),
) : ViewModel() {

    private val _completedDeals = MutableLiveData<List<DealOfferItem>>(emptyList())
    val completedDeals: LiveData<List<DealOfferItem>> = _completedDeals

    private val _pastDeals = MutableLiveData<List<DealOfferItem>>(emptyList())
    val pastDeals: LiveData<List<DealOfferItem>> = _pastDeals

    private val _isLoading = MutableLiveData(true)
    val isLoading: LiveData<Boolean> = _isLoading

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
            loadJob = viewModelScope.launch {
                StubState.dealsFlow.collect { deals ->
                    val filtered = deals.filter { it.creatorId == userId || it.brandId == userId }
                    partition(filtered, role)
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
                    partition(deals, role)
                    _isLoading.value = false
                }
        }
    }

    private suspend fun partition(deals: List<Deal>, role: String) {
        val completed = mutableListOf<DealOfferItem>()
        val past = mutableListOf<DealOfferItem>()

        deals.forEach { deal ->
            val otherUserId = if (role == Constants.ROLE_CREATOR) deal.brandId else deal.creatorId
            val otherUser = userRepository.getUserLite(otherUserId)
            when (deal.status) {
                Constants.STATUS_COMPLETED -> completed.add(DealOfferItem(deal, otherUser))
                Constants.STATUS_REJECTED,
                Constants.STATUS_CANCELLED,
                Constants.STATUS_EXPIRED -> past.add(DealOfferItem(deal, otherUser))
            }
        }

        _completedDeals.value = completed.sortedByDescending { it.deal.completedAt?.seconds ?: 0L }
        _pastDeals.value = past.sortedByDescending { it.deal.updatedAt?.seconds ?: 0L }
    }
}

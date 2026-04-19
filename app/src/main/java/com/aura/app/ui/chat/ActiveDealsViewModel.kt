package com.aura.app.ui.chat

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aura.app.data.repository.DealRepository
import com.aura.app.data.repository.UserRepository
import com.aura.app.utils.Constants
import com.aura.app.utils.StubSession
import com.aura.app.utils.StubState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class ActiveDealsViewModel(
    private val dealRepository: DealRepository = DealRepository(),
    private val userRepository: UserRepository = UserRepository(),
) : ViewModel() {

    private val _items = MutableLiveData<List<ActiveDealItem>>()
    val items: LiveData<List<ActiveDealItem>> = _items

    private val _isLoading = MutableLiveData(true)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private var loadJob: Job? = null

    fun load(userId: String, role: String) {
        loadJob?.cancel()
        _isLoading.value = true
        _error.value = null

        if (Constants.USE_STUBS) {
            loadJob = viewModelScope.launch {
                StubState.dealsFlow.collect { deals ->
                    val activeDeals = deals.filter {
                        it.chatUnlocked && it.status == Constants.STATUS_ACCEPTED &&
                        (it.creatorId == userId || it.brandId == userId)
                    }
                    val items = activeDeals.map { deal ->
                        val otherUserId = if (role == Constants.ROLE_CREATOR) deal.brandId else deal.creatorId
                        val otherUser = userRepository.getUserLite(otherUserId)
                        val unreadCount = (deal.unreadCounts[userId] ?: 0L).toInt()
                        ActiveDealItem(deal, otherUser, unreadCount)
                    }.sortedByDescending {
                        it.deal.lastMessageTime?.seconds ?: it.deal.updatedAt?.seconds ?: 0L
                    }
                    _items.value = items
                    _isLoading.value = false
                }
            }
            return
        }

        loadJob = viewModelScope.launch {
            val dealsFlow = if (role == Constants.ROLE_CREATOR) {
                dealRepository.getDealsForCreator(userId)
            } else {
                dealRepository.getDealsForBrand(userId)
            }

            dealsFlow
                .catch { e ->
                    _error.value = e.message
                    _isLoading.value = false
                }
                .collect { deals ->
                    val activeDeals = deals.filter {
                        it.chatUnlocked && it.status == Constants.STATUS_ACCEPTED
                    }
                    val items = activeDeals.map { deal ->
                        val otherUserId = if (role == Constants.ROLE_CREATOR) deal.brandId else deal.creatorId
                        val otherUser = userRepository.getUserLite(otherUserId)
                        val unreadCount = (deal.unreadCounts[userId] ?: 0L).toInt()
                        ActiveDealItem(deal, otherUser, unreadCount)
                    }.sortedByDescending {
                        it.deal.lastMessageTime?.seconds ?: it.deal.updatedAt?.seconds ?: 0L
                    }
                    _items.value = items
                    _isLoading.value = false
                }
        }
    }
}

package com.aura.app.ui.chat

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aura.app.data.model.Deal
import com.aura.app.data.repository.DealRepository
import com.aura.app.data.repository.UserRepository
import com.aura.app.utils.Constants
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

    private val authRepository = com.aura.app.data.repository.AuthRepository()

    private val _userRole = MutableLiveData<String?>(null)
    val userRole: LiveData<String?> = _userRole

    private val _isLoading = MutableLiveData(true)
    val isLoading: LiveData<Boolean> = _isLoading

    private var loadJob: Job? = null

    init {
        load()
    }

    fun load() {
        loadJob?.cancel()
        val userId = authRepository.currentUser?.uid ?: return
        _isLoading.value = true

        loadJob = viewModelScope.launch {
            val user = userRepository.getUserProfile(userId)
            val role = user?.role ?: Constants.ROLE_CREATOR
            _userRole.value = role

            val flow = if (role == Constants.ROLE_CREATOR) {
                dealRepository.getDealsForCreator(userId)
            } else {
                dealRepository.getDealsForBrand(userId)
            }

            flow.catch { 
                android.util.Log.e("DealHistoryVM", "Error loading history", it)
                _isLoading.value = false 
            }.collect { deals ->
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

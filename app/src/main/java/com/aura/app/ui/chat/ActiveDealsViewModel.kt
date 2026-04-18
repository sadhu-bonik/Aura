package com.aura.app.ui.chat

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aura.app.data.repository.DealRepository
import com.aura.app.data.repository.MessageRepository
import com.aura.app.data.repository.UserRepository
import com.aura.app.utils.Constants
import com.aura.app.utils.StubData
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class ActiveDealsViewModel(
    private val dealRepository: DealRepository = DealRepository(),
    private val messageRepository: MessageRepository = MessageRepository(),
    private val userRepository: UserRepository = UserRepository(),
) : ViewModel() {

    private val _items = MutableLiveData<List<ActiveDealItem>>()
    val items: LiveData<List<ActiveDealItem>> = _items

    private val _isLoading = MutableLiveData(true)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun load(userId: String, role: String) {
        _isLoading.value = true
        _error.value = null

        if (Constants.USE_STUBS) {
            val activeDeals = StubData.deals.filter {
                it.chatUnlocked && it.status == Constants.STATUS_ACCEPTED
            }
            viewModelScope.launch {
                val itemFlows = activeDeals.map { deal ->
                    val otherUserId = if (role == Constants.ROLE_CREATOR) deal.brandId else deal.creatorId
                    val otherUser = userRepository.getUserLite(otherUserId)
                    combine(
                        messageRepository.streamLastMessage(deal.dealId),
                        messageRepository.streamUnreadCount(deal.dealId, userId),
                    ) { last, unread ->
                        ActiveDealItem(deal, otherUser, last, unread)
                    }
                }

                if (itemFlows.isEmpty()) {
                    _items.value = emptyList()
                    _isLoading.value = false
                    return@launch
                }

                combine(itemFlows) { it.toList() }
                    .catch { e ->
                        _error.value = e.message
                        _isLoading.value = false
                    }
                    .collect { list ->
                        _items.value = list.sortedByDescending {
                            it.lastMessage?.sentAt?.seconds ?: it.deal.updatedAt?.seconds ?: 0L
                        }
                        _isLoading.value = false
                    }
            }
            return
        }

        viewModelScope.launch {
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
                        ActiveDealItem(deal, otherUser, null, 0)
                    }
                    _items.value = items
                    _isLoading.value = false
                }
        }
    }
}

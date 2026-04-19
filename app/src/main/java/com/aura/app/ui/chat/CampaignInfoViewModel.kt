package com.aura.app.ui.chat

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aura.app.data.model.Deal
import com.aura.app.data.model.Message
import com.aura.app.data.model.UserLite
import com.aura.app.data.repository.DealRepository
import com.aura.app.data.repository.MessageRepository
import com.aura.app.data.repository.UserRepository
import com.aura.app.utils.Constants
import com.aura.app.utils.StubSession
import com.aura.app.utils.StubState
import kotlinx.coroutines.launch

sealed class DealActionResult {
    object Success : DealActionResult()
    data class Error(val message: String) : DealActionResult()
}

class CampaignInfoViewModel(
    private val dealRepository: DealRepository = DealRepository(),
    private val messageRepository: MessageRepository = MessageRepository(),
    private val userRepository: UserRepository = UserRepository(),
) : ViewModel() {

    private val _deal = MutableLiveData<Deal>()
    val deal: LiveData<Deal> = _deal

    private val _otherParty = MutableLiveData<UserLite?>()
    val otherParty: LiveData<UserLite?> = _otherParty

    private val _sharedMedia = MutableLiveData<List<Message>>()
    val sharedMedia: LiveData<List<Message>> = _sharedMedia

    private val _saveError = MutableLiveData<String?>()
    val saveError: LiveData<String?> = _saveError

    private val _saveSuccess = MutableLiveData(false)
    val saveSuccess: LiveData<Boolean> = _saveSuccess

    private val _actionResult = MutableLiveData<DealActionResult?>()
    val actionResult: LiveData<DealActionResult?> = _actionResult

    private lateinit var dealId: String
    private lateinit var currentUserId: String

    fun load(dealId: String, currentUserId: String) {
        this.dealId = dealId
        this.currentUserId = currentUserId
        viewModelScope.launch {
            val deal = if (Constants.USE_STUBS) {
                StubState.currentDeals().firstOrNull { it.dealId == dealId }
            } else {
                dealRepository.getDeal(dealId).getOrNull()
            } ?: return@launch

            _deal.value = deal
            val otherUserId = if (deal.creatorId == currentUserId) deal.brandId else deal.creatorId
            _otherParty.value = userRepository.getUserLite(otherUserId)
            _sharedMedia.value = messageRepository.getSharedMedia(dealId)
        }
    }

    fun updateDealDetails(title: String, description: String) {
        if (title.isBlank()) return
        viewModelScope.launch {
            dealRepository.updateDealDetails(dealId, title, description)
                .onSuccess {
                    _deal.value = _deal.value?.copy(title = title.trim(), description = description.trim())
                    _saveSuccess.value = true
                    _saveSuccess.value = false
                }
                .onFailure { _saveError.value = it.message }
        }
    }

    fun cancelDeal(reason: String) {
        viewModelScope.launch {
            if (Constants.USE_STUBS) {
                StubState.cancelDeal(dealId, cancelledBy = currentUserId, reason = reason)
                _deal.value = _deal.value?.copy(
                    status = Constants.STATUS_CANCELLED,
                    chatUnlocked = true,
                    cancelledBy = currentUserId,
                    cancelReason = reason,
                )
                _actionResult.value = DealActionResult.Success
            } else {
                dealRepository.cancelDeal(dealId, cancelledBy = currentUserId, reason = reason)
                    .onSuccess {
                        _deal.value = _deal.value?.copy(
                            status = Constants.STATUS_CANCELLED,
                            chatUnlocked = true,
                            cancelledBy = currentUserId,
                            cancelReason = reason,
                        )
                        _actionResult.value = DealActionResult.Success
                    }
                    .onFailure { _actionResult.value = DealActionResult.Error(it.message ?: "Failed to cancel deal") }
            }
        }
    }

    fun requestCompletion() {
        viewModelScope.launch {
            if (Constants.USE_STUBS) {
                StubState.requestCompletion(dealId, currentUserId)
                _deal.value = _deal.value?.copy(completionRequestedBy = currentUserId)
                _actionResult.value = DealActionResult.Success
            } else {
                dealRepository.requestCompletion(dealId, currentUserId)
                    .onSuccess {
                        _deal.value = _deal.value?.copy(completionRequestedBy = currentUserId)
                        _actionResult.value = DealActionResult.Success
                    }
                    .onFailure { _actionResult.value = DealActionResult.Error(it.message ?: "Failed to request completion") }
            }
        }
    }

    fun consumeActionResult() {
        _actionResult.value = null
    }
}

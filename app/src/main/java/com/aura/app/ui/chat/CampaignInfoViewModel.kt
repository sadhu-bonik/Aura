package com.aura.app.ui.chat

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aura.app.data.model.Campaign
import com.aura.app.data.model.Deal
import com.aura.app.data.model.Message
import com.aura.app.data.model.UserLite
import com.aura.app.data.repository.AuthRepository
import com.aura.app.data.repository.CampaignRepository
import com.aura.app.data.repository.DealRepository
import com.aura.app.data.repository.MessageRepository
import com.aura.app.data.repository.UserRepository
import com.aura.app.utils.Constants
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

sealed class DealActionResult {
    object Success : DealActionResult()
    data class Error(val message: String) : DealActionResult()
}

class CampaignInfoViewModel(
    private val dealRepository: DealRepository = DealRepository(),
    private val messageRepository: MessageRepository = MessageRepository(),
    private val userRepository: UserRepository = UserRepository(),
    private val authRepository: AuthRepository = AuthRepository(),
    private val campaignRepository: CampaignRepository = CampaignRepository(FirebaseFirestore.getInstance()),
) : ViewModel() {

    private val _deal = MutableLiveData<Deal>()
    val deal: LiveData<Deal> = _deal

    private val _otherParty = MutableLiveData<UserLite?>()
    val otherParty: LiveData<UserLite?> = _otherParty

    /** The brand's userId — exposed so the sheet can navigate to their profile. */
    private val _brandId = MutableLiveData<String>()
    val brandId: LiveData<String> = _brandId

    private val _campaign = MutableLiveData<Campaign?>()
    val campaign: LiveData<Campaign?> = _campaign

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

    fun load(dealId: String, currentUserId: String = authRepository.currentUser?.uid ?: "") {
        this.dealId = dealId
        this.currentUserId = currentUserId
        viewModelScope.launch {
            val deal = dealRepository.getDeal(dealId).getOrNull() ?: return@launch

            _deal.value = deal
            _brandId.value = deal.brandId
            val otherUserId = if (deal.creatorId == currentUserId) deal.brandId else deal.creatorId
            _otherParty.value = userRepository.getUserLite(otherUserId)
            _sharedMedia.value = messageRepository.getSharedMedia(dealId)

            if (deal.campaignId.isNotBlank()) {
                _campaign.value = campaignRepository.getCampaign(deal.campaignId)
            } else {
                _campaign.value = null
            }
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

    /**
     * Pre-acceptance: brand withdraws their pending offer. Goes straight to CANCELLED — no
     * approval needed because the creator hasn't engaged yet.
     */
    fun withdrawDeal(reason: String) {
        viewModelScope.launch {
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
                .onFailure { _actionResult.value = DealActionResult.Error(it.message ?: "Failed to withdraw deal") }
        }
    }

    /**
     * Pre-acceptance: creator declines (rejects) a pending offer.
     */
    fun rejectDeal() {
        viewModelScope.launch {
            dealRepository.rejectDeal(dealId)
                .onSuccess {
                    _deal.value = _deal.value?.copy(status = Constants.STATUS_REJECTED)
                    _actionResult.value = DealActionResult.Success
                }
                .onFailure { _actionResult.value = DealActionResult.Error(it.message ?: "Failed to decline deal") }
        }
    }

    /**
     * Post-acceptance: cancel immediately. No counterparty approval or notification is needed.
     */
    fun cancelAcceptedDeal(reason: String) {
        viewModelScope.launch {
            dealRepository.cancelDeal(dealId, cancelledBy = currentUserId, reason = reason)
                .onSuccess {
                    _deal.value = _deal.value?.copy(
                        chatUnlocked = true,
                        cancelRequestedBy = currentUserId,
                        cancelReason = reason,
                        completionRequestedBy = "",
                    )
                    _actionResult.value = DealActionResult.Success
                }
                .onFailure { _actionResult.value = DealActionResult.Error(it.message ?: "Failed to cancel deal") }
        }
    }

    fun requestCompletion() {
        viewModelScope.launch {
            dealRepository.requestCompletion(dealId, currentUserId)
                .onSuccess {
                    _deal.value = _deal.value?.copy(completionRequestedBy = currentUserId)
                    _actionResult.value = DealActionResult.Success
                }
                .onFailure { _actionResult.value = DealActionResult.Error(it.message ?: "Failed to request completion") }
        }
    }

    fun consumeActionResult() {
        _actionResult.value = null
    }
}

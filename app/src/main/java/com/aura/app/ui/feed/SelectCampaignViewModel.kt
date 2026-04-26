package com.aura.app.ui.feed

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.aura.app.data.model.Campaign
import com.aura.app.data.repository.CampaignRepository
import com.aura.app.data.repository.DealRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class SelectCampaignUiState {
    object Loading : SelectCampaignUiState()
    data class Success(val campaigns: List<Campaign>) : SelectCampaignUiState()
    data class Error(val message: String) : SelectCampaignUiState()
}

sealed class SendDealUiState {
    object Idle : SendDealUiState()
    object Sending : SendDealUiState()
    object Sent : SendDealUiState()
    data class Error(val message: String) : SendDealUiState()
}

class SelectCampaignViewModel(
    private val campaignRepository: CampaignRepository,
    private val dealRepository: DealRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<SelectCampaignUiState>(SelectCampaignUiState.Loading)
    val state: StateFlow<SelectCampaignUiState> = _state

    private val _selectedCampaignId = MutableStateFlow<String?>(null)
    val selectedCampaignId: StateFlow<String?> = _selectedCampaignId

    private val _sendState = MutableStateFlow<SendDealUiState>(SendDealUiState.Idle)
    val sendState: StateFlow<SendDealUiState> = _sendState

    fun loadCampaigns(brandId: String) {
        android.util.Log.d("SelectCampaignVM", "loadCampaigns: brandId=$brandId")
        viewModelScope.launch {
            campaignRepository.getCampaignsForBrand(brandId).collect { campaigns ->
                android.util.Log.d("SelectCampaignVM", "Loaded ${campaigns.size} campaigns")
                if (campaigns.isEmpty()) {
                    _state.value = SelectCampaignUiState.Error("No campaigns found for $brandId")
                } else {
                    _state.value = SelectCampaignUiState.Success(campaigns)
                    if (_selectedCampaignId.value == null) {
                        _selectedCampaignId.value = campaigns.firstOrNull()?.campaignId
                    }
                }
            }
        }
    }

    fun selectCampaign(campaignId: String) {
        _selectedCampaignId.value = campaignId
    }

    fun sendDeal(brandId: String, creatorId: String) {
        if (_sendState.value is SendDealUiState.Sending) return

        val loaded = _state.value as? SelectCampaignUiState.Success
        val selectedId = _selectedCampaignId.value
        val campaign = loaded?.campaigns?.find { it.campaignId == selectedId }
        if (campaign == null) {
            _sendState.value = SendDealUiState.Error("Select a campaign first")
            return
        }
        if (brandId.isBlank() || creatorId.isBlank()) {
            _sendState.value = SendDealUiState.Error("Missing brand or creator id")
            return
        }

        _sendState.value = SendDealUiState.Sending
        viewModelScope.launch {
            val result = dealRepository.sendDeal(
                brandId = brandId,
                creatorId = creatorId,
                campaignId = campaign.campaignId,
                title = campaign.title,
                description = campaign.description,
                budget = campaign.budgetMax,
            )
            _sendState.value = result.fold(
                onSuccess = { SendDealUiState.Sent },
                onFailure = { SendDealUiState.Error(it.message ?: "Failed to send deal") },
            )
        }
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val db = FirebaseFirestore.getInstance()
            @Suppress("UNCHECKED_CAST")
            return SelectCampaignViewModel(
                campaignRepository = CampaignRepository(db),
                dealRepository = DealRepository(db),
            ) as T
        }
    }
}

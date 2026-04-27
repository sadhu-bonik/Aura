package com.aura.app.ui.feed

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.aura.app.data.model.Campaign
import com.aura.app.data.model.Deal
import com.aura.app.data.repository.CampaignRepository
import com.aura.app.data.repository.DealRepository
import com.aura.app.utils.Constants
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class SelectCampaignUiState {
    object Loading : SelectCampaignUiState()
    object Empty : SelectCampaignUiState()
    data class Success(val campaigns: List<Campaign>) : SelectCampaignUiState()
    data class Error(val message: String) : SelectCampaignUiState()
}

class SelectCampaignViewModel(
    private val campaignRepository: CampaignRepository,
    private val dealRepository: DealRepository
) : ViewModel() {

    private val _state = MutableStateFlow<SelectCampaignUiState>(SelectCampaignUiState.Loading)
    val state: StateFlow<SelectCampaignUiState> = _state

    private val _selectedCampaignId = MutableStateFlow<String?>(null)
    val selectedCampaignId: StateFlow<String?> = _selectedCampaignId

    private val _dealSentEvent = MutableSharedFlow<Result<String>>()
    val dealSentEvent: SharedFlow<Result<String>> = _dealSentEvent

    fun loadCampaigns(brandId: String) {
        android.util.Log.d("SelectCampaignVM", "loadCampaigns: brandId=$brandId")
        viewModelScope.launch {
            campaignRepository.getCampaignsForBrand(brandId).collect { campaigns ->
                android.util.Log.d("SelectCampaignVM", "Loaded ${campaigns.size} campaigns")
                if (campaigns.isEmpty()) {
                    _state.value = SelectCampaignUiState.Empty
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
        val campaignId = _selectedCampaignId.value ?: return
        val campaigns = (state.value as? SelectCampaignUiState.Success)?.campaigns ?: return
        val campaign = campaigns.find { it.campaignId == campaignId } ?: return

        viewModelScope.launch {
            val deal = Deal(
                brandId = brandId,
                creatorId = creatorId,
                campaignId = campaignId,
                title = campaign.title,
                description = campaign.description,
                budget = campaign.budgetMin,
                status = Constants.STATUS_PENDING
            )
            val result = dealRepository.createDeal(deal)
            _dealSentEvent.emit(result)
        }
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val db = FirebaseFirestore.getInstance()
            val campaignRepo = CampaignRepository(db)
            val dealRepo = DealRepository(db)
            @Suppress("UNCHECKED_CAST")
            return SelectCampaignViewModel(campaignRepo, dealRepo) as T
        }
    }
}

package com.aura.app.ui.feed

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.aura.app.data.model.Campaign
import com.aura.app.data.repository.CampaignRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class SelectCampaignUiState {
    object Loading : SelectCampaignUiState()
    data class Success(val campaigns: List<Campaign>) : SelectCampaignUiState()
    data class Error(val message: String) : SelectCampaignUiState()
}

class SelectCampaignViewModel(
    private val campaignRepository: CampaignRepository
) : ViewModel() {

    private val _state = MutableStateFlow<SelectCampaignUiState>(SelectCampaignUiState.Loading)
    val state: StateFlow<SelectCampaignUiState> = _state

    private val _selectedCampaignId = MutableStateFlow<String?>(null)
    val selectedCampaignId: StateFlow<String?> = _selectedCampaignId

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

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val db = FirebaseFirestore.getInstance()
            val repo = CampaignRepository(db)
            @Suppress("UNCHECKED_CAST")
            return SelectCampaignViewModel(repo) as T
        }
    }
}

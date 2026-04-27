package com.aura.app.ui.main

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.aura.app.data.model.Campaign
import com.aura.app.data.repository.CampaignRepository
import com.aura.app.data.repository.StorageRepository
import com.aura.app.utils.SessionManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class SetupCampaignUiState {
    object Idle : SetupCampaignUiState()
    object Loading : SetupCampaignUiState()
    data class Success(val campaign: Campaign?) : SetupCampaignUiState()
    data class Error(val message: String) : SetupCampaignUiState()
    object Saved : SetupCampaignUiState()
}

class SetupCampaignViewModel(
    private val campaignRepository: CampaignRepository,
    private val storageRepository: StorageRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _state = MutableStateFlow<SetupCampaignUiState>(SetupCampaignUiState.Idle)
    val state: StateFlow<SetupCampaignUiState> = _state

    fun loadCampaign(campaignId: String?) {
        if (campaignId == null) {
            _state.value = SetupCampaignUiState.Success(null)
            return
        }

        viewModelScope.launch {
            _state.value = SetupCampaignUiState.Loading
            val campaign = campaignRepository.getCampaign(campaignId)
            _state.value = SetupCampaignUiState.Success(campaign)
        }
    }

    fun saveCampaign(
        campaignId: String?,
        title: String,
        description: String,
        budgetMin: Long,
        budgetMax: Long,
        deliverables: List<String>,
        imageUri: android.net.Uri?
    ) {
        val brandId = sessionManager.getUserId() ?: return

        viewModelScope.launch {
            _state.value = SetupCampaignUiState.Loading
            try {
                var finalImageUrl = ""
                var finalImagePath = ""
                
                val targetCampaignId = campaignId ?: com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("campaigns").document().id

                if (imageUri != null) {
                    val uploadResult = storageRepository.uploadCampaignImage(brandId, targetCampaignId, imageUri)
                    finalImageUrl = uploadResult.downloadUrl
                    finalImagePath = uploadResult.storagePath
                }

                val campaign = Campaign(
                    campaignId = targetCampaignId,
                    brandId = brandId,
                    title = title,
                    description = description,
                    budgetMin = budgetMin,
                    budgetMax = budgetMax,
                    deliverables = deliverables,
                    imageUrl = finalImageUrl,
                    imagePath = finalImagePath
                )
                val result = campaignRepository.saveCampaign(campaign)
                if (result.isSuccess) {
                    _state.value = SetupCampaignUiState.Saved
                } else {
                    _state.value = SetupCampaignUiState.Error(result.exceptionOrNull()?.message ?: "Unknown error")
                }
            } catch (e: Exception) {
                _state.value = SetupCampaignUiState.Error(e.message ?: "Failed to upload image")
            }
        }
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val db = FirebaseFirestore.getInstance()
            return SetupCampaignViewModel(
                CampaignRepository(db),
                StorageRepository(),
                SessionManager(context)
            ) as T
        }
    }
}

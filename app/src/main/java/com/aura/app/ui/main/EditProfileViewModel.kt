package com.aura.app.ui.main

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.aura.app.data.model.BrandProfile
import com.aura.app.data.model.CreatorProfile
import com.aura.app.data.model.User
import com.aura.app.data.repository.StorageRepository
import com.aura.app.data.repository.UserRepository
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class EditProfileUiState {
    object Loading : EditProfileUiState()
    data class Success(
        val user: User,
        val creatorProfile: CreatorProfile? = null,
        val brandProfile: BrandProfile? = null,
    ) : EditProfileUiState()
    data class Error(val message: String) : EditProfileUiState()
}

sealed class EditProfileEvent {
    object Saving : EditProfileEvent()
    object SaveSuccess : EditProfileEvent()
    object SaveSuccessWithTagChange : EditProfileEvent()
    data class SaveError(val message: String) : EditProfileEvent()
}

class EditProfileViewModel(
    private val userRepository: UserRepository,
    private val storageRepository: StorageRepository
) : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    
    private val _state = MutableStateFlow<EditProfileUiState>(EditProfileUiState.Loading)
    val state: StateFlow<EditProfileUiState> = _state

    private val _event = MutableStateFlow<EditProfileEvent?>(null)
    val event: StateFlow<EditProfileEvent?> = _event

    init {
        loadProfile()
    }

    private fun loadProfile() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            _state.value = EditProfileUiState.Loading
            val user = userRepository.getUserProfile(uid)
            if (user == null) {
                _state.value = EditProfileUiState.Error("Failed to fetch user.")
                return@launch
            }
            val creatorProfile = if (user.role == "creator") userRepository.getCreatorProfile(uid) else null
            val brandProfile = if (user.role == "brand") {
                userRepository.getBrandProfile(uid).also {
                    Log.d(TAG, "loadProfile brand → bio='${it?.bio}' industryTags=${it?.industryTags}")
                }
            } else null
            _state.value = EditProfileUiState.Success(user, creatorProfile, brandProfile)
        }
    }

    fun saveProfile(
        displayName: String,
        headline: String,
        bio: String,
        phone: String,
        securityQuestion: String,
        securityAnswer: String,
        youtubeUrl: String,
        profileImageUri: Uri?,
        website: String = "",
        industry: String = "",
        nicheTags: List<String> = emptyList()
    ) {
        val uid = auth.currentUser?.uid ?: return
        if (displayName.isBlank()) {
            _event.value = EditProfileEvent.SaveError("Name cannot be empty")
            return
        }

        val currentRole = (_state.value as? EditProfileUiState.Success)?.user?.role

        viewModelScope.launch {
            _event.value = EditProfileEvent.Saving
            try {
                // Upload Image
                var updatedImageUrl: String? = null
                if (profileImageUri != null) {
                    updatedImageUrl = storageRepository.uploadProfilePicture(uid, profileImageUri)
                }

                // Update User display name / photo
                val userUpdates = mutableMapOf<String, Any>()
                if (displayName.isNotBlank()) userUpdates["displayName"] = displayName
                if (phone.isNotBlank()) userUpdates["phone"] = phone
                if (securityQuestion.isNotBlank() && securityQuestion != "Select a security question") userUpdates["securityQuestion"] = securityQuestion
                if (securityAnswer.isNotBlank()) userUpdates["securityAnswer"] = securityAnswer
                
                updatedImageUrl?.let { userUpdates["profileImageUrl"] = it }
                if (userUpdates.isNotEmpty()) {
                    userRepository.updateUserPartial(uid, userUpdates)
                }

                if (currentRole == "brand") {
                    // Route brand edits to brandProfiles collection.
                    val brandUpdates = mutableMapOf<String, Any>(
                        "bio" to bio,
                        "motto" to headline,
                        "brandName" to displayName,
                        "updatedAt" to Timestamp.now(),
                    )
                    if (website.isNotBlank()) brandUpdates["website"] = website
                    if (industry.isNotBlank()) brandUpdates["industry"] = industry
                    if (nicheTags.isNotEmpty()) brandUpdates["industryTags"] = nicheTags
                    
                    Log.d(TAG, "saveProfile brand payload → $brandUpdates")
                    userRepository.updateBrandProfilePartial(uid, brandUpdates)

                    _event.value = EditProfileEvent.SaveSuccess
                } else {
                    // Creator profile update
                    val creatorUpdates = mutableMapOf<String, Any>(
                        "bio" to bio,
                        "motto" to headline,
                        "youtubeHandle" to youtubeUrl,
                        "isProfileComplete" to true,
                        "updatedAt" to Timestamp.now(),
                    )
                    if (nicheTags.isNotEmpty()) creatorUpdates["tags"] = nicheTags
                    Log.d(TAG, "saveProfile creator payload → $creatorUpdates")
                    userRepository.updateCreatorProfilePartial(uid, creatorUpdates)

                    _event.value = EditProfileEvent.SaveSuccess
                }

            } catch (e: Exception) {
                _event.value = EditProfileEvent.SaveError(e.message ?: "Failed to save profile")
            }
        }
    }
    
    fun resetEvent() {
        _event.value = null
    }

    class Factory : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return EditProfileViewModel(UserRepository(), StorageRepository()) as T
        }
    }

    companion object {
        private const val TAG = "EditProfileViewModel"
    }
}

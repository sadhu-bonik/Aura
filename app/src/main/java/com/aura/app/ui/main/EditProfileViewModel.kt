package com.aura.app.ui.main

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.aura.app.data.model.BrandProfile
import com.aura.app.data.model.CreatorProfile
import com.aura.app.data.model.User
import com.aura.app.data.repository.AuthRepository
import com.aura.app.data.repository.StorageRepository
import com.aura.app.data.repository.UserRepository
import com.aura.app.utils.SessionManager
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
    object Deleting : EditProfileEvent()
    object DeleteSuccess : EditProfileEvent()
    data class DeleteError(val message: String) : EditProfileEvent()
}

class EditProfileViewModel(
    private val userRepository: UserRepository,
    private val storageRepository: StorageRepository,
    private val sessionManager: SessionManager,
    private val authRepository: AuthRepository
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
        val uid = resolveUserId()
        if (uid == null) {
            _state.value = EditProfileUiState.Error("No signed-in user found.")
            return
        }
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
        val uid = resolveUserId()
        if (uid == null) {
            _event.value = EditProfileEvent.SaveError("No signed-in user found.")
            return
        }
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
                    updatedImageUrl?.let { brandUpdates["logoUrl"] = it }
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

    fun deleteAccount() {
        val uid = resolveUserId()
        if (uid == null) {
            _event.value = EditProfileEvent.DeleteError("No signed-in user found.")
            return
        }

        viewModelScope.launch {
            _event.value = EditProfileEvent.Deleting

            val role = (_state.value as? EditProfileUiState.Success)?.user?.role
                ?: userRepository.getUserProfile(uid)?.role
                ?: ""

            val dataResult = userRepository.deleteAccountData(uid, role)
            if (dataResult.isFailure) {
                _event.value = EditProfileEvent.DeleteError(
                    dataResult.exceptionOrNull()?.message ?: "Failed to delete profile data."
                )
                return@launch
            }

            if (auth.currentUser != null) {
                authRepository.deleteCurrentUser()
            }
            authRepository.logout()
            sessionManager.clearSession()
            _event.value = EditProfileEvent.DeleteSuccess
        }
    }

    private fun resolveUserId(): String? =
        auth.currentUser?.uid ?: sessionManager.getUserId()

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return EditProfileViewModel(
                UserRepository(),
                StorageRepository(),
                SessionManager(context.applicationContext),
                AuthRepository()
            ) as T
        }
    }

    companion object {
        private const val TAG = "EditProfileViewModel"
    }
}

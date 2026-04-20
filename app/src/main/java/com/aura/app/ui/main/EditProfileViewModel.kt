package com.aura.app.ui.main

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.aura.app.data.model.CreatorProfile
import com.aura.app.data.model.User
import com.aura.app.data.repository.StorageRepository
import com.aura.app.data.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class EditProfileUiState {
    object Loading : EditProfileUiState()
    data class Success(val user: User, val creatorProfile: CreatorProfile?) : EditProfileUiState()
    data class Error(val message: String) : EditProfileUiState()
}

sealed class EditProfileEvent {
    object Saving : EditProfileEvent()
    object SaveSuccess : EditProfileEvent()
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
            _state.value = EditProfileUiState.Success(user, creatorProfile)
        }
    }

    fun saveProfile(name: String, bio: String, selectedTags: List<String>, uri: Uri?) {
        val uid = auth.currentUser?.uid ?: return
        if (name.isBlank()) {
            _event.value = EditProfileEvent.SaveError("Name cannot be empty")
            return
        }

        viewModelScope.launch {
            _event.value = EditProfileEvent.Saving
            try {
                // Upload Image
                var updatedImageUrl: String? = null
                if (uri != null) {
                    updatedImageUrl = storageRepository.uploadProfilePicture(uid, uri)
                }

                // Update User
                val userUpdates = mutableMapOf<String, Any>()
                if (name.isNotBlank()) userUpdates["displayName"] = name
                updatedImageUrl?.let { userUpdates["profileImageUrl"] = it }
                if (userUpdates.isNotEmpty()) {
                    userRepository.updateUserPartial(uid, userUpdates)
                }

                // Update Creator Profile
                val creatorUpdates = mapOf(
                    "bio" to bio,
                    "niche" to selectedTags.joinToString(", "),
                    "tags" to selectedTags,
                    "isProfileComplete" to true
                )
                userRepository.updateCreatorProfilePartial(uid, creatorUpdates)

                _event.value = EditProfileEvent.SaveSuccess

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
}

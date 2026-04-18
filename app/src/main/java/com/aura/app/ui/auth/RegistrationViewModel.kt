package com.aura.app.ui.auth

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aura.app.data.model.User
import com.aura.app.data.model.UserRole
import com.aura.app.data.repository.AuthRepository
import com.aura.app.data.repository.StorageRepository
import com.aura.app.data.repository.UserRepository
import kotlinx.coroutines.launch

/**
 * RegistrationViewModel - Shared state for the multi-step registration process.
 * Holds temporary data until the final "Finish" button is pressed.
 */
class RegistrationViewModel(
    private val authRepository: AuthRepository = AuthRepository(),
    private val userRepository: UserRepository = UserRepository(),
    private val storageRepository: StorageRepository = StorageRepository()
) : ViewModel() {

    // Common State
    private val _userRole = MutableLiveData<UserRole>(UserRole.CREATOR)
    val userRole: LiveData<UserRole> = _userRole

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    private val _registrationSuccess = MutableLiveData<Boolean>(false)
    val registrationSuccess: LiveData<Boolean> = _registrationSuccess

    // Temporary data storage
    var email = ""
    var password = ""
    var fullName = ""
    var phone = ""
    var securityQuestion = ""
    var securityAnswer = ""
    
    // Media Uris
    var profileImageUri: Uri? = null
    var verificationDocUri: Uri? = null
    var portfolioVideoUri: Uri? = null

    // Brand specific
    var brandName = ""
    var brandMotto = ""
    var brandBio = ""
    var industryTags = listOf<String>()
    var targetLocation = ""
    var website = ""
    var instagram = ""
    var youtube = ""

    // Creator specific
    var creatorMotto = ""
    var creatorBio = ""
    var youtubeLink = ""
    var niches = listOf<String>()
    var location = ""
    var audienceRegion = ""

    fun setUserRole(role: UserRole) {
        _userRole.value = role
    }

    /**
     * Finalizes the registration process:
     * 1. Creates Firebase Auth account
     * 2. Uploads media (photo, doc, video)
     * 3. Creates Firestore User profile
     */
    fun completeRegistration() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                // 1. Register with Firebase Auth
                val authResult = authRepository.register(email, password)
                if (authResult.isFailure) {
                    _error.value = authResult.exceptionOrNull()?.message ?: "Registration failed"
                    _isLoading.value = false
                    return@launch
                }

                val userId = authResult.getOrThrow().uid

                // 2. Upload Media
                val profileUrl = profileImageUri?.let { storageRepository.uploadProfilePicture(userId, it) } ?: ""
                val verificationUrl = verificationDocUri?.let { storageRepository.uploadVerificationDoc(userId, it) } ?: ""
                val videoUrl = portfolioVideoUri?.let { storageRepository.uploadPortfolioVideo(userId, it) } ?: ""

                // 3. Create Firestore User Profile
                val userProfile = if (_userRole.value == UserRole.CREATOR) {
                    User(
                        userId = userId,
                        email = email,
                        role = UserRole.CREATOR,
                        displayName = fullName,
                        profileImageUrl = profileUrl,
                        motto = creatorMotto,
                        bio = creatorBio,
                        niche = niches,
                        location = location,
                        audienceRegion = audienceRegion,
                        portfolioVideoUrl = videoUrl
                    )
                } else {
                    User(
                        userId = userId,
                        email = email,
                        role = UserRole.BRAND,
                        displayName = brandName,
                        profileImageUrl = profileUrl,
                        motto = brandMotto,
                        bio = brandBio,
                        companyName = brandName,
                        phone = phone,
                        verificationDocUrl = verificationUrl,
                        website = website,
                        instagram = instagram,
                        youtube = youtube,
                        industryTags = industryTags,
                        targetLocation = targetLocation
                    )
                }

                val firestoreResult = userRepository.createUserProfile(userProfile)
                if (firestoreResult.isSuccess) {
                    _registrationSuccess.value = true
                } else {
                    _error.value = firestoreResult.exceptionOrNull()?.message ?: "Failed to create profile"
                }

            } catch (e: Exception) {
                _error.value = e.message ?: "An unexpected error occurred"
            } finally {
                _isLoading.value = false
            }
        }
    }
}

package com.aura.app.ui.auth

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aura.app.data.model.CreatorProfile
import com.aura.app.data.model.User
import com.aura.app.data.repository.AuthRepository
import com.aura.app.data.repository.StorageRepository
import com.aura.app.data.repository.UserRepository
import kotlinx.coroutines.launch

/**
 * RegistrationViewModel — shared state for the CREATOR multi-step registration flow.
 *
 * NOTE: Brand registration now has its own dedicated BrandRegistrationViewModel.
 * This ViewModel only handles Creator registration.
 *
 * Passwords are passed directly to Firebase Auth and are NEVER stored in Firestore.
 */
class RegistrationViewModel(
    private val authRepository: AuthRepository = AuthRepository(),
    private val userRepository: UserRepository = UserRepository(),
    private val storageRepository: StorageRepository = StorageRepository()
) : ViewModel() {

    // Role state (kept for RoleSelectionFragment compatibility)
    private val _userRole = MutableLiveData<String>("creator")
    val userRole: LiveData<String> = _userRole

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    private val _registrationSuccess = MutableLiveData<Boolean>(false)
    val registrationSuccess: LiveData<Boolean> = _registrationSuccess

    // -------------------------------------------------------------------------
    // Creator draft fields — filled across 4 steps
    // -------------------------------------------------------------------------
    var email = ""
    var password = ""
    var fullName = ""
    var phone = ""
    var securityQuestion = ""
    var securityAnswer = ""

    // Creator specific
    var creatorMotto = ""
    var creatorBio = ""
    var instagramHandle = ""
    var niches = listOf<String>()
    var location = ""
    var audienceRegion = ""

    // Media URIs (upload deferred — placeholders until Storage is wired)
    var profileImageUri: Uri? = null
    var portfolioVideoUri: Uri? = null

    fun setUserRole(role: String) {
        _userRole.value = role
    }

    fun resetRegistrationSuccess() {
        _registrationSuccess.value = false
    }

    /** Call this when the user begins a new registration flow so stale draft data never leaks. */
    fun resetDraft() {
        email = ""; password = ""; fullName = ""; phone = ""
        securityQuestion = ""; securityAnswer = ""
        creatorMotto = ""; creatorBio = ""; instagramHandle = ""
        niches = listOf(); location = ""; audienceRegion = ""
        profileImageUri = null; portfolioVideoUri = null
        _registrationSuccess.value = false
        _error.value = null
    }

    /**
     * Finalizes creator registration:
     * 1. Creates Firebase Auth account
     * 2. (Placeholder) Uploads media
     * 3. Writes Firestore User + CreatorProfile documents
     */
    fun completeRegistration(context: android.content.Context) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                // 1. Firebase Auth
                val authResult = authRepository.register(email, password)
                if (authResult.isFailure) {
                    _error.value = authResult.exceptionOrNull()?.message ?: "Registration failed"
                    _isLoading.value = false
                    return@launch
                }

                val userId = authResult.getOrThrow().uid

                // 2. Placeholder — Storage upload will go here when wired
                val profileUrl = "" // TODO: storageRepository.uploadProfilePicture(userId, profileImageUri)
                val videoUrl   = "" // TODO: storageRepository.uploadPortfolioVideo(userId, portfolioVideoUri)

                // 3. Firestore User document
                val userProfile = User(
                    userId = userId,
                    email = email,
                    role = "creator",
                    displayName = fullName,
                    profileImageUrl = profileUrl,
                    phone = phone,
                    securityQuestion = securityQuestion,
                    securityAnswer = securityAnswer,
                    isProfileComplete = true
                )

                val creatorProfile = CreatorProfile(
                    userId = userId,
                    motto = creatorMotto,
                    bio = creatorBio,
                    instagramHandle = instagramHandle,
                    niche = niches.joinToString(", "),
                    tags = niches,
                    location = location,
                    isProfileComplete = true
                )

                val firestoreResult = userRepository.setupNewUser(
                    user = userProfile,
                    creatorProfile = creatorProfile,
                    brandProfile = null
                )

                if (firestoreResult.isSuccess) {
                    com.aura.app.utils.SessionManager(context).saveUserId(userId)
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

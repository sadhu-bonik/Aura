package com.aura.app.ui.auth

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aura.app.BuildConfig
import com.aura.app.data.model.CreatorProfile
import com.aura.app.data.model.PortfolioItem
import com.aura.app.data.model.User
import com.aura.app.data.repository.AuthRepository
import com.aura.app.data.repository.PortfolioRepository
import com.aura.app.data.repository.StorageRepository
import com.aura.app.data.repository.UserRepository
import com.aura.app.data.repository.YouTubeRepository
import com.aura.app.utils.SessionManager
import com.google.firebase.Timestamp
import kotlinx.coroutines.launch

/**
 * RegistrationViewModel — shared state for the CREATOR multi-step registration flow.
 *
 * Passwords are passed directly to Firebase Auth and NEVER stored in Firestore.
 */
class RegistrationViewModel(
    private val authRepository: AuthRepository = AuthRepository(),
    private val userRepository: UserRepository = UserRepository(),
    private val storageRepository: StorageRepository = StorageRepository(),
    private val portfolioRepository: PortfolioRepository = PortfolioRepository(),
    private val youtubeRepository: YouTubeRepository = YouTubeRepository(apiKey = BuildConfig.YOUTUBE_API_KEY)
) : ViewModel() {

    private val _userRole = MutableLiveData<String>("creator")
    val userRole: LiveData<String> = _userRole

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    private val _registrationSuccess = MutableLiveData<Boolean>(false)
    val registrationSuccess: LiveData<Boolean> = _registrationSuccess

    /** Tracks how many portfolio URIs are currently staged (drives the finish-button gate). */
    private val _pendingVideoCount = MutableLiveData<Int>(0)
    val pendingVideoCount: LiveData<Int> = _pendingVideoCount

    // -------------------------------------------------------------------------
    // Creator draft fields — filled across 4 steps
    // -------------------------------------------------------------------------
    var email = ""
    var password = ""
    var fullName = ""
    var phone = ""
    var securityQuestion = ""
    var securityAnswer = ""

    var creatorMotto = ""
    var creatorBio = ""
    var youtubeHandle = ""
    var niches = listOf<String>()
    var location = ""
    var audienceRegion = ""

    /** Optional profile photo — upload attempted but never fatal if it fails. */
    var profileImageUri: Uri? = null

    /** Local video URIs staged before the final submit (max 10). */
    val portfolioVideoUris: MutableList<Uri> = mutableListOf()

    fun setUserRole(role: String) { _userRole.value = role }

    fun resetRegistrationSuccess() { _registrationSuccess.value = false }

    /**
     * Adds a local video URI to the pending list.
     * Returns false (and does NOT add) when the list is already at the 10-item limit.
     */
    fun addPortfolioVideoUri(uri: Uri): Boolean {
        if (portfolioVideoUris.size >= 10) return false
        portfolioVideoUris.add(uri)
        _pendingVideoCount.value = portfolioVideoUris.size
        return true
    }

    fun removePortfolioVideoUri(index: Int) {
        if (index in portfolioVideoUris.indices) {
            portfolioVideoUris.removeAt(index)
            _pendingVideoCount.value = portfolioVideoUris.size
        }
    }

    fun resetDraft() {
        email = ""; password = ""; fullName = ""; phone = ""
        securityQuestion = ""; securityAnswer = ""
        creatorMotto = ""; creatorBio = ""; youtubeHandle = ""
        niches = listOf(); location = ""; audienceRegion = ""
        profileImageUri = null
        portfolioVideoUris.clear()
        _pendingVideoCount.value = 0
        _registrationSuccess.value = false
        _error.value = null
    }

    /**
     * Finalizes creator registration.
     *
     * Order of operations:
     *   1. Guard: must have ≥1 staged video URI; double-tap guard
     *   2. Firebase Auth account creation
     *   3. Profile image upload (optional — failure is non-fatal)
     *   4. Portfolio video uploads (parallel attempt; partial failures preserved)
     *   5. Firestore writes: User + CreatorProfile + one PortfolioItem per success
     *   6. Account marked Complete only when ≥1 video persisted successfully
     *
     * If ALL video uploads fail the account is still created but marked Incomplete
     * so the user can add videos from the profile edit screen later.
     */
    fun completeRegistration(context: android.content.Context) {
        if (_isLoading.value == true) return  // double-tap guard

        if (portfolioVideoUris.isEmpty()) {
            _error.value = "Add at least one portfolio video to finish setup."
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                // Step 1 — Firebase Auth
                val authResult = authRepository.register(email, password)
                if (authResult.isFailure) {
                    _error.value = authResult.exceptionOrNull()?.message ?: "Registration failed"
                    return@launch
                }
                val userId = authResult.getOrThrow().uid

                // Step 2 — Upload profile image (optional)
                var profileImageUrl = ""
                profileImageUri?.let { uri ->
                    runCatching { storageRepository.uploadProfilePicture(userId, uri) }
                        .onSuccess { url -> profileImageUrl = url }
                    // Profile photo is optional; silently continue on failure
                }

                // Step 3 — Upload portfolio videos; collect what succeeds
                val successfulItems = mutableListOf<PortfolioItem>()
                val failedIndices = mutableListOf<Int>()

                portfolioVideoUris.forEachIndexed { idx, uri ->
                    runCatching {
                        val itemId = portfolioRepository.generateItemId()
                        val upload = storageRepository.uploadPortfolioVideo(
                            userId = userId,
                            itemId = itemId,
                            uri = uri,
                            extension = "mp4"
                        )
                        PortfolioItem(
                            itemId = itemId,
                            creatorId = userId,
                            title = "Portfolio Video ${idx + 1}",
                            description = "",
                            mediaUrl = upload.downloadUrl,
                            mediaType = "video",
                            storagePath = upload.storagePath,
                            mimeType = "video/mp4",
                            public = true,
                            createdAt = Timestamp.now(),
                            updatedAt = Timestamp.now()
                        )
                    }.onSuccess { item ->
                        successfulItems.add(item)
                    }.onFailure {
                        failedIndices.add(idx + 1)
                    }
                }

                val isComplete = successfulItems.isNotEmpty()

                // Step 4 — Write Firestore: User + CreatorProfile
                val userProfile = User(
                    userId = userId,
                    email = email,
                    role = "creator",
                    displayName = fullName,
                    profileImageUrl = profileImageUrl,
                    phone = phone,
                    securityQuestion = securityQuestion,
                    securityAnswer = securityAnswer,
                    isProfileComplete = isComplete
                )
                val creatorProfile = CreatorProfile(
                    userId = userId,
                    motto = creatorMotto,
                    bio = creatorBio,
                    youtubeHandle = youtubeHandle,
                    niche = niches.joinToString(", "),
                    tags = niches,
                    location = location,
                    portfolioCount = successfulItems.size,
                    isProfileComplete = isComplete
                )

                val firestoreResult = userRepository.setupNewUser(
                    user = userProfile,
                    creatorProfile = creatorProfile,
                    brandProfile = null
                )
                if (firestoreResult.isFailure) {
                    _error.value = firestoreResult.exceptionOrNull()?.message
                        ?: "Failed to save profile"
                    return@launch
                }

                // Step 5 — YouTube analytics (non-fatal; enriches profile but never blocks registration)
                if (youtubeHandle.isNotBlank()) {
                    val analytics = youtubeRepository.fetchAndScore(youtubeHandle)
                    if (analytics != null) {
                        userRepository.updateCreatorProfilePartial(userId, analytics.toFirestoreMap())
                    }
                }

                // Step 7 — Save portfolio item metadata (best-effort; storage already has the files)
                for (item in successfulItems) {
                    portfolioRepository.savePortfolioItem(item)
                }

                // Step 8 — Persist session
                SessionManager(context).saveUserId(userId)

                if (failedIndices.isNotEmpty()) {
                    // Partial failure: account is created but some videos didn't upload.
                    // Surface a non-blocking message; the user can re-add from their profile.
                    _error.value = "Video${if (failedIndices.size > 1) "s" else ""} " +
                        "${failedIndices.joinToString(", ")} failed to upload. " +
                        "You can add them again from your profile."
                }

                if (!isComplete) {
                    _error.value = "All video uploads failed. " +
                        "Your account was created but is incomplete. " +
                        "Please add portfolio videos from your profile."
                }

                _registrationSuccess.value = true

            } catch (e: Exception) {
                _error.value = e.message ?: "An unexpected error occurred"
            } finally {
                _isLoading.value = false
            }
        }
    }
}

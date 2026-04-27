package com.aura.app.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.aura.app.data.model.BrandProfile
import com.aura.app.data.model.CreatorProfile
import com.aura.app.data.model.User
import com.aura.app.BuildConfig
import com.aura.app.data.repository.AccountSettingsRepository
import com.aura.app.data.repository.AuthRepository
import com.aura.app.data.repository.UserRepository
import com.aura.app.data.repository.YouTubeRepository
import com.aura.app.utils.ValidationUtils
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AccountSettingsState {
    data object Loading : AccountSettingsState()
    data class Ready(
        val user: User,
        val creatorProfile: CreatorProfile?,
        val brandProfile: BrandProfile?
    ) : AccountSettingsState()
    data class Error(val message: String) : AccountSettingsState()
}

sealed class AccountSettingsEvent {
    data object SaveStarted : AccountSettingsEvent()
    data object SaveSuccess : AccountSettingsEvent()
    data class SaveError(val message: String) : AccountSettingsEvent()

    data object PasswordStarted : AccountSettingsEvent()
    data object PasswordSuccess : AccountSettingsEvent()
    data class PasswordError(val message: String) : AccountSettingsEvent()
}

/**
 * AccountSettingsViewModel — backs the Account Settings screen.
 *
 * Handles loading the current user's User + role-specific profile,
 * saving common + role-specific fields, and changing the Firebase Auth
 * password (with mandatory re-authentication).
 *
 * Passwords are NEVER stored in Firestore — Firebase Auth manages them.
 */
class AccountSettingsViewModel(
    private val accountRepository: AccountSettingsRepository,
    private val authRepository: AuthRepository,
    private val youtubeRepository: YouTubeRepository = YouTubeRepository(apiKey = BuildConfig.YOUTUBE_API_KEY),
    private val userRepository: UserRepository = UserRepository()
) : ViewModel() {

    private val _state = MutableStateFlow<AccountSettingsState>(AccountSettingsState.Loading)
    val state: StateFlow<AccountSettingsState> = _state.asStateFlow()

    private val _event = MutableStateFlow<AccountSettingsEvent?>(null)
    val event: StateFlow<AccountSettingsEvent?> = _event.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _isChangingPassword = MutableStateFlow(false)
    val isChangingPassword: StateFlow<Boolean> = _isChangingPassword.asStateFlow()

    fun load() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid.isNullOrBlank()) {
            _state.value = AccountSettingsState.Error("Not signed in")
            return
        }
        viewModelScope.launch {
            _state.value = AccountSettingsState.Loading
            val result = accountRepository.loadSnapshot(uid)
            _state.value = result.fold(
                onSuccess = {
                    AccountSettingsState.Ready(it.user, it.creatorProfile, it.brandProfile)
                },
                onFailure = {
                    AccountSettingsState.Error(it.message ?: "Failed to load account")
                }
            )
            // Lazy weekly refresh: if cached YT analytics are >7 days old, fetch in background.
            // Doesn't block the UI — the screen renders against the cached snapshot immediately
            // and the refresh writes new fields next time the profile is read.
            (result.getOrNull())?.creatorProfile?.let { creator ->
                maybeRefreshYouTubeAnalytics(uid, creator.youtubeHandle, creator.youtubeAnalyticsUpdatedAt, force = false)
            }
        }
    }

    private fun maybeRefreshYouTubeAnalytics(
        userId: String,
        handle: String,
        lastUpdated: com.google.firebase.Timestamp,
        force: Boolean
    ) {
        if (handle.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                youtubeRepository.refreshIfStale(userId, handle, lastUpdated, force, userRepository)
            }
        }
    }

    fun resetEvent() { _event.value = null }

    /**
     * Persists the common user fields plus role-specific fields. Role is
     * never updated — it is permanent at registration.
     */
    fun saveAccount(
        fullName: String,
        phone: String,
        securityQuestion: String,
        securityAnswer: String,
        // creator-only
        bio: String,
        youtubeHandle: String,
        // brand-only
        website: String,
        linkedin: String
    ) {
        if (_isSaving.value) return

        val current = _state.value as? AccountSettingsState.Ready ?: run {
            _event.value = AccountSettingsEvent.SaveError("Account not loaded yet")
            return
        }
        val uid = current.user.userId
        val role = current.user.role

        val nameTrim = fullName.trim()
        val phoneTrim = phone.trim()
        val sqTrim = securityQuestion.trim()
        val saTrim = securityAnswer.trim()
        val bioTrim = bio.trim()
        val ytTrim = youtubeHandle.trim()
        val webTrim = website.trim()
        val liTrim = linkedin.trim()

        // Validation
        if (!ValidationUtils.isNonBlank(nameTrim)) {
            _event.value = AccountSettingsEvent.SaveError("Name cannot be blank")
            return
        }
        if (!ValidationUtils.isValidPhone(phoneTrim)) {
            _event.value = AccountSettingsEvent.SaveError("Phone number is invalid")
            return
        }
        if (sqTrim.isNotBlank() && saTrim.isBlank()) {
            _event.value = AccountSettingsEvent.SaveError("Security answer required")
            return
        }
        if (saTrim.isNotBlank() && sqTrim.isBlank()) {
            _event.value = AccountSettingsEvent.SaveError("Security question required")
            return
        }
        if (role == "brand") {
            if (!ValidationUtils.isValidUrl(webTrim)) {
                _event.value = AccountSettingsEvent.SaveError("Website is not a valid URL")
                return
            }
            if (!ValidationUtils.isValidUrl(liTrim)) {
                _event.value = AccountSettingsEvent.SaveError("LinkedIn is not a valid URL")
                return
            }
        }

        viewModelScope.launch {
            _isSaving.value = true
            _event.value = AccountSettingsEvent.SaveStarted

            val userUpdates = mutableMapOf<String, Any>(
                "displayName" to nameTrim,
                "phone" to phoneTrim,
                "securityQuestion" to sqTrim,
                "securityAnswer" to saTrim
            )
            val userResult = accountRepository.saveCommonUserFields(uid, userUpdates)
            if (userResult.isFailure) {
                _event.value = AccountSettingsEvent.SaveError(
                    userResult.exceptionOrNull()?.message ?: "Failed to save account"
                )
                _isSaving.value = false
                return@launch
            }

            val previousHandle = current.creatorProfile?.youtubeHandle.orEmpty()

            val roleResult = when (role) {
                "creator" -> {
                    val updates = mutableMapOf<String, Any>(
                        "bio" to bioTrim,
                        "youtubeHandle" to ytTrim
                    )
                    accountRepository.saveCreatorFields(uid, updates)
                }
                "brand" -> {
                    val updates = mutableMapOf<String, Any>(
                        "bio" to bioTrim,
                        "brandName" to nameTrim,
                        "website" to webTrim,
                        "linkedinUrl" to liTrim
                    )
                    accountRepository.saveBrandFields(uid, updates)
                }
                else -> Result.success(Unit)
            }

            if (roleResult.isFailure) {
                _event.value = AccountSettingsEvent.SaveError(
                    roleResult.exceptionOrNull()?.message ?: "Failed to save profile"
                )
                _isSaving.value = false
                return@launch
            }

            // Refresh local snapshot so subsequent edits start from saved state.
            val refreshed = accountRepository.loadSnapshot(uid)
            refreshed.getOrNull()?.let {
                _state.value = AccountSettingsState.Ready(it.user, it.creatorProfile, it.brandProfile)
            }

            _event.value = AccountSettingsEvent.SaveSuccess
            _isSaving.value = false

            // Force-refresh YT analytics when the handle just changed; otherwise rely on
            // the 7-day TTL check (no-op if the cache is still fresh).
            if (role == "creator") {
                val handleChanged = previousHandle.trim() != ytTrim
                val lastUpdated = refreshed.getOrNull()?.creatorProfile?.youtubeAnalyticsUpdatedAt
                    ?: com.google.firebase.Timestamp(0, 0)
                maybeRefreshYouTubeAnalytics(uid, ytTrim, lastUpdated, force = handleChanged)
            }
        }
    }

    /**
     * Changes the signed-in user's Firebase Auth password.
     * Re-authenticates first; never writes the password to Firestore.
     */
    fun changePassword(currentPassword: String, newPassword: String, confirmPassword: String) {
        if (_isChangingPassword.value) return

        if (currentPassword.isBlank()) {
            _event.value = AccountSettingsEvent.PasswordError("Enter your current password")
            return
        }
        val pwError = ValidationUtils.passwordError(newPassword)
        if (pwError != null) {
            _event.value = AccountSettingsEvent.PasswordError(pwError)
            return
        }
        if (!ValidationUtils.doPasswordsMatch(newPassword, confirmPassword)) {
            _event.value = AccountSettingsEvent.PasswordError("Passwords don't match")
            return
        }

        viewModelScope.launch {
            _isChangingPassword.value = true
            _event.value = AccountSettingsEvent.PasswordStarted

            val reauth = authRepository.reauthenticate(currentPassword)
            if (reauth.isFailure) {
                _event.value = AccountSettingsEvent.PasswordError("Current password is incorrect")
                _isChangingPassword.value = false
                return@launch
            }

            val update = authRepository.updatePassword(newPassword)
            if (update.isFailure) {
                _event.value = AccountSettingsEvent.PasswordError(
                    update.exceptionOrNull()?.message ?: "Could not update password"
                )
                _isChangingPassword.value = false
                return@launch
            }

            _event.value = AccountSettingsEvent.PasswordSuccess
            _isChangingPassword.value = false
        }
    }

    class Factory(
        private val accountRepository: AccountSettingsRepository = AccountSettingsRepository(),
        private val authRepository: AuthRepository = AuthRepository()
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            AccountSettingsViewModel(accountRepository, authRepository) as T
    }
}

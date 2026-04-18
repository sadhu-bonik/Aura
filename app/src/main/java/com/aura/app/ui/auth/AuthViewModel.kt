package com.aura.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.aura.app.data.model.User
import com.aura.app.data.repository.AuthRepository
import com.aura.app.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AuthState {
    data object Idle : AuthState()
    data object Loading : AuthState()
    data class Success(val user: User) : AuthState()
    data class Error(val message: String) : AuthState()
}

/**
 * AuthViewModel - Handles login, logout, and session-status checks.
 *
 * Registration is handled by [RegistrationViewModel]; this ViewModel is solely
 * responsible for the sign-in flow and profile observations once the user is
 * already authenticated.
 */
class AuthViewModel(
    private val authRepository: AuthRepository = AuthRepository(),
    private val userRepository: UserRepository = UserRepository()
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    /**
     * Signs in a user via Firebase Auth and loads their Firestore profile.
     * Firebase Auth handles all secure password verification — we never store or
     * compare passwords ourselves.
     */
    fun loginUser(email: String, password: String, context: android.content.Context) {
        if (email.isBlank() || password.isBlank()) {
            _authState.value = AuthState.Error("Fields cannot be empty")
            return
        }

        viewModelScope.launch {
            _authState.value = AuthState.Loading

            // Step 1: Firebase Auth — verifies password server-side
            val authResult = authRepository.login(email, password)
            if (authResult.isFailure) {
                val msg = authResult.exceptionOrNull()?.message ?: "Login failed"
                android.util.Log.e("AuthViewModel", "Firebase Auth failed: $msg")
                _authState.value = AuthState.Error(msg)
                return@launch
            }

            val firebaseUser = authResult.getOrThrow()
            android.util.Log.d("AuthViewModel", "Auth OK uid=${firebaseUser.uid}")

            // Step 2: Load Firestore profile
            val userProfile = try {
                userRepository.getUserProfile(firebaseUser.uid)
            } catch (e: Exception) {
                android.util.Log.e("AuthViewModel", "getUserProfile threw: ${e.message}", e)
                null
            }

            if (userProfile == null) {
                android.util.Log.e("AuthViewModel", "No Firestore doc for uid=${firebaseUser.uid}. " +
                    "Account may have been created with old code. Delete and re-register.")
                _authState.value = AuthState.Error(
                    "Account setup incomplete. Please delete this account and register again."
                )
                authRepository.logout()
                return@launch
            }

            android.util.Log.d("AuthViewModel",
                "Profile loaded: role=${userProfile.role} isProfileComplete=${userProfile.isProfileComplete}")

            // Step 3: Persist session
            com.aura.app.utils.SessionManager(context).saveUserId(firebaseUser.uid)
            _authState.value = AuthState.Success(userProfile)
        }
    }

    /**
     * Partial update to support subsequent steps without re-creating the User doc.
     */
    fun updateProfile(userId: String, updates: Map<String, Any>) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = userRepository.updateUserPartial(userId, updates)
            if (result.isSuccess) {
                val user = userRepository.getUserProfile(userId)
                if (user != null) {
                    _authState.value = AuthState.Success(user)
                } else {
                    _authState.value = AuthState.Error("Profile updated but could not be fetched")
                }
            } else {
                _authState.value = AuthState.Error(
                    result.exceptionOrNull()?.message ?: "Update failed"
                )
            }
        }
    }

    /**
     * Sends a Firebase password-reset email to the given address.
     */
    fun resetPassword(email: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = authRepository.resetPassword(email)
            if (result.isFailure) {
                _authState.value = AuthState.Error(
                    result.exceptionOrNull()?.message ?: "Password reset failed"
                )
            } else {
                _authState.value = AuthState.Idle
            }
        }
    }

    /**
     * Signs the current user out of Firebase Auth and clears the local session.
     */
    fun logout(context: android.content.Context) {
        com.aura.app.utils.SessionManager(context).clearSession()
        authRepository.logout()
        _authState.value = AuthState.Idle
    }

    /** Resets state to Idle. Useful after consuming a one-shot Success/Error. */
    fun resetState() {
        _authState.value = AuthState.Idle
    }

    class Factory(
        private val authRepository: AuthRepository = AuthRepository(),
        private val userRepository: UserRepository = UserRepository()
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            AuthViewModel(authRepository, userRepository) as T
    }
}

package com.aura.app.ui.main

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.aura.app.data.model.User
import com.aura.app.data.repository.UserRepository
import com.aura.app.utils.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ProfileUiState {
    data object Loading : ProfileUiState()
    data class Success(val user: User) : ProfileUiState()
    data class Error(val message: String) : ProfileUiState()
}

/**
 * ProfileViewModel - Loads and exposes the signed-in user's Firestore profile.
 */
class ProfileViewModel(
    private val userRepository: UserRepository = UserRepository(),
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val _state = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            _state.value = ProfileUiState.Loading
            val userId = sessionManager.getUserId()
            if (userId == null) {
                _state.value = ProfileUiState.Error("Not signed in")
                return@launch
            }
            val user = userRepository.getUserProfile(userId)
            _state.value = if (user != null) {
                ProfileUiState.Success(user)
            } else {
                ProfileUiState.Error("Could not load profile")
            }
        }
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ProfileViewModel(
                userRepository = UserRepository(),
                sessionManager = SessionManager(context)
            ) as T
    }
}

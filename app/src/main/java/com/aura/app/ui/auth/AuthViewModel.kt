package com.aura.app.ui.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aura.app.data.repository.AuthRepository
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.launch

/**
 * AuthViewModel - Handles simple login and session status checks.
 */
class AuthViewModel(
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _currentUser = MutableLiveData<FirebaseUser?>(authRepository.currentUser)
    val currentUser: LiveData<FirebaseUser?> = _currentUser

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    private val _loginSuccess = MutableLiveData<Boolean>(false)
    val loginSuccess: LiveData<Boolean> = _loginSuccess

    /**
     * Signs in a user with email and password.
     */
    fun login(email: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            // Bypass actual verification for now as requested
            // val result = authRepository.login(email, password)
            // if (result.isSuccess) {
            //     _currentUser.value = result.getOrNull()
            //     _loginSuccess.value = true
            // } else {
            //     _error.value = result.exceptionOrNull()?.message ?: "Login failed"
            // }
            _loginSuccess.value = true
            _isLoading.value = false
        }
    }

    /**
     * Resets password for a given email.
     */
    fun resetPassword(email: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            val result = authRepository.resetPassword(email)
            if (result.isFailure) {
                _error.value = result.exceptionOrNull()?.message ?: "Password reset failed"
            }
            _isLoading.value = false
        }
    }

    /**
     * Signs out the current user.
     */
    fun logout() {
        authRepository.logout()
        _currentUser.value = null
    }

    /**
     * Refreshes the current user session state.
     */
    fun refreshSession() {
        _currentUser.value = authRepository.currentUser
    }
}

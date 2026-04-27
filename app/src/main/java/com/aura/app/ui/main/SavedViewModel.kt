package com.aura.app.ui.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.aura.app.data.repository.ShortlistRepository
import com.aura.app.data.repository.UserRepository
import com.aura.app.utils.Constants
import com.aura.app.utils.StubSession
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SavedCreatorUI(
    val userId: String,
    val displayName: String,
    val profileImageUrl: String,
    val headline: String,
    val averageRating: Double
)

class SavedViewModel(
    private val shortlistRepository: ShortlistRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val auth = FirebaseAuth.getInstance()

    private val _uiState = MutableLiveData<List<SavedCreatorUI>>()
    val uiState: LiveData<List<SavedCreatorUI>> = _uiState

    private val _isLoading = MutableLiveData(true)
    val isLoading: LiveData<Boolean> = _isLoading

    fun loadSavedCreators() {
        val uid = if (Constants.USE_STUBS) StubSession.userId() else auth.currentUser?.uid
        if (uid == null) {
            _isLoading.value = false
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val savedIds = withContext(Dispatchers.IO) {
                    shortlistRepository.getShortlistedCreatorIds(uid)
                }

                if (savedIds.isEmpty()) {
                    _uiState.value = emptyList()
                    _isLoading.value = false
                    return@launch
                }

                // Fetch all profiles in parallel
                val creatorUIs = withContext(Dispatchers.IO) {
                    savedIds.map { creatorId ->
                        async {
                            val user = userRepository.getUserProfile(creatorId)
                            val profile = userRepository.getCreatorProfile(creatorId)

                            if (user != null) {
                                SavedCreatorUI(
                                    userId = user.userId,
                                    displayName = user.displayName,
                                    profileImageUrl = user.profileImageUrl,
                                    headline = profile?.bio ?: "",
                                    averageRating = profile?.averageRating ?: 0.0
                                )
                            } else {
                                null
                            }
                        }
                    }.awaitAll().filterNotNull()
                }

                _uiState.value = creatorUIs
            } catch (e: Exception) {
                // Return empty on error
                _uiState.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SavedViewModel(ShortlistRepository(), UserRepository()) as T
        }
    }
}

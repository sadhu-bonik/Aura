package com.aura.app.ui.feed

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.aura.app.data.repository.ShortlistRepository
import com.aura.app.utils.Constants
import com.aura.app.utils.StubSession
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class FeedActionsViewModel(
    private val shortlistRepository: ShortlistRepository,
) : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val _isBrand = MutableLiveData(false)
    val isBrand: LiveData<Boolean> = _isBrand

    private val _isShortlisted = MutableLiveData(false)
    val isShortlisted: LiveData<Boolean> = _isShortlisted

    private var currentCreatorId: String? = null

    init {
        loadUserRole()
    }

    fun setCurrentCreator(creatorId: String) {
        if (creatorId == currentCreatorId) return
        currentCreatorId = creatorId
        checkShortlistStatus()
    }

    fun getCurrentCreatorId(): String? = currentCreatorId

    fun getCurrentUserId(): String? {
        if (Constants.USE_STUBS) return StubSession.userId()
        return auth.currentUser?.uid
    }

    fun toggleShortlist() {
        val uid = auth.currentUser?.uid ?: return
        val creatorId = currentCreatorId ?: return
        if (uid == creatorId) return // Cannot save yourself
        viewModelScope.launch {
            val newState = withContext(Dispatchers.IO) {
                runCatching { shortlistRepository.toggleShortlist(uid, creatorId) }.getOrNull()
            }
            if (newState != null) _isShortlisted.value = newState
        }
    }

    private fun loadUserRole() {
        if (Constants.USE_STUBS) {
            _isBrand.value = StubSession.role() == Constants.ROLE_BRAND
            return
        }
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            val role = withContext(Dispatchers.IO) {
                runCatching {
                    db.collection("users").document(uid).get().await()
                        .getString("role")
                }.getOrNull()
            }
            _isBrand.value = role == "brand"
        }
    }

    private fun checkShortlistStatus() {
        val uid = auth.currentUser?.uid ?: return
        val creatorId = currentCreatorId ?: return
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching { shortlistRepository.isShortlisted(uid, creatorId) }.getOrNull()
            }
            _isShortlisted.value = result == true
        }
    }

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return FeedActionsViewModel(ShortlistRepository()) as T
        }
    }
}

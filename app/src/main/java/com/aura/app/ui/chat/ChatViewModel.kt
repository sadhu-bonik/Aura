package com.aura.app.ui.chat

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.aura.app.data.model.Deal
import com.aura.app.data.model.Message
import com.aura.app.data.model.UserLite
import com.aura.app.data.repository.DealRepository
import com.aura.app.data.repository.MessageRepository
import com.aura.app.data.repository.UserRepository
import com.aura.app.firebase.StorageManager
import com.aura.app.utils.Constants
import com.aura.app.utils.StubData
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val dealRepository = DealRepository()
    private val messageRepository = MessageRepository()
    private val userRepository = UserRepository()
    private val storageManager = StorageManager(application)

    private val _messages = MutableLiveData<List<Message>>()
    val messages: LiveData<List<Message>> = _messages

    private val _deal = MutableLiveData<Deal>()
    val deal: LiveData<Deal> = _deal

    private val _otherUser = MutableLiveData<UserLite?>()
    val otherUser: LiveData<UserLite?> = _otherUser

    private val _isLoading = MutableLiveData(true)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _isUploading = MutableLiveData(false)
    val isUploading: LiveData<Boolean> = _isUploading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _sendError = MutableLiveData<String?>()
    val sendError: LiveData<String?> = _sendError

    private var dealId: String = ""

    fun load(dealId: String, currentUserId: String) {
        this.dealId = dealId
        _isLoading.value = true
        _error.value = null

        viewModelScope.launch {
            val deal = if (Constants.USE_STUBS) {
                StubData.deals.firstOrNull { it.dealId == dealId }
            } else {
                dealRepository.getDeal(dealId).getOrNull()
            }

            if (deal == null) {
                _error.value = "Deal not found"
                _isLoading.value = false
                return@launch
            }
            _deal.value = deal

            val otherUserId = if (deal.creatorId == currentUserId) deal.brandId else deal.creatorId
            _otherUser.value = userRepository.getUserLite(otherUserId)

            messageRepository.streamMessages(dealId)
                .catch { e ->
                    _error.value = e.message
                    _isLoading.value = false
                }
                .collect { msgs ->
                    _messages.value = msgs
                    _isLoading.value = false
                }
        }
    }

    fun sendMessage(content: String, senderId: String, receiverId: String) {
        if (content.isBlank() || _isUploading.value == true) return
        viewModelScope.launch {
            if (Constants.USE_STUBS) {
                messageRepository.sendMessageDirect(dealId, senderId, receiverId, content.trim())
                    .onFailure { _sendError.value = it.message }
            } else {
                messageRepository.sendMessage(dealId, senderId, receiverId, content.trim())
                    .onFailure { _sendError.value = it.message }
            }
        }
    }

    fun sendAttachment(uri: Uri, mimeType: String, fileName: String, senderId: String, receiverId: String) {
        if (_isUploading.value == true) return
        _isUploading.value = true
        viewModelScope.launch {
            storageManager.uploadChatAttachment(dealId, uri, mimeType)
                .fold(
                    onSuccess = { downloadUrl ->
                        val mediaType = when {
                            mimeType.startsWith("image/") -> "image"
                            mimeType.startsWith("video/") -> "video"
                            else -> "file"
                        }
                        messageRepository.sendMediaMessage(
                            dealId = dealId,
                            senderId = senderId,
                            receiverId = receiverId,
                            downloadUrl = downloadUrl,
                            mediaType = mediaType,
                            fileName = fileName,
                        ).onFailure { _sendError.value = it.message }
                    },
                    onFailure = { _sendError.value = it.message }
                )
            _isUploading.value = false
        }
    }

    fun markAsRead(currentUserId: String) {
        viewModelScope.launch {
            messageRepository.markMessagesAsRead(dealId, currentUserId)
        }
    }

    fun resolveReceiverId(currentUserId: String): String {
        val d = _deal.value ?: return ""
        return if (d.creatorId == currentUserId) d.brandId else d.creatorId
    }
}

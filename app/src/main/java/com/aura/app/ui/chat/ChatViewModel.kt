package com.aura.app.ui.chat

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.aura.app.R
import com.aura.app.data.model.Deal
import com.aura.app.data.model.Message
import com.aura.app.data.model.UserLite
import com.aura.app.data.repository.DealRepository
import com.aura.app.data.repository.MessageRepository
import com.aura.app.data.repository.UserRepository
import com.aura.app.firebase.StorageManager
import com.aura.app.utils.Constants
import com.aura.app.utils.StubSession
import com.aura.app.utils.StubState
import com.google.firebase.Timestamp
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.util.UUID

sealed class ChatListItem {
    data class RegularMessage(val message: Message) : ChatListItem()
    data class FailedMessage(
        val tempId: String = UUID.randomUUID().toString(),
        val dealId: String,
        val senderId: String,
        val receiverId: String,
        val content: String,
        val mediaUrl: String = "",
        val mediaType: String = "text",
        val fileName: String = "",
        val sentAt: Timestamp = Timestamp.now(),
    ) : ChatListItem()
}

sealed class CompletionRequestState {
    object None : CompletionRequestState()
    object OutgoingFromMe : CompletionRequestState()
    data class IncomingFromOther(val initiatorName: String) : CompletionRequestState()
}

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val dealRepository = DealRepository()
    private val messageRepository = MessageRepository()
    private val userRepository = UserRepository()
    private val storageManager = StorageManager(application)

    private val _firestoreMessages = MutableLiveData<List<Message>>()
    private val _failedMessages = MutableLiveData<List<ChatListItem.FailedMessage>>(emptyList())

    private val _chatItems = MutableLiveData<List<ChatListItem>>()
    val chatItems: LiveData<List<ChatListItem>> = _chatItems

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

    private val _completionRequest = MutableLiveData<CompletionRequestState>(CompletionRequestState.None)
    val completionRequest: LiveData<CompletionRequestState> = _completionRequest

    private var dealId: String = ""
    private var currentUserId: String = ""
    private var dealObserveJob: Job? = null

    private fun rebuildChatItems() {
        val confirmed = _firestoreMessages.value?.map { ChatListItem.RegularMessage(it) } ?: emptyList()
        val failed = _failedMessages.value ?: emptyList()
        _chatItems.value = confirmed + failed
    }

    fun load(dealId: String, currentUserId: String) {
        this.dealId = dealId
        this.currentUserId = currentUserId
        _isLoading.value = true
        _error.value = null

        if (Constants.USE_STUBS) {
            dealObserveJob?.cancel()
            dealObserveJob = viewModelScope.launch {
                StubState.dealsFlow.collect { deals ->
                    val deal = deals.firstOrNull { it.dealId == dealId }
                    if (deal != null) {
                        _deal.value = deal
                        updateCompletionState(deal, currentUserId)
                    }
                }
            }

            val deal = StubState.currentDeals().firstOrNull { it.dealId == dealId }
            if (deal == null) {
                _error.value = "Deal not found"
                _isLoading.value = false
                return
            }
            _deal.value = deal
            updateCompletionState(deal, currentUserId)

            viewModelScope.launch {
                val otherUserId = if (deal.creatorId == currentUserId) deal.brandId else deal.creatorId
                _otherUser.value = userRepository.getUserLite(otherUserId)

                messageRepository.streamMessages(dealId)
                    .catch { e ->
                        _error.value = e.message
                        _isLoading.value = false
                    }
                    .collect { msgs ->
                        _firestoreMessages.value = msgs
                        rebuildChatItems()
                        _isLoading.value = false
                    }
            }
            return
        }

        viewModelScope.launch {
            val deal = dealRepository.getDeal(dealId).getOrNull()
            if (deal == null) {
                _error.value = "Deal not found"
                _isLoading.value = false
                return@launch
            }
            _deal.value = deal
            updateCompletionState(deal, currentUserId)

            val otherUserId = if (deal.creatorId == currentUserId) deal.brandId else deal.creatorId
            _otherUser.value = userRepository.getUserLite(otherUserId)

            messageRepository.streamMessages(dealId)
                .catch { e ->
                    _error.value = e.message
                    _isLoading.value = false
                }
                .collect { msgs ->
                    _firestoreMessages.value = msgs
                    rebuildChatItems()
                    _isLoading.value = false
                }
        }
    }

    private fun updateCompletionState(deal: Deal, myUserId: String) {
        _completionRequest.value = when {
            deal.completionRequestedBy.isEmpty() -> CompletionRequestState.None
            deal.completionRequestedBy == myUserId -> CompletionRequestState.OutgoingFromMe
            else -> {
                val name = StubSession.displayName()
                CompletionRequestState.IncomingFromOther(name)
            }
        }
    }

    fun respondToCompletion(accepted: Boolean) {
        viewModelScope.launch {
            if (accepted) {
                if (Constants.USE_STUBS) {
                    StubState.updateDealStatus(dealId, Constants.STATUS_COMPLETED, chatUnlocked = true)
                } else {
                    dealRepository.confirmCompletion(dealId)
                }
            } else {
                val systemText = getApplication<Application>().getString(R.string.system_msg_completion_declined)
                if (Constants.USE_STUBS) {
                    StubState.clearCompletionRequest(dealId)
                } else {
                    dealRepository.declineCompletion(dealId)
                }
                messageRepository.sendSystemMessage(dealId, systemText)
            }
        }
    }

    fun sendMessage(content: String, senderId: String, receiverId: String) {
        if (content.isBlank() || _isUploading.value == true) return
        viewModelScope.launch {
            val result = if (Constants.USE_STUBS) {
                val res = messageRepository.sendMessageDirect(dealId, senderId, receiverId, content.trim())
                if (res.isSuccess) {
                    StubState.updateLastMessage(dealId, content.trim(), Timestamp.now())
                }
                res
            } else {
                messageRepository.sendMessage(dealId, senderId, receiverId, content.trim())
            }
            result.onFailure {
                addFailedMessage(ChatListItem.FailedMessage(
                    dealId = dealId,
                    senderId = senderId,
                    receiverId = receiverId,
                    content = content.trim(),
                ))
            }
        }
    }

    fun retryFailed(item: ChatListItem.FailedMessage) {
        removeFailedMessage(item.tempId)
        viewModelScope.launch {
            val result = if (Constants.USE_STUBS) {
                messageRepository.sendMessageDirect(item.dealId, item.senderId, item.receiverId, item.content)
            } else {
                messageRepository.sendMessage(item.dealId, item.senderId, item.receiverId, item.content)
            }
            result.onFailure {
                addFailedMessage(item.copy(tempId = UUID.randomUUID().toString()))
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
                        val res = if (Constants.USE_STUBS) {
                            val msgRes = messageRepository.sendMediaMessageDirect(
                                dealId = dealId,
                                senderId = senderId,
                                receiverId = receiverId,
                                downloadUrl = downloadUrl,
                                mediaType = mediaType,
                                fileName = fileName,
                            )
                            if (msgRes.isSuccess) {
                                val preview = when (mediaType) {
                                    "image" -> "Photo"
                                    "video" -> "Video"
                                    "file" -> fileName.ifBlank { "File" }
                                    else -> "Attachment"
                                }
                                StubState.updateLastMessage(dealId, preview, Timestamp.now())
                            }
                            msgRes
                        } else {
                            messageRepository.sendMediaMessage(
                                dealId = dealId,
                                senderId = senderId,
                                receiverId = receiverId,
                                downloadUrl = downloadUrl,
                                mediaType = mediaType,
                                fileName = fileName,
                            )
                        }
                        
                        res.onFailure {
                            addFailedMessage(ChatListItem.FailedMessage(
                                dealId = dealId,
                                senderId = senderId,
                                receiverId = receiverId,
                                content = "",
                                mediaUrl = downloadUrl,
                                mediaType = mediaType,
                                fileName = fileName,
                            ))
                        }
                    },
                    onFailure = {
                        addFailedMessage(ChatListItem.FailedMessage(
                            dealId = dealId,
                            senderId = senderId,
                            receiverId = receiverId,
                            content = "[Attachment failed to upload]",
                        ))
                    }
                )
            _isUploading.value = false
        }
    }

    fun markAsRead(currentUserId: String) {
        viewModelScope.launch {
            if (Constants.USE_STUBS) {
                StubState.clearUnreadCount(dealId, currentUserId)
            }
            messageRepository.markMessagesAsRead(dealId, currentUserId)
        }
    }

    fun resolveReceiverId(currentUserId: String): String {
        val d = _deal.value ?: return ""
        return if (d.creatorId == currentUserId) d.brandId else d.creatorId
    }

    private fun addFailedMessage(item: ChatListItem.FailedMessage) {
        _failedMessages.value = (_failedMessages.value ?: emptyList()) + item
        rebuildChatItems()
    }

    private fun removeFailedMessage(tempId: String) {
        _failedMessages.value = _failedMessages.value?.filterNot { it.tempId == tempId } ?: emptyList()
        rebuildChatItems()
    }
}

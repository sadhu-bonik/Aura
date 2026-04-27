package com.aura.app.ui.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aura.app.data.model.Notification
import com.aura.app.data.repository.AuthRepository
import com.aura.app.data.repository.NotificationRepository
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/**
 * NotificationViewModel — drives the notification badge and inbox sheet.
 *
 * Observed by HomeContainerFragment (badge) and NotificationBottomSheet (list).
 */
class NotificationViewModel(
    private val notifRepo: NotificationRepository = NotificationRepository(),
    private val authRepo: AuthRepository = AuthRepository(),
) : ViewModel() {

    private val _unreadCount = MutableLiveData(0)
    val unreadCount: LiveData<Int> = _unreadCount

    private val _notifications = MutableLiveData<List<Notification>>(emptyList())
    val notifications: LiveData<List<Notification>> = _notifications

    init {
        val uid = authRepo.currentUser?.uid
        if (uid != null) {
            observeUnreadCount(uid)
            observeNotifications(uid)
        }
    }

    private fun observeUnreadCount(userId: String) {
        viewModelScope.launch {
            notifRepo.getUnreadCount(userId)
                .catch { android.util.Log.e("NotifVM", "Badge error", it) }
                .collect { count -> _unreadCount.value = count }
        }
    }

    private fun observeNotifications(userId: String) {
        viewModelScope.launch {
            notifRepo.getNotifications(userId)
                .catch { android.util.Log.e("NotifVM", "Inbox error", it) }
                .collect { list -> _notifications.value = list }
        }
    }

    /** Mark a single notification as read (called when the user taps it). */
    fun markRead(notifId: String) {
        if (notifId.isBlank()) return
        viewModelScope.launch { notifRepo.markRead(notifId) }
    }

    /** Bulk clear — wired to the "Mark all read" link in the inbox header. */
    fun markAllRead() {
        val uid = authRepo.currentUser?.uid ?: return
        viewModelScope.launch {
            notifRepo.markAllRead(uid)
        }
    }
}

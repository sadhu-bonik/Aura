package com.aura.app.ui.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aura.app.data.model.Notification
import com.aura.app.data.repository.NotificationRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/**
 * NotificationViewModel — drives the notification badge and inbox sheet.
 *
 * Activity-scoped, so it survives across login/logout. To avoid leaking the previous
 * user's notifications into the new session, the streams are torn down and rebound
 * on every FirebaseAuth user change (signIn, signOut, account switch).
 */
class NotificationViewModel(
    private val notifRepo: NotificationRepository = NotificationRepository(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
) : ViewModel() {

    private val _unreadCount = MutableLiveData(0)
    val unreadCount: LiveData<Int> = _unreadCount

    private val _notifications = MutableLiveData<List<Notification>>(emptyList())
    val notifications: LiveData<List<Notification>> = _notifications

    private var boundUid: String? = null
    private var unreadJob: Job? = null
    private var listJob: Job? = null

    private val authListener = FirebaseAuth.AuthStateListener { fa ->
        rebind(fa.currentUser?.uid)
    }

    init {
        // Bind to whatever user is currently signed in, then react to changes.
        auth.addAuthStateListener(authListener)
        rebind(auth.currentUser?.uid)
    }

    /**
     * Cancel any in-flight collectors, reset the LiveData, and (if a user is signed in)
     * re-attach Firestore listeners scoped strictly to that uid.
     */
    private fun rebind(uid: String?) {
        if (uid == boundUid) return
        boundUid = uid

        unreadJob?.cancel()
        listJob?.cancel()
        _unreadCount.postValue(0)
        _notifications.postValue(emptyList())

        if (uid.isNullOrBlank()) return

        unreadJob = viewModelScope.launch {
            notifRepo.getUnreadCount(uid)
                .distinctUntilChanged()
                .catch { 
                    android.util.Log.e("NotifVM", "Badge error", it)
                    _unreadCount.postValue(0)
                }
                .collect { count -> _unreadCount.postValue(count) }
        }
        listJob = viewModelScope.launch {
            notifRepo.getNotifications(uid)
                .distinctUntilChanged()
                .catch { 
                    android.util.Log.e("NotifVM", "Inbox error", it)
                    _notifications.postValue(emptyList())
                }
                .collect { list -> _notifications.postValue(list) }
        }
    }

    /** Mark a single notification as read (called when the user taps it). */
    fun markRead(notifId: String) {
        if (notifId.isBlank()) return
        viewModelScope.launch { notifRepo.markRead(notifId) }
    }

    /** Bulk clear — wired to the "Mark all read" link in the inbox header. */
    fun markAllRead() {
        val uid = boundUid ?: return
        viewModelScope.launch {
            notifRepo.markAllRead(uid)
        }
    }

    override fun onCleared() {
        auth.removeAuthStateListener(authListener)
        super.onCleared()
    }
}

package com.aura.app.utils

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object CurrentUser {
    fun uid(): String = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    suspend fun role(): String {
        val id = uid()
        if (id.isEmpty()) return ""
        return runCatching {
            FirebaseFirestore.getInstance()
                .collection(Constants.COLLECTION_USERS)
                .document(id).get().await()
                .getString("role") ?: ""
        }.getOrDefault("")
    }
}

package com.aura.app.data.repository

import com.aura.app.data.model.UserLite
import com.aura.app.utils.Constants
import com.aura.app.utils.StubData
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.concurrent.ConcurrentHashMap

class UserRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    private val cache = ConcurrentHashMap<String, UserLite>()

    suspend fun getUserLite(userId: String): UserLite? {
        if (Constants.USE_STUBS) return StubData.users[userId]
        cache[userId]?.let { return it }
        val doc = firestore.collection(COLLECTION).document(userId).get().await()
        val user = doc.toObject(UserLite::class.java) ?: return null
        cache[userId] = user
        return user
    }

    private companion object {
        const val COLLECTION = "users"
    }
}

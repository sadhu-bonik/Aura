package com.aura.app.data.repository

import com.aura.app.data.model.User
import com.aura.app.data.model.UserLite
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.concurrent.ConcurrentHashMap

class UserRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    private val cache = ConcurrentHashMap<String, UserLite>()

    /**
     * Saves a complete user profile to Firestore.
     */
    suspend fun createUserProfile(user: User): Result<Unit> {
        return try {
            firestore.collection(COLLECTION).document(user.userId).set(user).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Retrieves a complete user profile from Firestore.
     */
    suspend fun getUserProfile(userId: String): User? {
        return try {
            val doc = firestore.collection(COLLECTION).document(userId).get().await()
            doc.toObject(User::class.java)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getUserLite(userId: String): UserLite? {
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


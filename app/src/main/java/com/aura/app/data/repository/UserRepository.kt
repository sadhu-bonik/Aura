package com.aura.app.data.repository

import com.aura.app.data.model.User
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

    /**
     * Setups a new user. It writes the base user record to "users",
     * and an initial profile into either "creatorProfiles" or "brandProfiles".
     */
    suspend fun setupNewUser(
        user: User,
        creatorProfile: com.aura.app.data.model.CreatorProfile? = null,
        brandProfile: com.aura.app.data.model.BrandProfile? = null // kept for signature compat; unused for brand (handled by BrandRegistrationRepository)
    ): Result<Unit> {
        return try {
            firestore.collection(COLLECTION).document(user.userId).set(user).await()
            if (user.role == "creator") {
                val profile = creatorProfile ?: com.aura.app.data.model.CreatorProfile(userId = user.userId)
                firestore.collection("creatorProfiles").document(user.userId).set(profile).await()
            }
            // Brand Firestore writes are handled by BrandRegistrationRepository directly.
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Updates an existing user profile partially.
     */
    suspend fun updateUserPartial(userId: String, updates: Map<String, Any>): Result<Unit> {
        return try {
            firestore.collection(COLLECTION).document(userId).update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Updates Creator fields in creatorProfiles schema.
     */
    suspend fun updateCreatorProfilePartial(userId: String, updates: Map<String, Any>): Result<Unit> {
        return try {
            firestore.collection("creatorProfiles").document(userId).update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Retrieves a complete user profile from Firestore by email.
     */
    suspend fun getUserByEmail(email: String): User? {
        return try {
            val snapshot = firestore.collection(COLLECTION)
                .whereEqualTo("email", email)
                .get()
                .await()
            if (snapshot.isEmpty) null else snapshot.documents.first().toObject(User::class.java)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Retrieves a complete user profile from Firestore.
     */
    suspend fun getUserProfile(userId: String): User? {
        return try {
            val doc = firestore.collection(COLLECTION).document(userId).get().await()
            if (!doc.exists()) return null

            val data = doc.data ?: return null

            // Support both new schema (userId) and old brand schema (uid field).
            // Read every field directly from the map to avoid Kotlin boolean getter
            // naming issues with Firestore's bean introspection (isXxx vs getXxx).
            val resolvedUserId = (data["userId"] as? String)
                ?.takeIf { it.isNotBlank() }
                ?: (data["uid"] as? String)
                ?: userId

            User(
                userId = resolvedUserId,
                email = (data["email"] as? String) ?: "",
                role = (data["role"] as? String) ?: "",
                displayName = (data["displayName"] as? String)
                    ?: (data["brandName"] as? String)
                    ?: "",
                profileImageUrl = (data["profileImageUrl"] as? String) ?: "",
                phone = (data["phone"] as? String) ?: "",
                securityQuestion = (data["securityQuestion"] as? String) ?: "",
                securityAnswer = (data["securityAnswer"] as? String) ?: "",
                isProfileComplete = (data["isProfileComplete"] as? Boolean) ?: false,
                fcmToken = (data["fcmToken"] as? String) ?: ""
            )
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getUserLite(userId: String): UserLite? {
        if (Constants.USE_STUBS) return StubData.users[userId]
        cache[userId]?.let { return it }
        return try {
            val doc = firestore.collection(COLLECTION).document(userId).get().await()
            val user = doc.toObject(UserLite::class.java) ?: return null
            cache[userId] = user
            user
        } catch (e: Exception) {
            null
        }
    }

    private companion object {
        const val COLLECTION = "users"
    }
}


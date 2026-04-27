package com.aura.app.data.repository

import com.aura.app.data.model.BrandProfile
import com.aura.app.data.model.CreatorProfile
import com.aura.app.data.model.User
import com.google.firebase.firestore.FieldValue

/**
 * AccountSettingsRepository — thin wrapper that aggregates the Firestore reads
 * and writes the Account Settings screen needs.
 *
 * Delegates all I/O to the existing [UserRepository] so we keep a single
 * Firebase-touching layer. ViewModels never call Firestore directly.
 */
class AccountSettingsRepository(
    private val userRepository: UserRepository = UserRepository()
) {
    data class Snapshot(
        val user: User,
        val creatorProfile: CreatorProfile?,
        val brandProfile: BrandProfile?
    )

    suspend fun loadSnapshot(userId: String): Result<Snapshot> {
        return try {
            val user = userRepository.getUserProfile(userId)
                ?: return Result.failure(IllegalStateException("Account not found"))
            val creator = if (user.role == "creator") userRepository.getCreatorProfile(userId) else null
            val brand = if (user.role == "brand") userRepository.getBrandProfile(userId) else null
            Result.success(Snapshot(user, creator, brand))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveCommonUserFields(userId: String, updates: Map<String, Any>): Result<Unit> {
        if (updates.isEmpty()) return Result.success(Unit)
        val payload = updates.toMutableMap().apply {
            put("updatedAt", FieldValue.serverTimestamp())
        }
        return userRepository.updateUserPartial(userId, payload)
    }

    suspend fun saveCreatorFields(userId: String, updates: Map<String, Any>): Result<Unit> {
        if (updates.isEmpty()) return Result.success(Unit)
        val payload = updates.toMutableMap().apply {
            put("updatedAt", FieldValue.serverTimestamp())
        }
        return userRepository.updateCreatorProfilePartial(userId, payload)
    }

    suspend fun saveBrandFields(userId: String, updates: Map<String, Any>): Result<Unit> {
        if (updates.isEmpty()) return Result.success(Unit)
        val payload = updates.toMutableMap().apply {
            put("updatedAt", FieldValue.serverTimestamp())
        }
        return userRepository.updateBrandProfilePartial(userId, payload)
    }
}

package com.aura.app.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.tasks.await

/**
 * AuthRepository - Handles interactions with Firebase Authentication.
 */
class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    /**
     * Gets the current logged-in user.
     */
    val currentUser: FirebaseUser?
        get() = auth.currentUser

    /**
     * Authenticates a user with email and password.
     */
    suspend fun login(email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            result.user?.let {
                Result.success(it)
            } ?: Result.failure(Exception("Login failed: User is null"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Creates a new user account with email and password.
     */
    suspend fun register(email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            result.user?.let {
                Result.success(it)
            } ?: Result.failure(Exception("Registration failed: User is null"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Sends a password reset email.
     */
    suspend fun resetPassword(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Logs the current user out.
     */
    fun logout() {
        auth.signOut()
    }

    suspend fun deleteCurrentUser(): Result<Unit> {
        return try {
            val user = auth.currentUser
                ?: return Result.failure(Exception("No signed-in user found."))
            user.delete().await()
            Result.success(Unit)
        } catch (e: FirebaseAuthRecentLoginRequiredException) {
            Result.failure(Exception("Please log out and log back in before deleting this account."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

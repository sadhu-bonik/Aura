package com.aura.app.data.repository

import com.google.firebase.auth.EmailAuthProvider
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
     * Re-authenticates the currently signed-in user with their existing password.
     * Required by Firebase before sensitive operations like updatePassword().
     */
    suspend fun reauthenticate(currentPassword: String): Result<Unit> {
        val user = auth.currentUser
            ?: return Result.failure(IllegalStateException("Not signed in"))
        val email = user.email
            ?: return Result.failure(IllegalStateException("No email on this account"))
        return try {
            val credential = EmailAuthProvider.getCredential(email, currentPassword)
            user.reauthenticate(credential).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Updates the signed-in user's password via Firebase Auth.
     * Caller MUST have re-authenticated first — Firebase rejects otherwise.
     * Plaintext passwords are NEVER persisted in Firestore or local storage.
     */
    suspend fun updatePassword(newPassword: String): Result<Unit> {
        val user = auth.currentUser
            ?: return Result.failure(IllegalStateException("Not signed in"))
        return try {
            user.updatePassword(newPassword).await()
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

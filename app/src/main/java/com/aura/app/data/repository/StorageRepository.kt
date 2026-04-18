package com.aura.app.data.repository

import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

/**
 * StorageRepository - Handles file uploads (images, videos) to Firebase Storage.
 */
class StorageRepository(
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
) {
    /**
     * Uploads a profile picture and returns the download URL.
     */
    suspend fun uploadProfilePicture(userId: String, uri: Uri): String {
        val ref = storage.reference.child("users/$userId/profile_photo.jpg")
        ref.putFile(uri).await()
        return ref.downloadUrl.await().toString()
    }

    /**
     * Uploads a verification document and returns the download URL.
     */
    suspend fun uploadVerificationDoc(userId: String, uri: Uri): String {
        val extension = try {
            uri.toString().substringAfterLast(".", "pdf").take(5)
        } catch (e: Exception) {
            "pdf"
        }
        // Normalize extension to avoid invalid characters (:, /, etc.)
        val safeExtension = extension.filter { it.isLetterOrDigit() }.ifBlank { "bin" }
        val ref = storage.reference.child("users/$userId/verification_doc.$safeExtension")
        ref.putFile(uri).await()
        return ref.downloadUrl.await().toString()
    }

    /**
     * Uploads a portfolio video and returns the download URL.
     */
    suspend fun uploadPortfolioVideo(userId: String, uri: Uri): String {
        val ref = storage.reference.child("users/$userId/portfolio/video_${System.currentTimeMillis()}.mp4")
        ref.putFile(uri).await()
        return ref.downloadUrl.await().toString()
    }
}

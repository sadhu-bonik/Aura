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
     * Result of a portfolio video upload, containing both the download URL and the
     * storage path (needed for future deletion or rollback).
     */
    data class UploadResult(
        val downloadUrl: String,
        val storagePath: String,
    )

    /**
     * Uploads a portfolio video and returns the download URL + storage path.
     * Uses the schema path: portfolioItems/{userId}/{itemId}.{ext}
     */
    suspend fun uploadPortfolioVideo(
        userId: String,
        itemId: String,
        uri: Uri,
        extension: String = "mp4",
    ): UploadResult {
        val path = "portfolioItems/$userId/$itemId.$extension"
        val ref = storage.reference.child(path)
        ref.putFile(uri).await()
        val downloadUrl = ref.downloadUrl.await().toString()
        return UploadResult(downloadUrl = downloadUrl, storagePath = path)
    }

    /**
     * Uploads a verification document and returns download URL + storage path.
     * Mirrors uploadVerificationDoc() but returns UploadResult for callers that
     * need the storage path (e.g. brand registration).
     */
    suspend fun uploadVerificationDocResult(userId: String, uri: Uri): UploadResult {
        val extension = try {
            uri.toString().substringAfterLast(".", "pdf").take(5)
        } catch (e: Exception) {
            "pdf"
        }
        val safeExtension = extension.filter { it.isLetterOrDigit() }.ifBlank { "bin" }
        val path = "users/$userId/verification_doc.$safeExtension"
        val ref = storage.reference.child(path)
        ref.putFile(uri).await()
        val downloadUrl = ref.downloadUrl.await().toString()
        return UploadResult(downloadUrl = downloadUrl, storagePath = path)
    }

    /**
     * Deletes a file in Firebase Storage by its path. Used for rollback on
     * Firestore write failure.
     */
    suspend fun deleteFile(storagePath: String) {
        storage.reference.child(storagePath).delete().await()
    }
}

package com.aura.app.data.repository

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * BrandRegistrationRepository
 *
 * All Firebase I/O for the brand registration flow:
 *   1. Firebase Auth → create user
 *   2. Storage → upload logo + verification doc (if provided)
 *   3. Firestore users/{uid}         → user document
 *   4. Firestore brandProfiles/{uid} → full brand profile with all URLs
 *
 * Returns Result<String> (the UID on success).
 */
class BrandRegistrationRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val storageRepository: StorageRepository = StorageRepository()
) {
    companion object {
        private const val USERS = "users"
        private const val BRAND_PROFILES = "brandProfiles"
    }

    suspend fun registerBrand(
        // Step 1
        brandName: String,
        email: String,
        password: String,
        phone: String,
        securityQuestion: String,
        securityAnswer: String,
        // Step 2
        motto: String,
        bio: String,
        // Optional logo URI from Step 2
        logoUri: Uri? = null,
        // Step 3
        legalName: String,
        repName: String,
        companyEmail: String,
        linkedinUrl: String,
        twitterHandle: String,
        // Optional verification doc from Step 3
        verificationFileUri: Uri? = null,
        verificationFileName: String = "",
        verificationFileMimeType: String = "",
        // Step 4
        industryTags: List<String>,
        city: String,
        state: String,
        country: String,
        // Step 5 — optional campaign fields
        campaignName: String = "",
        campaignBrief: String = ""
    ): Result<String> {
        return try {
            // ── 1. Firebase Auth ──────────────────────────────────────────────
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val uid = authResult.user?.uid
                ?: return Result.failure(Exception("Auth succeeded but UID is null."))

            // ── 2. Upload logo (optional) ─────────────────────────────────────
            var logoUrl = ""
            var logoPath = ""
            if (logoUri != null) {
                // Failure is propagated — caller shows the error and does not silently proceed
                logoUrl = storageRepository.uploadProfilePicture(uid, logoUri)
                logoPath = "users/$uid/profile_photo.jpg"
            }

            // ── 3. Upload verification doc (optional) ─────────────────────────
            var verificationFileUrl = ""
            var verificationFilePath = ""
            if (verificationFileUri != null) {
                val result = storageRepository.uploadVerificationDocResult(uid, verificationFileUri)
                verificationFileUrl = result.downloadUrl
                verificationFilePath = result.storagePath
            }

            // ── 4. Compute profile completion ─────────────────────────────────
            // Complete when required fields (motto + at least 1 industry tag) are present.
            val isComplete = motto.isNotBlank() && industryTags.isNotEmpty()

            // ── 5. Write users/{uid} ──────────────────────────────────────────
            val userMap = mapOf(
                "userId"            to uid,
                "email"             to email,
                "role"              to "brand",
                "displayName"       to brandName,
                "profileImageUrl"   to logoUrl,
                "phone"             to phone,
                "securityQuestion"  to securityQuestion,
                "securityAnswer"    to securityAnswer,
                "isProfileComplete" to isComplete,
                "createdAt"         to com.google.firebase.Timestamp.now()
            )
            firestore.collection(USERS).document(uid).set(userMap).await()

            // ── 6. Write brandProfiles/{uid} ──────────────────────────────────
            val profileMap = mapOf(
                "uid"                  to uid,
                "brandName"            to brandName,
                "legalName"            to legalName,
                "repName"              to repName,
                "companyEmail"         to companyEmail,
                "motto"                to motto,
                "bio"                  to bio,
                "linkedinUrl"          to linkedinUrl,
                "twitterHandle"        to twitterHandle,
                "industryTags"         to industryTags,
                "city"                 to city,
                "state"                to state,
                "country"              to country,
                "firstCampaignName"    to campaignName,
                "firstCampaignBrief"   to campaignBrief,
                "logoUrl"              to logoUrl,
                "logoPath"             to logoPath,
                "verificationFileUrl"  to verificationFileUrl,
                "verificationFilePath" to verificationFilePath,
                "verificationFileName" to verificationFileName,
                "verificationMimeType" to verificationFileMimeType,
                "totalCampaigns"       to 0L,
                "activeDeals"          to 0L,
                "updatedAt"            to com.google.firebase.Timestamp.now()
            )
            firestore.collection(BRAND_PROFILES).document(uid).set(profileMap).await()

            Result.success(uid)

        } catch (e: FirebaseAuthUserCollisionException) {
            Result.failure(Exception("An account with this email already exists."))
        } catch (e: FirebaseAuthWeakPasswordException) {
            Result.failure(Exception("Password does not meet requirements."))
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            Result.failure(Exception("The email address is invalid."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

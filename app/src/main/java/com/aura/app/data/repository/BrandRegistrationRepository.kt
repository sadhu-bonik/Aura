package com.aura.app.data.repository

import com.aura.app.data.model.BrandProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * BrandRegistrationRepository
 *
 * All Firebase I/O for the brand registration flow lives here.
 * Steps 1–4 only populate the ViewModel draft (no network calls).
 * A SINGLE network operation fires at the end of Step 5:
 *   1. Firebase Auth → create user
 *   2. Firestore users/{uid}        → BrandAccount document
 *   3. Firestore brandProfiles/{uid} → full BrandProfile document
 */
class BrandRegistrationRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    companion object {
        private const val USERS = "users"
        private const val BRAND_PROFILES = "brandProfiles"
    }

    /**
     * Called once — on Step 5 "Finish".
     *
     * Creates the Firebase Auth user and writes both Firestore documents
     * atomically (best-effort: if Firestore fails after Auth succeeds,
     * the user can re-attempt; their Auth account exists and we can detect
     * it via [FirebaseAuthUserCollisionException]).
     *
     * Returns Result<uid>.
     */
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
        // Step 3
        legalName: String,
        repName: String,
        companyEmail: String,
        linkedinUrl: String,
        twitterHandle: String,
        // Step 4
        industryTags: List<String>,
        city: String,
        state: String,
        country: String,
        // Step 5
        campaignName: String,
        campaignBrief: String
    ): Result<String> {
        return try {
            // ── 1. Firebase Auth ──────────────────────────────────────────────
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val uid = authResult.user?.uid
                ?: return Result.failure(Exception("Auth succeeded but UID is null."))

            // ── 2. users/{uid} — written as explicit map to guarantee exact field names.
            // Using .set(model) lets Kotlin's 'is'-prefixed boolean getter rename
            // 'isProfileComplete' → 'profileComplete' in Firestore. A plain map avoids this.
            val userMap = mapOf(
                "userId"          to uid,
                "email"           to email,
                "role"            to "brand",
                "displayName"     to brandName,
                "phone"           to phone,
                "securityQuestion" to securityQuestion,
                "securityAnswer"  to securityAnswer,
                "isProfileComplete" to true,
                "createdAt"       to com.google.firebase.Timestamp.now()
            )
            firestore.collection(USERS).document(uid).set(userMap).await()

            // ── 3. brandProfiles/{uid} — also written as explicit map for the same reason ──
            val profileMap = mapOf(
                "uid"               to uid,
                "brandName"         to brandName,
                "legalName"         to legalName,
                "repName"           to repName,
                "companyEmail"      to companyEmail,
                "motto"             to motto,
                "bio"               to bio,
                "linkedinUrl"       to linkedinUrl,
                "twitterHandle"     to twitterHandle,
                "industryTags"      to industryTags,
                "city"              to city,
                "state"             to state,
                "country"           to country,
                "firstCampaignName" to campaignName,
                "firstCampaignBrief" to campaignBrief,
                "totalCampaigns"    to 0L,
                "activeDeals"       to 0L,
                "updatedAt"         to com.google.firebase.Timestamp.now()
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

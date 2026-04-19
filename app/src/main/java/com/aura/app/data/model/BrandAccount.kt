package com.aura.app.data.model

import com.google.firebase.Timestamp

/**
 * BrandAccount — minimal Firestore "users/{uid}" doc created on Step 1 (auth creation).
 *
 * onboardingStep tracks where the user is in the flow so they can resume later.
 * All fields must have defaults for Firestore toObject() deserialization.
 */
data class BrandAccount(
    val uid: String = "",
    val email: String = "",
    val phone: String = "",
    val role: String = "brand",
    val registrationType: String = "brand",
    val securityQuestion: String = "",
    val securityAnswer: String = "",
    val isProfileComplete: Boolean = false,
    val onboardingStep: Int = 1,                 // 1–5; set to 6 when complete
    val createdAt: Timestamp = Timestamp.now()
)

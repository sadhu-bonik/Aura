package com.aura.app.data.model

import com.google.firebase.Timestamp

/**
 * BrandProfile — stored at "brandProfiles/{uid}".
 *
 * Built progressively across Steps 2–5 via partial Firestore updates.
 * All fields default-valued so Firestore toObject() never throws.
 */
data class BrandProfile(
    // Identity (Step 1 seed, Step 3 full legal)
    val uid: String = "",
    val brandName: String = "",      // display name from Step 1
    val legalName: String = "",      // legal business name from Step 3
    val repName: String = "",        // representative name from Step 3
    val companyEmail: String = "",   // company email from Step 3

    // Identity & branding (Step 2)
    val motto: String = "",
    val bio: String = "",

    // Social links (Step 3 — text only for now)
    val linkedinUrl: String = "",
    val twitterHandle: String = "",

    // Market presence (Step 4)
    val industryTags: List<String> = emptyList(),
    val city: String = "",
    val state: String = "",
    val country: String = "",

    // First campaign (Step 5, optional)
    val firstCampaignName: String = "",
    val firstCampaignBrief: String = "",

    // Uploaded media
    val logoUrl: String = "",
    val logoPath: String = "",
    val verificationFileUrl: String = "",
    val verificationFilePath: String = "",
    val verificationFileName: String = "",
    val verificationMimeType: String = "",

    // Metadata
    val totalCampaigns: Long = 0,
    val activeDeals: Long = 0,
    val updatedAt: Timestamp = Timestamp.now()
)

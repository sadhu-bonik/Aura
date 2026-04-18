package com.aura.app.data.model

import com.google.firebase.Timestamp

/**
 * UserRole - Defines the type of user in the system.
 */
enum class UserRole {
    CREATOR,
    BRAND
}

/**
 * User - Full data model for a user profile in Firestore.
 */
data class User(
    val userId: String = "",
    val email: String = "",
    val role: UserRole = UserRole.CREATOR,
    val displayName: String = "",
    val profileImageUrl: String = "",
    val motto: String = "",
    val bio: String = "",
    
    // Creator Specific Fields
    val niche: List<String>? = null,
    val location: String? = null,
    val audienceRegion: String? = null,
    val portfolioVideoUrl: String? = null,
    
    // Brand Specific Fields
    val companyName: String? = null,
    val phone: String? = null,
    val verificationDocUrl: String? = null,
    val website: String? = null,
    val instagram: String? = null,
    val youtube: String? = null,
    val industryTags: List<String>? = null,
    val targetLocation: String? = null,
    
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now()
)

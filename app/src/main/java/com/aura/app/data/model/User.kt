package com.aura.app.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName

/**
 * User - Full data model for a user profile in Firestore's 'users' collection.
 *
 * NOTE: Passwords are managed exclusively by Firebase Auth — never stored here.
 * All fields must have defaults for Firestore toObject() deserialization.
 */
data class User(
    val userId: String = "",
    val email: String = "",
    val role: String = "", // "creator" or "brand"
    val displayName: String = "",
    val profileImageUrl: String = "",
    val phone: String = "",
    val securityQuestion: String = "",
    val securityAnswer: String = "",
    val createdAt: Timestamp = Timestamp.now(),
    val lastActiveAt: Timestamp = Timestamp.now(),
    @get:PropertyName("isProfileComplete") @set:PropertyName("isProfileComplete")
    var isProfileComplete: Boolean = false,
    val fcmToken: String = ""
)

package com.aura.app.data.model

import com.google.firebase.Timestamp

data class Review(
    val reviewId: String = "",
    val dealId: String = "",
    val dealTitle: String = "",
    val reviewerId: String = "",
    val reviewerRole: String = "",          // "creator" | "brand"
    val reviewerDisplayName: String = "",
    val reviewerPhotoUrl: String = "",
    val revieweeId: String = "",
    val revieweeRole: String = "",          // "creator" | "brand"
    val revieweeDisplayName: String = "",
    val rating: Double = 0.0,       // 1.0–5.0
    val comment: String = "",
    val createdAt: Timestamp? = null,
)

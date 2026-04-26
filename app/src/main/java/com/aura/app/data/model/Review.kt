package com.aura.app.data.model

import com.google.firebase.Timestamp

data class Review(
    val reviewId: String = "",
    val dealId: String = "",
    val reviewerId: String = "",
    val revieweeId: String = "",
    val rating: Double = 0.0,       // 1.0–5.0
    val comment: String = "",
    val createdAt: Timestamp? = null,
)

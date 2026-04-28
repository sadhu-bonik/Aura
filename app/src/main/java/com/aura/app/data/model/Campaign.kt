package com.aura.app.data.model

import com.google.firebase.Timestamp

data class Campaign(
    val campaignId: String = "",
    val brandId: String = "",
    val title: String = "",
    val description: String = "",
    val goals: List<String> = emptyList(),
    val deliverables: List<String> = emptyList(),
    val budgetRange: String = "",
    val budgetMin: Long = 0L,
    val budgetMax: Long = 0L,
    val timeline: Timestamp? = null,
    val imageUrl: String = "",
    val imagePath: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
    val isActive: Boolean = true
)

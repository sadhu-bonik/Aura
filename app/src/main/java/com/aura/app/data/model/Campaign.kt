package com.aura.app.data.model

import com.google.firebase.Timestamp

data class Campaign(
    val campaignId: String = "",
    val brandId: String = "",
    val title: String = "",
    val description: String = "",
    val deliverables: List<String> = emptyList(),
    val budgetMin: Long = 0L,
    val budgetMax: Long = 0L,
    val imageUrl: String = "",
    val imagePath: String = "",
    val createdAt: Timestamp? = null
)

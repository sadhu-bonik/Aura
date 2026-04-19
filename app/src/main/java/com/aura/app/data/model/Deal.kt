package com.aura.app.data.model

import com.google.firebase.Timestamp

data class Deal(
    val dealId: String = "",
    val brandId: String = "",
    val creatorId: String = "",
    val campaignId: String = "",
    val title: String = "",
    val description: String = "",
    val budget: Long = 0,
    val status: String = "pending",
    val chatUnlocked: Boolean = false,
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now(),
    val completedAt: Timestamp? = null,
)

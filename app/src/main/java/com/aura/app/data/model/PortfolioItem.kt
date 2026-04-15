package com.aura.app.data.model

import com.google.firebase.Timestamp

data class PortfolioItem(
    val itemId: String = "",
    val creatorId: String = "",
    val title: String = "",
    val description: String = "",
    val mediaUrl: String = "",
    val mediaType: String = "",
    val thumbnailUrl: String = "",
    val isPublic: Boolean = false,
    val createdAt: Timestamp? = null,
)

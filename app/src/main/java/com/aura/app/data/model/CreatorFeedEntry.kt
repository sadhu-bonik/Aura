package com.aura.app.data.model

data class CreatorFeedEntry(
    val creatorId: String,
    val items: List<PortfolioItem>,
    val creatorName: String = "",
    val creatorProfileImageUrl: String = "",
)

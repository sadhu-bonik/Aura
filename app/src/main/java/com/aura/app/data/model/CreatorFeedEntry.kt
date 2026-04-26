package com.aura.app.data.model

data class CreatorFeedEntry(
    val creatorId: String,
    val items: List<PortfolioItem>,
    val creatorName: String = "",
    val creatorProfileImageUrl: String = "",
    val bio: String = "",
    val tags: List<String> = emptyList(),
    val youtubeScore: Double = 0.0,
)

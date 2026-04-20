package com.aura.app.data.model

import com.google.firebase.Timestamp

data class CreatorProfile(
    val userId: String = "",
    val motto: String = "",
    val bio: String = "",
    val niche: String = "",
    val tags: List<String> = emptyList(),
    val instagramHandle: String = "",
    val youtubeHandle: String = "",
    val tiktokHandle: String = "",
    val followerCount: Long = 0,
    val averageRating: Double = 0.0,
    val totalReviews: Long = 0,
    val completedDeals: Long = 0,
    val isAvailable: Boolean = true,
    val minimumDealBudget: Long = 0,
    val location: String = "",
    val portfolioCount: Int = 0,
    val isProfileComplete: Boolean = false,
    val updatedAt: Timestamp = Timestamp.now()
)

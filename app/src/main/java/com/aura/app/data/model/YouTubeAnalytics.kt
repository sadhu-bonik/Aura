package com.aura.app.data.model

import com.aura.app.utils.Constants
import com.google.firebase.Timestamp

/**
 * All YouTube channel + computed score data for one creator.
 * Produced by YouTubeRepository and written to Firestore as a partial update.
 */
data class YouTubeAnalytics(
    // Channel-level public stats
    val channelId: String,
    val channelTitle: String,
    val channelThumbUrl: String,
    val subscriberCount: Long,
    val totalViews: Long,
    val videoCount: Long,
    val uploadsPlaylistId: String,

    // Computed scores (0–100)
    val engagementScore: Double,
    val consistencyScore: Double,
    val baseCreatorScore: Double,
) {
    /** Converts to a Map suitable for Firestore partial update. */
    fun toFirestoreMap(): Map<String, Any> = mapOf(
        Constants.FIELD_YT_CHANNEL_ID        to channelId,
        Constants.FIELD_YT_CHANNEL_TITLE     to channelTitle,
        Constants.FIELD_YT_CHANNEL_THUMB_URL to channelThumbUrl,
        Constants.FIELD_YT_SUBSCRIBER_COUNT  to subscriberCount,
        Constants.FIELD_YT_TOTAL_VIEWS       to totalViews,
        Constants.FIELD_YT_VIDEO_COUNT       to videoCount,
        Constants.FIELD_YT_UPLOADS_PLAYLIST  to uploadsPlaylistId,
        Constants.FIELD_YT_ENGAGEMENT_SCORE  to engagementScore,
        Constants.FIELD_YT_CONSISTENCY_SCORE to consistencyScore,
        Constants.FIELD_YT_BASE_CREATOR_SCORE to baseCreatorScore,
        Constants.FIELD_YT_ANALYTICS_UPDATED to Timestamp.now(),
    )
}

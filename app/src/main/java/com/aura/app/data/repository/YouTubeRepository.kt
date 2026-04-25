package com.aura.app.data.repository

import android.util.Log
import com.aura.app.data.model.YouTubeAnalytics
import com.aura.app.data.remote.VideoItem
import com.aura.app.data.remote.YouTubeApiService
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Fetches public YouTube channel and video data using YouTube Data API v3 (API key, no OAuth),
 * then calculates Engagement Score, Consistency Score, and Base Creator Score.
 *
 * All three API calls cost ~3 quota units total (well within the 10,000/day free limit).
 */
class YouTubeRepository(
    private val apiService: YouTubeApiService = YouTubeApiService.create(),
    private val apiKey: String
) {

    /**
     * Main entry point. Pass the raw handle as entered by the user (e.g. "@MrBeast",
     * "MrBeast", or "https://www.youtube.com/@MrBeast"). Returns null on any error.
     */
    suspend fun fetchAndScore(rawHandle: String): YouTubeAnalytics? {
        val handle = normalizeHandle(rawHandle)
        if (handle.isBlank()) {
            Log.w(TAG, "Blank handle after normalization — skipping YouTube analytics")
            return null
        }

        return try {
            // ── Step 1: Channel metadata ──────────────────────────────────────
            val channelResponse = apiService.getChannelByHandle(handle = handle, apiKey = apiKey)
            val channel = channelResponse.items?.firstOrNull() ?: run {
                Log.w(TAG, "No channel found for handle '$handle'")
                return null
            }

            val channelId    = channel.id
            val channelTitle = channel.snippet?.title ?: ""
            val thumbUrl     = channel.snippet?.thumbnails?.let {
                it.high?.url ?: it.medium?.url ?: it.default?.url
            } ?: ""

            val stats = channel.statistics
            val subscriberCount = if (stats?.hiddenSubscriberCount == true) 0L
                                  else stats?.subscriberCount?.toLongOrNull() ?: 0L
            val totalViews  = stats?.viewCount?.toLongOrNull()  ?: 0L
            val videoCount  = stats?.videoCount?.toLongOrNull() ?: 0L

            val uploadsPlaylistId = channel.contentDetails
                ?.relatedPlaylists?.uploads ?: ""

            // ── Step 2: Recent video IDs + publishedAt dates ──────────────────
            val recentVideoData = if (uploadsPlaylistId.isNotBlank()) {
                val playlistResponse = apiService.getPlaylistItems(
                    playlistId = uploadsPlaylistId,
                    apiKey = apiKey
                )
                playlistResponse.items?.mapNotNull { entry ->
                    val videoId     = entry.snippet?.resourceId?.videoId ?: return@mapNotNull null
                    val publishedAt = entry.snippet.publishedAt
                    Pair(videoId, publishedAt)
                } ?: emptyList()
            } else emptyList()

            // ── Step 3: Video statistics ──────────────────────────────────────
            val videoIds = recentVideoData.map { it.first }
            val videoStatsList = if (videoIds.isNotEmpty()) {
                val idsParam = videoIds.joinToString(",")
                apiService.getVideoStatistics(ids = idsParam, apiKey = apiKey).items
                    ?: emptyList()
            } else emptyList()

            // ── Step 4: Compute scores ────────────────────────────────────────
            val engagementScore  = computeEngagementScore(videoStatsList)
            val publishedAtDates = recentVideoData.map { it.second }
            val consistencyScore = computeConsistencyScore(publishedAtDates)
            val baseCreatorScore = 0.50 * engagementScore + 0.50 * consistencyScore

            Log.d(TAG, "Analytics for '$handle': " +
                "engagement=%.1f  consistency=%.1f  base=%.1f".format(
                    engagementScore, consistencyScore, baseCreatorScore))

            YouTubeAnalytics(
                channelId            = channelId,
                channelTitle         = channelTitle,
                channelThumbUrl      = thumbUrl,
                subscriberCount      = subscriberCount,
                totalViews           = totalViews,
                videoCount           = videoCount,
                uploadsPlaylistId    = uploadsPlaylistId,
                engagementScore      = engagementScore.roundTo2(),
                consistencyScore     = consistencyScore.roundTo2(),
                baseCreatorScore     = baseCreatorScore.roundTo2(),
            )
        } catch (e: Exception) {
            Log.e(TAG, "fetchAndScore failed for '$handle': ${e.message}")
            null
        }
    }

    // ── Score formulas ────────────────────────────────────────────────────────

    /**
     * Engagement Score (0–100).
     *
     * engagementRate per video = (likes + comments) / max(views, 1)
     * avgEngagementRate = mean across all videos
     * score = min(avgEngagementRate / ENGAGEMENT_CEILING, 1.0) * 100
     *
     * ENGAGEMENT_CEILING = 0.08 (8% — very high bar; maps 8% → 100).
     */
    private fun computeEngagementScore(videos: List<VideoItem>): Double {
        if (videos.isEmpty()) return 0.0
        val rates = videos.map { v ->
            val views    = v.statistics?.viewCount?.toLongOrNull()    ?: 0L
            val likes    = v.statistics?.likeCount?.toLongOrNull()    ?: 0L
            val comments = v.statistics?.commentCount?.toLongOrNull() ?: 0L
            (likes + comments).toDouble() / maxOf(views, 1L)
        }
        val avg = rates.average()
        return minOf(avg / ENGAGEMENT_CEILING, 1.0) * 100.0
    }

    /**
     * Consistency Score (0–100).
     *
     * intervals = days between consecutive publishedAt timestamps
     * frequencyScore  = min(TARGET_INTERVAL_DAYS / avgInterval, 1.0) * 100
     *   (uploading every day → 100; every 30+ days → 0)
     * regularityScore = max(0, 100 − (stdDev / max(avgInterval, 1)) × 100)
     *   (low variance in cadence → near 100)
     * consistencyScore = 0.6 × frequencyScore + 0.4 × regularityScore
     */
    private fun computeConsistencyScore(publishedAtIso: List<String>): Double {
        if (publishedAtIso.size < 2) return 0.0

        val dates = publishedAtIso.mapNotNull { parseIso8601(it) }.sortedDescending()
        if (dates.size < 2) return 0.0

        val intervals = dates.zipWithNext { a, b ->
            abs(a.time - b.time).toDouble() / MS_PER_DAY
        }
        val avg    = intervals.average()
        val stdDev = stdDev(intervals)

        val frequencyScore  = minOf(TARGET_INTERVAL_DAYS / maxOf(avg, 1.0), 1.0) * 100.0
        val regularityScore = maxOf(0.0, 100.0 - (stdDev / maxOf(avg, 1.0)) * 100.0)
        return 0.6 * frequencyScore + 0.4 * regularityScore
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Strips "@" prefix and extracts handle from a full YouTube URL if present. */
    private fun normalizeHandle(raw: String): String {
        val trimmed = raw.trim()
        // Full URL: https://www.youtube.com/@Handle or https://youtube.com/@Handle
        val urlPattern = Regex("youtube\\.com/@([\\w.%-]+)", RegexOption.IGNORE_CASE)
        urlPattern.find(trimmed)?.let { return it.groupValues[1] }
        // Plain handle with or without "@"
        return trimmed.removePrefix("@")
    }

    private fun parseIso8601(iso: String): Date? = runCatching {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
            .apply { timeZone = TimeZone.getTimeZone("UTC") }
        sdf.parse(iso)
    }.getOrNull()

    private fun stdDev(values: List<Double>): Double {
        if (values.size < 2) return 0.0
        val mean = values.average()
        val variance = values.sumOf { (it - mean) * (it - mean) } / values.size
        return sqrt(variance)
    }

    private fun Double.roundTo2(): Double =
        (this * 100.0).toLong().toDouble() / 100.0

    private companion object {
        const val TAG = "YouTubeRepository"
        const val ENGAGEMENT_CEILING    = 0.08   // 8% avg → score 100
        const val TARGET_INTERVAL_DAYS  = 7.0    // weekly uploads → frequency score 100
        const val MS_PER_DAY            = 86_400_000.0
    }
}

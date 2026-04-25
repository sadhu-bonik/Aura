package com.aura.app.data.remote

import com.google.gson.annotations.SerializedName

// ── channels endpoint ─────────────────────────────────────────────────────────

data class ChannelListResponse(
    @SerializedName("items") val items: List<ChannelItem>? = null
)

data class ChannelItem(
    @SerializedName("id")             val id: String = "",
    @SerializedName("snippet")        val snippet: ChannelSnippet? = null,
    @SerializedName("statistics")     val statistics: ChannelStatistics? = null,
    @SerializedName("contentDetails") val contentDetails: ChannelContentDetails? = null
)

data class ChannelSnippet(
    @SerializedName("title")     val title: String = "",
    @SerializedName("thumbnails") val thumbnails: ThumbnailMap? = null
)

data class ThumbnailMap(
    @SerializedName("default") val default: Thumbnail? = null,
    @SerializedName("medium")  val medium: Thumbnail? = null,
    @SerializedName("high")    val high: Thumbnail? = null
)

data class Thumbnail(
    @SerializedName("url") val url: String = ""
)

data class ChannelStatistics(
    @SerializedName("subscriberCount")    val subscriberCount: String = "0",
    @SerializedName("viewCount")          val viewCount: String = "0",
    @SerializedName("videoCount")         val videoCount: String = "0",
    @SerializedName("hiddenSubscriberCount") val hiddenSubscriberCount: Boolean = false
)

data class ChannelContentDetails(
    @SerializedName("relatedPlaylists") val relatedPlaylists: RelatedPlaylists? = null
)

data class RelatedPlaylists(
    @SerializedName("uploads") val uploads: String = ""
)

// ── playlistItems endpoint ────────────────────────────────────────────────────

data class PlaylistItemListResponse(
    @SerializedName("items") val items: List<PlaylistItemEntry>? = null
)

data class PlaylistItemEntry(
    @SerializedName("snippet") val snippet: PlaylistItemSnippet? = null
)

data class PlaylistItemSnippet(
    @SerializedName("publishedAt") val publishedAt: String = "",
    @SerializedName("resourceId")  val resourceId: PlaylistResourceId? = null
)

data class PlaylistResourceId(
    @SerializedName("videoId") val videoId: String = ""
)

// ── videos endpoint ───────────────────────────────────────────────────────────

data class VideoListResponse(
    @SerializedName("items") val items: List<VideoItem>? = null
)

data class VideoItem(
    @SerializedName("id")         val id: String = "",
    @SerializedName("statistics") val statistics: VideoStatistics? = null
)

data class VideoStatistics(
    @SerializedName("viewCount")    val viewCount: String = "0",
    @SerializedName("likeCount")    val likeCount: String = "0",
    @SerializedName("commentCount") val commentCount: String = "0"
)

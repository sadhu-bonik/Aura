package com.aura.app.data.remote

import com.aura.app.utils.Constants
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

interface YouTubeApiService {

    /**
     * Fetch channel metadata by YouTube handle (e.g. "MrBeast" — no leading @).
     * parts: snippet (title, thumbnail), statistics (counts), contentDetails (uploadsPlaylistId)
     */
    @GET("channels")
    suspend fun getChannelByHandle(
        @Query("part")      part: String = "snippet,statistics,contentDetails",
        @Query("forHandle") handle: String,
        @Query("key")       apiKey: String
    ): ChannelListResponse

    /**
     * Fetch the most recent video IDs from the uploads playlist.
     */
    @GET("playlistItems")
    suspend fun getPlaylistItems(
        @Query("part")       part: String = "snippet",
        @Query("playlistId") playlistId: String,
        @Query("maxResults") maxResults: Int = 10,
        @Query("key")        apiKey: String
    ): PlaylistItemListResponse

    /**
     * Fetch statistics for a batch of video IDs (comma-separated, up to 50).
     */
    @GET("videos")
    suspend fun getVideoStatistics(
        @Query("part") part: String = "statistics",
        @Query("id")   ids: String,
        @Query("key")  apiKey: String
    ): VideoListResponse

    companion object {
        fun create(): YouTubeApiService = Retrofit.Builder()
            .baseUrl(Constants.YOUTUBE_API_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(YouTubeApiService::class.java)
    }
}

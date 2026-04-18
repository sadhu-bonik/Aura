package com.aura.app.ui.feed

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.preload.DefaultPreloadManager
import androidx.media3.exoplayer.source.preload.TargetPreloadStatusControl
import com.aura.app.data.model.CreatorFeedEntry
import kotlin.math.abs

@OptIn(UnstableApi::class)
class FeedPreloadManager(context: Context) {

    val player: ExoPlayer
    private val preloadManager: DefaultPreloadManager

    private var allItems: List<String> = emptyList()
    private var creatorOffsets: List<Int> = emptyList()
    private var currentFlatIndex = 0
    private val registered = mutableMapOf<Int, MediaItem>()

    init {
        val statusControl = TargetPreloadStatusControl<Int> { rankingData ->
            val distance = abs(rankingData - currentFlatIndex)
            when {
                distance == 0 -> null
                distance <= 1 -> DefaultPreloadManager.Status(
                    DefaultPreloadManager.Status.STAGE_LOADED_FOR_DURATION_MS, 5_000
                )
                distance <= 3 -> DefaultPreloadManager.Status(
                    DefaultPreloadManager.Status.STAGE_LOADED_FOR_DURATION_MS, 2_000
                )
                distance <= 6 -> DefaultPreloadManager.Status(
                    DefaultPreloadManager.Status.STAGE_TRACKS_SELECTED
                )
                distance <= 10 -> DefaultPreloadManager.Status(
                    DefaultPreloadManager.Status.STAGE_SOURCE_PREPARED
                )
                else -> null
            }
        }
        val builder = DefaultPreloadManager.Builder(context, statusControl)
        preloadManager = builder.build()
        player = builder.buildExoPlayer()
    }

    fun updateFeedData(entries: List<CreatorFeedEntry>) {
        preloadManager.reset()
        registered.clear()

        val urls = mutableListOf<String>()
        val offsets = mutableListOf<Int>()
        for (entry in entries) {
            offsets.add(urls.size)
            entry.items.mapTo(urls) { it.mediaUrl }
        }
        allItems = urls
        creatorOffsets = offsets

        if (allItems.isNotEmpty()) refreshWindow()
    }

    fun updateCurrentPosition(creatorIndex: Int, itemIndex: Int) {
        if (creatorIndex < 0 || creatorIndex >= creatorOffsets.size) return
        val maxItem = nextCreatorOffset(creatorIndex) - creatorOffsets[creatorIndex]
        if (itemIndex < 0 || itemIndex >= maxItem) return
        currentFlatIndex = creatorOffsets[creatorIndex] + itemIndex
        preloadManager.setCurrentPlayingIndex(currentFlatIndex)
        refreshWindow()
    }

    fun getPreloadedMediaSource(mediaUrl: String): MediaSource? {
        return preloadManager.getMediaSource(MediaItem.fromUri(mediaUrl))
    }

    fun release() {
        preloadManager.release()
        player.release()
    }

    private fun refreshWindow() {
        val windowStart = (currentFlatIndex - WINDOW_RADIUS).coerceAtLeast(0)
        val windowEnd = (currentFlatIndex + WINDOW_RADIUS).coerceAtMost(allItems.size - 1)
        val newWindow = windowStart..windowEnd

        val toRemove = registered.keys.filter { it !in newWindow }
        for (idx in toRemove) {
            registered.remove(idx)?.let { preloadManager.remove(it) }
        }

        for (idx in newWindow) {
            if (idx == currentFlatIndex || idx in registered) continue
            val mediaItem = MediaItem.fromUri(allItems[idx])
            preloadManager.add(mediaItem, idx)
            registered[idx] = mediaItem
        }

        preloadManager.invalidate()
    }

    private fun nextCreatorOffset(creatorIndex: Int): Int {
        return if (creatorIndex + 1 < creatorOffsets.size) creatorOffsets[creatorIndex + 1]
        else allItems.size
    }

    companion object {
        private const val WINDOW_RADIUS = 5
    }
}

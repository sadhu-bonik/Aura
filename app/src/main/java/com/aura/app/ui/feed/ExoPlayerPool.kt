package com.aura.app.ui.feed

import android.content.Context
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import java.io.File

@OptIn(UnstableApi::class)
class ExoPlayerPool(context: Context) {

    private val appContext = context.applicationContext
    private val cache = SimpleCache(
        File(appContext.cacheDir, "video_cache"),
        LeastRecentlyUsedCacheEvictor(CACHE_SIZE_BYTES),
        androidx.media3.database.StandaloneDatabaseProvider(appContext)
    )
    private val players = List(POOL_SIZE) { createPlayer() }
    private val assignments = arrayOfNulls<Assignment>(POOL_SIZE)
    private val playbackPositionsMs = mutableMapOf<String, Long>()

    private data class Assignment(
        val url: String,
        var role: Role,
        var timestamp: Long = System.nanoTime(),
    )

    private enum class Role { ACTIVE, NEXT_VERT, PREV_VERT }

    val activePlayer: ExoPlayer?
        get() {
            for (i in assignments.indices) {
                if (assignments[i]?.role == Role.ACTIVE) return players[i]
            }
            return null
        }

    fun activate(url: String): ExoPlayer {
        for (i in assignments.indices) {
            val assignment = assignments[i] ?: continue
            if (assignment.role == Role.ACTIVE && assignment.url == url) {
                val player = players[i]
                val resumePosition = playbackPositionsMs[url] ?: player.currentPosition
                if (resumePosition > 0L) player.seekTo(resumePosition)
                player.playWhenReady = true
                assignments[i] = assignment.copy(timestamp = System.nanoTime())
                Log.d(TAG, "activate SAME_ACTIVE: $url @${player.currentPosition}ms")
                return player
            }
        }

        for (i in assignments.indices) {
            val assignment = assignments[i] ?: continue
            if (assignment.role == Role.ACTIVE) {
                playbackPositionsMs[assignment.url] = players[i].currentPosition
                players[i].playWhenReady = false
                assignments[i] = null
            }
        }

        for (i in assignments.indices) {
            if (assignments[i]?.url == url) {
                assignments[i] = Assignment(url, Role.ACTIVE)
                val player = players[i]
                val resumePosition = playbackPositionsMs[url] ?: 0L
                if (resumePosition > 0L) player.seekTo(resumePosition)
                player.playWhenReady = true
                Log.d(TAG, "activate REUSE: $url @${player.currentPosition}ms")
                return player
            }
        }

        val idx = stealSlot()
        val player = players[idx]
        player.stop()
        player.clearMediaItems()
        player.setMediaItem(MediaItem.fromUri(url))
        val resumePosition = playbackPositionsMs[url] ?: 0L
        if (resumePosition > 0L) player.seekTo(resumePosition)
        player.repeatMode = Player.REPEAT_MODE_ONE
        player.playWhenReady = true
        player.prepare()
        assignments[idx] = Assignment(url, Role.ACTIVE)
        Log.d(TAG, "activate COLD: $url @${resumePosition}ms")
        return player
    }

    fun prewarmVertical(nextUrl: String?, prevUrl: String?) {
        val targets = mapOf(Role.NEXT_VERT to nextUrl, Role.PREV_VERT to prevUrl)

        for ((role, url) in targets) {
            if (url == null) continue
            if (assignments.any { it?.url == url }) continue

            val idx = findSlotForPrewarm(role) ?: continue
            val player = players[idx]
            player.stop()
            player.clearMediaItems()
            player.setMediaItem(MediaItem.fromUri(url))
            player.repeatMode = Player.REPEAT_MODE_ONE
            player.playWhenReady = false
            player.prepare()
            assignments[idx] = Assignment(url, role)
            Log.d(TAG, "prewarm $role: $url")
        }
    }

    fun pauseAll() {
        for (i in players.indices) {
            val assignment = assignments[i]
            if (assignment != null) {
                playbackPositionsMs[assignment.url] = players[i].currentPosition
            }
            players[i].playWhenReady = false
        }
    }

    fun saveActivePlaybackPosition() {
        for (i in assignments.indices) {
            val assignment = assignments[i] ?: continue
            if (assignment.role == Role.ACTIVE) {
                playbackPositionsMs[assignment.url] = players[i].currentPosition
                break
            }
        }
    }

    fun release() {
        for (p in players) p.release()
        assignments.fill(null)
        playbackPositionsMs.clear()
        cache.release()
    }

    private fun stealSlot(): Int {
        for (i in assignments.indices) {
            if (assignments[i] == null) return i
        }
        var oldest = -1
        var oldestTime = Long.MAX_VALUE
        for (i in assignments.indices) {
            val a = assignments[i] ?: continue
            if (a.role != Role.ACTIVE && a.timestamp < oldestTime) {
                oldest = i
                oldestTime = a.timestamp
            }
        }
        if (oldest >= 0) {
            players[oldest].stop()
            assignments[oldest] = null
            return oldest
        }
        players[0].stop()
        assignments[0] = null
        return 0
    }

    private fun findSlotForPrewarm(targetRole: Role): Int? {
        for (i in assignments.indices) {
            if (assignments[i]?.role == targetRole) {
                players[i].stop()
                assignments[i] = null
                return i
            }
        }
        for (i in assignments.indices) {
            if (assignments[i] == null) return i
        }
        return null
    }

    private fun createPlayer(): ExoPlayer {
        val cacheFactory = CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(DefaultDataSource.Factory(appContext))
        val mediaSourceFactory = DefaultMediaSourceFactory(appContext)
            .setDataSourceFactory(cacheFactory)
        return ExoPlayer.Builder(appContext)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()
    }

    companion object {
        private const val TAG = "ExoPlayerPool"
        private const val POOL_SIZE = 3
        private const val CACHE_SIZE_BYTES = 100L * 1024 * 1024
    }
}

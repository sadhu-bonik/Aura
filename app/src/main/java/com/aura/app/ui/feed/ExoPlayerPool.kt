package com.aura.app.ui.feed

import android.content.Context
import androidx.media3.exoplayer.ExoPlayer

/**
 * Bounded pool of ExoPlayer instances. A vertical feed only needs a handful live at once
 * (current page plus neighbours pre-bound by ViewPager2), so we cap it and lazily grow.
 */
class ExoPlayerPool(private val context: Context, private val maxSize: Int = 4) {
    private val available = ArrayDeque<ExoPlayer>()
    private var leased = 0

    fun acquire(): ExoPlayer {
        val player = available.removeFirstOrNull() ?: ExoPlayer.Builder(context).build()
        leased++
        return player
    }

    fun release(player: ExoPlayer) {
        leased--
        player.stop()
        player.clearMediaItems()
        if (available.size + leased < maxSize) {
            available.addLast(player)
        } else {
            player.release()
        }
    }

    fun releaseAll() {
        available.forEach { it.release() }
        available.clear()
    }
}

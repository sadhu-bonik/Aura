package com.aura.app.ui.feed

import android.content.Context
import android.os.Looper
import androidx.media3.exoplayer.ExoPlayer

/**
 * Bounded pool of ExoPlayer instances.
 *
 * A vertical feed only needs a handful of players live at once (current page plus
 * neighbours pre-bound by ViewPager2), so we cap the total and lazily grow.
 *
 * **Thread safety:** All methods MUST be called from the main thread, which matches
 * the Android View lifecycle (ViewHolder bind/unbind, onPause/onResume). A debug
 * assertion is included to catch violations early.
 */
class ExoPlayerPool(private val context: Context, private val maxSize: Int = 8) {

    private val available = ArrayDeque<ExoPlayer>(maxSize)
    private var leased = 0

    private fun assertMainThread() {
        check(Looper.myLooper() == Looper.getMainLooper()) {
            "ExoPlayerPool must be accessed from the main thread"
        }
    }

    fun acquire(): ExoPlayer {
        assertMainThread()
        val player = available.removeFirstOrNull() ?: ExoPlayer.Builder(context).build()
        leased++
        return player
    }

    fun release(player: ExoPlayer) {
        assertMainThread()
        leased--
        player.stop()
        player.clearMediaItems()
        if (available.size + leased < maxSize) {
            available.addLast(player)
        } else {
            player.release()
        }
    }

    fun warmUp(count: Int) {
        assertMainThread()
        repeat(count) { available.addLast(ExoPlayer.Builder(context).build()) }
    }

    fun releaseAll() {
        assertMainThread()
        available.forEach { it.release() }
        available.clear()
    }
}

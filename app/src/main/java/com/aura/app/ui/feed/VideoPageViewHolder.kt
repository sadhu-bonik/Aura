package com.aura.app.ui.feed

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.recyclerview.widget.RecyclerView
import com.aura.app.R
import com.aura.app.data.model.PortfolioItem
import com.bumptech.glide.Glide

class VideoPageViewHolder(
    itemView: View,
    private val pool: ExoPlayerPool,
) : RecyclerView.ViewHolder(itemView) {

    private val playerView: PlayerView = itemView.findViewById(R.id.player_view)
    private val thumbnail: ImageView = itemView.findViewById(R.id.thumbnail)
    private val errorLabel: TextView = itemView.findViewById(R.id.error_label)
    private val itemTitle: TextView = itemView.findViewById(R.id.item_title)
    private val playPauseIndicator: ImageView = itemView.findViewById(R.id.play_pause_indicator)

    private var player: ExoPlayer? = null

    init {
        itemView.setOnClickListener { togglePlayback() }
    }

    private val errorListener = object : Player.Listener {
        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
            errorLabel.visibility = View.VISIBLE
        }

        override fun onRenderedFirstFrame() {
            thumbnail.visibility = View.GONE
        }
    }

    fun bind(item: PortfolioItem) {
        errorLabel.visibility = View.GONE
        thumbnail.visibility = View.VISIBLE
        itemTitle.text = item.title

        if (item.thumbnailUrl.isNotEmpty()) {
            Glide.with(thumbnail).load(item.thumbnailUrl).into(thumbnail)
        } else {
            thumbnail.setImageDrawable(null)
        }

        playerView.player?.let { releasePlayer() }
        val exo = pool.acquire().also { player = it }
        exo.addListener(errorListener)
        exo.setMediaItem(MediaItem.fromUri(item.mediaUrl))
        exo.repeatMode = Player.REPEAT_MODE_ONE
        exo.playWhenReady = false
        exo.prepare()
        playerView.player = exo
    }

    fun play() {
        player?.playWhenReady = true
    }

    fun pause() {
        player?.playWhenReady = false
    }

    fun togglePlayback() {
        val p = player ?: return
        if (p.playWhenReady) {
            pause()
            showIndicator(R.drawable.ic_feed_play)
        } else {
            play()
            showIndicator(R.drawable.ic_feed_pause)
        }
    }

    private fun showIndicator(@DrawableRes iconRes: Int) {
        playPauseIndicator.animate().cancel()
        playPauseIndicator.setImageResource(iconRes)
        playPauseIndicator.visibility = View.VISIBLE
        playPauseIndicator.alpha = 0.8f
        playPauseIndicator.animate()
            .alpha(0f)
            .setDuration(500)
            .setStartDelay(200)
            .withEndAction { playPauseIndicator.visibility = View.GONE }
            .start()
    }

    fun releasePlayer() {
        playPauseIndicator.animate().cancel()
        playPauseIndicator.visibility = View.GONE
        player?.let {
            it.removeListener(errorListener)
            playerView.player = null
            pool.release(it)
        }
        player = null
    }
}

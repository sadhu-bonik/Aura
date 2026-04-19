package com.aura.app.ui.feed

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.recyclerview.widget.RecyclerView
import com.aura.app.R
import com.aura.app.data.model.PortfolioItem
import com.bumptech.glide.Glide

class VideoPageViewHolder(
    itemView: View,
    private val callback: ActiveVideoCallback,
) : RecyclerView.ViewHolder(itemView) {

    private val playerView: PlayerView = itemView.findViewById(R.id.player_view)
    private val thumbnail: ImageView = itemView.findViewById(R.id.thumbnail)
    private val errorLabel: TextView = itemView.findViewById(R.id.error_label)
    private val itemTitle: TextView = itemView.findViewById(R.id.item_title)
    private val playPauseIndicator: ImageView = itemView.findViewById(R.id.play_pause_indicator)

    var boundItem: PortfolioItem? = null
        private set

    private var retryCount = 0

    private val playerListener = object : Player.Listener {
        override fun onPlayerError(error: PlaybackException) {
            val p = playerView.player ?: return
            if (retryCount < 2) {
                retryCount++
                p.prepare()
            } else {
                errorLabel.visibility = View.VISIBLE
            }
        }

        override fun onRenderedFirstFrame() {
            thumbnail.visibility = View.GONE
            retryCount = 0
        }
    }

    init {
        itemView.setOnClickListener {
            if (playerView.player != null) {
                callback.togglePlayback()
                showIndicator(
                    if (playerView.player?.playWhenReady == true)
                        R.drawable.ic_feed_pause
                    else
                        R.drawable.ic_feed_play
                )
            }
        }
    }

    fun bind(item: PortfolioItem) {
        boundItem = item
        retryCount = 0
        errorLabel.visibility = View.GONE
        thumbnail.visibility = View.VISIBLE
        itemTitle.text = item.title
        playerView.player = null

        if (item.thumbnailUrl.isNotEmpty()) {
            Glide.with(thumbnail).load(item.thumbnailUrl).into(thumbnail)
        } else if (item.mediaUrl.isNotEmpty()) {
            Glide.with(thumbnail)
                .asBitmap()
                .load(item.mediaUrl)
                .apply(com.bumptech.glide.request.RequestOptions().frame(1_000_000))
                .into(thumbnail)
        } else {
            thumbnail.setImageDrawable(null)
        }
    }

    fun onPlayerAttached(player: ExoPlayer) {
        retryCount = 0
        errorLabel.visibility = View.GONE
        thumbnail.visibility = View.VISIBLE
        player.addListener(playerListener)
        playerView.player = player
    }

    fun onPlayerDetached() {
        playerView.player?.removeListener(playerListener)
        playerView.player = null
        thumbnail.visibility = View.VISIBLE
        playPauseIndicator.animate().cancel()
        playPauseIndicator.visibility = View.GONE
    }

    fun onRecycled() {
        onPlayerDetached()
        boundItem = null
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
}

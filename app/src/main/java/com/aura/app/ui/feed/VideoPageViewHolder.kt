package com.aura.app.ui.feed

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.recyclerview.widget.RecyclerView
import com.aura.app.R
import com.aura.app.data.model.PortfolioItem
import com.aura.app.data.repository.UserRepository
import com.bumptech.glide.Glide
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class VideoPageViewHolder(
    itemView: View,
    private val pool: ExoPlayerPool,
    private val userRepository: UserRepository,
    private val scope: CoroutineScope,
) : RecyclerView.ViewHolder(itemView) {

    private val playerView: PlayerView = itemView.findViewById(R.id.player_view)
    private val thumbnail: ImageView = itemView.findViewById(R.id.thumbnail)
    private val errorLabel: TextView = itemView.findViewById(R.id.error_label)
    private val creatorAvatar: ImageView = itemView.findViewById(R.id.creator_avatar)
    private val creatorName: TextView = itemView.findViewById(R.id.creator_name)
    private val itemTitle: TextView = itemView.findViewById(R.id.item_title)

    private var player: ExoPlayer? = null
    private var boundItem: PortfolioItem? = null
    private var creatorJob: Job? = null

    private val errorListener = object : Player.Listener {
        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
            errorLabel.visibility = View.VISIBLE
        }

        override fun onRenderedFirstFrame() {
            thumbnail.visibility = View.GONE
        }
    }

    fun bind(item: PortfolioItem) {
        boundItem = item
        errorLabel.visibility = View.GONE
        thumbnail.visibility = View.VISIBLE
        itemTitle.text = item.title
        creatorName.text = ""
        creatorAvatar.setImageDrawable(null)

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

        creatorJob?.cancel()
        creatorJob = scope.launch {
            val user = withContext(Dispatchers.IO) {
                runCatching { userRepository.getUserLite(item.creatorId) }.getOrNull()
            } ?: return@launch
            if (boundItem?.itemId != item.itemId) return@launch
            creatorName.text = user.displayName
            if (user.profileImageUrl.isNotEmpty()) {
                Glide.with(creatorAvatar)
                    .load(user.profileImageUrl)
                    .circleCrop()
                    .into(creatorAvatar)
            }
        }
    }

    fun play() {
        player?.playWhenReady = true
    }

    fun pause() {
        player?.playWhenReady = false
    }

    fun releasePlayer() {
        creatorJob?.cancel()
        creatorJob = null
        player?.let {
            it.removeListener(errorListener)
            playerView.player = null
            pool.release(it)
        }
        player = null
    }
}

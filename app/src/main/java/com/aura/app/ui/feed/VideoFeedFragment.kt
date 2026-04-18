package com.aura.app.ui.feed

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.OptIn
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.aura.app.R
import com.aura.app.data.model.PortfolioItem
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
class VideoFeedFragment : Fragment(R.layout.fragment_video_feed) {

    private val viewModel: VideoFeedViewModel by viewModels { VideoFeedViewModel.Factory() }

    private var pager: ViewPager2? = null
    private var loading: ProgressBar? = null
    private var message: TextView? = null
    private var adapter: CreatorPageAdapter? = null
    private var preload: FeedPreloadManager? = null
    private val player get() = preload?.player
    private var activeHolder: VideoPageViewHolder? = null
    private var activeCreatorPosition = RecyclerView.NO_POSITION

    private val activeVideoCallback = object : ActiveVideoCallback {
        override fun attachPlayer(target: VideoPageViewHolder, item: PortfolioItem) {
            val p = player ?: return
            if (activeHolder === target) return
            activeHolder?.onPlayerDetached()
            p.stop()
            p.clearMediaItems()
            val src = preload?.getPreloadedMediaSource(item.mediaUrl)
            if (src != null) p.setMediaSource(src) else p.setMediaItem(MediaItem.fromUri(item.mediaUrl))
            p.repeatMode = Player.REPEAT_MODE_ONE
            p.playWhenReady = true
            p.prepare()
            target.onPlayerAttached(p)
            activeHolder = target
        }

        override fun detachPlayer(target: VideoPageViewHolder) {
            if (activeHolder === target) {
                player?.stop()
                player?.clearMediaItems()
                target.onPlayerDetached()
                activeHolder = null
            }
        }

        override fun togglePlayback() {
            val p = player ?: return
            p.playWhenReady = !p.playWhenReady
        }

        override fun onItemPositionChanged(creatorPosition: Int, itemPosition: Int) {
            preload?.updateCurrentPosition(creatorPosition, itemPosition)
        }
    }

    private val pageCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            updateActiveCreator(position)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View = inflater.inflate(R.layout.fragment_video_feed, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        pager = view.findViewById(R.id.video_pager)
        loading = view.findViewById(R.id.feed_loading)
        message = view.findViewById(R.id.feed_message)

        preload = FeedPreloadManager(requireContext().applicationContext)

        val feedAdapter = CreatorPageAdapter(
            activeVideoCallback, viewModel.userRepository, viewLifecycleOwner.lifecycleScope
        ).also { adapter = it }
        pager?.adapter = feedAdapter
        pager?.offscreenPageLimit = 1
        pager?.registerOnPageChangeCallback(pageCallback)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { render(it) }
            }
        }
    }

    private fun render(state: FeedUiState) {
        when (state) {
            FeedUiState.Loading -> {
                loading?.visibility = View.VISIBLE
                message?.visibility = View.GONE
                pager?.visibility = View.GONE
            }
            FeedUiState.Empty -> {
                loading?.visibility = View.GONE
                pager?.visibility = View.GONE
                message?.apply {
                    text = getString(R.string.feed_empty)
                    visibility = View.VISIBLE
                }
            }
            is FeedUiState.Error -> {
                loading?.visibility = View.GONE
                pager?.visibility = View.GONE
                message?.apply {
                    text = getString(R.string.feed_error)
                    visibility = View.VISIBLE
                }
            }
            is FeedUiState.Content -> {
                loading?.visibility = View.GONE
                message?.visibility = View.GONE
                pager?.visibility = View.VISIBLE
                preload?.updateFeedData(state.entries)
                adapter?.submitList(state.entries) {
                    pager?.let { updateActiveCreator(it.currentItem) }
                }
            }
        }
    }

    private fun updateActiveCreator(selectedPosition: Int) {
        val pg = pager ?: return
        val recycler = pg.getChildAt(0) as? RecyclerView ?: return
        val prev = activeCreatorPosition
        activeCreatorPosition = selectedPosition
        preload?.updateCurrentPosition(selectedPosition, 0)
        for (i in 0 until recycler.childCount) {
            val holder = recycler.getChildViewHolder(recycler.getChildAt(i)) as? CreatorPageViewHolder
                ?: continue
            val pos = holder.bindingAdapterPosition
            if (pos == selectedPosition) {
                holder.activate()
            } else if (pos == prev || holder.isActive) {
                holder.deactivate()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        player?.playWhenReady = false
    }

    override fun onResume() {
        super.onResume()
        pager?.let { updateActiveCreator(it.currentItem) }
    }

    override fun onDestroyView() {
        pager?.unregisterOnPageChangeCallback(pageCallback)
        pager?.adapter = null
        adapter = null
        pager = null
        loading = null
        message = null
        activeHolder?.onPlayerDetached()
        activeHolder = null
        preload?.release()
        preload = null
        super.onDestroyView()
    }
}

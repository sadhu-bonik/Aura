package com.aura.app.ui.feed

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.aura.app.R
import com.aura.app.data.model.CreatorFeedEntry
import com.aura.app.data.model.PortfolioItem
import kotlinx.coroutines.launch

class VideoFeedFragment : Fragment(R.layout.fragment_video_feed) {

    private val viewModel: VideoFeedViewModel by viewModels { VideoFeedViewModel.Factory(requireContext()) }
    private val actionsViewModel: FeedActionsViewModel by viewModels { FeedActionsViewModel.Factory() }
    private val authViewModel: com.aura.app.ui.auth.AuthViewModel by activityViewModels { com.aura.app.ui.auth.AuthViewModel.Factory() }

    private var pager: ViewPager2? = null
    private var loading: ProgressBar? = null
    private var message: TextView? = null
    private var refreshLayout: androidx.swiperefreshlayout.widget.SwipeRefreshLayout? = null
    private var adapter: CreatorPageAdapter? = null
    private var pool: ExoPlayerPool? = null
    private var activeHolder: VideoPageViewHolder? = null
    private var activeCreatorPosition = RecyclerView.NO_POSITION
    private var activeItemPosition = 0
    private var currentEntries: List<CreatorFeedEntry> = emptyList()
    private var pendingRestoreCreatorPosition: Int? = null

    private val activeVideoCallback = object : ActiveVideoCallback {
        override fun attachPlayer(target: VideoPageViewHolder, item: PortfolioItem) {
            val pool = pool ?: return
            if (activeHolder === target) {
                pool.activePlayer?.playWhenReady = true
                prewarmVertical()
                return
            }
            activeHolder?.onPlayerDetached()
            val p = pool.activate(item.mediaUrl)
            target.onPlayerAttached(p)
            activeHolder = target
            prewarmVertical()
        }

        override fun detachPlayer(target: VideoPageViewHolder) {
            if (activeHolder === target) {
                target.onPlayerDetached()
                activeHolder = null
            }
        }

        override fun togglePlayback() {
            val p = pool?.activePlayer ?: return
            p.playWhenReady = !p.playWhenReady
        }

        override fun onCreatorProfileClicked(creatorId: String) {
            val currentPosition = activeCreatorPosition
                .takeIf { it != RecyclerView.NO_POSITION }
                ?: (pager?.currentItem ?: 0)
            pool?.saveActivePlaybackPosition()
            findNavController().currentBackStackEntry
                ?.savedStateHandle
                ?.set(KEY_FEED_CREATOR_POSITION, currentPosition)
            val bundle = android.os.Bundle().apply { putString("creatorId", creatorId) }
            // Navigate within the home graph so back-stack is preserved
            findNavController().navigate(R.id.action_feed_to_creator_profile, bundle)
        }

        override fun onItemPositionChanged(creatorPosition: Int, itemPosition: Int) {
            activeItemPosition = itemPosition
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
        refreshLayout = view.findViewById(R.id.feed_refresh)
        pendingRestoreCreatorPosition = findNavController()
            .currentBackStackEntry
            ?.savedStateHandle
            ?.remove(KEY_FEED_CREATOR_POSITION)

        refreshLayout?.setOnRefreshListener {
            viewModel.loadCreatorFeed()
        }

        if (pool == null) {
            pool = ExoPlayerPool(requireContext().applicationContext)
        }

        FeedActionsOverlay(view, actionsViewModel, viewLifecycleOwner, this).setup()

        val feedAdapter = CreatorPageAdapter(activeVideoCallback).also { adapter = it }
        pager?.adapter = feedAdapter
        pager?.offscreenPageLimit = 1
        pager?.registerOnPageChangeCallback(pageCallback)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    if (state !is FeedUiState.Loading) {
                        refreshLayout?.isRefreshing = false
                    }
                    render(state)
                }
            }
        }
        
        view.findViewById<View>(R.id.btn_logout).setOnClickListener {
            authViewModel.logout(requireContext())
            val navOptions = androidx.navigation.NavOptions.Builder()
                .setPopUpTo(R.id.homeContainerFragment, true)
                .build()
            requireActivity().findNavController(R.id.nav_host_fragment).navigate(R.id.welcomeFragment, null, navOptions)
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
                    text = state.message
                    visibility = View.VISIBLE
                }
            }
            is FeedUiState.Content -> {
                loading?.visibility = View.GONE
                message?.visibility = View.GONE
                pager?.visibility = View.VISIBLE
                currentEntries = state.entries
                adapter?.submitList(state.entries) {
                    val restorePosition = pendingRestoreCreatorPosition
                    if (restorePosition != null && restorePosition in state.entries.indices) {
                        pager?.setCurrentItem(restorePosition, false)
                    }
                    pendingRestoreCreatorPosition = null
                    // post: give ViewPager2 one frame to attach its child ViewHolders before activating
                    pager?.post { pager?.let { updateActiveCreator(it.currentItem) } }
                }
            }
        }
    }

    private fun updateActiveCreator(selectedPosition: Int) {
        val pg = pager ?: return
        val recycler = pg.getChildAt(0) as? RecyclerView ?: return
        val prev = activeCreatorPosition
        activeCreatorPosition = selectedPosition
        activeItemPosition = 0
        val creatorId = currentEntries.getOrNull(selectedPosition)?.creatorId
        if (creatorId != null) actionsViewModel.setCurrentCreator(creatorId)
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

    private fun prewarmVertical() {
        val pool = pool ?: return
        val entries = currentEntries
        val ci = activeCreatorPosition
        if (ci < 0 || ci >= entries.size) return

        val nextUrl = entries.getOrNull(ci + 1)?.items?.firstOrNull()?.mediaUrl
        val prevUrl = entries.getOrNull(ci - 1)?.items?.firstOrNull()?.mediaUrl
        pool.prewarmVertical(nextUrl, prevUrl)
    }

    override fun onPause() {
        super.onPause()
        pool?.pauseAll()
    }

    override fun onResume() {
        super.onResume()
        pager?.post { pager?.let { updateActiveCreator(it.currentItem) } }
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
        currentEntries = emptyList()
        super.onDestroyView()
    }

    override fun onDestroy() {
        pool?.release()
        pool = null
        super.onDestroy()
    }

    private companion object {
        const val KEY_FEED_CREATOR_POSITION = "feed_creator_position"
    }
}

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
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.aura.app.R
import kotlinx.coroutines.launch

class VideoFeedFragment : Fragment(R.layout.fragment_video_feed) {

    private val viewModel: VideoFeedViewModel by viewModels { VideoFeedViewModel.Factory() }
    private val authViewModel: com.aura.app.ui.auth.AuthViewModel by activityViewModels { com.aura.app.ui.auth.AuthViewModel.Factory() }

    private var pager: ViewPager2? = null
    private var loading: ProgressBar? = null
    private var message: TextView? = null
    private var adapter: CreatorPageAdapter? = null
    private var playerPool: ExoPlayerPool? = null

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

        val pool = ExoPlayerPool(requireContext().applicationContext).also {
            playerPool = it
            it.warmUp(4)
        }
        val feedAdapter = CreatorPageAdapter(pool, viewModel.userRepository, viewLifecycleOwner.lifecycleScope)
            .also { adapter = it }
        pager?.adapter = feedAdapter
        pager?.offscreenPageLimit = 1
        pager?.registerOnPageChangeCallback(pageCallback)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { render(it) }
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
                    text = getString(R.string.feed_error)
                    visibility = View.VISIBLE
                }
            }
            is FeedUiState.Content -> {
                loading?.visibility = View.GONE
                message?.visibility = View.GONE
                pager?.visibility = View.VISIBLE
                adapter?.submitList(state.entries) {
                    pager?.let { updateActiveCreator(it.currentItem) }
                }
            }
        }
    }

    private fun updateActiveCreator(selectedPosition: Int) {
        val pg = pager ?: return
        val recycler = pg.getChildAt(0) as? RecyclerView ?: return
        for (i in 0 until recycler.childCount) {
            val holder = recycler.getChildViewHolder(recycler.getChildAt(i)) as? CreatorPageViewHolder
                ?: continue
            if (holder.bindingAdapterPosition == selectedPosition) {
                holder.activate()
            } else {
                holder.deactivate()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        deactivateAll()
    }

    override fun onResume() {
        super.onResume()
        pager?.let { updateActiveCreator(it.currentItem) }
    }

    private fun deactivateAll() {
        val pg = pager ?: return
        val recycler = pg.getChildAt(0) as? RecyclerView ?: return
        for (i in 0 until recycler.childCount) {
            (recycler.getChildViewHolder(recycler.getChildAt(i)) as? CreatorPageViewHolder)
                ?.deactivate()
        }
    }

    override fun onDestroyView() {
        pager?.unregisterOnPageChangeCallback(pageCallback)
        pager?.adapter = null
        adapter = null
        pager = null
        loading = null
        message = null
        playerPool?.releaseAll()
        playerPool = null
        super.onDestroyView()
    }
}

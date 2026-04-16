package com.aura.app.ui.feed

import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.aura.app.R
import com.aura.app.data.model.CreatorFeedEntry
import com.aura.app.data.repository.UserRepository
import com.bumptech.glide.Glide
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CreatorPageViewHolder(
    itemView: View,
    private val pool: ExoPlayerPool,
    private val userRepository: UserRepository,
    private val scope: CoroutineScope,
) : RecyclerView.ViewHolder(itemView) {

    private val itemsRecycler: RecyclerView = itemView.findViewById(R.id.items_recycler)
    private val creatorAvatar: ImageView = itemView.findViewById(R.id.creator_avatar)
    private val creatorName: TextView = itemView.findViewById(R.id.creator_name)
    private val dotIndicator: LinearLayout = itemView.findViewById(R.id.dot_indicator)

    private var innerAdapter: ItemPageAdapter? = null
    private var creatorJob: Job? = null
    private var currentInnerPosition = 0
    private var isActive = false

    private val layoutManager = LinearLayoutManager(
        itemView.context, LinearLayoutManager.HORIZONTAL, false
    )
    private val snapHelper = PagerSnapHelper()

    private val scrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                val snappedView = snapHelper.findSnapView(layoutManager) ?: return
                val position = layoutManager.getPosition(snappedView)
                if (position != currentInnerPosition) {
                    if (isActive) {
                        pauseInnerAt(currentInnerPosition)
                        playInnerAt(position)
                    }
                    currentInnerPosition = position
                    updateDots(position)
                }
            }
        }
    }

    fun bind(entry: CreatorFeedEntry) {
        val adapter = ItemPageAdapter(pool).also { innerAdapter = it }
        itemsRecycler.layoutManager = layoutManager
        itemsRecycler.adapter = adapter
        if (itemsRecycler.onFlingListener == null) {
            snapHelper.attachToRecyclerView(itemsRecycler)
        }
        itemsRecycler.addOnScrollListener(scrollListener)
        adapter.submitList(entry.items)
        currentInnerPosition = 0

        creatorName.text = ""
        creatorAvatar.setImageDrawable(null)
        creatorJob?.cancel()
        creatorJob = scope.launch {
            val user = withContext(Dispatchers.IO) {
                runCatching { userRepository.getUserLite(entry.creatorId) }.getOrNull()
            } ?: return@launch
            creatorName.text = user.displayName
            if (user.profileImageUrl.isNotEmpty()) {
                Glide.with(creatorAvatar)
                    .load(user.profileImageUrl)
                    .circleCrop()
                    .into(creatorAvatar)
            }
        }

        setupDots(entry.items.size)
    }

    fun activate() {
        isActive = true
        playInnerAt(currentInnerPosition)
    }

    fun deactivate() {
        isActive = false
        pauseInnerAt(currentInnerPosition)
    }

    fun releaseAllPlayers() {
        for (i in 0 until itemsRecycler.childCount) {
            (itemsRecycler.getChildViewHolder(itemsRecycler.getChildAt(i)) as? VideoPageViewHolder)
                ?.releasePlayer()
        }
    }

    fun unbind() {
        itemsRecycler.removeOnScrollListener(scrollListener)
        snapHelper.attachToRecyclerView(null)
        itemsRecycler.adapter = null
        innerAdapter = null
        creatorJob?.cancel()
        creatorJob = null
    }

    private fun playInnerAt(position: Int) {
        itemsRecycler.post { findInnerHolder(position)?.play() }
    }

    private fun pauseInnerAt(position: Int) {
        itemsRecycler.post { findInnerHolder(position)?.pause() }
    }

    private fun findInnerHolder(position: Int): VideoPageViewHolder? {
        for (i in 0 until itemsRecycler.childCount) {
            val vh = itemsRecycler.getChildViewHolder(itemsRecycler.getChildAt(i)) as? VideoPageViewHolder
            if (vh?.bindingAdapterPosition == position) return vh
        }
        return null
    }

    private fun setupDots(count: Int) {
        dotIndicator.removeAllViews()
        if (count <= 1) {
            dotIndicator.visibility = View.GONE
            return
        }
        dotIndicator.visibility = View.VISIBLE
        for (i in 0 until count) {
            val dot = View(itemView.context).apply {
                val size = (8 * itemView.resources.displayMetrics.density).toInt()
                val margin = (4 * itemView.resources.displayMetrics.density).toInt()
                layoutParams = LinearLayout.LayoutParams(size, size).apply {
                    setMargins(margin, 0, margin, 0)
                }
                background = android.graphics.drawable.GradientDrawable().apply {
                    shape = android.graphics.drawable.GradientDrawable.OVAL
                    setColor(if (i == 0) 0xFFFFFFFF.toInt() else 0x80FFFFFF.toInt())
                }
            }
            dotIndicator.addView(dot)
        }
    }

    private fun updateDots(selected: Int) {
        for (i in 0 until dotIndicator.childCount) {
            val dot = dotIndicator.getChildAt(i)
            (dot.background as? android.graphics.drawable.GradientDrawable)?.setColor(
                if (i == selected) 0xFFFFFFFF.toInt() else 0x80FFFFFF.toInt()
            )
        }
    }
}

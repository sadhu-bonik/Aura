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
import com.bumptech.glide.Glide

class CreatorPageViewHolder(
    itemView: View,
    private val callback: ActiveVideoCallback,
) : RecyclerView.ViewHolder(itemView) {

    private val itemsRecycler: RecyclerView = itemView.findViewById(R.id.items_recycler)
    private val creatorAvatar: ImageView = itemView.findViewById(R.id.creator_avatar)
    private val creatorName: TextView = itemView.findViewById(R.id.creator_name)
    private val dotIndicator: LinearLayout = itemView.findViewById(R.id.dot_indicator)
    private val creatorInfoContainer: View = itemView.findViewById(R.id.creator_info_container)
    private val videoDescription: TextView = itemView.findViewById(R.id.tv_video_description)
    private val videoTitle: TextView = itemView.findViewById(R.id.tv_video_title)

    private var innerAdapter: ItemPageAdapter? = null
    private var currentEntry: CreatorFeedEntry? = null
    private var currentInnerPosition = 0
    var isActive = false
        private set

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
                    currentInnerPosition = position
                    updateDots(position)
                    updateVideoMetadata(position)
                    callback.onItemPositionChanged(bindingAdapterPosition, position)
                }
                if (isActive) {
                    activateCurrentInner()
                }
            }
        }
    }

    fun bind(entry: CreatorFeedEntry) {
        currentEntry = entry
        val adapter = ItemPageAdapter(callback).also { innerAdapter = it }
        itemsRecycler.layoutManager = layoutManager
        itemsRecycler.adapter = adapter
        if (itemsRecycler.onFlingListener == null) {
            snapHelper.attachToRecyclerView(itemsRecycler)
        }
        itemsRecycler.addOnScrollListener(scrollListener)
        adapter.submitList(entry.items)
        currentInnerPosition = 0

        // Bind pre-resolved creator identity (no Firestore call needed here)
        creatorName.text = entry.creatorName.ifBlank { "Unknown Creator" }
        creatorAvatar.setImageDrawable(null)
        if (entry.creatorProfileImageUrl.isNotEmpty()) {
            Glide.with(creatorAvatar)
                .load(entry.creatorProfileImageUrl)
                .circleCrop()
                .into(creatorAvatar)
        }

        // Navigate to creator profile on tap
        creatorInfoContainer.setOnClickListener {
            callback.onCreatorProfileClicked(entry.creatorId)
        }

        setupDots(entry.items.size)
        updateVideoMetadata(0)
    }

    fun activate() {
        isActive = true
        activateCurrentInner()
    }

    fun deactivate() {
        isActive = false
        findInnerHolder(currentInnerPosition)?.let { callback.detachPlayer(it) }
    }

    fun unbind() {
        itemsRecycler.removeOnScrollListener(scrollListener)
        snapHelper.attachToRecyclerView(null)
        itemsRecycler.adapter = null
        innerAdapter = null
        currentEntry = null
    }

    private fun updateVideoMetadata(position: Int) {
        val item = currentEntry?.items?.getOrNull(position)

        val desc = item?.description?.trim() ?: ""
        if (desc.isEmpty()) {
            videoDescription.visibility = View.GONE
        } else {
            videoDescription.text = desc
            videoDescription.visibility = View.VISIBLE
        }

        val title = item?.title?.trim() ?: ""
        if (title.isEmpty()) {
            videoTitle.visibility = View.GONE
        } else {
            videoTitle.text = title
            videoTitle.visibility = View.VISIBLE
        }
    }

    private fun activateCurrentInner() {
        itemsRecycler.post {
            val holder = findInnerHolder(currentInnerPosition) ?: return@post
            val item = innerAdapter?.currentList?.getOrNull(currentInnerPosition) ?: return@post
            callback.attachPlayer(holder, item)
        }
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

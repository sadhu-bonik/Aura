package com.aura.app.ui.feed

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.aura.app.R
import com.aura.app.data.model.PortfolioItem

class ItemPageAdapter(
    private val pool: ExoPlayerPool,
) : ListAdapter<PortfolioItem, VideoPageViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoPageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_video_page, parent, false)
        return VideoPageViewHolder(view, pool)
    }

    override fun onBindViewHolder(holder: VideoPageViewHolder, position: Int) {
        val parent = holder.itemView.parent as? androidx.recyclerview.widget.RecyclerView
        if (parent != null && parent.width > 0) {
            holder.itemView.layoutParams.width = parent.width
            holder.itemView.layoutParams.height = parent.height
        }
        holder.bind(getItem(position))
    }

    override fun onViewRecycled(holder: VideoPageViewHolder) {
        holder.releasePlayer()
    }

    private companion object {
        val DIFF = object : DiffUtil.ItemCallback<PortfolioItem>() {
            override fun areItemsTheSame(a: PortfolioItem, b: PortfolioItem) = a.itemId == b.itemId
            override fun areContentsTheSame(a: PortfolioItem, b: PortfolioItem) = a == b
        }
    }
}

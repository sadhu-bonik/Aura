package com.aura.app.ui.feed

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.aura.app.R
import com.aura.app.data.model.CreatorFeedEntry

class CreatorPageAdapter(
    private val callback: ActiveVideoCallback,
) : ListAdapter<CreatorFeedEntry, CreatorPageViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CreatorPageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_creator_page, parent, false)
        return CreatorPageViewHolder(view, callback)
    }

    override fun onBindViewHolder(holder: CreatorPageViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onViewRecycled(holder: CreatorPageViewHolder) {
        holder.unbind()
    }

    private companion object {
        val DIFF = object : DiffUtil.ItemCallback<CreatorFeedEntry>() {
            override fun areItemsTheSame(a: CreatorFeedEntry, b: CreatorFeedEntry) =
                a.creatorId == b.creatorId
            override fun areContentsTheSame(a: CreatorFeedEntry, b: CreatorFeedEntry) =
                a == b
        }
    }
}

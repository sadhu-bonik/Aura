package com.aura.app.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.aura.app.data.model.PortfolioItem
import com.aura.app.databinding.ItemPortfolioVideoBinding
import com.bumptech.glide.Glide

class PortfolioAdapter : ListAdapter<PortfolioItem, PortfolioAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPortfolioVideoBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(private val binding: ItemPortfolioVideoBinding) :
        RecyclerView.ViewHolder(binding.root) {
        
        fun bind(item: PortfolioItem) {
            Glide.with(binding.ivThumbnail)
                .load(item.thumbnailUrl.ifBlank { item.mediaUrl })
                .centerCrop()
                .into(binding.ivThumbnail)
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<PortfolioItem>() {
        override fun areItemsTheSame(oldItem: PortfolioItem, newItem: PortfolioItem): Boolean =
            oldItem.itemId == newItem.itemId

        override fun areContentsTheSame(oldItem: PortfolioItem, newItem: PortfolioItem): Boolean =
            oldItem == newItem
    }
}

package com.aura.app.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.aura.app.data.model.PortfolioItem
import com.aura.app.databinding.ItemPortfolioVideoBinding
import com.bumptech.glide.Glide

class PortfolioAdapter : ListAdapter<PortfolioItem, PortfolioAdapter.ViewHolder>(DiffCallback) {

    /**
     * Optional delete callback. When non-null, the delete button on each item
     * is made visible and invokes this lambda with the tapped item.
     * Set to null (default) for read-only / viewer mode.
     */
    var onDeleteClick: ((PortfolioItem) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPortfolioVideoBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), onDeleteClick)
    }

    class ViewHolder(private val binding: ItemPortfolioVideoBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: PortfolioItem, onDeleteClick: ((PortfolioItem) -> Unit)?) {
            Glide.with(binding.ivThumbnail)
                .load(item.thumbnailUrl.ifBlank { item.mediaUrl })
                .centerCrop()
                .placeholder(com.aura.app.R.color.colorSurfaceContainerHigh)
                .into(binding.ivThumbnail)

            if (onDeleteClick != null) {
                binding.btnDelete.visibility = View.VISIBLE
                binding.btnDelete.setOnClickListener { onDeleteClick(item) }
            } else {
                binding.btnDelete.visibility = View.GONE
                binding.btnDelete.setOnClickListener(null)
            }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<PortfolioItem>() {
        override fun areItemsTheSame(oldItem: PortfolioItem, newItem: PortfolioItem): Boolean =
            oldItem.itemId == newItem.itemId

        override fun areContentsTheSame(oldItem: PortfolioItem, newItem: PortfolioItem): Boolean =
            oldItem == newItem
    }
}

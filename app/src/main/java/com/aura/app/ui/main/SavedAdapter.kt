package com.aura.app.ui.main

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.aura.app.R
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.request.RequestOptions
import java.util.Locale

class SavedAdapter(
    private val onClick: (SavedCreatorUI) -> Unit
) : ListAdapter<SavedCreatorUI, SavedAdapter.SavedViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SavedViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_saved_creator, parent, false)
        return SavedViewHolder(view, onClick)
    }

    override fun onBindViewHolder(holder: SavedViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class SavedViewHolder(
        view: View,
        val onClick: (SavedCreatorUI) -> Unit
    ) : RecyclerView.ViewHolder(view) {
        private val ivBg: ImageView = view.findViewById(R.id.iv_creator_bg)
        private val ivAvatar: ImageView = view.findViewById(R.id.iv_creator_avatar)
        private val tvName: TextView = view.findViewById(R.id.tv_creator_name)
        private val tvHeadline: TextView = view.findViewById(R.id.tv_creator_headline)
        private val tvRating: TextView = view.findViewById(R.id.tv_rating)

        fun bind(item: SavedCreatorUI) {
            tvName.text = item.displayName
            tvHeadline.text = item.headline
            tvRating.text = String.format(Locale.US, "%.1f", item.averageRating)

            Glide.with(itemView)
                .load(item.profileImageUrl)
                .placeholder(R.drawable.bg_avatar_placeholder)
                .centerCrop()
                .into(ivBg)

            Glide.with(itemView)
                .load(item.profileImageUrl)
                .placeholder(R.drawable.bg_avatar_placeholder)
                .into(ivAvatar)

            itemView.setOnClickListener {
                onClick(item)
            }
        }
    }

    object DiffCallback : DiffUtil.ItemCallback<SavedCreatorUI>() {
        override fun areItemsTheSame(oldItem: SavedCreatorUI, newItem: SavedCreatorUI): Boolean {
            return oldItem.userId == newItem.userId
        }
        override fun areContentsTheSame(oldItem: SavedCreatorUI, newItem: SavedCreatorUI): Boolean {
            return oldItem == newItem
        }
    }
}

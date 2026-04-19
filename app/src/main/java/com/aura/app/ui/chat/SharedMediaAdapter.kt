package com.aura.app.ui.chat

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.aura.app.data.model.Message
import com.aura.app.databinding.ItemSharedMediaBinding
import com.bumptech.glide.Glide

class SharedMediaAdapter(
    private val onVideoClick: ((String) -> Unit)? = null,
) : ListAdapter<Message, SharedMediaAdapter.ViewHolder>(DiffCallback) {

    inner class ViewHolder(private val binding: ItemSharedMediaBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(message: Message) {
            Glide.with(binding.ivMediaThumb)
                .load(message.mediaUrl)
                .centerCrop()
                .into(binding.ivMediaThumb)
            binding.ivPlayOverlay.isVisible = message.mediaType == "video"
            binding.root.setOnClickListener {
                if (message.mediaType == "video") onVideoClick?.invoke(message.mediaUrl)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(ItemSharedMediaBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(getItem(position))

    private object DiffCallback : DiffUtil.ItemCallback<Message>() {
        override fun areItemsTheSame(oldItem: Message, newItem: Message) = oldItem.messageId == newItem.messageId
        override fun areContentsTheSame(oldItem: Message, newItem: Message) = oldItem == newItem
    }
}

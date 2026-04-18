package com.aura.app.ui.chat

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.aura.app.R
import com.aura.app.databinding.ItemActiveDealBinding
import com.bumptech.glide.Glide
import java.text.SimpleDateFormat
import java.util.Locale

class ActiveDealAdapter(
    private val onDealClick: (ActiveDealItem) -> Unit,
) : ListAdapter<ActiveDealItem, ActiveDealAdapter.ViewHolder>(DiffCallback) {

    inner class ViewHolder(private val binding: ItemActiveDealBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ActiveDealItem) {
            val deal = item.deal

            binding.tvDealTitle.text = deal.title
            val ctx = binding.root.context
            binding.tvOtherPartyName.text = ctx.getString(
                R.string.label_deal_with,
                item.otherUser?.displayName ?: "—"
            )

            binding.tvLastMessage.text = item.lastMessage?.let {
                when (it.mediaType) {
                    "image" -> ctx.getString(R.string.label_no_messages).let { "Photo" }
                    "video" -> "Video"
                    "file" -> it.fileName.ifEmpty { "File" }
                    else -> it.content.ifEmpty { ctx.getString(R.string.label_no_messages) }
                }
            } ?: ctx.getString(R.string.label_no_messages)

            binding.viewUnreadDot.isVisible = item.unreadCount > 0

            item.lastMessage?.sentAt?.toDate()?.let { date ->
                val fmt = SimpleDateFormat("h:mm a", Locale.getDefault())
                binding.tvTimestamp.text = fmt.format(date)
            }

            Glide.with(binding.ivAvatar)
                .load(item.otherUser?.profileImageUrl)
                .circleCrop()
                .placeholder(R.drawable.ic_launcher_foreground)
                .into(binding.ivAvatar)

            binding.chipStatus.visibility = View.GONE

            binding.root.setOnClickListener { onDealClick(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(ItemActiveDealBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(getItem(position))

    private companion object DiffCallback : DiffUtil.ItemCallback<ActiveDealItem>() {
        override fun areItemsTheSame(oldItem: ActiveDealItem, newItem: ActiveDealItem) =
            oldItem.deal.dealId == newItem.deal.dealId
        override fun areContentsTheSame(oldItem: ActiveDealItem, newItem: ActiveDealItem) =
            oldItem == newItem
    }
}


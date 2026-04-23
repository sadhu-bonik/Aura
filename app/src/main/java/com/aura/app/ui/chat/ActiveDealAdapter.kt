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
import com.aura.app.utils.Constants
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
            val ctx = binding.root.context

            binding.tvDealTitle.text = deal.title
            binding.tvOtherPartyName.text = ctx.getString(
                R.string.label_deal_with,
                item.otherUser?.displayName ?: "—"
            )

            binding.tvLastMessage.text = deal.lastMessageText.ifBlank {
                ctx.getString(R.string.label_no_messages)
            }

            binding.viewUnreadDot.isVisible = item.unreadCount > 0

            deal.lastMessageTime?.toDate()?.let { date ->
                binding.tvTimestamp.text = SimpleDateFormat("h:mm a", Locale.getDefault()).format(date)
            }

            Glide.with(binding.ivAvatar)
                .load(item.otherUser?.profileImageUrl)
                .circleCrop()
                .placeholder(R.drawable.bg_avatar_placeholder)
                .fallback(R.drawable.bg_avatar_placeholder)
                .into(binding.ivAvatar)

            if (deal.status == Constants.STATUS_COMPLETED) {
                binding.chipStatus.visibility = View.VISIBLE
                binding.chipStatus.text = ctx.getString(R.string.chip_review_required)
                binding.chipStatus.setBackgroundResource(R.drawable.chip_status_accepted)
                binding.chipStatus.setTextColor(ctx.getColor(R.color.colorPrimary))
                binding.viewUnreadDot.isVisible = false
            } else {
                binding.chipStatus.visibility = View.GONE
            }

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

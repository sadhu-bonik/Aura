package com.aura.app.ui.chat

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.aura.app.data.model.Notification
import com.aura.app.databinding.ItemNotificationBinding

class NotificationAdapter(
    private val onClick: (Notification) -> Unit,
) : ListAdapter<Notification, NotificationAdapter.ViewHolder>(DiffCallback) {

    inner class ViewHolder(private val binding: ItemNotificationBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(notif: Notification) {
            binding.tvNotifMessage.text = notif.message

            val timeMs = notif.createdAt?.toDate()?.time
            binding.tvNotifTime.text = if (timeMs != null) {
                DateUtils.getRelativeTimeSpanString(
                    timeMs,
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS,
                    DateUtils.FORMAT_ABBREV_RELATIVE,
                )
            } else ""

            binding.viewUnreadDot.isVisible = !notif.read

            // Dim read notifications slightly
            binding.root.alpha = if (notif.read) 0.65f else 1f

            binding.root.setOnClickListener { onClick(notif) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(ItemNotificationBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(getItem(position))

    private companion object DiffCallback : DiffUtil.ItemCallback<Notification>() {
        override fun areItemsTheSame(oldItem: Notification, newItem: Notification) =
            oldItem.notifId == newItem.notifId
        override fun areContentsTheSame(oldItem: Notification, newItem: Notification) =
            oldItem == newItem
    }
}

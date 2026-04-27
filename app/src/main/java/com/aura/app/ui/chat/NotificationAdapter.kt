package com.aura.app.ui.chat

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.aura.app.R
import com.aura.app.data.model.Notification
import com.aura.app.databinding.ItemNotificationBinding

class NotificationAdapter(
    private val onClick: (Notification) -> Unit,
) : ListAdapter<Notification, NotificationAdapter.ViewHolder>(DiffCallback) {

    inner class ViewHolder(private val binding: ItemNotificationBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(notif: Notification) {
            val ctx = binding.root.context

            // Title — prefer deal title; fall back to actor name; final fallback "Notifications"
            binding.tvNotifTitle.text = when {
                notif.dealTitle.isNotBlank() -> notif.dealTitle
                notif.actorName.isNotBlank() -> notif.actorName
                else -> ctx.getString(R.string.notif_title)
            }
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

            // Type-driven icon
            val iconRes = when (notif.type) {
                Notification.TYPE_REVIEW_REQUESTED -> R.drawable.ic_aura_star_filled
                Notification.TYPE_DEAL_COMPLETED,
                Notification.TYPE_DEAL_COMPLETION_REQUESTED -> R.drawable.ic_check_circle
                Notification.TYPE_DEAL_REJECTED,
                Notification.TYPE_DEAL_RETRACTED,
                Notification.TYPE_DEAL_CANCEL_REQUESTED,
                Notification.TYPE_DEAL_CANCEL_DECLINED,
                Notification.TYPE_DEAL_CANCELED -> R.drawable.ic_close
                else -> R.drawable.ic_handshake
            }
            binding.ivNotifIcon.setImageResource(iconRes)

            // Read/unread visual:
            //  - unread: full-strength title, dot visible, accent background tint via primaryContainer
            //  - read:   subdued title, dot hidden
            binding.viewUnreadDot.isVisible = !notif.read
            binding.tvNotifTitle.alpha = if (notif.read) 0.55f else 1f
            binding.tvNotifMessage.alpha = if (notif.read) 0.55f else 1f
            binding.root.setBackgroundColor(
                if (notif.read) ContextCompat.getColor(ctx, android.R.color.transparent)
                else ContextCompat.getColor(ctx, R.color.colorSurfaceContainer)
            )

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

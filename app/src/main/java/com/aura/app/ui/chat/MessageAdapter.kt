package com.aura.app.ui.chat

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.aura.app.data.model.Message
import com.aura.app.databinding.ItemMessageFailedBinding
import com.aura.app.databinding.ItemMessageReceivedBinding
import com.aura.app.databinding.ItemMessageSentBinding
import com.aura.app.databinding.ItemMessageSystemBinding
import com.bumptech.glide.Glide
import java.text.SimpleDateFormat
import java.util.Locale

class MessageAdapter(
    private val currentUserId: String,
    private val senderAvatarUrl: String? = null,
    private val onVideoClick: ((String) -> Unit)? = null,
    private val onRetry: ((ChatListItem.FailedMessage) -> Unit)? = null,
) : ListAdapter<ChatListItem, RecyclerView.ViewHolder>(DiffCallback) {

    inner class SentViewHolder(private val binding: ItemMessageSentBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(message: Message) {
            bindBubble(
                content = message.content,
                mediaUrl = message.mediaUrl,
                mediaType = message.mediaType,
                fileName = message.fileName,
                sentAt = message.sentAt,
                frameMedia = binding.frameMedia,
                ivMedia = binding.ivMedia,
                ivPlayOverlay = binding.ivPlayOverlay,
                llFile = binding.llFile,
                tvFileName = binding.tvFileName,
                tvContent = binding.tvMessageContent,
                tvTimestamp = binding.tvTimestamp,
            )
            binding.frameMedia.setOnClickListener {
                if (message.mediaType == "video") onVideoClick?.invoke(message.mediaUrl)
            }
            // Receipt: ✓ = sent (not yet read), ✓✓ = seen
            binding.tvReceipt.text = if (message.isRead) "✓✓" else "✓"
            binding.tvReceipt.setTextColor(
                if (message.isRead) Color.parseColor("#6C63FF") else Color.parseColor("#AAFFFFFF")
            )
        }
    }

    inner class ReceivedViewHolder(private val binding: ItemMessageReceivedBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(message: Message) {
            Glide.with(binding.ivSenderAvatar)
                .load(senderAvatarUrl)
                .circleCrop()
                .into(binding.ivSenderAvatar)

            bindBubble(
                content = message.content,
                mediaUrl = message.mediaUrl,
                mediaType = message.mediaType,
                fileName = message.fileName,
                sentAt = message.sentAt,
                frameMedia = binding.frameMedia,
                ivMedia = binding.ivMedia,
                ivPlayOverlay = binding.ivPlayOverlay,
                llFile = binding.llFile,
                tvFileName = binding.tvFileName,
                tvContent = binding.tvMessageContent,
                tvTimestamp = binding.tvTimestamp,
            )
            binding.frameMedia.setOnClickListener {
                if (message.mediaType == "video") onVideoClick?.invoke(message.mediaUrl)
            }
        }
    }

    inner class SystemViewHolder(private val binding: ItemMessageSystemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(message: Message) {
            binding.tvSystemMessage.text = message.content
        }
    }

    inner class FailedViewHolder(private val binding: ItemMessageFailedBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ChatListItem.FailedMessage) {
            binding.tvFailedContent.text = item.content.ifBlank {
                when (item.mediaType) {
                    "image" -> "Photo"
                    "video" -> "Video"
                    "file" -> item.fileName.ifBlank { "File" }
                    else -> item.content
                }
            }
            binding.btnRetry.setOnClickListener { onRetry?.invoke(item) }
        }
    }

    private fun bindBubble(
        content: String,
        mediaUrl: String,
        mediaType: String,
        fileName: String,
        sentAt: com.google.firebase.Timestamp?,
        frameMedia: View,
        ivMedia: android.widget.ImageView,
        ivPlayOverlay: android.widget.ImageView,
        llFile: View,
        tvFileName: android.widget.TextView,
        tvContent: android.widget.TextView,
        tvTimestamp: android.widget.TextView,
    ) {
        val isMedia = mediaType == "image" || mediaType == "video"
        val isFile = mediaType == "file"

        frameMedia.isVisible = isMedia
        if (isMedia) {
            Glide.with(ivMedia).load(mediaUrl).centerCrop().into(ivMedia)
            ivPlayOverlay.isVisible = mediaType == "video"
        }

        llFile.isVisible = isFile
        if (isFile) tvFileName.text = fileName.ifEmpty { "File" }

        tvContent.isVisible = content.isNotBlank()
        if (content.isNotBlank()) tvContent.text = content

        sentAt?.toDate()?.let { date ->
            tvTimestamp.text = SimpleDateFormat("h:mm a", Locale.getDefault()).format(date)
        }
    }

    override fun getItemViewType(position: Int): Int = when (val item = getItem(position)) {
        is ChatListItem.RegularMessage -> when {
            item.message.isSystem -> VIEW_SYSTEM
            item.message.senderId == currentUserId -> VIEW_SENT
            else -> VIEW_RECEIVED
        }
        is ChatListItem.FailedMessage -> VIEW_FAILED
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        when (viewType) {
            VIEW_SENT -> SentViewHolder(
                ItemMessageSentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            )
            VIEW_RECEIVED -> ReceivedViewHolder(
                ItemMessageReceivedBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            )
            VIEW_SYSTEM -> SystemViewHolder(
                ItemMessageSystemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            )
            else -> FailedViewHolder(
                ItemMessageFailedBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            )
        }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is SentViewHolder -> holder.bind((getItem(position) as ChatListItem.RegularMessage).message)
            is ReceivedViewHolder -> holder.bind((getItem(position) as ChatListItem.RegularMessage).message)
            is SystemViewHolder -> holder.bind((getItem(position) as ChatListItem.RegularMessage).message)
            is FailedViewHolder -> holder.bind(getItem(position) as ChatListItem.FailedMessage)
        }
    }

    private companion object {
        const val VIEW_SENT = 0
        const val VIEW_RECEIVED = 1
        const val VIEW_FAILED = 2
        const val VIEW_SYSTEM = 3
    }

    private object DiffCallback : DiffUtil.ItemCallback<ChatListItem>() {
        override fun areItemsTheSame(oldItem: ChatListItem, newItem: ChatListItem): Boolean = when {
            oldItem is ChatListItem.RegularMessage && newItem is ChatListItem.RegularMessage ->
                oldItem.message.messageId == newItem.message.messageId
            oldItem is ChatListItem.FailedMessage && newItem is ChatListItem.FailedMessage ->
                oldItem.tempId == newItem.tempId
            else -> false
        }
        override fun areContentsTheSame(oldItem: ChatListItem, newItem: ChatListItem) = oldItem == newItem
    }
}

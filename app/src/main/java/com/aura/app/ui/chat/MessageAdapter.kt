package com.aura.app.ui.chat

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.aura.app.data.model.Message
import com.aura.app.databinding.ItemMessageReceivedBinding
import com.aura.app.databinding.ItemMessageSentBinding
import com.bumptech.glide.Glide
import java.text.SimpleDateFormat
import java.util.Locale

class MessageAdapter(
    private val currentUserId: String,
    private val senderAvatarUrl: String? = null,
    private val onVideoClick: ((String) -> Unit)? = null,
) : ListAdapter<Message, RecyclerView.ViewHolder>(DiffCallback) {

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

    override fun getItemViewType(position: Int): Int =
        if (getItem(position).senderId == currentUserId) VIEW_SENT else VIEW_RECEIVED

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        if (viewType == VIEW_SENT) {
            SentViewHolder(ItemMessageSentBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        } else {
            ReceivedViewHolder(ItemMessageReceivedBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is SentViewHolder -> holder.bind(getItem(position))
            is ReceivedViewHolder -> holder.bind(getItem(position))
        }
    }

    private companion object {
        const val VIEW_SENT = 0
        const val VIEW_RECEIVED = 1
    }

    private object DiffCallback : DiffUtil.ItemCallback<Message>() {
        override fun areItemsTheSame(oldItem: Message, newItem: Message) = oldItem.messageId == newItem.messageId
        override fun areContentsTheSame(oldItem: Message, newItem: Message) = oldItem == newItem
    }
}

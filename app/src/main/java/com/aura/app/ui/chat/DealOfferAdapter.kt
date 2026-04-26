package com.aura.app.ui.chat

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.aura.app.R
import com.aura.app.data.model.Deal
import com.aura.app.databinding.ItemDealOfferBinding
import com.aura.app.utils.Constants
import com.aura.app.utils.StubSession
import com.bumptech.glide.Glide

enum class OfferCardMode { NEW_DEALS, COMPLETED, PAST }

class DealOfferAdapter(
    private val mode: OfferCardMode,
    private val onItemClick: ((DealOfferItem) -> Unit)? = null,
    private val onChevronClick: ((DealOfferItem) -> Unit)? = null,
    private val onAccept: ((String) -> Unit)? = null,
    private val onReject: ((String) -> Unit)? = null,
) : ListAdapter<DealOfferItem, DealOfferAdapter.ViewHolder>(DiffCallback) {

    private var reviewsLoaded = false
    private var reviewsMap = emptyMap<String, com.aura.app.data.model.Review>()

    fun setReviewsData(loaded: Boolean, map: Map<String, com.aura.app.data.model.Review>) {
        reviewsLoaded = loaded
        reviewsMap = map
        notifyItemRangeChanged(0, itemCount)
    }

    inner class ViewHolder(private val binding: ItemDealOfferBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: DealOfferItem) {
            val deal = item.deal
            val role = StubSession.role()

            if (role == Constants.ROLE_CREATOR) {
                binding.tvPrimary.text = item.otherUser?.displayName ?: "—"
                binding.tvSecondary.text = deal.title
            } else {
                binding.tvPrimary.text = deal.title
                binding.tvSecondary.text = item.otherUser?.displayName ?: "—"
            }

            Glide.with(binding.ivOfferAvatar)
                .load(item.otherUser?.profileImageUrl)
                .circleCrop()
                .placeholder(R.drawable.bg_avatar_placeholder)
                .fallback(R.drawable.bg_avatar_placeholder)
                .into(binding.ivOfferAvatar)

            when (mode) {
                OfferCardMode.NEW_DEALS -> bindNewDeal(item, deal, role)
                OfferCardMode.COMPLETED -> bindCompleted(item)
                OfferCardMode.PAST -> bindPast(item, deal)
            }
        }

        private fun bindNewDeal(item: DealOfferItem, deal: Deal, role: String) {
            binding.ivChevron.isVisible = true
            binding.tvStatusChip.visibility = View.GONE
            binding.root.alpha = 1f
            binding.root.isClickable = true
            binding.root.isFocusable = true
            binding.root.setOnClickListener { onItemClick?.invoke(item) }

            val showInlineActions = role == Constants.ROLE_CREATOR && deal.status == Constants.STATUS_PENDING
            binding.tvBtnAccept.isVisible = showInlineActions
            binding.ivBtnReject.isVisible = showInlineActions

            if (showInlineActions) {
                binding.tvBtnAccept.setOnClickListener { onAccept?.invoke(deal.dealId) }
                binding.ivBtnReject.setOnClickListener { onReject?.invoke(deal.dealId) }
            }
        }

        private fun bindCompleted(item: DealOfferItem) {
            val deal = item.deal
            binding.ivChevron.isVisible = true
            binding.tvStatusChip.visibility = View.VISIBLE
            binding.tvBtnAccept.isVisible = false
            binding.ivBtnReject.isVisible = false
            
            // Re-purpose status chip for Review button
            val ctx = binding.root.context
            binding.tvStatusChip.setOnClickListener(null)
            
            val isReviewed = reviewsLoaded && reviewsMap.containsKey(deal.dealId)
            if (isReviewed) {
                binding.tvStatusChip.visibility = View.VISIBLE
                binding.tvStatusChip.text = "★ " + ctx.getString(R.string.review_aura_reviewed_badge)
                binding.tvStatusChip.setBackgroundResource(R.drawable.chip_status_accepted)
                binding.tvStatusChip.setTextColor(ctx.getColor(R.color.colorPrimary))
            } else {
                binding.tvStatusChip.visibility = View.GONE
            }

            binding.root.alpha = 1f
            binding.root.isClickable = true
            binding.root.isFocusable = true
            binding.root.setOnClickListener { onItemClick?.invoke(item) }
            binding.ivChevron.setOnClickListener { onChevronClick?.invoke(item) }
        }

        private fun bindPast(item: DealOfferItem, deal: Deal) {
            val ctx = binding.root.context
            binding.tvStatusChip.visibility = View.VISIBLE
            binding.tvBtnAccept.isVisible = false
            binding.ivBtnReject.isVisible = false
            binding.ivChevron.isVisible = true

            binding.ivChevron.setOnClickListener { onChevronClick?.invoke(item) }

            when (deal.status) {
                Constants.STATUS_REJECTED -> {
                    binding.tvStatusChip.text = ctx.getString(R.string.chip_status_declined)
                    binding.tvStatusChip.setBackgroundResource(R.drawable.chip_status_rejected)
                    binding.tvStatusChip.setTextColor(ctx.getColor(R.color.colorError))
                    setTappableRootForInfo(item)
                }
                Constants.STATUS_EXPIRED -> {
                    binding.tvStatusChip.text = ctx.getString(R.string.chip_status_expired)
                    binding.tvStatusChip.setBackgroundResource(R.drawable.chip_status_expired)
                    binding.tvStatusChip.setTextColor(ctx.getColor(R.color.colorWarning))
                    setTappableRootForInfo(item)
                }
                Constants.STATUS_CANCELLED -> {
                    binding.tvStatusChip.text = ctx.getString(R.string.chip_status_cancelled)
                    binding.tvStatusChip.setBackgroundResource(R.drawable.chip_status_cancelled)
                    binding.tvStatusChip.setTextColor(ctx.getColor(R.color.colorTextSecondary))
                    binding.root.alpha = 1f
                    binding.root.isClickable = true
                    binding.root.isFocusable = true
                    binding.root.setOnClickListener { onItemClick?.invoke(item) }
                }
            }
        }

        private fun setTappableRootForInfo(item: DealOfferItem) {
            binding.root.alpha = 1f
            binding.root.isClickable = true
            binding.root.isFocusable = true
            binding.root.setOnClickListener { onChevronClick?.invoke(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(ItemDealOfferBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(getItem(position))

    private companion object DiffCallback : DiffUtil.ItemCallback<DealOfferItem>() {
        override fun areItemsTheSame(oldItem: DealOfferItem, newItem: DealOfferItem) =
            oldItem.deal.dealId == newItem.deal.dealId
        override fun areContentsTheSame(oldItem: DealOfferItem, newItem: DealOfferItem) =
            oldItem == newItem
    }
}

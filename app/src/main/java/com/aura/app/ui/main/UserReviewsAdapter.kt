package com.aura.app.ui.main

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.aura.app.R
import com.aura.app.data.model.Review
import com.aura.app.databinding.ItemUserReviewBinding
import com.bumptech.glide.Glide

/**
 * Renders one review row on the per-user reviews screen.
 * Reviewer = the OTHER party (avatar, name, deal title); rating + comment are the payload.
 */
class UserReviewsAdapter :
    ListAdapter<Review, UserReviewsAdapter.ViewHolder>(DiffCallback) {

    inner class ViewHolder(private val binding: ItemUserReviewBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private val starViews by lazy {
            listOf(
                binding.ivReviewStar1, binding.ivReviewStar2, binding.ivReviewStar3,
                binding.ivReviewStar4, binding.ivReviewStar5,
            )
        }

        fun bind(review: Review) {
            val ctx = binding.root.context

            binding.tvReviewerName.text = review.reviewerDisplayName.ifBlank { "Anonymous" }
            binding.tvDealTitle.text = review.dealTitle
            binding.tvDealTitle.isVisible = review.dealTitle.isNotBlank()
            binding.tvRating.text = String.format("%.1f", review.rating)

            // Comment (optional)
            if (review.comment.isNotBlank()) {
                binding.tvReviewComment.text = review.comment
                binding.tvReviewComment.isVisible = true
            } else {
                binding.tvReviewComment.isVisible = false
            }

            // Stars
            val filledCount = review.rating.toInt().coerceIn(0, 5)
            starViews.forEachIndexed { idx, iv ->
                if (idx < filledCount) {
                    iv.setImageResource(R.drawable.ic_aura_star_filled)
                    iv.setColorFilter(ContextCompat.getColor(ctx, R.color.colorPrimary))
                } else {
                    iv.setImageResource(R.drawable.ic_aura_star_outline)
                    iv.setColorFilter(ContextCompat.getColor(ctx, R.color.colorOnSurfaceVariant))
                }
            }

            // Reviewer photo
            Glide.with(binding.ivReviewerPhoto)
                .load(review.reviewerPhotoUrl)
                .placeholder(R.drawable.bg_avatar_placeholder)
                .fallback(R.drawable.bg_avatar_placeholder)
                .into(binding.ivReviewerPhoto)

            // Time
            val timeMs = review.createdAt?.toDate()?.time
            binding.tvReviewTime.text = if (timeMs != null) {
                DateUtils.getRelativeTimeSpanString(
                    timeMs,
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS,
                    DateUtils.FORMAT_ABBREV_RELATIVE,
                ).toString()
            } else ""
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemUserReviewBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(getItem(position))

    private companion object DiffCallback : DiffUtil.ItemCallback<Review>() {
        override fun areItemsTheSame(o: Review, n: Review) = o.reviewId == n.reviewId
        override fun areContentsTheSame(o: Review, n: Review) = o == n
    }
}

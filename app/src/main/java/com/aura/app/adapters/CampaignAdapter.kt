package com.aura.app.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.aura.app.data.model.Campaign
import com.aura.app.databinding.ItemCampaignBinding
import com.bumptech.glide.Glide
import com.aura.app.R

class CampaignAdapter(
    var onCampaignClick: (Campaign) -> Unit,
    var onEditClick: (Campaign) -> Unit = {}
) : ListAdapter<Campaign, CampaignAdapter.CampaignViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CampaignViewHolder {
        val binding = ItemCampaignBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return CampaignViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CampaignViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CampaignViewHolder(private val binding: ItemCampaignBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(campaign: Campaign) {
            binding.tvCampaignTitle.text = campaign.title
            
            val budgetText = if (campaign.budgetMin > 0 || campaign.budgetMax > 0) {
                "$${campaign.budgetMin} - $${campaign.budgetMax}"
            } else {
                "Budget TBD"
            }
            binding.tvCampaignBudget.text = budgetText

            if (campaign.imageUrl.isNotBlank()) {
                Glide.with(binding.ivCampaignThumb.context)
                    .load(campaign.imageUrl)
                    .centerCrop()
                    .into(binding.ivCampaignThumb)
            } else {
                binding.ivCampaignThumb.setImageResource(R.drawable.bg_pill_surface_low)
            }

            binding.root.setOnClickListener { onCampaignClick(campaign) }
            binding.btnEditCampaign.setOnClickListener { onEditClick(campaign) }
        }
        
        fun setEditVisible(visible: Boolean) {
            binding.btnEditCampaign.visibility = if (visible) View.VISIBLE else View.GONE
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<Campaign>() {
        override fun areItemsTheSame(oldItem: Campaign, newItem: Campaign) =
            oldItem.campaignId == newItem.campaignId

        override fun areContentsTheSame(oldItem: Campaign, newItem: Campaign) =
            oldItem == newItem
    }
}

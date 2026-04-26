package com.aura.app.adapters

import android.graphics.Color
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.aura.app.R
import com.aura.app.data.model.Campaign
import com.aura.app.databinding.ItemCampaignSelectBinding

class CampaignSelectAdapter(
    private val onCampaignSelected: (String) -> Unit
) : ListAdapter<Campaign, CampaignSelectAdapter.ViewHolder>(DiffCallback) {

    private var selectedCampaignId: String? = null

    fun setSelectedCampaign(campaignId: String?) {
        val old = selectedCampaignId
        selectedCampaignId = campaignId
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCampaignSelectBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemCampaignSelectBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(campaign: Campaign) {
            val ctx = binding.root.context

            binding.tvCampaignTitle.text = campaign.title
            binding.tvCampaignDesc.text = campaign.description
            binding.tvDeliverables.text = campaign.deliverables.joinToString(", ")
            binding.tvBudget.text = "$${campaign.budgetMin} - $${campaign.budgetMax}"

            val isSelected = campaign.campaignId == selectedCampaignId

            binding.chipSelected.visibility = if (isSelected) View.VISIBLE else View.GONE

            val strokePx = if (isSelected) (2 * ctx.resources.displayMetrics.density).toInt() else 0
            binding.root.strokeWidth = strokePx

            binding.root.setCardBackgroundColor(
                ContextCompat.getColor(ctx,
                    if (isSelected) R.color.colorSurfaceContainerHigh else R.color.colorSurfaceContainerLow
                )
            )

            binding.root.cardElevation = if (isSelected) 8f else 0f
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val glowColor = if (isSelected) Color.parseColor("#26CAC1ED") else Color.TRANSPARENT
                binding.root.outlineAmbientShadowColor = glowColor
                binding.root.outlineSpotShadowColor = glowColor
            }

            val pillBg = if (isSelected) R.drawable.bg_campaign_pill_selected else R.drawable.bg_campaign_pill_unselected
            binding.pillDeliverables.setBackgroundResource(pillBg)
            binding.pillBudget.setBackgroundResource(pillBg)

            val pillTextColor = ContextCompat.getColor(ctx,
                if (isSelected) R.color.colorOnSurface else R.color.colorOnSurfaceVariant
            )
            binding.tvDeliverables.setTextColor(pillTextColor)
            binding.tvBudget.setTextColor(pillTextColor)

            binding.root.setOnClickListener {
                onCampaignSelected(campaign.campaignId)
            }
        }
    }

    object DiffCallback : DiffUtil.ItemCallback<Campaign>() {
        override fun areItemsTheSame(oldItem: Campaign, newItem: Campaign) =
            oldItem.campaignId == newItem.campaignId

        override fun areContentsTheSame(oldItem: Campaign, newItem: Campaign) =
            oldItem == newItem
    }
}

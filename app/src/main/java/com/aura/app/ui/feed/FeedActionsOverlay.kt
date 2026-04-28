package com.aura.app.ui.feed

import android.content.Intent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import com.aura.app.R
import com.aura.app.ui.feed.SelectCampaignBottomSheet

class FeedActionsOverlay(
    private val root: View,
    private val viewModel: FeedActionsViewModel,
    private val lifecycleOwner: LifecycleOwner,
    private val fragment: Fragment,
) {

    private val container: View = root.findViewById(R.id.feed_actions_container)
    private val btnShortlist: ImageButton = root.findViewById(R.id.btn_shortlist)
    private val btnDeal: ImageButton = root.findViewById(R.id.btn_deal)
    private val dealGlow: View = root.findViewById(R.id.deal_glow)
    private val btnMore: ImageButton = root.findViewById(R.id.btn_more)
    private val shortlistCreatorDrop: Float =
        fragment.resources.getDimension(R.dimen.feed_actions_shortlist_creator_drop)
    private var isDealSent = false
    private val baseBottomMargin: Int =
        fragment.resources.getDimensionPixelSize(R.dimen.feed_actions_margin_bottom)
    private val extraLift: Int =
        fragment.resources.getDimensionPixelSize(R.dimen.feed_actions_extra_lift)

    fun setup() {
        applyActionRailInsets()

        viewModel.isBrand.observe(lifecycleOwner) { isBrand ->
            container.visibility = View.VISIBLE

            btnShortlist.translationY = if (isBrand) 0f else shortlistCreatorDrop
            
            if (isBrand) {
                btnDeal.visibility = View.VISIBLE
                dealGlow.visibility = View.VISIBLE
            } else {
                btnDeal.visibility = View.GONE
                dealGlow.visibility = View.GONE
            }

            // Reset deal state whenever we switch to a new creator
            isDealSent = false
            btnDeal.setImageResource(R.drawable.ic_nav_deals)
            dealGlow.animate().cancel()
            dealGlow.alpha = 0f
        }

        viewModel.isShortlisted.observe(lifecycleOwner) { shortlisted ->
            btnShortlist.setImageResource(
                if (shortlisted) R.drawable.ic_shortlist_filled
                else R.drawable.ic_shortlist_outline
            )
        }

        btnShortlist.setOnClickListener {
            val uid = viewModel.getCurrentUserId()
            val creatorId = viewModel.getCurrentCreatorId()
            if (uid != null && creatorId != null) {
                if (uid == creatorId) {
                    Toast.makeText(fragment.requireContext(), "Cannot save yourself", Toast.LENGTH_SHORT).show()
                } else {
                    val currentlyShortlisted = viewModel.isShortlisted.value == true
                    val msg = if (currentlyShortlisted) "Creator unsaved" else "Creator saved"
                    Toast.makeText(fragment.requireContext(), msg, Toast.LENGTH_SHORT).show()
                    viewModel.toggleShortlist()
                }
            }
        }

        btnDeal.setOnClickListener {
            val brandId = viewModel.getCurrentUserId()
            val creatorId = viewModel.getCurrentCreatorId()
            android.util.Log.d("FeedActionsOverlay", "Deal clicked: brandId=$brandId, creatorId=$creatorId")
            
            if (brandId == null || creatorId == null) return@setOnClickListener

            SelectCampaignBottomSheet.newInstance(brandId, creatorId)
                .show(fragment.childFragmentManager, SelectCampaignBottomSheet.TAG)
        }

        btnMore.setOnClickListener {
            val creatorId = viewModel.getCurrentCreatorId() ?: return@setOnClickListener
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, "Check out this creator on Aura: https://aura.example.com/profile/$creatorId")
                type = "text/plain"
            }
            fragment.startActivity(Intent.createChooser(shareIntent, "Share Creator"))
        }
    }

    private fun applyActionRailInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(container) { view, insets ->
            val navInset = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            val lp = view.layoutParams as? ViewGroup.MarginLayoutParams
            if (lp != null) {
                lp.bottomMargin = baseBottomMargin + extraLift + navInset
                view.layoutParams = lp
            }
            insets
        }
        ViewCompat.requestApplyInsets(container)
    }
}

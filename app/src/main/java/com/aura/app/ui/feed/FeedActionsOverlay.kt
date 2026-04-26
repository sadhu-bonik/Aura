package com.aura.app.ui.feed

import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import com.aura.app.R

class FeedActionsOverlay(
    private val root: View,
    private val viewModel: FeedActionsViewModel,
    private val lifecycleOwner: LifecycleOwner,
    private val fragment: Fragment,
) {

    private val container: View = root.findViewById(R.id.feed_actions_container)
    private val btnShortlist: ImageButton = root.findViewById(R.id.btn_shortlist)
    private val btnDeal: ImageButton = root.findViewById(R.id.btn_deal)
    private val btnMore: ImageButton = root.findViewById(R.id.btn_more)
    private val baseBottomMargin: Int =
        (container.layoutParams as? ViewGroup.MarginLayoutParams)?.bottomMargin ?: 0

    fun setup() {
        applyActionRailInsets()

        viewModel.isBrand.observe(lifecycleOwner) { isBrand ->
            container.visibility = View.VISIBLE
            btnDeal.isEnabled = isBrand
            btnDeal.alpha = if (isBrand) 1f else 0.45f
        }

        viewModel.isShortlisted.observe(lifecycleOwner) { shortlisted ->
            btnShortlist.setImageResource(
                if (shortlisted) R.drawable.ic_shortlist_filled
                else R.drawable.ic_shortlist_outline
            )
        }

        btnShortlist.setOnClickListener { viewModel.toggleShortlist() }

        btnDeal.setOnClickListener {
            Toast.makeText(fragment.requireContext(), R.string.action_deal, Toast.LENGTH_SHORT).show()
        }

        btnMore.setOnClickListener {
            Toast.makeText(fragment.requireContext(), R.string.action_share, Toast.LENGTH_SHORT).show()
        }
    }

    private fun applyActionRailInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(container) { view, insets ->
            val navInset = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            val lp = view.layoutParams as? ViewGroup.MarginLayoutParams
            if (lp != null) {
                lp.bottomMargin = baseBottomMargin + navInset
                view.layoutParams = lp
            }
            insets
        }
        ViewCompat.requestApplyInsets(container)
    }
}

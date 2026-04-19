package com.aura.app.ui.feed

import android.view.View
import android.widget.ImageButton
import android.widget.Toast
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

    fun setup() {
        viewModel.isBrand.observe(lifecycleOwner) { isBrand ->
            container.visibility = if (isBrand) View.VISIBLE else View.GONE
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
            Toast.makeText(fragment.requireContext(), R.string.action_more, Toast.LENGTH_SHORT).show()
        }
    }
}

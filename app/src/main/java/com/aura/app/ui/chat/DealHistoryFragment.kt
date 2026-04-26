package com.aura.app.ui.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.aura.app.R
import com.aura.app.databinding.FragmentDealHistoryBinding

class DealHistoryFragment : Fragment() {

    private var _binding: FragmentDealHistoryBinding? = null
    private val binding get() = _binding!!

    val viewModel: DealHistoryViewModel by viewModels()

    private var pageChangeCallback: ViewPager2.OnPageChangeCallback? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDealHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener { findNavController().navigateUp() }

        binding.viewPagerHistory.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount() = 2
            override fun createFragment(position: Int): Fragment = when (position) {
                0 -> CompletedDealsTabFragment()
                else -> PastDealsTabFragment()
            }
        }

        binding.btnPillCompleted.setOnClickListener { binding.viewPagerHistory.currentItem = 0 }
        binding.btnPillPast.setOnClickListener { binding.viewPagerHistory.currentItem = 1 }

        pageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) = renderPillSelection(position)
        }
        binding.viewPagerHistory.registerOnPageChangeCallback(pageChangeCallback!!)
        renderPillSelection(0)
    }

    private fun renderPillSelection(position: Int) {
        val ctx = requireContext()
        val onContainer = ContextCompat.getColor(ctx, R.color.colorOnPrimaryContainer)
        val onVariant = ContextCompat.getColor(ctx, R.color.colorOnSurfaceVariant)
        val transparent = android.R.color.transparent
        val bold = ResourcesCompat.getFont(ctx, R.font.manrope_bold)

        if (position == 0) {
            binding.btnPillCompleted.setBackgroundResource(R.drawable.bg_history_pill_selected)
            binding.btnPillCompleted.setTextColor(onContainer)
            binding.btnPillCompleted.typeface = bold

            binding.btnPillPast.setBackgroundResource(transparent)
            binding.btnPillPast.setTextColor(onVariant)
            binding.btnPillPast.typeface = bold
        } else {
            binding.btnPillPast.setBackgroundResource(R.drawable.bg_history_pill_selected)
            binding.btnPillPast.setTextColor(onContainer)
            binding.btnPillPast.typeface = bold

            binding.btnPillCompleted.setBackgroundResource(transparent)
            binding.btnPillCompleted.setTextColor(onVariant)
            binding.btnPillCompleted.typeface = bold
        }
    }

    override fun onDestroyView() {
        pageChangeCallback?.let { binding.viewPagerHistory.unregisterOnPageChangeCallback(it) }
        pageChangeCallback = null
        super.onDestroyView()
        _binding = null
    }
}

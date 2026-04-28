package com.aura.app.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.aura.app.R
import com.aura.app.databinding.FragmentUserReviewsBinding
import kotlinx.coroutines.launch

class UserReviewsFragment : Fragment() {

    private var _binding: FragmentUserReviewsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: UserReviewsViewModel by viewModels()
    private val reviewsAdapter = UserReviewsAdapter()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUserReviewsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val revieweeId = arguments?.getString("revieweeId") ?: return
        val displayName = arguments?.getString("displayName") ?: "User"

        // Static setup (name / back button)
        binding.btnBack.setOnClickListener { findNavController().navigateUp() }
        binding.tvScreenTitle.text = "$displayName's Reviews"
        binding.tvRevieweeName.text = displayName

        // Show 0 initially — will be updated by the live stats observer
        binding.tvAvgRating.text = "0.0"
        binding.tvTotalReviews.text = "0 reviews"
        updateStars(0.0)

        setupRecyclerView()

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Observe live stats → update header card
                launch {
                    viewModel.stats.collect { stats ->
                        binding.tvAvgRating.text = String.format("%.1f", stats.averageRating)
                        val count = stats.totalReviews
                        binding.tvTotalReviews.text = if (count == 1L) "1 review" else "$count reviews"
                        updateStars(stats.averageRating)
                    }
                }

                // Observe live review list → update RecyclerView + empty state
                launch {
                    viewModel.reviews.collect { reviews ->
                        reviewsAdapter.submitList(reviews)
                        val isEmpty = reviews.isEmpty() && !viewModel.isLoading.value
                        binding.rvReviews.visibility = if (isEmpty) View.GONE else View.VISIBLE
                        binding.layoutEmptyReviews.visibility = if (isEmpty) View.VISIBLE else View.GONE
                    }
                }
            }
        }

        viewModel.loadReviewsForUser(revieweeId)
    }

    private fun setupRecyclerView() {
        binding.rvReviews.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = reviewsAdapter
        }
    }

    private fun updateStars(rating: Double) {
        // Support half-star display: fill n whole stars, add half if fraction >= 0.5
        val stars = listOf(
            binding.ivAvgStar1, binding.ivAvgStar2, binding.ivAvgStar3,
            binding.ivAvgStar4, binding.ivAvgStar5
        )
        val whole = rating.toInt().coerceIn(0, 5)
        val half = (rating - whole) >= 0.5

        stars.forEachIndexed { index, iv ->
            val res = when {
                index < whole -> R.drawable.ic_aura_star_filled
                index == whole && half -> R.drawable.ic_aura_star_filled // use filled; swap to half if you add that drawable
                else -> R.drawable.ic_aura_star_outline
            }
            val tint = if (index < whole || (index == whole && half))
                R.color.colorPrimary else R.color.colorOnSurfaceVariant
            iv.setImageResource(res)
            iv.setColorFilter(requireContext().getColor(tint))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

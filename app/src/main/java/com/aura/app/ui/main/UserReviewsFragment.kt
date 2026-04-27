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
        val averageRating = arguments?.getDouble("averageRating") ?: 0.0
        val totalReviews = arguments?.getLong("totalReviews") ?: 0L

        setupUI(displayName, averageRating, totalReviews)
        setupRecyclerView()

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.reviews.collect { reviews ->
                    reviewsAdapter.submitList(reviews)
                    
                    if (reviews.isEmpty() && !viewModel.isLoading.value) {
                        binding.rvReviews.visibility = View.GONE
                        binding.layoutEmptyReviews.visibility = View.VISIBLE
                    } else {
                        binding.rvReviews.visibility = View.VISIBLE
                        binding.layoutEmptyReviews.visibility = View.GONE
                        
                        // Update total reviews count if it has changed
                        if (reviews.size.toLong() != totalReviews) {
                            binding.tvTotalReviews.text = "${reviews.size} reviews"
                        }
                    }
                }
            }
        }

        viewModel.loadReviewsForUser(revieweeId)
    }

    private fun setupUI(displayName: String, averageRating: Double, totalReviews: Long) {
        binding.btnBack.setOnClickListener { findNavController().navigateUp() }
        
        binding.tvScreenTitle.text = "$displayName's Reviews"
        binding.tvRevieweeName.text = displayName
        binding.tvTotalReviews.text = "$totalReviews reviews"
        
        if (totalReviews > 0) {
            binding.tvAvgRating.text = String.format("%.1f", averageRating)
            updateStars(averageRating)
        } else {
            binding.tvAvgRating.text = "0.0"
            updateStars(0.0)
        }
    }

    private fun setupRecyclerView() {
        binding.rvReviews.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = reviewsAdapter
        }
    }

    private fun updateStars(rating: Double) {
        val filledCount = rating.toInt().coerceIn(0, 5)
        val stars = listOf(
            binding.ivAvgStar1, binding.ivAvgStar2, binding.ivAvgStar3,
            binding.ivAvgStar4, binding.ivAvgStar5
        )
        stars.forEachIndexed { index, imageView ->
            if (index < filledCount) {
                imageView.setImageResource(com.aura.app.R.drawable.ic_aura_star_filled)
                imageView.setColorFilter(requireContext().getColor(com.aura.app.R.color.colorPrimary))
            } else {
                imageView.setImageResource(com.aura.app.R.drawable.ic_aura_star_outline)
                imageView.setColorFilter(requireContext().getColor(com.aura.app.R.color.colorOnSurfaceVariant))
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

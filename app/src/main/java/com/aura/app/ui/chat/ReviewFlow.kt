package com.aura.app.ui.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.aura.app.R
import com.aura.app.databinding.FragmentReviewFlowBinding
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class ReviewFlow : BottomSheetDialogFragment() {

    private var _binding: FragmentReviewFlowBinding? = null
    private val binding get() = _binding!!

    private val reviewViewModel: ReviewViewModel by activityViewModels()

    private var selectedRating = 0.0
    private var reviewId: String? = null

    private val dealId: String by lazy { requireArguments().getString("dealId")!! }
    private val revieweeId: String by lazy { requireArguments().getString("revieweeId")!! }
    private val revieweeName: String by lazy { requireArguments().getString("revieweeName")!! }
    private val revieweePhotoUrl: String by lazy { requireArguments().getString("revieweePhotoUrl")!! }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentReviewFlowBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Step 1 setup
        binding.tvRevieweeName.text = revieweeName
        Glide.with(this)
            .load(revieweePhotoUrl)
            .circleCrop()
            .placeholder(R.drawable.bg_avatar_placeholder)
            .into(binding.ivRevieweePhoto)

        binding.btnClose.setOnClickListener { dismiss() }

        val stars = listOf(binding.ivStar1, binding.ivStar2, binding.ivStar3, binding.ivStar4, binding.ivStar5)
        stars.forEachIndexed { index, imageView ->
            imageView.setOnClickListener {
                if (reviewId != null) return@setOnClickListener // Already submitted rating
                selectedRating = (index + 1).toDouble()
                updateStars(stars)
                submitRating(stars)
            }
        }

        // Step 2 setup
        binding.tvStep2Subtitle.text = getString(R.string.review_step2_subtitle, revieweeName)

        binding.btnDone.setOnClickListener {
            submitComment()
        }
    }

    override fun onStart() {
        super.onStart()
        val behavior = BottomSheetBehavior.from(requireView().parent as View)
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
    }

    private fun updateStars(stars: List<ImageView>) {
        stars.forEachIndexed { index, imageView ->
            if (index < selectedRating) {
                imageView.setImageResource(R.drawable.ic_aura_star_filled)
                imageView.setColorFilter(requireContext().getColor(R.color.colorPrimary))
            } else {
                imageView.setImageResource(R.drawable.ic_aura_star_outline)
                imageView.setColorFilter(requireContext().getColor(R.color.colorTextSecondary))
            }
        }
    }

    private fun submitRating(stars: List<ImageView>) {
        stars.forEach { it.isEnabled = false }
        binding.pbLoadingRating.visibility = View.VISIBLE

        viewLifecycleOwner.lifecycleScope.launch {
            reviewViewModel.submitRating(dealId, revieweeId, selectedRating).collect { result ->
                if (result != null) {
                    binding.pbLoadingRating.visibility = View.GONE
                    if (result.isSuccess) {
                        reviewId = result.getOrThrow()
                        binding.vfSteps.showNext() // Transition to Step 2
                    } else {
                        stars.forEach { it.isEnabled = true }
                        val msg = result.exceptionOrNull()?.message ?: "Error saving rating"
                        Snackbar.make(binding.root, msg, Snackbar.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun submitComment() {
        val currentReviewId = reviewId ?: return
        val comment = binding.etComment.text.toString().trim()
        
        binding.btnDone.text = ""
        binding.btnDone.isEnabled = false
        binding.pbLoadingComment.visibility = View.VISIBLE

        viewLifecycleOwner.lifecycleScope.launch {
            reviewViewModel.submitComment(currentReviewId, dealId, comment).collect { result ->
                if (result != null) {
                    if (result.isSuccess) {
                        Snackbar.make(requireActivity().findViewById(android.R.id.content), R.string.review_success_snackbar, Snackbar.LENGTH_SHORT).show()
                        dismiss()
                    } else {
                        binding.pbLoadingComment.visibility = View.GONE
                        binding.btnDone.isEnabled = true
                        binding.btnDone.text = getString(R.string.review_done_button)
                        val msg = result.exceptionOrNull()?.message ?: "Error saving comment"
                        Snackbar.make(binding.root, msg, Snackbar.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(dealId: String, revieweeId: String, revieweeName: String, revieweePhotoUrl: String): ReviewFlow {
            return ReviewFlow().apply {
                arguments = bundleOf(
                    "dealId" to dealId,
                    "revieweeId" to revieweeId,
                    "revieweeName" to revieweeName,
                    "revieweePhotoUrl" to revieweePhotoUrl
                )
            }
        }
    }
}

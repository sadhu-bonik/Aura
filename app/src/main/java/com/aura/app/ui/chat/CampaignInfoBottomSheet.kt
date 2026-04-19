package com.aura.app.ui.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.aura.app.R
import com.aura.app.databinding.FragmentCampaignInfoBottomSheetBinding
import com.aura.app.utils.Constants
import com.aura.app.utils.StubSession
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar

class CampaignInfoBottomSheet : BottomSheetDialogFragment() {

    private var _binding: FragmentCampaignInfoBottomSheetBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CampaignInfoViewModel by viewModels()
    private val mediaAdapter = SharedMediaAdapter()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCampaignInfoBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvSharedMedia.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rvSharedMedia.adapter = mediaAdapter

        binding.btnClose.setOnClickListener { dismiss() }
        binding.btnViewProfile.isEnabled = false

        val dealId = arguments?.getString(ARG_DEAL_ID) ?: return
        viewModel.load(dealId, StubSession.userId())

        setupEditControls()
        setupActionButtons()
        observeViewModel()
    }

    private fun setupEditControls() {
        binding.ivEditTitle.setOnClickListener { enterEditMode() }
        binding.ivEditDescription.setOnClickListener { enterEditMode() }

        binding.btnCancelEdit.setOnClickListener { exitEditMode() }

        binding.btnSaveEdit.setOnClickListener {
            val title = binding.etTitle.text?.toString()?.trim() ?: ""
            val description = binding.etDescription.text?.toString()?.trim() ?: ""
            viewModel.updateDealDetails(title, description)
        }
    }

    private fun setupActionButtons() {
        binding.btnCancelDeal.setOnClickListener { showCancelDialog() }
        binding.btnCompleteDeal.setOnClickListener { viewModel.requestCompletion() }
    }

    private fun showCancelDialog() {
        val reasonInput = EditText(requireContext()).apply {
            hint = getString(R.string.hint_cancel_reason)
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
            maxLines = 3
            filters = arrayOf(android.text.InputFilter.LengthFilter(140))
        }
        val container = android.widget.FrameLayout(requireContext()).apply {
            val padding = resources.getDimensionPixelSize(R.dimen.space_md)
            setPadding(padding, 0, padding, 0)
            addView(reasonInput)
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_cancel_reason_title)
            .setView(container)
            .setNegativeButton(R.string.btn_cancel, null)
            .setPositiveButton(R.string.btn_cancel_deal) { _, _ ->
                val reason = reasonInput.text?.toString()?.trim() ?: ""
                viewModel.cancelDeal(reason)
            }
            .show()
    }

    private fun enterEditMode() {
        val deal = viewModel.deal.value ?: return
        binding.etTitle.setText(deal.title)
        binding.etDescription.setText(deal.description)

        binding.tvCampaignTitle.isVisible = false
        binding.ivEditTitle.isVisible = false
        binding.tvCampaignDescription.isVisible = false
        binding.ivEditDescription.isVisible = false

        binding.tilTitle.isVisible = true
        binding.tilDescription.isVisible = true
        binding.llEditActions.isVisible = true
    }

    private fun exitEditMode() {
        binding.tilTitle.isVisible = false
        binding.tilDescription.isVisible = false
        binding.llEditActions.isVisible = false

        binding.tvCampaignTitle.isVisible = true
        binding.tvCampaignDescription.isVisible = true
        val isEditable = viewModel.deal.value?.status == Constants.STATUS_ACCEPTED
        binding.ivEditTitle.isVisible = isEditable
        binding.ivEditDescription.isVisible = isEditable
    }

    private fun observeViewModel() {
        viewModel.deal.observe(viewLifecycleOwner) { deal ->
            binding.tvCampaignTitle.text = deal.title
            binding.tvCampaignDescription.text = deal.description

            val isEditable = deal.status == Constants.STATUS_ACCEPTED
            binding.ivEditTitle.isVisible = isEditable
            binding.ivEditDescription.isVisible = isEditable

            val myId = StubSession.userId()
            val isAccepted = deal.status == Constants.STATUS_ACCEPTED

            when {
                !isAccepted -> {
                    // Not accepted — no actions available
                    binding.llDealActions.isVisible = false
                    binding.tvWaitingHelper.isVisible = false
                }
                deal.completionRequestedBy == myId -> {
                    // I'm waiting — show waiting helper, disable actions
                    binding.llDealActions.isVisible = true
                    binding.btnCancelDeal.isEnabled = false
                    binding.btnCompleteDeal.isEnabled = false
                    binding.btnCompleteDeal.text = getString(R.string.btn_waiting_response)
                    binding.tvWaitingHelper.isVisible = true
                }
                deal.completionRequestedBy.isNotEmpty() && deal.completionRequestedBy != myId -> {
                    // Other party is waiting — hide action buttons (respond via chat bar)
                    binding.llDealActions.isVisible = false
                    binding.tvWaitingHelper.isVisible = false
                }
                else -> {
                    // Normal accepted state — both buttons enabled
                    binding.llDealActions.isVisible = true
                    binding.btnCancelDeal.isEnabled = true
                    binding.btnCompleteDeal.isEnabled = true
                    binding.btnCompleteDeal.text = getString(R.string.btn_complete_deal)
                    binding.tvWaitingHelper.isVisible = false
                }
            }

            val isClosedState = deal.status in setOf(Constants.STATUS_CANCELLED, Constants.STATUS_REJECTED, Constants.STATUS_EXPIRED)
            if (isClosedState) {
                binding.tvSharedMediaLabel.isVisible = false
                binding.rvSharedMedia.isVisible = false
                binding.tvEmptyMedia.isVisible = false

                binding.tvReasonLabel.isVisible = true
                binding.tvReasonText.isVisible = true

                binding.tvReasonText.text = when (deal.status) {
                    Constants.STATUS_EXPIRED -> "Oops! The deal died after 7 days."
                    Constants.STATUS_REJECTED -> {
                        val byMe = deal.creatorId == myId
                        "Declined by ${if (byMe) "you" else "other party"}."
                    }
                    Constants.STATUS_CANCELLED -> {
                        val byMe = deal.cancelledBy == myId
                        val baseText = "Cancelled by ${if (byMe) "you" else "other party"}"
                        if (deal.cancelReason.isNotBlank()) {
                            "$baseText because \"${deal.cancelReason}\""
                        } else {
                            "$baseText."
                        }
                    }
                    else -> ""
                }
            } else {
                binding.tvSharedMediaLabel.isVisible = true
                val media = viewModel.sharedMedia.value
                if (media != null) {
                    binding.rvSharedMedia.isVisible = media.isNotEmpty()
                    binding.tvEmptyMedia.isVisible = media.isEmpty()
                }
                binding.tvReasonLabel.isVisible = false
                binding.tvReasonText.isVisible = false
            }
        }

        viewModel.otherParty.observe(viewLifecycleOwner) { user ->
            binding.tvProfileName.text = user?.displayName ?: ""
            Glide.with(binding.ivProfilePhoto)
                .load(user?.profileImageUrl)
                .circleCrop()
                .into(binding.ivProfilePhoto)
        }

        viewModel.sharedMedia.observe(viewLifecycleOwner) { messages ->
            mediaAdapter.submitList(messages)
            val deal = viewModel.deal.value
            val isClosedState = deal?.status in setOf(Constants.STATUS_CANCELLED, Constants.STATUS_REJECTED, Constants.STATUS_EXPIRED)
            if (isClosedState != true) {
                binding.rvSharedMedia.isVisible = messages.isNotEmpty()
                binding.tvEmptyMedia.isVisible = messages.isEmpty()
            }
        }

        viewModel.saveSuccess.observe(viewLifecycleOwner) { success ->
            if (success) exitEditMode()
        }

        viewModel.saveError.observe(viewLifecycleOwner) { error ->
            if (error != null) {
                Snackbar.make(binding.root, error, Snackbar.LENGTH_SHORT).show()
            }
        }

        viewModel.actionResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                is DealActionResult.Success -> {
                    viewModel.consumeActionResult()
                    dismiss()
                }
                is DealActionResult.Error -> {
                    viewModel.consumeActionResult()
                    Snackbar.make(binding.root, result.message, Snackbar.LENGTH_SHORT).show()
                }
                null -> Unit
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "CampaignInfoBottomSheet"
        private const val ARG_DEAL_ID = "dealId"

        fun newInstance(dealId: String) = CampaignInfoBottomSheet().apply {
            arguments = Bundle().apply { putString(ARG_DEAL_ID, dealId) }
        }
    }
}

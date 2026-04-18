package com.aura.app.ui.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.aura.app.databinding.FragmentCampaignInfoBottomSheetBinding
import com.aura.app.utils.Constants
import com.aura.app.utils.StubSession
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
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
            binding.rvSharedMedia.isVisible = messages.isNotEmpty()
            binding.tvEmptyMedia.isVisible = messages.isEmpty()
        }

        viewModel.saveSuccess.observe(viewLifecycleOwner) { success ->
            if (success) exitEditMode()
        }

        viewModel.saveError.observe(viewLifecycleOwner) { error ->
            if (error != null) {
                Snackbar.make(binding.root, error, Snackbar.LENGTH_SHORT).show()
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

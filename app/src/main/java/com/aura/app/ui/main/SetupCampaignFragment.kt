package com.aura.app.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.aura.app.databinding.FragmentSetupCampaignBinding
import com.google.android.material.chip.Chip
import kotlinx.coroutines.launch

class SetupCampaignFragment : Fragment() {

    private var _binding: FragmentSetupCampaignBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SetupCampaignViewModel by viewModels {
        SetupCampaignViewModel.Factory(requireContext())
    }

    private var campaignId: String? = null
    private var selectedImageUri: android.net.Uri? = null

    private val imagePickerLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        uri?.let {
            selectedImageUri = it
            binding.ivCampaignPreview.setImageURI(it)
            binding.layoutAddImagePlaceholder.visibility = android.view.View.GONE
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSetupCampaignBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        campaignId = arguments?.getString("campaignId")

        setupListeners()
        observeState()

        viewModel.loadCampaign(campaignId)
    }

    private fun setupListeners() {
        binding.toolbarSetupCampaign.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        binding.cardCampaignImage.setOnClickListener {
            imagePickerLauncher.launch("image/*")
        }

        binding.btnSaveCampaign.setOnClickListener {
            val title = binding.etTitle.text.toString()
            val description = binding.etDescription.text.toString()
            val budgetMin = binding.etBudgetMin.text.toString().toLongOrNull() ?: 0L
            val budgetMax = binding.etBudgetMax.text.toString().toLongOrNull() ?: 0L
            
            val deliverables = mutableListOf<String>()
            if (binding.chipPost.isChecked) deliverables.add("Post")
            if (binding.chipReel.isChecked) deliverables.add("Reel")
            if (binding.chipStory.isChecked) deliverables.add("Story")
            if (binding.chipVideo.isChecked) deliverables.add("YouTube Video")

            if (title.isBlank()) {
                binding.tilTitle.error = "Title is required"
                return@setOnClickListener
            }

            viewModel.saveCampaign(
                campaignId = campaignId,
                title = title,
                description = description,
                budgetMin = budgetMin,
                budgetMax = budgetMax,
                deliverables = deliverables,
                imageUri = selectedImageUri
            )
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    when (state) {
                        is SetupCampaignUiState.Loading -> {
                            binding.btnSaveCampaign.isEnabled = false
                        }
                        is SetupCampaignUiState.Success -> {
                            binding.btnSaveCampaign.isEnabled = true
                            state.campaign?.let { populateFields(it) }
                        }
                        is SetupCampaignUiState.Saved -> {
                            Toast.makeText(requireContext(), "Campaign saved!", Toast.LENGTH_SHORT).show()
                            findNavController().navigateUp()
                        }
                        is SetupCampaignUiState.Error -> {
                            binding.btnSaveCampaign.isEnabled = true
                            Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    private fun populateFields(campaign: com.aura.app.data.model.Campaign) {
        binding.etTitle.setText(campaign.title)
        binding.etDescription.setText(campaign.description)
        binding.etBudgetMin.setText(campaign.budgetMin.toString())
        binding.etBudgetMax.setText(campaign.budgetMax.toString())
        
        if (campaign.imageUrl.isNotBlank()) {
            com.bumptech.glide.Glide.with(this)
                .load(campaign.imageUrl)
                .centerCrop()
                .into(binding.ivCampaignPreview)
            binding.layoutAddImagePlaceholder.visibility = android.view.View.GONE
        }

        campaign.deliverables.forEach { deliverable ->
            when (deliverable) {
                "Post" -> binding.chipPost.isChecked = true
                "Reel" -> binding.chipReel.isChecked = true
                "Story" -> binding.chipStory.isChecked = true
                "YouTube Video" -> binding.chipVideo.isChecked = true
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

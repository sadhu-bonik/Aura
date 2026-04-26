package com.aura.app.ui.feed

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.aura.app.R
import com.aura.app.adapters.CampaignSelectAdapter
import com.aura.app.databinding.FragmentSelectCampaignBottomSheetBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch

class SelectCampaignBottomSheet : BottomSheetDialogFragment() {

    private var _binding: FragmentSelectCampaignBottomSheetBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SelectCampaignViewModel by viewModels {
        SelectCampaignViewModel.Factory(requireContext())
    }

    private lateinit var adapter: CampaignSelectAdapter

    private var brandId: String = ""
    private var creatorId: String = ""

    override fun getTheme(): Int = R.style.Theme_Aura_BottomSheetDialog_SelectCampaign

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSelectCampaignBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        brandId = arguments?.getString(ARG_BRAND_ID) ?: ""
        creatorId = arguments?.getString(ARG_CREATOR_ID) ?: ""

        setupRecyclerView()
        setupListeners()

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.state.collect { state ->
                        render(state)
                    }
                }
                launch {
                    viewModel.selectedCampaignId.collect { selectedId ->
                        adapter.setSelectedCampaign(selectedId)
                        updateSendButtonEnabled()
                    }
                }
                launch {
                    viewModel.sendState.collect { sendState ->
                        renderSendState(sendState)
                    }
                }
            }
        }

        viewModel.loadCampaigns(brandId)
    }

    private fun updateSendButtonEnabled() {
        val selected = viewModel.selectedCampaignId.value != null
        val notSending = viewModel.sendState.value !is SendDealUiState.Sending
        binding.btnSendDeal.isEnabled = selected && notSending
    }

    private fun renderSendState(state: SendDealUiState) {
        when (state) {
            SendDealUiState.Idle -> {
                binding.btnSendDeal.text = "Send Deal"
                updateSendButtonEnabled()
            }
            SendDealUiState.Sending -> {
                binding.btnSendDeal.text = "Sending..."
                binding.btnSendDeal.isEnabled = false
            }
            SendDealUiState.Sent -> {
                Toast.makeText(requireContext(), "Deal sent!", Toast.LENGTH_SHORT).show()
                dismiss()
            }
            is SendDealUiState.Error -> {
                Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                binding.btnSendDeal.text = "Send Deal"
                updateSendButtonEnabled()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val bottomSheet = dialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet) ?: return
        val behavior = BottomSheetBehavior.from(bottomSheet)

        val displayHeight = resources.displayMetrics.heightPixels
        val targetHeight = (displayHeight * 0.65).toInt()

        bottomSheet.layoutParams.height = targetHeight
        bottomSheet.requestLayout()

        behavior.peekHeight = targetHeight
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
        behavior.skipCollapsed = true
    }

    private fun setupRecyclerView() {
        adapter = CampaignSelectAdapter { campaignId ->
            viewModel.selectCampaign(campaignId)
        }
        binding.rvCampaigns.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@SelectCampaignBottomSheet.adapter
        }
    }

    private fun setupListeners() {
        binding.btnClose.setOnClickListener { dismiss() }
        binding.btnCreateCampaign.setOnClickListener {
            Toast.makeText(requireContext(), "Create campaign — coming soon", Toast.LENGTH_SHORT).show()
        }
        binding.btnSendDeal.setOnClickListener {
            viewModel.sendDeal(brandId, creatorId)
        }
    }

    private fun render(state: SelectCampaignUiState) {
        when (state) {
            is SelectCampaignUiState.Loading -> {}
            is SelectCampaignUiState.Success -> {
                adapter.submitList(state.campaigns)
            }
            is SelectCampaignUiState.Error -> {
                Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                dismiss()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "SelectCampaignBottomSheet"
        private const val ARG_BRAND_ID = "brand_id"
        private const val ARG_CREATOR_ID = "creator_id"

        fun newInstance(brandId: String, creatorId: String) = SelectCampaignBottomSheet().apply {
            arguments = Bundle().apply {
                putString(ARG_BRAND_ID, brandId)
                putString(ARG_CREATOR_ID, creatorId)
            }
        }
    }
}

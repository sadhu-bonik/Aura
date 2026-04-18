package com.aura.app.ui.auth.creator

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.aura.app.R
import com.aura.app.databinding.FragmentCreatorRegStep3Binding
import com.aura.app.ui.auth.RegistrationViewModel
import com.google.android.material.chip.Chip

class CreatorRegStep3Fragment : Fragment() {
    private var _binding: FragmentCreatorRegStep3Binding? = null
    private val binding get() = _binding!!

    private val registrationViewModel: RegistrationViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCreatorRegStep3Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupAudienceDropdown()

        // Pre-fill
        binding.etCity.setText(registrationViewModel.location)
        if (registrationViewModel.audienceRegion.isNotEmpty()) {
            binding.acvAudienceRegion.setText(registrationViewModel.audienceRegion, false)
        }

        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }
        binding.layoutBottomNav.btnNavBack.setOnClickListener { findNavController().navigateUp() }
        binding.layoutBottomNav.btnNavNext.setOnClickListener {
            // Save to ViewModel
            registrationViewModel.location = binding.etCity.text.toString().trim()
            registrationViewModel.audienceRegion = binding.acvAudienceRegion.text.toString()

            // Collect selected chips
            val selectedNiches = mutableListOf<String>()
            for (i in 0 until binding.chipGroupNiches.childCount) {
                val chip = binding.chipGroupNiches.getChildAt(i) as Chip
                if (chip.isChecked) {
                    selectedNiches.add(chip.text.toString())
                }
            }
            registrationViewModel.niches = selectedNiches

            findNavController().navigate(R.id.action_creator_step3_to_step4)
        }
    }

    private fun setupAudienceDropdown() {
        val regions = listOf(getString(R.string.region_north_america), getString(R.string.region_europe), getString(R.string.region_asia_pacific), getString(R.string.region_latin_america), getString(R.string.region_mea))
        binding.acvAudienceRegion.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, regions))
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}

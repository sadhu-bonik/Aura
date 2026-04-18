package com.aura.app.ui.auth.brand

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.aura.app.R
import com.aura.app.databinding.FragmentBrandRegStep4Binding
import com.aura.app.ui.auth.RegistrationViewModel
import com.google.android.material.chip.Chip

class BrandRegStep4Fragment : Fragment() {
    private var _binding: FragmentBrandRegStep4Binding? = null
    private val binding get() = _binding!!

    private val registrationViewModel: RegistrationViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentBrandRegStep4Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Pre-fill
        // Note: targetLocation is a single string in ViewModel, we combine city/state/country
        if (registrationViewModel.targetLocation.isNotEmpty()) {
            val parts = registrationViewModel.targetLocation.split(", ")
            if (parts.size >= 1) binding.etCity.setText(parts[0])
            if (parts.size >= 2) binding.etState.setText(parts[1])
            if (parts.size >= 3) binding.etCountry.setText(parts[2])
        }

        binding.ivClose.setOnClickListener { findNavController().navigateUp() }
        binding.layoutBottomNav.btnNavBack.setOnClickListener { findNavController().navigateUp() }
        binding.layoutBottomNav.btnNavNext.setOnClickListener {
            // Combine location fields into one string for simplicity in ViewModel
            val city = binding.etCity.text.toString().trim()
            val state = binding.etState.text.toString().trim()
            val country = binding.etCountry.text.toString().trim()
            registrationViewModel.targetLocation = listOfNotNull(
                city.ifBlank { null },
                state.ifBlank { null },
                country.ifBlank { null }
            ).joinToString(", ")

            // Collect selected chips
            val selectedTags = mutableListOf<String>()
            for (i in 0 until binding.chipGroupIndustries.childCount) {
                val chip = binding.chipGroupIndustries.getChildAt(i) as Chip
                if (chip.isChecked) {
                    selectedTags.add(chip.text.toString())
                }
            }
            registrationViewModel.industryTags = selectedTags

            findNavController().navigate(R.id.action_brand_step4_to_step5)
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}

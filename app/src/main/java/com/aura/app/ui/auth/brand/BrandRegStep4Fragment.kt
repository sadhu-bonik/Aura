package com.aura.app.ui.auth.brand

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.aura.app.R
import com.aura.app.databinding.FragmentBrandRegStep4Binding
import com.google.android.material.chip.Chip

/**
 * BrandRegStep4Fragment — Market Presence: Industry Tags + Target Location
 *
 * Sends: industryTags (List<String>), city, state, country
 * Calls: vm.submitStep4()
 * Navigates: on stepSaved(4) → brand_step5
 */
class BrandRegStep4Fragment : Fragment() {

    private var _binding: FragmentBrandRegStep4Binding? = null
    private val binding get() = _binding!!

    private val vm: BrandRegistrationViewModel by activityViewModels { BrandRegistrationViewModel.Factory() }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBrandRegStep4Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefillFields()
        setupObservers()
        setupClickListeners()
    }

    private fun prefillFields() {
        binding.etCity.setText(vm.city)
        binding.etState.setText(vm.state)
        binding.etCountry.setText(vm.country)

        // Re-check previously selected industry chips
        if (vm.industryTags.isNotEmpty()) {
            val chipGroup = binding.chipGroupIndustries
            for (i in 0 until chipGroup.childCount) {
                val chip = chipGroup.getChildAt(i) as? Chip ?: continue
                chip.isChecked = vm.industryTags.contains(chip.text.toString())
            }
        }
    }

    private fun setupObservers() {
        // Disable Next while Firebase call is in flight
        vm.isLoading.observe(viewLifecycleOwner) { loading ->
            binding.layoutBottomNav.btnNavNext.isEnabled = !loading
            binding.layoutBottomNav.btnNavNext.alpha = if (loading) 0.5f else 1.0f
        }

        vm.error.observe(viewLifecycleOwner) { msg ->
            if (msg != null) {
                Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
                vm.clearError()
            }
        }

        // Navigate to feed when Firebase Auth + Firestore complete
        vm.registrationComplete.observe(viewLifecycleOwner) { complete ->
            if (complete) {
                val navOptions = androidx.navigation.NavOptions.Builder()
                    .setPopUpTo(R.id.welcomeFragment, inclusive = true)
                    .build()
                findNavController().navigate(R.id.action_brand_step4_to_home, null, navOptions)
            }
        }
    }

    private fun setupClickListeners() {
        binding.ivClose.setOnClickListener { findNavController().navigateUp() }

        binding.layoutBottomNav.btnNavBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.layoutBottomNav.btnNavNext.setOnClickListener {
            if (!validateAndCollect()) return@setOnClickListener
            // All data collected — fire the single Firebase call
            vm.completeRegistration()
        }
    }

    private fun validateAndCollect(): Boolean {
        val city = binding.etCity.text.toString().trim()
        val state = binding.etState.text.toString().trim()
        val country = binding.etCountry.text.toString().trim()

        if (city.isBlank() || state.isBlank() || country.isBlank()) {
            Toast.makeText(requireContext(), "Please fill all location fields", Toast.LENGTH_SHORT).show()
            return false
        }

        val selectedTags = mutableListOf<String>()
        val chipGroup = binding.chipGroupIndustries
        for (i in 0 until chipGroup.childCount) {
            val chip = chipGroup.getChildAt(i) as? Chip ?: continue
            if (chip.isChecked) selectedTags.add(chip.text.toString())
        }

        if (selectedTags.isEmpty()) {
            Toast.makeText(requireContext(), "Please select at least one industry", Toast.LENGTH_SHORT).show()
            return false
        }

        vm.industryTags = selectedTags
        vm.city = city
        vm.state = state
        vm.country = country
        return true
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}

package com.aura.app.ui.auth.brand

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.aura.app.R
import com.aura.app.databinding.FragmentBrandRegStep4Binding
import com.aura.app.utils.BrandIndustryTags
import com.aura.app.utils.TargetAudienceTags
import com.google.android.material.chip.Chip

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
        setupNicheChips()
        setupTargetAudienceChips()
        prefillFields()
        setupObservers()
        setupClickListeners()
    }

    private fun setupNicheChips() {
        val chipGroup = binding.chipGroupIndustries
        chipGroup.removeAllViews()
        BrandIndustryTags.INDUSTRY_TAGS.forEach { tag ->
            val chip = Chip(requireContext()).apply {
                text = tag
                isCheckable = true
                isChecked = vm.industryTags.contains(tag)
            }
            chipGroup.addView(chip)
        }
    }

    private fun prefillFields() {
        binding.etCity.setText(vm.city)
        binding.etState.setText(vm.state)
        binding.etCountry.setText(vm.country)
        binding.etWebsite.setText(vm.website)
    }

    private fun setupTargetAudienceChips() {
        val chipGroup = binding.chipGroupTargetAudience
        chipGroup.removeAllViews()
        TargetAudienceTags.AUDIENCE_TAGS.forEach { audience ->
            val chip = Chip(requireContext()).apply {
                text = audience
                isCheckable = true
                isChecked = vm.targetAudience.contains(audience)
            }
            chipGroup.addView(chip)
        }
    }

    private fun setupObservers() {
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

        vm.registrationComplete.observe(viewLifecycleOwner) { complete ->
            if (complete) {
                val navOptions = NavOptions.Builder()
                    .setPopUpTo(R.id.welcomeFragment, inclusive = true)
                    .build()
                findNavController().navigate(R.id.action_brand_step4_to_home, null, navOptions)
            }
        }
    }

    private fun setupClickListeners() {
        binding.ivClose.setOnClickListener { findNavController().navigateUp() }

        binding.layoutBottomNav.btnNavCancel.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.layoutBottomNav.btnNavNext.setOnClickListener {
            if (!validateAndCollect()) return@setOnClickListener
            vm.completeRegistration(requireContext())
        }
    }

    private fun validateAndCollect(): Boolean {
        val city = binding.etCity.text.toString().trim()
        val state = binding.etState.text.toString().trim()
        val country = binding.etCountry.text.toString().trim()
        val website = binding.etWebsite.text.toString().trim()

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
            Toast.makeText(requireContext(), getString(R.string.error_industry_min_one), Toast.LENGTH_SHORT).show()
            return false
        }

        val selectedAudience = collectCheckedChips(binding.chipGroupTargetAudience)
        if (selectedAudience.isEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.error_target_audience_required), Toast.LENGTH_SHORT).show()
            return false
        }

        vm.industryTags = selectedTags
        vm.targetAudience = selectedAudience
        vm.website = website
        vm.city = city
        vm.state = state
        vm.country = country
        return true
    }

    private fun collectCheckedChips(group: com.google.android.material.chip.ChipGroup): List<String> {
        val selected = mutableListOf<String>()
        for (i in 0 until group.childCount) {
            val chip = group.getChildAt(i) as? Chip ?: continue
            if (chip.isChecked) selected.add(chip.text.toString())
        }
        return selected
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}

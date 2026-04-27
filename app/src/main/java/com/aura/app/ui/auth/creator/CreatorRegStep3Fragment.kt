package com.aura.app.ui.auth.creator

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.aura.app.R
import com.aura.app.databinding.FragmentCreatorRegStep3Binding
import com.aura.app.ui.auth.RegistrationViewModel
import com.aura.app.utils.CreatorNicheTags
import com.aura.app.utils.TargetAudienceTags
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
        setupNicheChips()
        setupTargetAudienceChips()

        // Pre-fill location (stored as "City, State, Country")
        if (registrationViewModel.location.isNotEmpty()) {
            val parts = registrationViewModel.location.split(", ")
            if (parts.size >= 1) binding.etCity.setText(parts[0])
            if (parts.size >= 2) binding.etState.setText(parts[1])
            if (parts.size >= 3) binding.etCountry.setText(parts[2])
        }
        binding.etPortfolioLink.setText(registrationViewModel.portfolioLink)

        binding.ivBack.setOnClickListener { findNavController().navigateUp() }
        binding.layoutBottomNav.btnNavCancel.setOnClickListener { findNavController().navigateUp() }
        binding.layoutBottomNav.btnNavNext.setOnClickListener {
            val cityStr = binding.etCity.text.toString().trim()
            val stateStr = binding.etState.text.toString().trim()
            val countryStr = binding.etCountry.text.toString().trim()

            var valid = true
            if (cityStr.isBlank()) { binding.tilCity.error = "Required"; valid = false } else { binding.tilCity.error = null }
            if (stateStr.isBlank()) { binding.tilState.error = "Required"; valid = false } else { binding.tilState.error = null }
            if (countryStr.isBlank()) { binding.tilCountry.error = "Required"; valid = false } else { binding.tilCountry.error = null }

            if (!valid) return@setOnClickListener

            val selectedNiches = collectCheckedChips(binding.chipGroupNiches)

            if (selectedNiches.isEmpty()) {
                Toast.makeText(requireContext(), getString(R.string.error_categories_min_one), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val selectedAudience = collectCheckedChips(binding.chipGroupTargetAudience)
            if (selectedAudience.isEmpty()) {
                Toast.makeText(requireContext(), getString(R.string.error_target_audience_required), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Deduplicate address parts
            val parts = mutableListOf<String>()
            parts.addAll(cityStr.split(",").map { it.trim() })
            parts.addAll(stateStr.split(",").map { it.trim() })
            parts.addAll(countryStr.split(",").map { it.trim() })
            registrationViewModel.location = parts.filter { it.isNotBlank() }.distinct().joinToString(", ")

            registrationViewModel.targetAudience = selectedAudience
            registrationViewModel.audienceRegion = selectedAudience.joinToString(", ")
            registrationViewModel.niches = selectedNiches
            registrationViewModel.portfolioLink = binding.etPortfolioLink.text.toString().trim()

            findNavController().navigate(R.id.action_creator_step3_to_step2)
        }
    }

    private fun setupNicheChips() {
        binding.chipGroupNiches.removeAllViews()
        CreatorNicheTags.NICHE_TAGS.forEach { niche ->
            val chip = Chip(requireContext()).apply {
                text = niche
                isCheckable = true
                isChecked = registrationViewModel.niches.contains(niche)
                setOnCheckedChangeListener { buttonView, isChecked ->
                    // Enforce max 5 selections
                    val selectedCount = (0 until binding.chipGroupNiches.childCount).count {
                        (binding.chipGroupNiches.getChildAt(it) as? Chip)?.isChecked == true
                    }
                    if (isChecked && selectedCount > 5) {
                        buttonView.isChecked = false
                        Toast.makeText(requireContext(), "You can select up to 5 niches", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            binding.chipGroupNiches.addView(chip)
        }
    }

    private fun setupTargetAudienceChips() {
        binding.chipGroupTargetAudience.removeAllViews()
        TargetAudienceTags.AUDIENCE_TAGS.forEach { audience ->
            val chip = Chip(requireContext()).apply {
                text = audience
                isCheckable = true
                isChecked = registrationViewModel.targetAudience.contains(audience) ||
                    registrationViewModel.audienceRegion.split(", ").contains(audience)
            }
            binding.chipGroupTargetAudience.addView(chip)
        }
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

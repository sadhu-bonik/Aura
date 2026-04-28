package com.aura.app.ui.auth.creator

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
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

        binding.etLocation.setText(registrationViewModel.location)
        binding.etPortfolioLink.setText(registrationViewModel.portfolioLink)

        binding.ivBack.setOnClickListener { findNavController().navigateUp() }
        binding.btnSaveExit.setOnClickListener { findNavController().navigateUp() }
        binding.btnFinishSetup.setOnClickListener {
            val location = binding.etLocation.text.toString().trim()

            var valid = true
            binding.tilLocation.error = null
            binding.tvCategoriesError.visibility = View.GONE
            binding.tvAudienceError.visibility = View.GONE

            val selectedNiches = collectCheckedChips(binding.chipGroupNiches)
            if (selectedNiches.isEmpty()) {
                binding.tvCategoriesError.visibility = View.VISIBLE
                valid = false
            }

            val selectedAudience = collectCheckedChips(binding.chipGroupTargetAudience)
            if (selectedAudience.isEmpty()) {
                binding.tvAudienceError.visibility = View.VISIBLE
                valid = false
            }

            if (location.isBlank()) {
                binding.tilLocation.error = getString(R.string.error_location_required)
                valid = false
            }

            if (!valid) return@setOnClickListener

            registrationViewModel.location = location

            registrationViewModel.targetAudience = selectedAudience
            registrationViewModel.audienceRegion = selectedAudience.joinToString(", ")
            registrationViewModel.niches = selectedNiches
            registrationViewModel.portfolioLink = binding.etPortfolioLink.text.toString().trim()

            registrationViewModel.completeRegistration(requireContext())
        }

        setupObservers()
    }

    private fun setupNicheChips() {
        binding.chipGroupNiches.removeAllViews()
        CreatorNicheTags.NICHE_TAGS.forEach { niche ->
            val chip = Chip(requireContext()).apply {
                text = niche
                isCheckable = true
                isChecked = registrationViewModel.niches.contains(niche)
                styleCreatorChip(this)
                setOnCheckedChangeListener { buttonView, isChecked ->
                    // Enforce max 5 selections
                    val selectedCount = (0 until binding.chipGroupNiches.childCount).count {
                        (binding.chipGroupNiches.getChildAt(it) as? Chip)?.isChecked == true
                    }
                    if (isChecked && selectedCount > 5) {
                        buttonView.isChecked = false
                    }
                    styleCreatorChip(buttonView as Chip)
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
                styleCreatorChip(this)
                setOnCheckedChangeListener { buttonView, _ ->
                    styleCreatorChip(buttonView as Chip)
                }
            }
            binding.chipGroupTargetAudience.addView(chip)
        }
    }

    private fun styleCreatorChip(chip: Chip) {
        val checked = chip.isChecked
        chip.chipBackgroundColor = ColorStateList.valueOf(
            requireContext().getColor(
                if (checked) R.color.colorSurfaceContainerHighest else R.color.colorSurfaceContainerLow
            )
        )
        chip.chipStrokeColor = ColorStateList.valueOf(
            requireContext().getColor(if (checked) R.color.colorPrimary else android.R.color.transparent)
        )
        chip.chipStrokeWidth = resources.getDimension(R.dimen.role_card_unselected_stroke)
        chip.setTextColor(
            requireContext().getColor(if (checked) R.color.colorPrimary else R.color.colorOnSurfaceVariant)
        )
        chip.typeface = ResourcesCompat.getFont(
            requireContext(),
            if (checked) R.font.manrope_semibold else R.font.manrope_medium
        )
        chip.minHeight = resources.getDimensionPixelSize(R.dimen.creator_chip_height)
    }

    private fun setupObservers() {
        registrationViewModel.registrationSuccess.observe(viewLifecycleOwner) { success ->
            if (success) {
                registrationViewModel.resetRegistrationSuccess()
                findNavController().navigate(R.id.action_creator_step3_to_home)
            }
        }

        registrationViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.btnFinishSetup.isEnabled = !isLoading
            binding.btnSaveExit.isEnabled = !isLoading
            binding.btnFinishSetup.text = getString(
                if (isLoading) R.string.nav_btn_creating_profile else R.string.nav_btn_finish
            )
        }

        registrationViewModel.error.observe(viewLifecycleOwner) { errorMsg ->
            if (!errorMsg.isNullOrBlank()) {
                binding.tilLocation.error = errorMsg
            }
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

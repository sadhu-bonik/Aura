package com.aura.app.ui.auth.brand

import android.app.DatePickerDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.aura.app.R
import com.aura.app.databinding.FragmentBrandRegStep5Binding
import com.aura.app.utils.BudgetRanges
import com.aura.app.utils.CampaignDeliverables
import com.aura.app.utils.CampaignGoals
import com.google.android.material.chip.Chip
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * BrandRegStep5Fragment — Final step: campaign setup.
 *
 * This is the ONLY step that talks to Firebase.
 * On "Finish":
 *   - Validates campaign fields
 *   - Calls vm.completeRegistration() which fires:
 *       1. Firebase Auth (create user)
 *       2. Firestore users/{uid}         (BrandAccount)
 *       3. Firestore brandProfiles/{uid} (full BrandProfile with all 5 steps)
 *   - On success → navigates to HomeContainerFragment (feed), clearing back stack
 */
class BrandRegStep5Fragment : Fragment() {

    private var _binding: FragmentBrandRegStep5Binding? = null
    private val binding get() = _binding!!

    private val vm: BrandRegistrationViewModel by activityViewModels { BrandRegistrationViewModel.Factory() }
    private val dateFormatter = SimpleDateFormat("MMM d, yyyy", Locale.US)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBrandRegStep5Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefillFields()
        setupCampaignControls()
        setupCharCounter()
        setupObservers()
        setupClickListeners()
    }

    private fun prefillFields() {
        binding.etCampaignName.setText(vm.campaignName)
        binding.etCampaignBrief.setText(vm.campaignBrief)
        binding.acvBudgetRange.setText(vm.campaignBudgetRange, false)
        vm.campaignTimeline?.toDate()?.let { binding.etTimeline.setText(dateFormatter.format(it)) }
    }

    private fun setupCampaignControls() {
        binding.acvBudgetRange.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, BudgetRanges.RANGES)
        )
        setupChipGroup(binding.chipGroupGoals, CampaignGoals.GOALS, vm.campaignGoals)
        setupChipGroup(binding.chipGroupDeliverables, CampaignDeliverables.DELIVERABLES, vm.campaignDeliverables)
    }

    private fun setupCharCounter() {
        binding.etCampaignBrief.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                binding.tvCharCount.text = "${s?.length ?: 0} / 2000"
            }
        })
        binding.tvCharCount.text = "${vm.campaignBrief.length} / 2000"
    }

    private fun setupObservers() {
        // Disable Finish button while the single Firebase call is in flight
        vm.isLoading.observe(viewLifecycleOwner) { loading ->
            binding.layoutBottomNav.btnNavNext.isEnabled = !loading
            binding.layoutBottomNav.btnNavNext.alpha = if (loading) 0.5f else 1.0f
            
            if (loading) {
                binding.layoutBottomNav.btnNavNext.text = ""
                binding.layoutBottomNav.btnNavNext.icon = null
                binding.layoutBottomNav.progressLoading.visibility = View.VISIBLE
            } else {
                binding.layoutBottomNav.btnNavNext.text = "Finish"
                binding.layoutBottomNav.progressLoading.visibility = View.GONE
            }
        }

        // Show any error from the Firebase call
        vm.error.observe(viewLifecycleOwner) { msg ->
            if (msg != null) {
                Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
                vm.clearError()
            }
        }

        // Navigate to feed when registration completes — brand always needs profile setup.
        vm.registrationComplete.observe(viewLifecycleOwner) { complete ->
            if (complete) {
                val bundle = android.os.Bundle().apply {
                    putBoolean("brandSetupRequired", true)
                }
                val navOptions = NavOptions.Builder()
                    .setPopUpTo(R.id.welcomeFragment, inclusive = true)
                    .build()
                findNavController().navigate(R.id.action_brand_finish_to_home, bundle, navOptions)
            }
        }
    }

    private fun setupClickListeners() {
        binding.ivClose.setOnClickListener { findNavController().navigateUp() }

        binding.btnAddCampaign.setOnClickListener {
            Toast.makeText(requireContext(), "Multiple campaigns coming soon", Toast.LENGTH_SHORT).show()
        }

        binding.etTimeline.setOnClickListener {
            showTimelinePicker()
        }

        binding.layoutBottomNav.btnNavCancel.setOnClickListener {
            findNavController().navigateUp()
        }

        // ── THE ONLY FIREBASE CALL IN THE ENTIRE BRAND FLOW ──────────────────
        binding.layoutBottomNav.btnNavNext.setOnClickListener {
            if (!validateForm()) return@setOnClickListener

            vm.campaignName = binding.etCampaignName.text.toString().trim()
            vm.campaignBrief = binding.etCampaignBrief.text.toString().trim()
            vm.campaignGoals = collectCheckedChips(binding.chipGroupGoals)
            vm.campaignBudgetRange = binding.acvBudgetRange.text.toString().trim()
            vm.campaignDeliverables = collectCheckedChips(binding.chipGroupDeliverables)

            vm.completeRegistration(requireContext())
        }
    }

    private fun validateForm(): Boolean {
        val name = binding.etCampaignName.text.toString().trim()
        val brief = binding.etCampaignBrief.text.toString().trim()
        val goals = collectCheckedChips(binding.chipGroupGoals)
        val budget = binding.acvBudgetRange.text.toString().trim()
        val deliverables = collectCheckedChips(binding.chipGroupDeliverables)

        binding.tilCampaignName.error = null
        binding.tilCampaignBrief.error = null
        binding.tilBudget.error = null
        binding.tilTimeline.error = null

        var valid = true
        if (name.isBlank()) {
            binding.tilCampaignName.error = getString(R.string.error_campaign_title_required)
            valid = false
        }
        if (brief.isBlank()) {
            binding.tilCampaignBrief.error = getString(R.string.error_campaign_description_required)
            valid = false
        }
        if (goals.isEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.error_campaign_goals_required), Toast.LENGTH_SHORT).show()
            valid = false
        }
        if (budget.isBlank()) {
            binding.tilBudget.error = getString(R.string.error_budget_required)
            valid = false
        }
        if (vm.campaignTimeline == null) {
            binding.tilTimeline.error = getString(R.string.error_timeline_required)
            valid = false
        }
        if (deliverables.isEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.error_deliverables_required), Toast.LENGTH_SHORT).show()
            valid = false
        }
        return valid
    }

    private fun setupChipGroup(
        group: com.google.android.material.chip.ChipGroup,
        values: List<String>,
        selected: List<String>
    ) {
        group.removeAllViews()
        values.forEach { value ->
            val chip = Chip(requireContext()).apply {
                text = value
                isCheckable = true
                isChecked = selected.contains(value)
            }
            group.addView(chip)
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

    private fun showTimelinePicker() {
        val calendar = Calendar.getInstance()
        vm.campaignTimeline?.toDate()?.let { calendar.time = it }
        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                calendar.set(Calendar.MILLISECOND, 0)
                vm.campaignTimeline = Timestamp(calendar.time)
                binding.etTimeline.setText(dateFormatter.format(calendar.time))
                binding.tilTimeline.error = null
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}

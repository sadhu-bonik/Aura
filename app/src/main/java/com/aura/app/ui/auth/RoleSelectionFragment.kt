package com.aura.app.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import android.widget.Toast
import com.aura.app.R
import com.aura.app.databinding.FragmentRoleSelectionBinding
import com.aura.app.ui.auth.brand.BrandRegistrationViewModel

/** RoleSelectionFragment — Creator or Brand role picker. */
class RoleSelectionFragment : Fragment() {

    private var _binding: FragmentRoleSelectionBinding? = null
    private val binding get() = _binding!!

    private val registrationViewModel: RegistrationViewModel by activityViewModels()
    private val onboardingDraftViewModel: OnboardingDraftViewModel by activityViewModels()
    private val brandRegistrationViewModel: BrandRegistrationViewModel by activityViewModels {
        BrandRegistrationViewModel.Factory()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRoleSelectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        registrationViewModel.userRole.observe(viewLifecycleOwner) { role ->
            updateCardState(role)
        }

        binding.cardCreator.setOnClickListener {
            registrationViewModel.setUserRole("creator")
        }

        binding.cardBrand.setOnClickListener {
            registrationViewModel.setUserRole("brand")
        }

        binding.btnContinue.setOnClickListener {
            when (registrationViewModel.userRole.value) {
                "creator" -> {
                    onboardingDraftViewModel.role = "creator"
                    onboardingDraftViewModel.applyToCreator(registrationViewModel)
                    findNavController().navigate(R.id.action_role_to_creator_step3)
                }
                "brand" -> {
                    onboardingDraftViewModel.role = "brand"
                    onboardingDraftViewModel.applyToBrand(brandRegistrationViewModel)
                    findNavController().navigate(R.id.action_role_to_brand_step2)
                }
                else -> Toast.makeText(requireContext(), "Please select a role to continue", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnBack.setOnClickListener { findNavController().navigateUp() }
    }

    private fun updateCardState(selected: String) {
        val dp2 = (2 * resources.displayMetrics.density).toInt()
        val dp1 = (1 * resources.displayMetrics.density).toInt()
        val colorPrimary = requireContext().getColor(R.color.colorPrimary)
        val colorTertiary = requireContext().getColor(R.color.colorTertiary)
        val colorOutlineVariant = requireContext().getColor(R.color.colorOutlineVariant)

        // Creator card
        if (selected == "creator") {
            binding.cardCreator.strokeWidth = dp2
            binding.cardCreator.strokeColor = colorPrimary
            binding.ivCreatorCheck.visibility = View.VISIBLE
        } else {
            binding.cardCreator.strokeWidth = dp1
            binding.cardCreator.strokeColor = colorOutlineVariant
            binding.ivCreatorCheck.visibility = View.GONE
        }

        // Brand card
        if (selected == "brand") {
            binding.cardBrand.strokeWidth = dp2
            binding.cardBrand.strokeColor = colorTertiary
            binding.ivBrandCheck.visibility = View.VISIBLE
        } else {
            binding.cardBrand.strokeWidth = dp1
            binding.cardBrand.strokeColor = colorOutlineVariant
            binding.ivBrandCheck.visibility = View.GONE
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

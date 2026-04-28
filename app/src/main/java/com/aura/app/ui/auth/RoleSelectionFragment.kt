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

        if (registrationViewModel.userRole.value.isNullOrBlank()) {
            registrationViewModel.setUserRole("creator")
        }
        
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
                    findNavController().navigate(R.id.action_role_to_creator_step2)
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
        val selectedStroke = resources.getDimensionPixelSize(R.dimen.role_card_selected_stroke)
        val unselectedStroke = resources.getDimensionPixelSize(R.dimen.role_card_unselected_stroke)
        val selectedColor = requireContext().getColor(R.color.colorRoleViolet)
        val unselectedColor = requireContext().getColor(R.color.colorRoleBorder)
        val mutedColor = requireContext().getColor(R.color.colorRoleMuted)

        val creatorSelected = selected == "creator"
        val brandSelected = selected == "brand"

        binding.cardCreator.strokeWidth = if (creatorSelected) selectedStroke else unselectedStroke
        binding.cardCreator.strokeColor = if (creatorSelected) selectedColor else unselectedColor
        binding.radioCreator.setBackgroundResource(
            if (creatorSelected) R.drawable.bg_role_radio_selected else R.drawable.bg_role_radio_unselected
        )
        binding.viewCreatorDot.visibility = if (creatorSelected) View.VISIBLE else View.GONE
        binding.layoutCreatorIcon.setBackgroundResource(
            if (creatorSelected) R.drawable.bg_role_icon_selected else R.drawable.bg_role_icon_unselected
        )
        binding.ivCreatorIcon.setColorFilter(if (creatorSelected) selectedColor else mutedColor)

        binding.cardBrand.strokeWidth = if (brandSelected) selectedStroke else unselectedStroke
        binding.cardBrand.strokeColor = if (brandSelected) selectedColor else unselectedColor
        binding.radioBrand.setBackgroundResource(
            if (brandSelected) R.drawable.bg_role_radio_selected else R.drawable.bg_role_radio_unselected
        )
        binding.viewBrandDot.visibility = if (brandSelected) View.VISIBLE else View.GONE
        binding.layoutBrandIcon.setBackgroundResource(
            if (brandSelected) R.drawable.bg_role_icon_selected else R.drawable.bg_role_icon_unselected
        )
        binding.ivBrandIcon.setColorFilter(if (brandSelected) selectedColor else mutedColor)
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

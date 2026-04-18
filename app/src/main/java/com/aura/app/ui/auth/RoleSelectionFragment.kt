package com.aura.app.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.aura.app.R
import com.aura.app.data.model.UserRole
import com.aura.app.databinding.FragmentRoleSelectionBinding

/** RoleSelectionFragment — Creator or Brand role picker. */
class RoleSelectionFragment : Fragment() {

    private var _binding: FragmentRoleSelectionBinding? = null
    private val binding get() = _binding!!

    private val registrationViewModel: RegistrationViewModel by activityViewModels()

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
            registrationViewModel.setUserRole(UserRole.CREATOR)
        }

        binding.cardBrand.setOnClickListener {
            registrationViewModel.setUserRole(UserRole.BRAND)
        }

        binding.btnContinue.setOnClickListener {
            when (registrationViewModel.userRole.value) {
                UserRole.CREATOR -> findNavController().navigate(R.id.action_role_to_creator_step1)
                UserRole.BRAND -> findNavController().navigate(R.id.action_role_to_brand_step1)
                else -> {}
            }
        }

        binding.btnBack.setOnClickListener { findNavController().navigateUp() }
    }

    private fun updateCardState(selected: UserRole) {
        binding.cardCreator.setBackgroundResource(
            if (selected == UserRole.CREATOR) R.drawable.bg_card_role_selected else R.drawable.bg_card_glass
        )
        binding.cardBrand.setBackgroundResource(
            if (selected == UserRole.BRAND) R.drawable.bg_card_role_selected else R.drawable.bg_card_glass
        )
        binding.ivCreatorCheck.visibility = if (selected == UserRole.CREATOR) View.VISIBLE else View.GONE
        binding.ivBrandCheck.visibility = if (selected == UserRole.BRAND) View.VISIBLE else View.GONE
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

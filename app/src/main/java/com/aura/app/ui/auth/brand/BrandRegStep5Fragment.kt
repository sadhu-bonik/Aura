package com.aura.app.ui.auth.brand

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.aura.app.R
import com.aura.app.databinding.FragmentBrandRegStep5Binding
import com.aura.app.ui.auth.RegistrationViewModel

class BrandRegStep5Fragment : Fragment() {
    private var _binding: FragmentBrandRegStep5Binding? = null
    private val binding get() = _binding!!

    private val registrationViewModel: RegistrationViewModel by activityViewModels()


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentBrandRegStep5Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupObservers()

        binding.ivClose.setOnClickListener { findNavController().navigateUp() }

        binding.btnAddCampaign.setOnClickListener {
            // TODO: Add another campaign card dynamically
        }

        binding.btnNavBack.setOnClickListener { findNavController().navigateUp() }
        binding.btnNavSaveExit.setOnClickListener { /* TODO: Save progress */ }
        binding.btnNavFinish.setOnClickListener {
            // Final submission
            registrationViewModel.completeRegistration()
        }
    }

    private fun setupObservers() {
        registrationViewModel.registrationSuccess.observe(viewLifecycleOwner) { success ->
            if (success) {
                findNavController().navigate(R.id.action_brand_finish_to_home)
            }
        }

        registrationViewModel.error.observe(viewLifecycleOwner) { errorMsg ->
            if (errorMsg != null) {
                // TODO: Show alert dialog
            }
        }

        registrationViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.btnNavFinish.isEnabled = !isLoading
            binding.btnNavFinish.alpha = if (isLoading) 0.5f else 1.0f
            // TODO: Show progress overlay
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}

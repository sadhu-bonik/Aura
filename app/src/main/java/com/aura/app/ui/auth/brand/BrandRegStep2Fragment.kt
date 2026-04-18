package com.aura.app.ui.auth.brand

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.aura.app.R
import com.aura.app.databinding.FragmentBrandRegStep2Binding
import com.aura.app.ui.auth.RegistrationViewModel

class BrandRegStep2Fragment : Fragment() {
    private var _binding: FragmentBrandRegStep2Binding? = null
    private val binding get() = _binding!!

    private val registrationViewModel: RegistrationViewModel by activityViewModels()

    private val photoPickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            registrationViewModel.profileImageUri = it
            // Update UI to show selection
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentBrandRegStep2Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Pre-fill
        binding.etMotto.setText(registrationViewModel.brandMotto)
        binding.etBio.setText(registrationViewModel.brandBio)

        binding.ivClose.setOnClickListener { findNavController().navigateUp() }
        
        binding.layoutLogoUpload.setOnClickListener { 
            photoPickerLauncher.launch("image/*")
        }

        binding.layoutBottomNav.btnNavBack.setOnClickListener { findNavController().navigateUp() }
        binding.layoutBottomNav.btnNavNext.setOnClickListener {
            // Save to ViewModel
            registrationViewModel.brandMotto = binding.etMotto.text.toString().trim()
            registrationViewModel.brandBio = binding.etBio.text.toString().trim()

            findNavController().navigate(R.id.action_brand_step2_to_step3)
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}

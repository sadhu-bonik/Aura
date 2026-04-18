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
import com.aura.app.databinding.FragmentBrandRegStep3Binding
import com.aura.app.ui.auth.RegistrationViewModel

class BrandRegStep3Fragment : Fragment() {
    private var _binding: FragmentBrandRegStep3Binding? = null
    private val binding get() = _binding!!

    private val registrationViewModel: RegistrationViewModel by activityViewModels()

    private val docPickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            registrationViewModel.verificationDocUri = it
            // Update UI to show selection
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentBrandRegStep3Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Pre-fill
        binding.etLegalName.setText(registrationViewModel.brandName)
        binding.etCompanyEmail.setText(registrationViewModel.email)
        binding.etLinkedin.setText(registrationViewModel.instagram)
        binding.etTwitter.setText(registrationViewModel.youtube)

        binding.ivClose.setOnClickListener { findNavController().navigateUp() }
        
        binding.layoutFileUpload.setOnClickListener { 
            docPickerLauncher.launch("*/*")
        }

        binding.btnNavBack.setOnClickListener { findNavController().navigateUp() }
        binding.btnNavContinue.setOnClickListener {
            // Save to ViewModel
            registrationViewModel.brandName = binding.etLegalName.text.toString().trim()
            registrationViewModel.email = binding.etCompanyEmail.text.toString().trim()
            registrationViewModel.instagram = binding.etLinkedin.text.toString().trim()
            registrationViewModel.youtube = binding.etTwitter.text.toString().trim()

            findNavController().navigate(R.id.action_brand_step3_to_step4)
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}

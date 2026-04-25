package com.aura.app.ui.auth.brand

import android.net.Uri
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
import com.bumptech.glide.Glide

class BrandRegStep2Fragment : Fragment() {

    private var _binding: FragmentBrandRegStep2Binding? = null
    private val binding get() = _binding!!

    private val vm: BrandRegistrationViewModel by activityViewModels { BrandRegistrationViewModel.Factory() }

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            vm.logoUri = it
            showLogoPreview(it)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBrandRegStep2Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefillFields()
        setupClickListeners()
        // Restore preview on rotation
        vm.logoUri?.let { showLogoPreview(it) }
    }

    private fun prefillFields() {
        binding.etMotto.setText(vm.motto)
        binding.etBio.setText(vm.bio)
    }

    private fun showLogoPreview(uri: Uri) {
        binding.layoutLogoPrompt.visibility = View.GONE
        binding.ivLogoPreview.visibility = View.VISIBLE
        Glide.with(this).load(uri).centerCrop().into(binding.ivLogoPreview)
    }

    private fun setupClickListeners() {
        binding.ivClose.setOnClickListener { findNavController().navigateUp() }

        binding.layoutLogoUpload.setOnClickListener {
            imagePickerLauncher.launch("image/*")
        }

        binding.layoutBottomNav.btnNavBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.layoutBottomNav.btnNavNext.setOnClickListener {
            if (!validateForm()) return@setOnClickListener

            vm.motto = binding.etMotto.text.toString().trim()
            vm.bio = binding.etBio.text.toString().trim()

            findNavController().navigate(R.id.action_brand_step2_to_step3)
        }
    }

    private fun validateForm(): Boolean {
        binding.tilMotto.error = null
        if (binding.etMotto.text.isNullOrBlank()) {
            binding.tilMotto.error = "Required"
            return false
        }
        return true
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}

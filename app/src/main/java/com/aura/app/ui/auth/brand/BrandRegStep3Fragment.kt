package com.aura.app.ui.auth.brand

import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.aura.app.R
import com.aura.app.databinding.FragmentBrandRegStep3Binding

class BrandRegStep3Fragment : Fragment() {

    private var _binding: FragmentBrandRegStep3Binding? = null
    private val binding get() = _binding!!

    private val vm: BrandRegistrationViewModel by activityViewModels { BrandRegistrationViewModel.Factory() }

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { handleSelectedFile(it) }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBrandRegStep3Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefillFields()
        setupClickListeners()
        // Restore filename label on rotation
        if (vm.verificationFileName.isNotBlank()) showFileName(vm.verificationFileName)
    }

    private fun prefillFields() {
        binding.etLegalName.setText(vm.legalName)
        binding.etRepName.setText(vm.repName)
        binding.etCompanyEmail.setText(vm.companyEmail)
        binding.etLinkedin.setText(vm.linkedinUrl)
        binding.etTwitter.setText(vm.twitterHandle)
    }

    private fun handleSelectedFile(uri: Uri) {
        val context = requireContext()
        val mime = context.contentResolver.getType(uri) ?: "application/octet-stream"
        val fileName = queryFileName(uri) ?: "document"

        vm.verificationFileUri = uri
        vm.verificationFileName = fileName
        vm.verificationFileMimeType = mime
        showFileName(fileName)
    }

    private fun showFileName(name: String) {
        binding.layoutFilePrompt.visibility = View.GONE
        binding.tvFileSelected.visibility = View.VISIBLE
        binding.tvFileSelected.text = name
    }

    private fun queryFileName(uri: Uri): String? {
        val cursor = requireContext().contentResolver.query(uri, null, null, null, null)
        return cursor?.use {
            if (it.moveToFirst()) {
                val idx = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) it.getString(idx) else null
            } else null
        }
    }

    private fun setupClickListeners() {
        binding.ivClose.setOnClickListener { findNavController().navigateUp() }

        binding.layoutFileUpload.setOnClickListener {
            filePickerLauncher.launch("*/*")
        }

        binding.layoutBottomNav.btnNavBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.layoutBottomNav.btnNavNext.setOnClickListener {
            if (!validateForm()) return@setOnClickListener

            vm.legalName = binding.etLegalName.text.toString().trim()
            vm.repName = binding.etRepName.text.toString().trim()
            vm.companyEmail = binding.etCompanyEmail.text.toString().trim()
            vm.linkedinUrl = binding.etLinkedin.text.toString().trim()
            vm.twitterHandle = binding.etTwitter.text.toString().trim()

            findNavController().navigate(R.id.action_brand_step3_to_step4)
        }
    }

    private fun validateForm(): Boolean {
        var valid = true
        binding.tilLegalName.error = null
        binding.tilRepName.error = null
        binding.tilCompanyEmail.error = null

        if (binding.etLegalName.text.isNullOrBlank()) {
            binding.tilLegalName.error = "Required"; valid = false
        }
        if (binding.etRepName.text.isNullOrBlank()) {
            binding.tilRepName.error = "Required"; valid = false
        }

        val email = binding.etCompanyEmail.text?.toString()?.trim() ?: ""
        if (email.isBlank()) {
            binding.tilCompanyEmail.error = "Required"; valid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilCompanyEmail.error = "Enter a valid email"; valid = false
        }

        return valid
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}

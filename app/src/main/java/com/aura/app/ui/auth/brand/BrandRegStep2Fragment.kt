package com.aura.app.ui.auth.brand

import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
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

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { handleSelectedFile(it) }
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
        if (vm.verificationFileName.isNotBlank()) showFileName(vm.verificationFileName)
    }

    private fun prefillFields() {
        binding.etBrandName.setText(vm.brandName)
        binding.etMotto.setText(vm.motto)
        binding.etBio.setText(vm.bio)
    }

    private fun showLogoPreview(uri: Uri) {
        binding.layoutLogoPrompt.visibility = View.GONE
        binding.ivLogoPreview.visibility = View.VISIBLE
        Glide.with(this).load(uri).centerCrop().into(binding.ivLogoPreview)
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

        binding.layoutLogoUpload.setOnClickListener {
            imagePickerLauncher.launch("image/*")
        }

        binding.layoutFileUpload.setOnClickListener {
            filePickerLauncher.launch(arrayOf("application/pdf", "image/*"))
        }

        binding.layoutBottomNav.btnNavCancel.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.layoutBottomNav.btnNavNext.setOnClickListener {
            if (!validateForm()) return@setOnClickListener

            vm.brandName = binding.etBrandName.text.toString().trim()
            vm.motto = binding.etMotto.text.toString().trim()
            vm.bio = binding.etBio.text.toString().trim()

            findNavController().navigate(R.id.action_brand_step2_to_step4)
        }
    }

    private fun validateForm(): Boolean {
        binding.tilBrandName.error = null
        binding.tilMotto.error = null
        if (binding.etBrandName.text.isNullOrBlank()) {
            binding.tilBrandName.error = getString(R.string.error_brand_name_required)
            return false
        }
        if (binding.etMotto.text.isNullOrBlank()) {
            binding.tilMotto.error = "Required"
            return false
        }
        if (vm.verificationFileUri == null) {
            android.widget.Toast.makeText(
                requireContext(),
                getString(R.string.error_business_license_required),
                android.widget.Toast.LENGTH_SHORT
            ).show()
            return false
        }
        return true
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}

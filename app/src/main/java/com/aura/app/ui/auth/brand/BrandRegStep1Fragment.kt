package com.aura.app.ui.auth.brand

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.aura.app.R
import com.aura.app.databinding.FragmentBrandRegStep1Binding
import com.aura.app.ui.auth.RegistrationViewModel

class BrandRegStep1Fragment : Fragment() {
    private var _binding: FragmentBrandRegStep1Binding? = null
    private val binding get() = _binding!!

    private val registrationViewModel: RegistrationViewModel by activityViewModels()


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentBrandRegStep1Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupSecurityQuestionDropdown()

        // Pre-fill
        binding.etBrandName.setText(registrationViewModel.brandName)
        binding.etEmail.setText(registrationViewModel.email)
        binding.etPhone.setText(registrationViewModel.phone)
        binding.etSecurityAnswer.setText(registrationViewModel.securityAnswer)

        binding.ivNavBack.setOnClickListener { findNavController().navigateUp() }
        binding.ivClose.setOnClickListener { findNavController().popBackStack(R.id.welcomeFragment, false) }
        binding.btnBack.setOnClickListener { findNavController().navigateUp() }
        binding.btnNext.setOnClickListener {
            if (validateForm()) {
                // Save to ViewModel
                registrationViewModel.brandName = binding.etBrandName.text.toString().trim()
                registrationViewModel.email = binding.etEmail.text.toString().trim()
                registrationViewModel.password = binding.etPassword.text.toString()
                registrationViewModel.phone = binding.etPhone.text.toString().trim()
                registrationViewModel.securityQuestion = binding.acvSecurityQuestion.text.toString()
                registrationViewModel.securityAnswer = binding.etSecurityAnswer.text.toString().trim()

                findNavController().navigate(R.id.action_brand_step1_to_step2)
            }
        }
    }

    private fun setupSecurityQuestionDropdown() {
        val questions = listOf(getString(R.string.sq_placeholder), getString(R.string.sq_pet_name), getString(R.string.sq_mothers_maiden_name), getString(R.string.sq_birth_city), getString(R.string.sq_high_school_mascot))
        binding.acvSecurityQuestion.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, questions))
        binding.acvSecurityQuestion.setText(registrationViewModel.securityQuestion.ifBlank { questions[0] }, false)
    }

    private fun validateForm(): Boolean {
        var valid = true
        binding.tilBrandName.error = null; binding.tilEmail.error = null; binding.tilPassword.error = null; binding.tilConfirmPassword.error = null
        if (binding.etBrandName.text.isNullOrBlank()) { binding.tilBrandName.error = "Required"; valid = false }
        if (binding.etEmail.text.isNullOrBlank()) { binding.tilEmail.error = "Required"; valid = false }
        val pw = binding.etPassword.text?.toString() ?: ""
        if (pw.length < 8) { binding.tilPassword.error = "Min 8 chars"; valid = false }
        if ((binding.etConfirmPassword.text?.toString() ?: "") != pw) { binding.tilConfirmPassword.error = "Don't match"; valid = false }
        return valid
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}

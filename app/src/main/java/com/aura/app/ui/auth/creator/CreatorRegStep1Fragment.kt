package com.aura.app.ui.auth.creator

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.aura.app.R
import com.aura.app.databinding.FragmentCreatorRegStep1Binding
import com.aura.app.ui.auth.RegistrationViewModel

class CreatorRegStep1Fragment : Fragment() {
    private var _binding: FragmentCreatorRegStep1Binding? = null
    private val binding get() = _binding!!

    private val registrationViewModel: RegistrationViewModel by activityViewModels()


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCreatorRegStep1Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupSecurityQuestionDropdown()
        
        // Pre-fill if already set
        binding.etFullName.setText(registrationViewModel.fullName)
        binding.etEmail.setText(registrationViewModel.email)
        binding.etPhone.setText(registrationViewModel.phone)
        binding.etSecurityAnswer.setText(registrationViewModel.securityAnswer)

        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }
        binding.btnCancel.setOnClickListener { findNavController().popBackStack(R.id.welcomeFragment, false) }
        binding.layoutBottomNav.btnNavBack.setOnClickListener { findNavController().navigateUp() }
        binding.layoutBottomNav.btnNavNext.setOnClickListener {
            if (validateForm()) {
                // Save to ViewModel
                registrationViewModel.fullName = binding.etFullName.text.toString().trim()
                registrationViewModel.email = binding.etEmail.text.toString().trim()
                registrationViewModel.password = binding.etPassword.text.toString()
                registrationViewModel.phone = binding.etPhone.text.toString().trim()
                registrationViewModel.securityQuestion = binding.acvSecurityQuestion.text.toString()
                registrationViewModel.securityAnswer = binding.etSecurityAnswer.text.toString().trim()

                findNavController().navigate(R.id.action_creator_step1_to_step2)
            }
        }
    }


    private fun setupSecurityQuestionDropdown() {
        val questions = listOf(getString(R.string.sq_placeholder), getString(R.string.sq_pet_name), getString(R.string.sq_mothers_maiden_name), getString(R.string.sq_birth_city), getString(R.string.sq_high_school_mascot))
        binding.acvSecurityQuestion.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, questions))
        binding.acvSecurityQuestion.setText(questions[0], false)
    }

    private fun validateForm(): Boolean {
        var valid = true
        binding.tilFullName.error = null; binding.tilEmail.error = null; binding.tilPassword.error = null; binding.tilConfirmPassword.error = null
        binding.tilPhone.error = null; binding.tilSecurityAnswer.error = null

        if (binding.etFullName.text.isNullOrBlank()) { binding.tilFullName.error = "Required"; valid = false }
        if (binding.etEmail.text.isNullOrBlank()) { binding.tilEmail.error = "Required"; valid = false }
        if (binding.etPhone.text.isNullOrBlank()) { binding.tilPhone.error = "Required"; valid = false }
        if (binding.etSecurityAnswer.text.isNullOrBlank()) { binding.tilSecurityAnswer.error = "Required"; valid = false }

        val sq = binding.acvSecurityQuestion.text.toString()
        if (sq.isBlank() || sq == getString(R.string.sq_placeholder)) {
            // Usually we'd want a red outline on TextInputLayout, but standard simple validation here
            valid = false
        }

        val pw = binding.etPassword.text?.toString() ?: ""
        if (pw.length < 8) { binding.tilPassword.error = "Min 8 chars"; valid = false }
        if ((binding.etConfirmPassword.text?.toString() ?: "") != pw) { binding.tilConfirmPassword.error = "Passwords don't match"; valid = false }
        return valid
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}

package com.aura.app.ui.auth

import android.os.Bundle
import android.telephony.PhoneNumberUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.aura.app.R
import com.aura.app.databinding.FragmentCommonProfileSetupBinding

class CommonProfileSetupFragment : Fragment() {
    private var _binding: FragmentCommonProfileSetupBinding? = null
    private val binding get() = _binding!!

    private val draftViewModel: OnboardingDraftViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCommonProfileSetupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupSecurityQuestionDropdown()
        prefillFields()

        binding.ivBack.setOnClickListener { findNavController().navigateUp() }
        binding.btnCancel.setOnClickListener { findNavController().navigateUp() }
        binding.btnNext.icon = null
        binding.btnNext.iconPadding = 0
        binding.btnNext.setOnClickListener {
            if (!validateForm()) return@setOnClickListener

            draftViewModel.firstName = binding.etFirstName.text.toString().trim()
            draftViewModel.lastName = binding.etLastName.text.toString().trim()
            draftViewModel.phone = normalizedPhone()
            draftViewModel.securityQuestion = binding.acvSecurityQuestion.text.toString()
            draftViewModel.securityAnswer = binding.etSecurityAnswer.text.toString().trim()
            findNavController().navigate(R.id.action_common_profile_to_role_selection)
        }
    }

    private fun setupSecurityQuestionDropdown() {
        val questions = listOf(
            getString(R.string.sq_placeholder),
            getString(R.string.sq_pet_name),
            getString(R.string.sq_mothers_maiden_name),
            getString(R.string.sq_birth_city),
            getString(R.string.sq_high_school_mascot)
        )
        binding.acvSecurityQuestion.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, questions)
        )
        binding.acvSecurityQuestion.setText(
            draftViewModel.securityQuestion.ifBlank { questions[0] },
            false
        )
    }

    private fun prefillFields() {
        binding.etFirstName.setText(draftViewModel.firstName)
        binding.etLastName.setText(draftViewModel.lastName)
        binding.etPhone.setText(draftViewModel.phone)
        binding.etSecurityAnswer.setText(draftViewModel.securityAnswer)
    }

    private fun validateForm(): Boolean {
        var valid = true
        binding.tilFirstName.error = null
        binding.tilLastName.error = null
        binding.tilPhone.error = null
        binding.tilSecurityQuestion.error = null
        binding.tilSecurityAnswer.error = null

        val firstName = binding.etFirstName.text?.toString()?.trim() ?: ""
        val lastName = binding.etLastName.text?.toString()?.trim() ?: ""
        val phone = binding.etPhone.text?.toString()?.trim() ?: ""
        val question = binding.acvSecurityQuestion.text?.toString()?.trim() ?: ""
        val answer = binding.etSecurityAnswer.text?.toString()?.trim() ?: ""

        if (firstName.isBlank()) {
            binding.tilFirstName.error = getString(R.string.error_first_name_required)
            valid = false
        }
        if (lastName.isBlank()) {
            binding.tilLastName.error = getString(R.string.error_last_name_required)
            valid = false
        }
        if (phone.isBlank()) {
            binding.tilPhone.error = getString(R.string.error_phone_required)
            valid = false
        } else if (!PhoneNumberUtils.isGlobalPhoneNumber(normalizedPhone().filterNot { it.isWhitespace() || it == '-' || it == '(' || it == ')' })) {
            binding.tilPhone.error = getString(R.string.error_phone_invalid)
            valid = false
        }
        if (question.isBlank() || question == getString(R.string.sq_placeholder)) {
            binding.tilSecurityQuestion.error = getString(R.string.error_security_question_required)
            valid = false
        }
        if (answer.isBlank()) {
            binding.tilSecurityAnswer.error = getString(R.string.error_security_answer_required)
            valid = false
        }

        return valid
    }

    private fun normalizedPhone(): String {
        val raw = binding.etPhone.text?.toString()?.trim() ?: ""
        return if (raw.startsWith("+")) raw else "+1 $raw"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

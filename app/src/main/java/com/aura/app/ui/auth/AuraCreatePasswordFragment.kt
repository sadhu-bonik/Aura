package com.aura.app.ui.auth

import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.aura.app.R
import com.aura.app.databinding.FragmentCreatePasswordBinding

class AuraCreatePasswordFragment : Fragment() {
    private var _binding: FragmentCreatePasswordBinding? = null
    private val binding get() = _binding!!

    private val draftViewModel: OnboardingDraftViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreatePasswordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.etEmail.setText(draftViewModel.email)

        binding.ivBack.setOnClickListener { findNavController().navigateUp() }
        binding.layoutBottomNav.btnNavCancel.setOnClickListener { findNavController().navigateUp() }
        binding.layoutBottomNav.btnNavNext.text = getString(R.string.button_create_account)
        binding.layoutBottomNav.btnNavNext.icon = null
        binding.layoutBottomNav.btnNavNext.iconPadding = 0
        binding.layoutBottomNav.btnNavNext.setOnClickListener {
            if (!validateForm()) return@setOnClickListener

            draftViewModel.email = binding.etEmail.text.toString().trim()
            draftViewModel.password = binding.etPassword.text.toString()
            findNavController().navigate(R.id.action_create_password_to_common_profile)
        }
    }

    private fun validateForm(): Boolean {
        var valid = true
        binding.tilEmail.error = null
        binding.tilPassword.error = null
        binding.tilConfirmPassword.error = null

        val email = binding.etEmail.text?.toString()?.trim() ?: ""
        val password = binding.etPassword.text?.toString() ?: ""
        val confirm = binding.etConfirmPassword.text?.toString() ?: ""

        if (email.isBlank()) {
            binding.tilEmail.error = getString(R.string.error_email_required)
            valid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = getString(R.string.error_email_invalid)
            valid = false
        }

        if (password.isBlank()) {
            binding.tilPassword.error = getString(R.string.error_password_required)
            valid = false
        } else if (password.length < 8 || !password.any { it.isUpperCase() } || !password.any { it.isDigit() }) {
            binding.tilPassword.error = getString(R.string.error_password_weak)
            valid = false
        }

        if (confirm != password) {
            binding.tilConfirmPassword.error = getString(R.string.error_password_mismatch)
            valid = false
        }

        return valid
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

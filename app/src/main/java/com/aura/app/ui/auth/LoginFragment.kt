package com.aura.app.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.aura.app.R
import com.aura.app.databinding.FragmentLoginBinding

/** LoginFragment — Password entry screen. */
class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val authViewModel: AuthViewModel by activityViewModels()
    private val registrationViewModel: RegistrationViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set current email from registration state (passed from Welcome screen)
        binding.tvUserEmailDisplay.text = registrationViewModel.email

        setupObservers()

        binding.btnLogin.setOnClickListener {
            val password = binding.etPassword.text?.toString() ?: ""
            if (password.isNotBlank()) {
                authViewModel.login(registrationViewModel.email, password)
            } else {
                binding.tilPassword.error = "Password cannot be empty"
            }
        }

        binding.btnCancel.setOnClickListener { findNavController().navigateUp() }

        binding.tvForgotPassword.setOnClickListener {
            findNavController().navigate(R.id.action_login_to_forgot_password)
        }
    }

    private fun setupObservers() {
        authViewModel.loginSuccess.observe(viewLifecycleOwner) { success ->
            if (success) {
                findNavController().navigate(R.id.action_login_to_home)
            }
        }

        authViewModel.error.observe(viewLifecycleOwner) { errorMsg ->
            if (errorMsg != null) {
                binding.tilPassword.error = errorMsg
            }
        }

        authViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.btnLogin.isEnabled = !isLoading
            binding.btnLogin.alpha = if (isLoading) 0.5f else 1.0f
            // TODO: Show a progress indicator
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

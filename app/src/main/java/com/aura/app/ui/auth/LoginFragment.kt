package com.aura.app.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.aura.app.R
import com.aura.app.databinding.FragmentLoginBinding
import kotlinx.coroutines.launch

/** LoginFragment — Password entry screen. */
class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private var failedAttempts = 0

    private val authViewModel: AuthViewModel by activityViewModels { AuthViewModel.Factory() }
    private val registrationViewModel: RegistrationViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Resolve email: prefer the navArg passed by WelcomeFragment (covers returning users),
        // fall back to registrationViewModel.email for same-session registration→login redirects.
        val email = arguments?.getString("email").orEmpty().ifBlank { registrationViewModel.email }
        binding.tvUserEmailDisplay.text = email

        setupObservers()

        binding.btnLogin.setOnClickListener {
            binding.tilPassword.error = null
            val password = binding.etPassword.text?.toString() ?: ""
            authViewModel.loginUser(email, password, requireContext())
        }

        binding.btnCancel.setOnClickListener { findNavController().navigateUp() }

        binding.tvForgotPassword.setOnClickListener {
            val bundle = android.os.Bundle().apply { putString("email", email) }
            findNavController().navigate(R.id.action_login_to_forgot_password, bundle)
        }
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                authViewModel.authState.collect { state ->
                    when (state) {
                        is AuthState.Idle -> {
                            binding.btnLogin.isEnabled = true
                            binding.btnLogin.alpha = 1.0f
                        }
                        is AuthState.Loading -> {
                            binding.btnLogin.isEnabled = false
                            binding.btnLogin.alpha = 0.5f
                        }
                        is AuthState.Success -> {
                            // Reset state so re-entry to this screen doesn't re-trigger navigation
                            authViewModel.resetState()
                            if (!state.user.isProfileComplete) {
                                if (state.user.role == "creator") {
                                    findNavController().navigate(R.id.action_login_to_creator_step1)
                                } else {
                                    // Incomplete brand: go to home and open edit profile to complete setup.
                                    // Routing to brand Step 1 would re-run Firebase Auth (account creation)
                                    // and crash with a collision error — the account already exists.
                                    val bundle = android.os.Bundle().apply {
                                        putBoolean("brandSetupRequired", true)
                                    }
                                    findNavController().navigate(R.id.action_login_to_home, bundle)
                                }
                            } else {
                                findNavController().navigate(R.id.action_login_to_home)
                            }
                        }
                        is AuthState.Error -> {
                            failedAttempts++
                            authViewModel.resetState()
                            if (failedAttempts >= 3) {
                                binding.btnLogin.isEnabled = false
                                binding.btnLogin.alpha = 0.5f
                                binding.tilPassword.error = "Too many failed attempts — use Forgot Password below"
                            } else {
                                binding.btnLogin.isEnabled = true
                                binding.btnLogin.alpha = 1.0f
                                binding.tilPassword.error = state.message
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

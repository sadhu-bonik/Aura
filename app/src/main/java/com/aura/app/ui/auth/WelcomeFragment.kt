package com.aura.app.ui.auth

import android.os.Bundle
import android.util.Patterns
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
import com.aura.app.databinding.FragmentWelcomeBinding
import kotlinx.coroutines.launch

/**
 * WelcomeFragment — First screen in the auth flow.
 * Accepts email and navigates to Login or Role Selection.
 */
class WelcomeFragment : Fragment() {

    private var _binding: FragmentWelcomeBinding? = null
    private val binding get() = _binding!!

    private val authViewModel: AuthViewModel by activityViewModels()
    private val registrationViewModel: RegistrationViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWelcomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sessionManager = com.aura.app.utils.SessionManager(requireContext())
        if (sessionManager.getUserId() != null) {
            val navOptions = androidx.navigation.NavOptions.Builder()
                .setPopUpTo(R.id.welcomeFragment, true)
                .build()
            findNavController().navigate(R.id.homeContainerFragment, null, navOptions)
            return
        }

        binding.btnContinue.setOnClickListener {
            val email = binding.etEmail.text?.toString()?.trim() ?: ""
            if (!isValidEmail(email)) {
                binding.tilEmail.error = "Please enter a valid email address"
                return@setOnClickListener
            }
            binding.tilEmail.error = null
            registrationViewModel.email = email
            authViewModel.checkEmail(email)
        }

        binding.tvSignUpLink.setOnClickListener {
            registrationViewModel.resetDraft()
            findNavController().navigate(R.id.action_welcome_to_role_selection)
        }

        observeEmailCheck()
    }

    private fun observeEmailCheck() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                authViewModel.emailCheckState.collect { state ->
                    when (state) {
                        is EmailCheckState.Loading -> {
                            binding.btnContinue.isEnabled = false
                            binding.btnContinue.alpha = 0.5f
                        }
                        is EmailCheckState.Exists -> {
                            authViewModel.resetEmailCheck()
                            binding.btnContinue.isEnabled = true
                            binding.btnContinue.alpha = 1.0f
                            val bundle = android.os.Bundle().apply {
                                putString("email", registrationViewModel.email)
                            }
                            findNavController().navigate(R.id.action_welcome_to_login, bundle)
                        }
                        is EmailCheckState.New -> {
                            authViewModel.resetEmailCheck()
                            binding.btnContinue.isEnabled = true
                            binding.btnContinue.alpha = 1.0f
                            registrationViewModel.resetDraft()
                            findNavController().navigate(R.id.action_welcome_to_role_selection)
                        }
                        is EmailCheckState.Error -> {
                            authViewModel.resetEmailCheck()
                            binding.btnContinue.isEnabled = true
                            binding.btnContinue.alpha = 1.0f
                            binding.tilEmail.error = state.message
                        }
                        is EmailCheckState.Idle -> {
                            binding.btnContinue.isEnabled = true
                            binding.btnContinue.alpha = 1.0f
                        }
                    }
                }
            }
        }
    }

    private fun isValidEmail(email: String) =
        email.isNotBlank() && Patterns.EMAIL_ADDRESS.matcher(email).matches()

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

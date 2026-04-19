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
import com.aura.app.databinding.FragmentWelcomeBinding

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
            if (isValidEmail(email)) {
                // Also store in RegistrationViewModel so same-session registration->login
                // flows can still read it if needed.
                registrationViewModel.email = email
                val bundle = android.os.Bundle().apply { putString("email", email) }
                findNavController().navigate(R.id.action_welcome_to_login, bundle)
            } else {
                binding.tilEmail.error = "Please enter a valid email address"
            }
        }


        binding.tvSignUpLink.setOnClickListener {
            findNavController().navigate(R.id.action_welcome_to_role_selection)
        }
    }

    private fun isValidEmail(email: String) =
        email.isNotBlank() && Patterns.EMAIL_ADDRESS.matcher(email).matches()

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

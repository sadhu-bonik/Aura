package com.aura.app.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.aura.app.data.repository.UserRepository
import com.aura.app.databinding.FragmentForgotPasswordBinding
import kotlinx.coroutines.launch

/**
 * ForgotPasswordFragment — security-question-gated password recovery.
 *
 * Flow:
 *   1. User arrives with an email (passed as nav arg from LoginFragment).
 *   2. Look up the user's stored security question and answer in Firestore.
 *   3. User enters answer. We compare against the stored value.
 *      Three wrong attempts in a session lock the form.
 *   4. On match, we trigger Firebase Auth's sendPasswordResetEmail(email)
 *      and the user finishes the reset via the email link.
 *
 * SECURITY NOTE — The SRA spec asks for a security-question-gated reset
 * inside the app. Firebase Auth does NOT permit clients to set another
 * user's password just because a security answer matched, and we MUST
 * NOT store plaintext passwords in Firestore as a workaround. The secure
 * way to bypass the email step would be a Cloud Function using the
 * Firebase Admin SDK, called only after server-side verification of the
 * security answer. We don't have a backend in this prototype, so we fall
 * back to Firebase's email reset link after the security answer matches.
 *
 * TODO: When a Cloud Function / Admin SDK backend exists, replace
 *       sendPasswordResetEmail() with a call to that function so the
 *       user can set a new password directly inside the app.
 */
class ForgotPasswordFragment : Fragment() {

    private var _binding: FragmentForgotPasswordBinding? = null
    private val binding get() = _binding!!

    private val authViewModel: AuthViewModel by activityViewModels { AuthViewModel.Factory() }

    private val userRepository = UserRepository()
    private var correctAnswer: String = ""
    private var failedAnswerAttempts = 0
    private var resetInFlight = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentForgotPasswordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val email = arguments?.getString("email").orEmpty()
        binding.tvEmailDisplay.text = email.ifBlank { "Unknown" }

        if (email.isNotBlank()) {
            loadSecurityQuestion(email)
        } else {
            binding.tvSecurityQuestion.text = "Email not available — please go back."
            binding.btnResetPassword.isEnabled = false
        }

        binding.btnResetPassword.setOnClickListener { handleSubmit(email) }
        binding.btnCancel.setOnClickListener { findNavController().navigateUp() }

        observeAuthState()
    }

    private fun loadSecurityQuestion(email: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            val user = userRepository.getUserByEmail(email)
            if (user == null) {
                binding.tvSecurityQuestion.text = "No account found for this email."
                binding.btnResetPassword.isEnabled = false
                return@launch
            }
            correctAnswer = user.securityAnswer
            if (user.securityQuestion.isBlank() || correctAnswer.isBlank()) {
                binding.tvSecurityQuestion.text = "No security question set. Contact support."
                binding.tilSecurityAnswer.isEnabled = false
                binding.btnResetPassword.isEnabled = false
            } else {
                binding.tvSecurityQuestion.text = user.securityQuestion
            }
        }
    }

    private fun handleSubmit(email: String) {
        if (failedAnswerAttempts >= 3 || resetInFlight) return

        val answer = binding.etSecurityAnswer.text?.toString()?.trim() ?: ""
        binding.tilSecurityAnswer.error = null

        if (answer.isBlank()) {
            binding.tilSecurityAnswer.error = "Required"
            return
        }

        if (!answer.equals(correctAnswer, ignoreCase = true)) {
            failedAnswerAttempts++
            if (failedAnswerAttempts >= 3) {
                binding.tilSecurityAnswer.error = "Too many incorrect attempts"
                binding.btnResetPassword.isEnabled = false
                binding.etSecurityAnswer.isEnabled = false
            } else {
                val left = 3 - failedAnswerAttempts
                binding.tilSecurityAnswer.error = "Incorrect answer ($left attempt${if (left == 1) "" else "s"} left)"
            }
            return
        }

        resetInFlight = true
        authViewModel.resetPassword(email)
    }

    private fun observeAuthState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                authViewModel.authState.collect { state ->
                    when (state) {
                        is AuthState.Loading -> {
                            binding.btnResetPassword.isEnabled = false
                            binding.btnResetPassword.alpha = 0.5f
                        }
                        is AuthState.Idle -> {
                            binding.btnResetPassword.alpha = 1.0f
                            if (resetInFlight) {
                                // Loading → Idle with resetInFlight means success
                                resetInFlight = false
                                Toast.makeText(
                                    requireContext(),
                                    "Password reset email sent — check your inbox",
                                    Toast.LENGTH_LONG
                                ).show()
                                findNavController().navigateUp()
                            } else {
                                binding.btnResetPassword.isEnabled = true
                            }
                        }
                        is AuthState.Error -> {
                            resetInFlight = false
                            authViewModel.resetState()
                            binding.btnResetPassword.isEnabled = true
                            binding.btnResetPassword.alpha = 1.0f
                            Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                        }
                        is AuthState.Success -> authViewModel.resetState()
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

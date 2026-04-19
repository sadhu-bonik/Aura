package com.aura.app.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.aura.app.databinding.FragmentLoginBinding

/**
 * ForgotPasswordFragment — Allows user to reset their password using
 * their email + security question/answer pair.
 *
 * NOTE: Backend logic is omitted in the UI-only phase.
 */
class ForgotPasswordFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Re-uses the login layout as a placeholder; replace with dedicated layout if needed.
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Customise the reused layout texts
        binding.tvAppName.text = "Forgot Password?"
        binding.tvLoginSubtitle.text = "Enter your email and security answer to reset access."
        binding.btnLogin.text = "Reset Password"

        binding.btnLogin.setOnClickListener {
            // TODO: Password reset logic
            findNavController().navigateUp()
        }

        binding.btnCancel.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

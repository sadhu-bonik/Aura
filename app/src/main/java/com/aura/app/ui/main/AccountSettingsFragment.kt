package com.aura.app.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.aura.app.R
import com.aura.app.databinding.FragmentAccountSettingsBinding
import com.bumptech.glide.Glide
import kotlinx.coroutines.launch

/**
 * AccountSettingsFragment — lets the signed-in user edit their account
 * details, role-specific links, and Firebase Auth password.
 *
 * Fragment is view-only: it observes [AccountSettingsViewModel] state and
 * forwards user actions. All Firebase I/O happens in the repositories.
 */
class AccountSettingsFragment : Fragment() {

    private var _binding: FragmentAccountSettingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AccountSettingsViewModel by viewModels {
        AccountSettingsViewModel.Factory()
    }

    private var prefilled: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAccountSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupSecurityQuestionDropdown()
        setupListeners()
        observe()

        viewModel.load()
    }

    private fun setupSecurityQuestionDropdown() {
        val questions = arrayOf(
            getString(R.string.sq_mothers_maiden_name),
            getString(R.string.sq_pet_name),
            getString(R.string.sq_birth_city),
            getString(R.string.sq_high_school_mascot)
        )
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            questions
        )
        binding.etSecurityQuestion.setAdapter(adapter)
    }

    private fun setupListeners() {
        binding.btnAccountBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnAccountSave.setOnClickListener {
            clearFieldErrors()
            viewModel.saveAccount(
                fullName = binding.etFullName.text?.toString().orEmpty(),
                phone = binding.etPhone.text?.toString().orEmpty(),
                securityQuestion = binding.etSecurityQuestion.text?.toString().orEmpty(),
                securityAnswer = binding.etSecurityAnswer.text?.toString().orEmpty(),
                bio = binding.etBio.text?.toString().orEmpty(),
                youtubeHandle = binding.etYoutube.text?.toString().orEmpty(),
                website = binding.etWebsite.text?.toString().orEmpty(),
                linkedin = binding.etLinkedin.text?.toString().orEmpty()
            )
        }

        binding.btnChangePassword.setOnClickListener {
            clearPasswordErrors()
            viewModel.changePassword(
                currentPassword = binding.etCurrentPassword.text?.toString().orEmpty(),
                newPassword = binding.etNewPassword.text?.toString().orEmpty(),
                confirmPassword = binding.etConfirmPassword.text?.toString().orEmpty()
            )
        }
    }

    private fun observe() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.state.collect(::renderState) }
                launch { viewModel.event.collect(::handleEvent) }
                launch {
                    viewModel.isSaving.collect { saving ->
                        binding.btnAccountSave.isEnabled = !saving
                        binding.btnAccountSave.alpha = if (saving) 0.5f else 1.0f
                        binding.progressAccount.visibility =
                            if (saving) View.VISIBLE else View.GONE
                    }
                }
                launch {
                    viewModel.isChangingPassword.collect { changing ->
                        binding.btnChangePassword.isEnabled = !changing
                        binding.btnChangePassword.alpha = if (changing) 0.5f else 1.0f
                    }
                }
            }
        }
    }

    private fun renderState(state: AccountSettingsState) {
        when (state) {
            is AccountSettingsState.Loading -> {
                binding.progressAccount.visibility = View.VISIBLE
            }
            is AccountSettingsState.Error -> {
                binding.progressAccount.visibility = View.GONE
                Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
            }
            is AccountSettingsState.Ready -> {
                binding.progressAccount.visibility = View.GONE
                if (!prefilled) {
                    populate(state)
                    prefilled = true
                }
                applyRoleVisibility(state.user.role)
            }
        }
    }

    private fun populate(state: AccountSettingsState.Ready) {
        val user = state.user
        binding.etFullName.setText(
            user.displayName.ifBlank {
                state.brandProfile?.brandName.orEmpty()
            }
        )
        binding.etEmail.setText(user.email)
        binding.etPhone.setText(user.phone)
        binding.etSecurityAnswer.setText(user.securityAnswer)
        if (user.securityQuestion.isNotBlank()) {
            binding.etSecurityQuestion.setText(user.securityQuestion, false)
        }

        when (user.role) {
            "creator" -> {
                binding.etBio.setText(state.creatorProfile?.bio.orEmpty())
                binding.etYoutube.setText(state.creatorProfile?.youtubeHandle.orEmpty())
            }
            "brand" -> {
                binding.etBio.setText(state.brandProfile?.bio.orEmpty())
                binding.etWebsite.setText(state.brandProfile?.website.orEmpty())
                binding.etLinkedin.setText(state.brandProfile?.linkedinUrl.orEmpty())
            }
        }

        if (user.profileImageUrl.isNotBlank()) {
            Glide.with(this)
                .load(user.profileImageUrl)
                .centerCrop()
                .into(binding.ivAccountAvatar)
        }
    }

    /** Role is permanent — only show the fields that match. */
    private fun applyRoleVisibility(role: String) {
        binding.layoutCreatorFields.visibility =
            if (role == "creator") View.VISIBLE else View.GONE
        binding.layoutBrandFields.visibility =
            if (role == "brand") View.VISIBLE else View.GONE
    }

    private fun handleEvent(event: AccountSettingsEvent?) {
        when (event) {
            null, AccountSettingsEvent.SaveStarted, AccountSettingsEvent.PasswordStarted -> Unit
            AccountSettingsEvent.SaveSuccess -> {
                Toast.makeText(
                    requireContext(),
                    R.string.account_settings_saved,
                    Toast.LENGTH_SHORT
                ).show()
                viewModel.resetEvent()
            }
            is AccountSettingsEvent.SaveError -> {
                applySaveFieldError(event.message)
                Toast.makeText(requireContext(), event.message, Toast.LENGTH_LONG).show()
                viewModel.resetEvent()
            }
            AccountSettingsEvent.PasswordSuccess -> {
                Toast.makeText(
                    requireContext(),
                    R.string.account_settings_password_updated,
                    Toast.LENGTH_SHORT
                ).show()
                binding.etCurrentPassword.text?.clear()
                binding.etNewPassword.text?.clear()
                binding.etConfirmPassword.text?.clear()
                viewModel.resetEvent()
            }
            is AccountSettingsEvent.PasswordError -> {
                applyPasswordFieldError(event.message)
                Toast.makeText(requireContext(), event.message, Toast.LENGTH_LONG).show()
                viewModel.resetEvent()
            }
        }
    }

    private fun clearFieldErrors() {
        binding.tilFullName.error = null
        binding.tilPhone.error = null
        binding.tilSecurityQuestion.error = null
        binding.tilSecurityAnswer.error = null
        binding.tilYoutube.error = null
        binding.tilWebsite.error = null
        binding.tilLinkedin.error = null
    }

    private fun clearPasswordErrors() {
        binding.tilCurrentPassword.error = null
        binding.tilNewPassword.error = null
        binding.tilConfirmPassword.error = null
    }

    /** Best-effort mapping: surface the validation error on the relevant field. */
    private fun applySaveFieldError(message: String) {
        when {
            message.contains("Name", ignoreCase = true) -> binding.tilFullName.error = message
            message.contains("Phone", ignoreCase = true) -> binding.tilPhone.error = message
            message.contains("question", ignoreCase = true) -> binding.tilSecurityQuestion.error = message
            message.contains("answer", ignoreCase = true) -> binding.tilSecurityAnswer.error = message
            message.contains("Website", ignoreCase = true) -> binding.tilWebsite.error = message
            message.contains("LinkedIn", ignoreCase = true) -> binding.tilLinkedin.error = message
        }
    }

    private fun applyPasswordFieldError(message: String) {
        when {
            message.contains("current password", ignoreCase = true) ->
                binding.tilCurrentPassword.error = message
            message.contains("don't match", ignoreCase = true) ->
                binding.tilConfirmPassword.error = message
            else -> binding.tilNewPassword.error = message
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

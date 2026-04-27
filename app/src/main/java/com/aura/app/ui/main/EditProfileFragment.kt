package com.aura.app.ui.main

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.aura.app.R
import com.aura.app.databinding.FragmentEditProfileBinding
import android.widget.ArrayAdapter
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class EditProfileFragment : Fragment() {

    private var _binding: FragmentEditProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: EditProfileViewModel by viewModels {
        EditProfileViewModel.Factory()
    }

    private val feedViewModel: com.aura.app.ui.feed.VideoFeedViewModel by activityViewModels {
        com.aura.app.ui.feed.VideoFeedViewModel.Factory(requireContext().applicationContext)
    }

    private var selectedImageUri: Uri? = null

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { chosen ->
            // Show confirmation before committing the photo change
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.dialog_confirm_photo_title)
                .setMessage(R.string.dialog_confirm_photo_message)
                .setPositiveButton(R.string.btn_upload) { _, _ ->
                    selectedImageUri = chosen
                    Glide.with(this).load(chosen).centerCrop().into(binding.ivEditPhoto)
                }
                .setNegativeButton(R.string.btn_cancel, null)
                .show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupSecurityQuestions()
        setupListeners()
        observeState()
        observeEvents()
    }

    private fun setupSecurityQuestions() {
        val questions = arrayOf(
            "What is your mother's maiden name?",
            "What was your first pet's name?",
            "In what city were you born?",
            "What is your favorite book?",
            "What was your childhood nickname?"
        )
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, questions)
        binding.etSecurityQuestion.setAdapter(adapter)
    }

    private fun setupListeners() {
        binding.toolbarEditProfile.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnUploadPhoto.setOnClickListener {
            imagePickerLauncher.launch("image/*")
        }

        binding.btnSave.setOnClickListener {
            val name = binding.etName.text.toString()
            val headline = binding.etHeadline.text.toString()
            val bio = binding.etBio.text.toString()
            val phone = binding.etPhone.text.toString()
            val secQuestion = binding.etSecurityQuestion.text.toString()
            val secAnswer = binding.etSecurityAnswer.text.toString()
            val youtube = binding.etYoutube.text.toString()
            
            val website = binding.etWebsite.text.toString()
            val industry = binding.etIndustry.text.toString()

            viewModel.saveProfile(
                displayName = name,
                headline = headline,
                bio = bio,
                phone = phone,
                securityQuestion = secQuestion,
                securityAnswer = secAnswer,
                youtubeUrl = youtube,
                profileImageUri = selectedImageUri,
                website = website,
                industry = industry
            )
        }

        binding.btnDeleteProfile.setOnClickListener {
            Toast.makeText(requireContext(), "Delete profile coming soon", Toast.LENGTH_SHORT).show()
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    when (state) {
                        is EditProfileUiState.Loading -> {
                            binding.layoutLoading.root.visibility = View.VISIBLE
                        }
                        is EditProfileUiState.Error -> {
                            binding.layoutLoading.root.visibility = View.GONE
                            Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                        }
                        is EditProfileUiState.Success -> {
                            binding.layoutLoading.root.visibility = View.GONE
                            populateForm(state)
                        }
                    }
                }
            }
        }
    }

    private fun observeEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.event.collect { event ->
                    when (event) {
                        is EditProfileEvent.Saving -> {
                            binding.layoutLoading.root.visibility = View.VISIBLE
                        }
                        is EditProfileEvent.SaveError -> {
                            binding.layoutLoading.root.visibility = View.GONE
                            Toast.makeText(requireContext(), event.message, Toast.LENGTH_LONG).show()
                            viewModel.resetEvent()
                        }
                        is EditProfileEvent.SaveSuccessWithTagChange -> {
                            binding.layoutLoading.root.visibility = View.GONE
                            Toast.makeText(requireContext(), "Profile updated (Tags changed)", Toast.LENGTH_SHORT).show()
                            feedViewModel.clearSimilarCreatorFeedCache()
                            feedViewModel.loadCreatorFeed(forceRefresh = true)
                            viewModel.resetEvent()
                            findNavController().popBackStack()
                        }
                        is EditProfileEvent.SaveSuccess -> {
                            binding.layoutLoading.root.visibility = View.GONE
                            Toast.makeText(requireContext(), "Profile updated", Toast.LENGTH_SHORT).show()
                            viewModel.resetEvent()
                            findNavController().popBackStack()
                        }
                        null -> {}
                    }
                }
            }
        }
    }

    private fun populateForm(state: EditProfileUiState.Success) {
        val user = state.user
        val isBrand = user.role == "brand"

        // Bio and headline: source depends on role
        val bio = if (isBrand) state.brandProfile?.bio ?: "" else state.creatorProfile?.bio ?: ""
        val headline = if (isBrand) state.brandProfile?.motto ?: "" else state.creatorProfile?.motto ?: ""
        val youtube = if (!isBrand) state.creatorProfile?.youtubeHandle ?: "" else ""

        if (isBrand) {
            binding.tilHeadline.visibility = View.GONE
            binding.layoutCreatorYoutube.visibility = View.GONE
            binding.layoutBrandWebsite.visibility = View.VISIBLE
            binding.layoutBrandIndustry.visibility = View.VISIBLE

            if (binding.etWebsite.text.isNullOrBlank()) {
                binding.etWebsite.setText(state.brandProfile?.website ?: "")
            }
            if (binding.etIndustry.text.isNullOrBlank()) {
                binding.etIndustry.setText(state.brandProfile?.industry ?: "")
            }
        } else {
            binding.tilHeadline.visibility = View.VISIBLE
            binding.layoutCreatorYoutube.visibility = View.VISIBLE
            binding.layoutBrandWebsite.visibility = View.GONE
            binding.layoutBrandIndustry.visibility = View.GONE
        }

        // Only prefill if fields haven't been touched (guards against config-change overwrites)
        if (binding.etName.text.isNullOrBlank()) {
            binding.etName.setText(user.displayName.ifBlank { user.email })
        }

        if (binding.etHeadline.text.isNullOrBlank()) {
            binding.etHeadline.setText(headline)
        }

        if (binding.etBio.text.isNullOrBlank()) {
            binding.etBio.setText(bio)
        }

        if (binding.etPhone.text.isNullOrBlank()) {
            binding.etPhone.setText(user.phone)
        }

        if (binding.etSecurityQuestion.text.isNullOrBlank() && user.securityQuestion.isNotBlank()) {
            binding.etSecurityQuestion.setText(user.securityQuestion, false) // false to prevent showing dropdown
        }

        if (binding.etSecurityAnswer.text.isNullOrBlank()) {
            binding.etSecurityAnswer.setText(user.securityAnswer)
        }

        if (binding.etYoutube.text.isNullOrBlank()) {
            binding.etYoutube.setText(youtube)
        }

        // Only load Glide if user hasn't selected a new local image
        if (selectedImageUri == null && user.profileImageUrl.isNotEmpty()) {
            Glide.with(this)
                .load(user.profileImageUrl)
                .centerCrop()
                .into(binding.ivEditPhoto)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

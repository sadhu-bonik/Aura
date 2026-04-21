package com.aura.app.ui.main

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.aura.app.R
import com.aura.app.databinding.FragmentEditProfileBinding
import com.aura.app.utils.CreatorNicheTags
import com.bumptech.glide.Glide
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class EditProfileFragment : Fragment() {

    private var _binding: FragmentEditProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: EditProfileViewModel by viewModels {
        EditProfileViewModel.Factory()
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

        setupListeners()
        observeState()
        observeEvents()
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
            val bio = binding.etBio.text.toString()

            val selectedTags = mutableListOf<String>()
            for (i in 0 until binding.cgNicheTags.childCount) {
                val chip = binding.cgNicheTags.getChildAt(i) as? Chip ?: continue
                if (chip.isChecked) selectedTags.add(chip.text.toString())
            }

            val isBrand = (viewModel.state.value as? EditProfileUiState.Success)?.user?.role == "brand"
            if (selectedTags.isEmpty()) {
                val msg = if (isBrand) "Select at least 1 industry tag" else "Select at least 1 specialty tag"
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.saveProfile(name, bio, selectedTags, selectedImageUri)
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

        // Bio and tags: source depends on role
        val bio = if (isBrand) state.brandProfile?.bio ?: "" else state.creatorProfile?.bio ?: ""
        val activeTags: List<String> = if (isBrand) {
            state.brandProfile?.industryTags ?: emptyList()
        } else {
            state.creatorProfile?.tags ?: emptyList()
        }

        // Only prefill if fields haven't been touched (guards against config-change overwrites)
        if (binding.etName.text.isNullOrBlank()) {
            binding.etName.setText(user.displayName.ifBlank { user.email })
        }

        if (binding.etBio.text.isNullOrBlank()) {
            binding.etBio.setText(bio)
        }

        // Update label to reflect role-appropriate copy
        binding.tvNicheLabel.text = if (isBrand) {
            "Industry Tags (Select at least 1)"
        } else {
            "Specialties (Select 1–5)"
        }

        if (binding.cgNicheTags.childCount == 0) {
            CreatorNicheTags.NICHE_TAGS.forEach { niche ->
                val chip = Chip(requireContext()).apply {
                    text = niche
                    isCheckable = true
                    isChecked = activeTags.contains(niche)
                    if (!isBrand) {
                        setOnCheckedChangeListener { buttonView, checked ->
                            if (checked) {
                                val selectedCount = (0 until binding.cgNicheTags.childCount).count {
                                    (binding.cgNicheTags.getChildAt(it) as? Chip)?.isChecked == true
                                }
                                if (selectedCount > 5) {
                                    buttonView.isChecked = false
                                    Toast.makeText(requireContext(), "You can pick up to 5 specialties", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                }
                binding.cgNicheTags.addView(chip)
            }
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

package com.aura.app.ui.auth.creator

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.aura.app.R
import com.aura.app.databinding.FragmentCreatorRegStep2Binding
import com.aura.app.ui.auth.RegistrationViewModel
import com.bumptech.glide.Glide

class CreatorRegStep2Fragment : Fragment() {
    private var _binding: FragmentCreatorRegStep2Binding? = null
    private val binding get() = _binding!!

    private val registrationViewModel: RegistrationViewModel by activityViewModels()

    private val photoPickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            registrationViewModel.profileImageUri = it
            showPhotoPreview()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCreatorRegStep2Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Pre-fill text fields from ViewModel draft
        binding.etMotto.setText(registrationViewModel.creatorMotto)
        binding.etBio.setText(registrationViewModel.creatorBio)
        binding.etInstagram.setText(registrationViewModel.instagramHandle)

        // Restore photo preview if already selected (e.g. after rotation)
        if (registrationViewModel.profileImageUri != null) showPhotoPreview()

        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }

        binding.layoutPhotoUpload.setOnClickListener {
            photoPickerLauncher.launch("image/*")
        }

        binding.layoutBottomNav.btnNavBack.setOnClickListener { findNavController().navigateUp() }
        binding.layoutBottomNav.btnNavNext.setOnClickListener {
            val motto = binding.etMotto.text.toString().trim()
            val bio = binding.etBio.text.toString().trim()

            if (motto.isBlank()) {
                binding.tilMotto.error = "Required"
                return@setOnClickListener
            } else {
                binding.tilMotto.error = null
            }

            if (bio.isBlank()) {
                binding.tilBio.error = "Required"
                return@setOnClickListener
            } else {
                binding.tilBio.error = null
            }

            registrationViewModel.creatorMotto = motto
            registrationViewModel.creatorBio = bio
            registrationViewModel.instagramHandle = binding.etInstagram.text.toString().trim()

            findNavController().navigate(R.id.action_creator_step2_to_step3)
        }
    }

    private fun showPhotoPreview() {
        val uri = registrationViewModel.profileImageUri ?: return
        binding.layoutPhotoPrompt.visibility = View.GONE
        binding.ivProfilePhotoPreview.visibility = View.VISIBLE
        Glide.with(this)
            .load(uri)
            .centerCrop()
            .into(binding.ivProfilePhotoPreview)
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}

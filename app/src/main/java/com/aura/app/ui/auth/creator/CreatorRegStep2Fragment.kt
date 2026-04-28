package com.aura.app.ui.auth.creator

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.aura.app.R
import com.aura.app.databinding.FragmentCreatorRegStep2Binding
import com.aura.app.databinding.ViewCreatorVideoSlotBinding
import com.aura.app.ui.auth.RegistrationViewModel
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.request.RequestOptions

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

    private val videoPickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            if (registrationViewModel.portfolioVideoUris.size >= 3) {
                Toast.makeText(requireContext(), getString(R.string.creator_setup_video_limit), Toast.LENGTH_SHORT).show()
                return@let
            }
            registrationViewModel.addPortfolioVideoUri(it)
            binding.tvVideoError.visibility = View.GONE
            refreshVideoSlots()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCreatorRegStep2Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.etMotto.setText(registrationViewModel.creatorHeadline.ifBlank { registrationViewModel.creatorMotto })
        binding.etBio.setText(registrationViewModel.creatorBio)
        binding.etYoutube.setText(registrationViewModel.youtubeHandle)

        if (registrationViewModel.profileImageUri != null) showPhotoPreview()

        setupVideoSlots()
        setupObservers()

        binding.ivBack.setOnClickListener { findNavController().navigateUp() }
        binding.btnSaveExit.setOnClickListener { findNavController().navigateUp() }
        binding.layoutPhotoUpload.setOnClickListener { photoPickerLauncher.launch("image/*") }
        binding.btnPhotoUpload.setOnClickListener { photoPickerLauncher.launch("image/*") }

        binding.btnNext.setOnClickListener {
            val headline = binding.etMotto.text.toString().trim()
            val bio = binding.etBio.text.toString().trim()

            binding.tilMotto.error = null
            binding.tilBio.error = null
            binding.tvVideoError.visibility = View.GONE

            var valid = true
            if (headline.isBlank()) {
                binding.tilMotto.error = getString(R.string.error_headline_required)
                valid = false
            }
            if (bio.isBlank()) {
                binding.tilBio.error = getString(R.string.error_bio_required)
                valid = false
            }
            if (registrationViewModel.portfolioVideoUris.isEmpty()) {
                binding.tvVideoError.visibility = View.VISIBLE
                valid = false
            }

            if (!valid) return@setOnClickListener

            registrationViewModel.creatorHeadline = headline
            registrationViewModel.creatorBio = bio
            registrationViewModel.youtubeHandle = binding.etYoutube.text.toString().trim()

            findNavController().navigate(R.id.action_creator_step2_to_step3)
        }

        refreshVideoSlots()
    }

    private fun setupVideoSlots() {
        videoSlotBindings().forEach { slot ->
            slot.root.setOnClickListener {
                if (registrationViewModel.portfolioVideoUris.size >= 3) {
                    Toast.makeText(requireContext(), getString(R.string.creator_setup_video_limit), Toast.LENGTH_SHORT).show()
                } else {
                    videoPickerLauncher.launch("video/*")
                }
            }
        }
    }

    private fun setupObservers() {
        registrationViewModel.pendingVideoCount.observe(viewLifecycleOwner) {
            refreshVideoSlots()
        }

        registrationViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.btnNext.isEnabled = !isLoading
            binding.btnSaveExit.isEnabled = !isLoading
            binding.layoutPhotoUpload.isEnabled = !isLoading
            binding.btnPhotoUpload.isEnabled = !isLoading
            videoSlotBindings().forEach { it.root.isEnabled = !isLoading }
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE

            if (isLoading) {
                binding.btnNext.text = ""
                binding.btnNext.icon = null
                binding.progressLoading.visibility = View.VISIBLE
            } else {
                binding.btnNext.text = getString(R.string.nav_btn_next)
                binding.btnNext.setIconResource(R.drawable.ic_chevron_right)
                binding.progressLoading.visibility = View.GONE
            }
        }

        registrationViewModel.error.observe(viewLifecycleOwner) { errorMsg ->
            if (!errorMsg.isNullOrBlank()) {
                Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun refreshVideoSlots() {
        val uris = registrationViewModel.portfolioVideoUris
        videoSlotBindings().forEachIndexed { index, slot ->
            val uri = uris.getOrNull(index)
            if (uri == null) {
                slot.ivVideoThumbnail.visibility = View.GONE
                slot.layoutVideoPrompt.visibility = View.VISIBLE
            } else {
                slot.layoutVideoPrompt.visibility = View.GONE
                slot.ivVideoThumbnail.visibility = View.VISIBLE
                Glide.with(slot.ivVideoThumbnail)
                    .asBitmap()
                    .load(uri)
                    .apply(RequestOptions.frameOf(1_000_000L).transform(CenterCrop()))
                    .placeholder(R.color.colorSurfaceContainerHigh)
                    .into(slot.ivVideoThumbnail)
            }
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

    private fun videoSlotBindings(): List<ViewCreatorVideoSlotBinding> =
        listOf(binding.videoSlot1, binding.videoSlot2, binding.videoSlot3)

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

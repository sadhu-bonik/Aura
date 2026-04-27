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
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.aura.app.R
import com.aura.app.databinding.FragmentCreatorRegStep2Binding
import com.aura.app.databinding.ItemPortfolioVideoBinding
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

    private val pendingAdapter = PendingVideoAdapter { index ->
        registrationViewModel.removePortfolioVideoUri(index)
    }

    private val videoPickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            val added = registrationViewModel.addPortfolioVideoUri(it)
            if (!added) {
                Toast.makeText(requireContext(), getString(R.string.toast_portfolio_full), Toast.LENGTH_SHORT).show()
            }
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
        binding.etYoutube.setText(registrationViewModel.youtubeHandle)

        // Restore photo preview if already selected
        if (registrationViewModel.profileImageUri != null) showPhotoPreview()

        setupRecyclerView()
        setupObservers()

        binding.ivBack.setOnClickListener { findNavController().navigateUp() }
        binding.layoutBottomNav.btnNavCancel.setOnClickListener { findNavController().navigateUp() }

        binding.layoutPhotoUpload.setOnClickListener {
            photoPickerLauncher.launch("image/*")
        }

        binding.btnAddVideo.setOnClickListener {
            if (registrationViewModel.portfolioVideoUris.size >= 3) {
                Toast.makeText(requireContext(), "You can only select up to 3 videos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            videoPickerLauncher.launch("video/*")
        }

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

            if (registrationViewModel.portfolioVideoUris.isEmpty()) {
                Toast.makeText(requireContext(), getString(R.string.toast_portfolio_video_required), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            registrationViewModel.creatorMotto = motto
            registrationViewModel.creatorBio = bio
            registrationViewModel.youtubeHandle = binding.etYoutube.text.toString().trim()

            registrationViewModel.completeRegistration(requireContext())
        }

        refreshAdapter()
    }

    private fun setupRecyclerView() {
        binding.rvVideos.apply {
            layoutManager = GridLayoutManager(requireContext(), 3)
            adapter = pendingAdapter
        }
    }

    private fun setupObservers() {
        registrationViewModel.pendingVideoCount.observe(viewLifecycleOwner) { count ->
            refreshAdapter()
            val hasVideos = count > 0
            binding.rvVideos.visibility = if (hasVideos) View.VISIBLE else View.GONE
            // Optional: Hide/Disable add button if max 3 reached
            binding.btnAddVideo.alpha = if (count >= 3) 0.5f else 1.0f
        }

        registrationViewModel.registrationSuccess.observe(viewLifecycleOwner) { success ->
            if (success) {
                registrationViewModel.resetRegistrationSuccess()
                findNavController().navigate(R.id.action_creator_step2_to_home)
            }
        }

        registrationViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.layoutBottomNav.btnNavNext.isEnabled = !isLoading
            binding.btnAddVideo.isEnabled = !isLoading
            binding.layoutPhotoUpload.isEnabled = !isLoading
            binding.progressBar?.visibility = if (isLoading) View.VISIBLE else View.GONE

            if (isLoading) {
                binding.layoutBottomNav.btnNavNext.text = ""
                binding.layoutBottomNav.btnNavNext.icon = null
                binding.layoutBottomNav.progressLoading.visibility = View.VISIBLE
            } else {
                binding.layoutBottomNav.btnNavNext.text = "Finish"
                binding.layoutBottomNav.progressLoading.visibility = View.GONE
            }
        }

        registrationViewModel.error.observe(viewLifecycleOwner) { errorMsg ->
            if (!errorMsg.isNullOrBlank()) {
                Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun refreshAdapter() {
        pendingAdapter.submitList(registrationViewModel.portfolioVideoUris.toList())
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

// ---------------------------------------------------------------------------
// Adapter for locally-staged video URIs (registration flow only)
// ---------------------------------------------------------------------------

private class PendingVideoAdapter(
    private val onRemoveClick: (index: Int) -> Unit
) : ListAdapter<Uri, PendingVideoAdapter.ViewHolder>(UriDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPortfolioVideoBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemPortfolioVideoBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(uri: Uri) {
            Glide.with(binding.ivThumbnail)
                .asBitmap()
                .load(uri)
                .apply(RequestOptions.frameOf(1_000_000L).transform(CenterCrop()))
                .placeholder(R.color.colorSurfaceContainerHigh)
                .into(binding.ivThumbnail)

            binding.btnDelete.visibility = View.VISIBLE
            binding.btnDelete.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) onRemoveClick(pos)
            }
        }
    }

    private object UriDiffCallback : DiffUtil.ItemCallback<Uri>() {
        override fun areItemsTheSame(oldItem: Uri, newItem: Uri) = oldItem == newItem
        override fun areContentsTheSame(oldItem: Uri, newItem: Uri) = oldItem == newItem
    }
}

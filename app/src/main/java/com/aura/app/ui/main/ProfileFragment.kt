package com.aura.app.ui.main

import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.aura.app.adapters.PortfolioAdapter
import com.aura.app.databinding.FragmentProfileBinding
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.aura.app.R
import kotlinx.coroutines.launch


class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProfileViewModel by viewModels {
        ProfileViewModel.Factory(requireContext())
    }

    private val portfolioAdapter = PortfolioAdapter()

    private val videoPickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { handleSelectedVideo(it) }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupListeners()
        setupBottomSheetListener()
        observeUploadEvents()

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state -> render(state) }
            }
        }

        val creatorId = arguments?.getString("creatorId")

        // Back button: visible when viewing someone else's profile (viewer mode)
        if (creatorId != null) {
            binding.btnBack.visibility = View.VISIBLE
            binding.btnBack.setOnClickListener { returnToFeed() }
            requireActivity().onBackPressedDispatcher.addCallback(
                viewLifecycleOwner,
                object : OnBackPressedCallback(true) {
                    override fun handleOnBackPressed() {
                        returnToFeed()
                    }
                }
            )
        } else {
            binding.btnBack.visibility = View.GONE
        }

        viewModel.loadProfile(creatorId)
    }

    private fun setupRecyclerView() {
        binding.rvPortfolio.apply {
            layoutManager = GridLayoutManager(requireContext(), 3)
            adapter = portfolioAdapter
        }
    }

    private fun setupListeners() {
        binding.btnAddPortfolio.setOnClickListener {
            AddVideoBottomSheet().show(childFragmentManager, AddVideoBottomSheet.TAG)
        }
        binding.btnEditProfile.setOnClickListener {
            findNavController().navigate(R.id.action_profile_to_editProfile)
        }
    }



    private fun setupBottomSheetListener() {
        childFragmentManager.setFragmentResultListener(
            AddVideoBottomSheet.REQUEST_KEY,
            viewLifecycleOwner
        ) { _, bundle ->
            when (bundle.getString(AddVideoBottomSheet.ACTION_KEY)) {
                AddVideoBottomSheet.ACTION_GALLERY -> {
                    videoPickerLauncher.launch("video/*")
                }
                AddVideoBottomSheet.ACTION_CAMERA -> {
                    Toast.makeText(requireContext(), "Camera recording coming soon", Toast.LENGTH_SHORT).show()
                }
            }
        }

        childFragmentManager.setFragmentResultListener(
            ConfirmVideoUploadBottomSheet.REQUEST_KEY,
            viewLifecycleOwner
        ) { _, bundle ->
            val uriStr = bundle.getString(ConfirmVideoUploadBottomSheet.RESULT_KEY_URI)
            val title = bundle.getString(ConfirmVideoUploadBottomSheet.RESULT_KEY_TITLE) ?: ""
            val desc = bundle.getString(ConfirmVideoUploadBottomSheet.RESULT_KEY_DESC) ?: ""
            val mime = bundle.getString(ConfirmVideoUploadBottomSheet.RESULT_KEY_MIME) ?: ""
            val duration = bundle.getLong(ConfirmVideoUploadBottomSheet.RESULT_KEY_DURATION)

            if (uriStr != null) {
                viewModel.uploadPortfolioVideo(Uri.parse(uriStr), mime, title, desc, duration)
            }
        }
    }

    /** Observe one-shot upload events for progress/error/success feedback. */
    private fun observeUploadEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uploadEvent.collect { event ->
                    when (event) {
                        is UploadEvent.Started -> {
                            binding.progressUpload.visibility = View.VISIBLE
                            binding.btnAddPortfolio.isEnabled = false
                        }
                        is UploadEvent.Progress -> {
                            // Progress bar stays visible; could add a status label later
                        }
                        is UploadEvent.Success -> {
                            binding.progressUpload.visibility = View.GONE
                            binding.btnAddPortfolio.isEnabled = true
                            // Toast suppressed — the RecyclerView auto-updates via the snapshot listener
                        }
                        is UploadEvent.Failure -> {
                            binding.progressUpload.visibility = View.GONE
                            binding.btnAddPortfolio.isEnabled = true
                            Toast.makeText(requireContext(), event.message, Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }

    /**
     * Called after the system picker returns a video URI.
     * Extracts metadata, validates, and delegates to the ViewModel for upload.
     */
    private fun handleSelectedVideo(uri: Uri) {
        val context = requireContext()
        val contentResolver = context.contentResolver

        // --- MIME type from ContentResolver ---
        val mimeType = contentResolver.getType(uri) ?: "video/mp4"
        if (!mimeType.startsWith("video/")) {
            Toast.makeText(context, "Selected file is not a video", Toast.LENGTH_LONG).show()
            return
        }

        // --- Duration via MediaMetadataRetriever ---
        val durationSec = try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, uri)
            val ms = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 0L
            retriever.release()
            ms / 1000
        } catch (e: Exception) {
            0L
        }

        // --- Original file name ---
        val fileName = queryFileName(uri) ?: "video_${System.currentTimeMillis()}.mp4"

        // Show standard input bottom sheet natively preventing automatic actions.
        ConfirmVideoUploadBottomSheet.newInstance(
            uri = uri.toString(),
            fileName = fileName,
            mimeType = mimeType,
            durationSec = durationSec
        ).show(childFragmentManager, ConfirmVideoUploadBottomSheet.TAG)
    }

    /** Reads the display name from content:// URIs via the OpenableColumns cursor. */
    private fun queryFileName(uri: Uri): String? {
        val cursor = requireContext().contentResolver.query(uri, null, null, null, null)
        return cursor?.use {
            if (it.moveToFirst()) {
                val idx = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) it.getString(idx) else null
            } else null
        }
    }

    private fun render(state: ProfileUiState) {
        when (state) {
            is ProfileUiState.Loading -> {
                binding.tvProfileName.text = "Loading…"
                binding.tvProfileBio.text = ""
                binding.btnAddPortfolio.visibility = View.GONE
            }
            is ProfileUiState.Error -> {
                binding.tvProfileName.text = state.message
                binding.tvProfileBio.text = ""
                binding.btnAddPortfolio.visibility = View.GONE
            }
            is ProfileUiState.Success -> {
                val user = state.user
                val isBrand = user.role == "brand"
                binding.tvProfileName.text = user.displayName.ifBlank { user.email }

                // Headline: creator niche or brand name / role label
                val headlineText = if (isBrand) {
                    state.brandProfile?.motto?.takeIf { it.isNotBlank() }
                        ?: user.role.replaceFirstChar { it.uppercase() }
                } else {
                    state.creatorProfile?.niche?.takeIf { it.isNotBlank() }
                        ?: user.role.replaceFirstChar { it.uppercase() }
                }
                binding.tvProfileHeadline.text = headlineText

                // Bio: use brandProfile for brands, creatorProfile for creators
                val bioText = if (isBrand) {
                    state.brandProfile?.bio?.takeIf { it.isNotBlank() } ?: "No bio added yet"
                } else {
                    state.creatorProfile?.bio?.takeIf { it.isNotBlank() } ?: "No bio added yet"
                }
                binding.tvProfileBio.text = bioText

                // Stats: brands show deal/campaign counts; creators show social stats
                if (isBrand) {
                    binding.tvStatsFollowers.text = state.brandProfile?.totalCampaigns?.toString() ?: "0"
                    binding.tvStatsDeals.text = state.brandProfile?.activeDeals?.toString() ?: "0"
                    binding.tvStatsRating.text = "—"
                } else {
                    binding.tvStatsFollowers.text = state.creatorProfile?.followerCount?.toString() ?: "0"
                    binding.tvStatsDeals.text = state.creatorProfile?.completedDeals?.toString() ?: "0"
                    binding.tvStatsRating.text = state.creatorProfile?.averageRating?.toString() ?: "0.0"
                }

                binding.btnEditProfile.visibility =
                    if (state.isOwner) View.VISIBLE else View.GONE

                binding.btnAddPortfolio.visibility =
                    if (state.isOwner && !isBrand) View.VISIBLE else View.GONE

                if (user.profileImageUrl.isNotEmpty()) {
                    Glide.with(this)
                        .load(user.profileImageUrl)
                        .centerCrop()
                        .into(binding.ivCoverImage)
                } else {
                    binding.ivCoverImage.setBackgroundResource(R.color.colorSurfaceContainerHigh)
                }

                // Tags: industryTags for brands, niche tags for creators
                val tags: List<String> = if (isBrand) {
                    state.brandProfile?.industryTags ?: emptyList()
                } else {
                    state.creatorProfile?.tags ?: emptyList()
                }
                if (tags.isEmpty()) {
                    binding.tvTagsLabel.visibility = View.GONE
                    binding.cgTags.visibility = View.GONE
                } else {
                    binding.tvTagsLabel.visibility = View.VISIBLE
                    binding.cgTags.visibility = View.VISIBLE
                    binding.cgTags.removeAllViews()
                    tags.forEach { tag ->
                        val chip = com.google.android.material.chip.Chip(requireContext())
                        chip.text = tag
                        chip.isClickable = false
                        chip.isCheckable = false
                        chip.setChipBackgroundColorResource(R.color.colorSurfaceVariant)
                        chip.setTextColor(requireContext().getColor(R.color.colorOnSurface))
                        binding.cgTags.addView(chip)
                    }
                }

                // Arm delete callback for the owner (creator only); disable for viewer / brand
                portfolioAdapter.onDeleteClick = if (state.isOwner && !isBrand) { item ->
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle(R.string.dialog_remove_video_title)
                        .setMessage(R.string.dialog_remove_video_message)
                        .setPositiveButton(R.string.btn_remove) { _, _ ->
                            viewModel.deletePortfolioItem(item)
                        }
                        .setNegativeButton(R.string.btn_cancel, null)
                        .show()
                } else null

                portfolioAdapter.submitList(state.portfolio)

                val hasPortfolio = state.portfolio.isNotEmpty()
                binding.rvPortfolio.visibility = if (hasPortfolio) View.VISIBLE else View.GONE
                binding.tvNoPortfolio.visibility =
                    if (hasPortfolio || isBrand) View.GONE else View.VISIBLE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun returnToFeed() {
        val navController = findNavController()
        val popped = navController.popBackStack()
        if (!popped) {
            navController.navigateUp()
        }
    }
}

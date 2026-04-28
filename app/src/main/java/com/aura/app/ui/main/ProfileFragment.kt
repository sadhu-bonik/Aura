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
import androidx.appcompat.widget.PopupMenu
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.aura.app.R
import com.aura.app.data.model.Deal
import com.aura.app.ui.feed.SelectCampaignBottomSheet
import com.aura.app.utils.rootNavController
import kotlinx.coroutines.launch


class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProfileViewModel by viewModels {
        ProfileViewModel.Factory(requireContext())
    }

    private val portfolioAdapter = PortfolioAdapter()
    private val campaignAdapter = com.aura.app.adapters.CampaignAdapter(
        onCampaignClick = { campaign ->
            openSetupCampaignPopup(campaign.campaignId)
        },
        onEditClick = { campaign ->
            openSetupCampaignPopup(campaign.campaignId)
        },
        onDeleteClick = { campaign ->
            viewModel.deleteCampaign(campaign.campaignId)
        }
    )

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

        setupRecyclerViews()
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

    private fun setupRecyclerViews() {
        portfolioAdapter.onItemClick = { item ->
            if (item.mediaUrl.isNotBlank()) {
                rootNavController().navigate(
                    R.id.videoPlayerFragment,
                    Bundle().apply { putString("videoUrl", item.mediaUrl) }
                )
            }
        }

        binding.rvPortfolio.apply {
            layoutManager = GridLayoutManager(requireContext(), 3)
            adapter = portfolioAdapter
        }
        binding.rvCampaigns.apply {
            layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext())
            adapter = campaignAdapter
        }
    }

    private fun setupListeners() {
        binding.btnAddPortfolio.setOnClickListener {
            AddVideoBottomSheet().show(childFragmentManager, AddVideoBottomSheet.TAG)
        }
        binding.btnAddCampaign.setOnClickListener {
            openSetupCampaignPopup()
        }
        binding.btnMoreOptions.setOnClickListener { view ->
            showMoreOptionsMenu(view)
        }
    }

    private fun openSetupCampaignPopup(campaignId: String? = null) {
        SetupCampaignFragment.newInstance(campaignId)
            .show(childFragmentManager, SetupCampaignFragment.TAG)
    }

    private fun showMoreOptionsMenu(view: View) {
        val popup = PopupMenu(requireContext(), view)
        popup.menu.add(0, 2, 0, "Settings")
        
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {

                2 -> {
                    findNavController().navigate(R.id.action_profile_to_settings)
                    true
                }
                else -> false
            }
        }
        popup.show()
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
                    state.creatorProfile?.motto?.takeIf { it.isNotBlank() }
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
                    binding.tvStatsFollowers.text = state.campaigns.size.toString()
                    binding.tvStatsDeals.text = state.deals.count { 
                        it.status == com.aura.app.utils.Constants.STATUS_PENDING || 
                        it.status == com.aura.app.utils.Constants.STATUS_ACCEPTED 
                    }.toString()
                    binding.tvStatsRating.text = String.format(java.util.Locale.US, "%.1f", state.brandProfile?.averageRating ?: 0.0)
                    binding.tvStatsFollowersLabel.text = "CAMPAIGNS"
                    binding.tvStatsDealsLabel.text = "DEALS"
                    binding.tvStatsRatingLabel.text = "RATING"
                } else {
                    binding.tvStatsFollowers.text = state.creatorProfile?.youtubeTotalViews?.let { formatCount(it) } ?: "0"
                    binding.tvStatsDeals.text = state.deals.count { 
                        it.status == com.aura.app.utils.Constants.STATUS_COMPLETED 
                    }.toString()
                    binding.tvStatsRating.text = String.format(java.util.Locale.US, "%.1f", state.creatorProfile?.averageRating ?: 0.0)
                    binding.tvStatsFollowersLabel.text = "VIEWS"
                    binding.tvStatsDealsLabel.text = "DEALS"
                    binding.tvStatsRatingLabel.text = "RATING"
                }

                binding.layoutStatsRating.setOnClickListener {
                    val bundle = Bundle().apply {
                        putString("revieweeId", user.userId)
                        putString("displayName", user.displayName)
                        putFloat("averageRating", (if (isBrand) state.brandProfile?.averageRating else state.creatorProfile?.averageRating)?.toFloat() ?: 0f)
                        putLong("totalReviews", (if (isBrand) state.brandProfile?.totalReviews else state.creatorProfile?.totalReviews) ?: 0L)
                    }
                    findNavController().navigate(R.id.action_profile_to_userReviews, bundle)
                }

                binding.btnMoreOptions.visibility =
                    if (state.isOwner) View.VISIBLE else View.GONE

                binding.btnSendDeal.visibility =
                    if (!state.isOwner && state.viewerRole == "brand" && user.role == "creator") View.VISIBLE else View.GONE

                binding.btnSendDeal.setOnClickListener {
                    val viewerId = state.viewerId ?: return@setOnClickListener
                    SelectCampaignBottomSheet.newInstance(viewerId, user.userId)
                        .show(childFragmentManager, SelectCampaignBottomSheet.TAG)
                }

                // Section Visibility: Portfolio for Creators, Campaigns for Brands
                binding.layoutPortfolioHeader.visibility = if (!isBrand) View.VISIBLE else View.GONE
                binding.layoutCampaignsHeader.visibility = if (isBrand) View.VISIBLE else View.GONE
                
                binding.btnAddPortfolio.visibility =
                    if (state.isOwner && !isBrand) View.VISIBLE else View.GONE
                binding.btnAddCampaign.visibility =
                    if (state.isOwner && isBrand) View.VISIBLE else View.GONE

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
                    binding.cgTags.visibility = View.GONE
                } else {
                    binding.cgTags.visibility = View.VISIBLE
                    binding.cgTags.removeAllViews()
                    tags.forEach { tag ->
                        val chip = com.google.android.material.chip.Chip(requireContext())
                        chip.text = tag
                        chip.isClickable = false
                        chip.isCheckable = false
                        chip.setChipBackgroundColorResource(R.color.colorSurfaceVariant)
                        chip.setTextColor(requireContext().getColor(R.color.colorOnSurfaceVariant))
                        chip.textSize = 10f
                        chip.chipStrokeWidth = 1f
                        chip.setChipStrokeColorResource(R.color.colorOutlineVariant)
                        chip.ensureAccessibleTouchTarget(10)
                        binding.cgTags.addView(chip)
                    }
                }

                // Campaigns (Brands)
                if (isBrand) {
                    campaignAdapter.submitList(state.campaigns)
                    val hasCampaigns = state.campaigns.isNotEmpty()
                    binding.rvCampaigns.visibility = if (hasCampaigns) View.VISIBLE else View.GONE
                    binding.tvNoCampaigns.visibility = if (!hasCampaigns) View.VISIBLE else View.GONE
                }

                // Portfolio (Creators)
                if (!isBrand) {
                    portfolioAdapter.onDeleteClick = if (state.isOwner) { item ->
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
                    binding.tvNoPortfolio.visibility = if (!hasPortfolio) View.VISIBLE else View.GONE
                }
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
    private fun formatCount(count: Long): String {
        return when {
            count >= 1_000_000 -> String.format("%.1fM", count / 1_000_000.0)
            count >= 1_000 -> String.format("%.1fK", count / 1_000.0)
            else -> count.toString()
        }
    }
}

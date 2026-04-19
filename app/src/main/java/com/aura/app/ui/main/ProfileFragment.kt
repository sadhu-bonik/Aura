package com.aura.app.ui.main

import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
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
import androidx.recyclerview.widget.GridLayoutManager
import com.aura.app.adapters.PortfolioAdapter
import com.aura.app.databinding.FragmentProfileBinding
import com.bumptech.glide.Glide
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
                            Toast.makeText(requireContext(), "Video uploaded!", Toast.LENGTH_SHORT).show()
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

        // Hand off to ViewModel
        viewModel.uploadPortfolioVideo(
            videoUri = uri,
            mimeType = mimeType,
            fileName = fileName,
            durationSec = durationSec,
        )
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
                binding.tvProfileName.text = user.displayName.ifBlank { user.email }
                binding.tvProfileBio.text = user.role.replaceFirstChar { it.uppercase() }

                binding.btnAddPortfolio.visibility =
                    if (user.role == "creator") View.VISIBLE else View.GONE

                if (user.profileImageUrl.isNotEmpty()) {
                    Glide.with(this)
                        .load(user.profileImageUrl)
                        .centerCrop()
                        .into(binding.ivProfilePic)
                }

                portfolioAdapter.submitList(state.portfolio)

                val hasPortfolio = state.portfolio.isNotEmpty()
                binding.rvPortfolio.visibility = if (hasPortfolio) View.VISIBLE else View.GONE
                binding.tvNoPortfolio.visibility =
                    if (hasPortfolio || user.role != "creator") View.GONE else View.VISIBLE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

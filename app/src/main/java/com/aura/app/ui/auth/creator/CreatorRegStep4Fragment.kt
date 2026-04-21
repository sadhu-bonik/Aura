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
import com.aura.app.databinding.FragmentCreatorRegStep4Binding
import com.aura.app.databinding.ItemPortfolioVideoBinding
import com.aura.app.ui.auth.RegistrationViewModel
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.load.resource.bitmap.CenterCrop

class CreatorRegStep4Fragment : Fragment() {

    private var _binding: FragmentCreatorRegStep4Binding? = null
    private val binding get() = _binding!!

    private val registrationViewModel: RegistrationViewModel by activityViewModels()

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
        _binding = FragmentCreatorRegStep4Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupObservers()

        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }

        binding.cardAddVideo.setOnClickListener {
            if (registrationViewModel.portfolioVideoUris.size >= 10) {
                Toast.makeText(requireContext(), getString(R.string.toast_portfolio_full), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            videoPickerLauncher.launch("video/*")
        }

        binding.btnNavBack.setOnClickListener { findNavController().navigateUp() }

        binding.btnNavFinish.setOnClickListener {
            if (registrationViewModel.portfolioVideoUris.isEmpty()) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.toast_portfolio_video_required),
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }
            registrationViewModel.completeRegistration(requireContext())
        }

        // Restore list after rotation
        refreshAdapter()
    }

    private fun setupRecyclerView() {
        binding.rvPortfolioVideos.apply {
            layoutManager = GridLayoutManager(requireContext(), 3)
            adapter = pendingAdapter
        }
    }

    private fun setupObservers() {
        registrationViewModel.pendingVideoCount.observe(viewLifecycleOwner) { count ->
            refreshAdapter()
            val hasVideos = count > 0
            binding.rvPortfolioVideos.visibility = if (hasVideos) View.VISIBLE else View.GONE
            binding.layoutVideoSlotsPreview.visibility = if (hasVideos) View.GONE else View.VISIBLE
            // Dim the Finish button and show hint when no videos are staged
            binding.btnNavFinish.alpha = if (hasVideos) 1.0f else 0.4f
        }

        registrationViewModel.registrationSuccess.observe(viewLifecycleOwner) { success ->
            if (success) {
                registrationViewModel.resetRegistrationSuccess()
                findNavController().navigate(R.id.action_creator_finish_to_home)
            }
        }

        registrationViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.btnNavFinish.isEnabled = !isLoading
            binding.cardAddVideo.isEnabled = !isLoading
            if (isLoading) {
                binding.pbVideo1.visibility = View.VISIBLE
            } else {
                binding.pbVideo1.visibility = View.GONE
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
            // Extract first frame from local video URI
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

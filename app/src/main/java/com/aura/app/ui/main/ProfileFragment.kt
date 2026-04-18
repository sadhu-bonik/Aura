package com.aura.app.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.aura.app.databinding.FragmentProfileBinding
import com.bumptech.glide.Glide
import kotlinx.coroutines.launch

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProfileViewModel by viewModels {
        ProfileViewModel.Factory(requireContext())
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

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state -> render(state) }
            }
        }
    }

    private fun render(state: ProfileUiState) {
        when (state) {
            is ProfileUiState.Loading -> {
                binding.tvProfileName.text = ""
                binding.tvProfileBio.text = ""
            }
            is ProfileUiState.Error -> {
                binding.tvProfileName.text = state.message
                binding.tvProfileBio.text = ""
            }
            is ProfileUiState.Success -> {
                val user = state.user
                binding.tvProfileName.text = user.displayName.ifBlank { user.email }
                // bio lives in creatorProfiles/brandProfiles — show email role as subtitle for now
                binding.tvProfileBio.text = user.role.replaceFirstChar { it.uppercase() }

                if (user.profileImageUrl.isNotEmpty()) {
                    Glide.with(this)
                        .load(user.profileImageUrl)
                        .centerCrop()
                        .into(binding.ivProfilePic)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

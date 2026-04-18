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
import com.aura.app.databinding.FragmentCreatorRegStep4Binding
import com.aura.app.ui.auth.RegistrationViewModel

class CreatorRegStep4Fragment : Fragment() {
    private var _binding: FragmentCreatorRegStep4Binding? = null
    private val binding get() = _binding!!

    private val registrationViewModel: RegistrationViewModel by activityViewModels()

    private val videoPickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            registrationViewModel.portfolioVideoUri = it
            // Show the processing state as requested
            binding.pbVideo1.visibility = View.VISIBLE
            // Simulate processing finish after a delay for UI demo
            binding.pbVideo1.postDelayed({
                binding.pbVideo1.visibility = View.GONE
                // Ideally we'd show a video thumbnail here
                binding.cardVideoSlot1.setBackgroundResource(R.color.colorSurfaceContainerHighest)
            }, 2000)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCreatorRegStep4Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupObservers()

        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }
        
        binding.cardAddVideo.setOnClickListener { 
            videoPickerLauncher.launch("video/*")
        }

        binding.btnNavBack.setOnClickListener { findNavController().navigateUp() }
        
        binding.btnNavFinish.setOnClickListener {
            registrationViewModel.completeRegistration(requireContext())
        }
    }

    private fun setupObservers() {
        registrationViewModel.registrationSuccess.observe(viewLifecycleOwner) { success ->
            if (success) {
                findNavController().navigate(R.id.action_creator_finish_to_home)
            }
        }

        registrationViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.btnNavFinish.isEnabled = !isLoading
            binding.btnNavFinish.alpha = if (isLoading) 0.5f else 1.0f
        }

        registrationViewModel.error.observe(viewLifecycleOwner) { errorMsg ->
            if (errorMsg != null) {
                android.widget.Toast.makeText(requireContext(), errorMsg, android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}

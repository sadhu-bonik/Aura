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

class CreatorRegStep2Fragment : Fragment() {
    private var _binding: FragmentCreatorRegStep2Binding? = null
    private val binding get() = _binding!!

    private val registrationViewModel: RegistrationViewModel by activityViewModels()

    private val photoPickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            registrationViewModel.profileImageUri = it
            // Update UI to show selected image
            // In a real app, we'd use Glide/Coil to show the URI in an ImageView
            // For now, let's just show a success hint or update the upload zone
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCreatorRegStep2Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Pre-fill
        binding.etMotto.setText(registrationViewModel.creatorMotto)
        binding.etBio.setText(registrationViewModel.creatorBio)
        binding.etYoutube.setText(registrationViewModel.youtubeLink)

        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }
        
        binding.layoutPhotoUpload.setOnClickListener { 
            photoPickerLauncher.launch("image/*")
        }

        binding.layoutBottomNav.btnNavBack.setOnClickListener { findNavController().navigateUp() }
        binding.layoutBottomNav.btnNavNext.setOnClickListener {
            // Save to ViewModel
            registrationViewModel.creatorMotto = binding.etMotto.text.toString().trim()
            registrationViewModel.creatorBio = binding.etBio.text.toString().trim()
            registrationViewModel.youtubeLink = binding.etYoutube.text.toString().trim()

            findNavController().navigate(R.id.action_creator_step2_to_step3)
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}

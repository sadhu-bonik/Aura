package com.aura.app.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import com.aura.app.R
import com.aura.app.databinding.LayoutAddVideoBottomSheetBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class AddVideoBottomSheet : BottomSheetDialogFragment() {

    private var _binding: LayoutAddVideoBottomSheetBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = LayoutAddVideoBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.cardGallery.setOnClickListener {
            setFragmentResult(REQUEST_KEY, bundleOf(ACTION_KEY to ACTION_GALLERY))
            dismiss()
        }

        binding.cardCamera.setOnClickListener {
            setFragmentResult(REQUEST_KEY, bundleOf(ACTION_KEY to ACTION_CAMERA))
            dismiss()
        }
    }

    override fun getTheme(): Int = R.style.Theme_Aura_BottomSheetDialog

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "AddVideoBottomSheet"
        const val REQUEST_KEY = "add_video_request"
        const val ACTION_KEY = "action"
        const val ACTION_GALLERY = "gallery"
        const val ACTION_CAMERA = "camera"
    }
}

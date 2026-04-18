package com.aura.app.ui.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import com.aura.app.databinding.BottomSheetAttachmentPickerBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class AttachmentPickerSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetAttachmentPickerBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = BottomSheetAttachmentPickerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.llPickMedia.setOnClickListener {
            setFragmentResult(REQUEST_KEY, bundleOf(KEY_TYPE to TYPE_MEDIA))
            dismiss()
        }

        binding.llPickFile.setOnClickListener {
            setFragmentResult(REQUEST_KEY, bundleOf(KEY_TYPE to TYPE_FILE))
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "AttachmentPickerSheet"
        const val REQUEST_KEY = "attachment_picker_request"
        const val KEY_TYPE = "pick_type"
        const val TYPE_MEDIA = "media"
        const val TYPE_FILE = "file"
    }
}

package com.aura.app.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import com.aura.app.R
import com.aura.app.databinding.FragmentConfirmVideoUploadBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class ConfirmVideoUploadBottomSheet : BottomSheetDialogFragment() {

    private var _binding: FragmentConfirmVideoUploadBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentConfirmVideoUploadBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val uriString = arguments?.getString(ARG_URI) ?: ""
        val fileName = arguments?.getString(ARG_FILE_NAME) ?: ""
        val mimeType = arguments?.getString(ARG_MIME_TYPE) ?: ""
        val durationSec = arguments?.getLong(ARG_DURATION_SEC) ?: 0L

        // Prefill default file name natively mapped
        if (fileName.isNotEmpty()) {
            binding.etVideoTitle.setText(fileName)
        }

        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        binding.btnUpload.setOnClickListener {
            val titleText = binding.etVideoTitle.text.toString().trim()
            val finalTitle = titleText.ifEmpty { fileName }
            val descriptionText = binding.etVideoDescription.text.toString().trim()

            val bundle = bundleOf(
                RESULT_KEY_URI to uriString,
                RESULT_KEY_TITLE to finalTitle,
                RESULT_KEY_DESC to descriptionText,
                RESULT_KEY_MIME to mimeType,
                RESULT_KEY_DURATION to durationSec
            )

            setFragmentResult(REQUEST_KEY, bundle)
            dismiss()
        }
    }

    override fun getTheme(): Int = R.style.Theme_Aura_BottomSheetDialog

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "ConfirmVideoUploadBottomSheet"
        const val REQUEST_KEY = "confirm_video_upload"

        const val ARG_URI = "arg_uri"
        const val ARG_FILE_NAME = "arg_file_name"
        const val ARG_MIME_TYPE = "arg_mime_type"
        const val ARG_DURATION_SEC = "arg_duration_sec"

        const val RESULT_KEY_URI = "res_uri"
        const val RESULT_KEY_TITLE = "res_title"
        const val RESULT_KEY_DESC = "res_desc"
        const val RESULT_KEY_MIME = "res_mime"
        const val RESULT_KEY_DURATION = "res_duration"

        fun newInstance(uri: String, fileName: String, mimeType: String, durationSec: Long): ConfirmVideoUploadBottomSheet {
            val fragment = ConfirmVideoUploadBottomSheet()
            fragment.arguments = bundleOf(
                ARG_URI to uri,
                ARG_FILE_NAME to fileName,
                ARG_MIME_TYPE to mimeType,
                ARG_DURATION_SEC to durationSec
            )
            return fragment
        }
    }
}

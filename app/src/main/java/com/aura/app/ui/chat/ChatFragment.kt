package com.aura.app.ui.chat

import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.aura.app.R
import com.aura.app.databinding.FragmentChatBinding
import com.aura.app.utils.Constants
import com.aura.app.utils.StubSession
import com.bumptech.glide.Glide
import com.google.android.material.snackbar.Snackbar

class ChatFragment : Fragment() {

    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ChatViewModel by viewModels()
    private lateinit var messageAdapter: MessageAdapter

    private val dealId: String by lazy { arguments?.getString("dealId") ?: "" }

    private val pickMedia = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> uri?.let { handleAttachment(it) } }

    private val pickFile = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { handleAttachment(it) } }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        messageAdapter = MessageAdapter(
            currentUserId = StubSession.userId(),
            onVideoClick = { videoUrl ->
                findNavController().navigate(
                    R.id.action_chatFragment_to_videoPlayerFragment,
                    Bundle().apply { putString("videoUrl", videoUrl) }
                )
            }
        )

        binding.rvMessages.layoutManager = LinearLayoutManager(requireContext()).apply {
            stackFromEnd = true
        }
        binding.rvMessages.adapter = messageAdapter

        binding.ivBack.setOnClickListener { findNavController().navigateUp() }

        val openInfo = View.OnClickListener {
            CampaignInfoBottomSheet.newInstance(dealId)
                .show(childFragmentManager, CampaignInfoBottomSheet.TAG)
        }
        binding.cvChatBanner.setOnClickListener(openInfo)
        binding.ivInfo.setOnClickListener(openInfo)

        binding.etMessage.doAfterTextChanged { text ->
            binding.btnSend.isEnabled = !text.isNullOrBlank() && viewModel.isUploading.value != true
        }

        binding.btnSend.setOnClickListener {
            val content = binding.etMessage.text?.toString() ?: return@setOnClickListener
            val receiverId = viewModel.resolveReceiverId(StubSession.userId())
            viewModel.sendMessage(content, StubSession.userId(), receiverId)
            binding.etMessage.text?.clear()
        }

        binding.btnAttach.setOnClickListener {
            AttachmentPickerSheet().show(childFragmentManager, AttachmentPickerSheet.TAG)
        }

        childFragmentManager.setFragmentResultListener(AttachmentPickerSheet.REQUEST_KEY, viewLifecycleOwner) { _, bundle ->
            when (bundle.getString(AttachmentPickerSheet.KEY_TYPE)) {
                AttachmentPickerSheet.TYPE_MEDIA ->
                    pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
                AttachmentPickerSheet.TYPE_FILE ->
                    pickFile.launch("*/*")
            }
        }

        observeViewModel()
        viewModel.load(dealId, StubSession.userId())
    }

    override fun onResume() {
        super.onResume()
        viewModel.markAsRead(StubSession.userId())
    }

    private fun handleAttachment(uri: Uri) {
        val mimeType = requireContext().contentResolver.getType(uri) ?: "application/octet-stream"
        val fileName = resolveFileName(uri)
        val receiverId = viewModel.resolveReceiverId(StubSession.userId())
        viewModel.sendAttachment(uri, mimeType, fileName, StubSession.userId(), receiverId)
    }

    private fun resolveFileName(uri: Uri): String =
        requireContext().contentResolver.query(
            uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null
        )?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        } ?: "file"

    private fun observeViewModel() {
        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            binding.pbLoading.isVisible = loading
            binding.rvMessages.isVisible = !loading
        }

        viewModel.isUploading.observe(viewLifecycleOwner) { uploading ->
            binding.layoutUploadOverlay.isVisible = uploading
            binding.btnAttach.isEnabled = !uploading
            binding.btnSend.isEnabled = !uploading && !binding.etMessage.text.isNullOrBlank()
            binding.etMessage.isEnabled = !uploading
        }

        viewModel.otherUser.observe(viewLifecycleOwner) { user ->
            binding.tvOtherName.text = user?.displayName ?: ""
            Glide.with(this)
                .load(user?.profileImageUrl)
                .circleCrop()
                .placeholder(R.drawable.ic_launcher_foreground)
                .into(binding.ivOtherAvatar)
        }

        viewModel.deal.observe(viewLifecycleOwner) { deal ->
            binding.tvDealTitleBanner.text = deal.title
            binding.llInputBar.isVisible = deal.status != Constants.STATUS_COMPLETED
        }

        viewModel.messages.observe(viewLifecycleOwner) { messages ->
            messageAdapter.submitList(messages) {
                if (messages.isNotEmpty()) binding.rvMessages.scrollToPosition(messages.size - 1)
            }
            binding.layoutEmpty.isVisible = messages.isEmpty() && viewModel.isLoading.value != true
            binding.rvMessages.isVisible = messages.isNotEmpty()
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            if (error != null) {
                binding.layoutEmpty.isVisible = false
                binding.rvMessages.isVisible = false
            }
        }

        viewModel.sendError.observe(viewLifecycleOwner) { error ->
            if (error != null) {
                Snackbar.make(binding.root, R.string.error_upload_failed, Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

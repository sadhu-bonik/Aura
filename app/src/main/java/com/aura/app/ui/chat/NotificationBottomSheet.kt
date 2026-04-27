package com.aura.app.ui.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.aura.app.R
import com.aura.app.data.model.Notification
import com.aura.app.databinding.FragmentNotificationsBinding
import com.aura.app.ui.main.NotificationViewModel
import com.aura.app.utils.rootNavController
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

/**
 * NotificationBottomSheet — shows the in-app notification inbox.
 *
 * Clears the badge when opened by calling NotificationViewModel.markAllRead().
 * Tapping a notification navigates to the relevant deal's chat.
 */
class NotificationBottomSheet : BottomSheetDialogFragment() {

    private var _binding: FragmentNotificationsBinding? = null
    private val binding get() = _binding!!

    // Use activityViewModels so the same instance is shared with HomeContainerFragment's badge
    private val notifViewModel: NotificationViewModel by activityViewModels()

    private lateinit var adapter: NotificationAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = NotificationAdapter { notif -> onNotifTapped(notif) }
        binding.rvNotifications.layoutManager = LinearLayoutManager(requireContext())
        binding.rvNotifications.adapter = adapter

        notifViewModel.notifications.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list)
            binding.layoutNotifEmpty.isVisible = list.isEmpty()
            binding.rvNotifications.isVisible = list.isNotEmpty()
        }

        binding.tvMarkAllRead.setOnClickListener {
            notifViewModel.markAllRead()
        }
    }

    private fun onNotifTapped(notif: Notification) {
        // Mark this single notification as read so the badge count stays accurate.
        if (!notif.read) notifViewModel.markRead(notif.notifId)

        if (notif.dealId.isNotBlank()) {
            dismiss()
            rootNavController().navigate(
                R.id.action_homeContainer_to_chat,
                bundleOf(
                    "dealId" to notif.dealId,
                    // ChatFragment opens ReviewFlow automatically when this flag is set.
                    "openReviewPopup" to notif.openReviewPopup,
                ),
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance() = NotificationBottomSheet()
    }
}

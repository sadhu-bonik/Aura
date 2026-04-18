package com.aura.app.ui.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.aura.app.R
import com.aura.app.databinding.FragmentActiveDealsBinding
import com.aura.app.utils.Constants
import com.aura.app.utils.StubSession

class ActiveDealsFragment : Fragment() {

    private var _binding: FragmentActiveDealsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ActiveDealsViewModel by viewModels()
    private lateinit var adapter: ActiveDealAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentActiveDealsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = ActiveDealAdapter(
            onDealClick = { item ->
                findNavController().navigate(
                    R.id.action_activeDealsFragment_to_chatFragment,
                    android.os.Bundle().apply { putString("dealId", item.deal.dealId) }
                )
            }
        )

        binding.rvActiveDeals.layoutManager = LinearLayoutManager(requireContext())
        binding.rvActiveDeals.adapter = adapter

        binding.tbActiveDeals.setNavigationOnClickListener { findNavController().navigateUp() }

        binding.tbActiveDeals.inflateMenu(R.menu.menu_active_deals)
        updateSwitchMenuTitle()
        binding.tbActiveDeals.setOnMenuItemClickListener { menuItem ->
            if (menuItem.itemId == R.id.action_switch_user) {
                StubSession.switchRole()
                val msg = if (StubSession.role() == Constants.ROLE_BRAND) {
                    getString(R.string.toast_switched_to_brand)
                } else {
                    getString(R.string.toast_switched_to_creator)
                }
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                updateSwitchMenuTitle()
                viewModel.load(StubSession.userId(), StubSession.role())
                true
            } else false
        }

        binding.btnRetry.setOnClickListener {
            viewModel.load(StubSession.userId(), StubSession.role())
        }

        observeViewModel()
        viewModel.load(StubSession.userId(), StubSession.role())
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            binding.pbLoading.isVisible = loading
            binding.rvActiveDeals.isVisible = !loading
        }

        viewModel.items.observe(viewLifecycleOwner) { items ->
            adapter.submitList(items)
            binding.layoutEmpty.isVisible = items.isEmpty()
            binding.rvActiveDeals.isVisible = items.isNotEmpty()
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            val hasError = error != null
            binding.layoutError.isVisible = hasError
            binding.rvActiveDeals.isVisible = !hasError
            binding.tvErrorMessage.text = error ?: getString(R.string.error_active_deals)
        }
    }

    private fun updateSwitchMenuTitle() {
        val item = binding.tbActiveDeals.menu.findItem(R.id.action_switch_user) ?: return
        item.title = if (StubSession.role() == Constants.ROLE_CREATOR) {
            getString(R.string.menu_switch_to_brand)
        } else {
            getString(R.string.menu_switch_to_creator)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

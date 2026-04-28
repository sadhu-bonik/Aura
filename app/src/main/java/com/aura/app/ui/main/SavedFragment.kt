package com.aura.app.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aura.app.R
import kotlinx.coroutines.launch

class SavedFragment : Fragment() {

    private val viewModel: SavedViewModel by viewModels { SavedViewModel.Factory() }
    private lateinit var adapter: SavedAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_saved, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val rv = view.findViewById<RecyclerView>(R.id.rv_saved_creators)
        val progressBar = view.findViewById<ProgressBar>(R.id.progress_bar)
        val emptyState = view.findViewById<LinearLayout>(R.id.layout_empty_state)

        adapter = SavedAdapter { creator ->
            val bundle = Bundle().apply {
                putString("creatorId", creator.userId)
            }
            findNavController().navigate(R.id.action_saved_to_creator_profile, bundle)
        }

        rv.layoutManager = GridLayoutManager(requireContext(), 2)
        rv.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                val updateVisibility = {
                    val creators = viewModel.uiState.value ?: emptyList()
                    val loading = viewModel.isLoading.value ?: true

                    if (creators.isEmpty() && !loading) {
                        emptyState.visibility = View.VISIBLE
                        rv.visibility = View.GONE
                    } else if (creators.isNotEmpty()) {
                        emptyState.visibility = View.GONE
                        rv.visibility = View.VISIBLE
                    }
                }

                viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
                    progressBar.visibility = if (loading) View.VISIBLE else View.GONE
                    updateVisibility()
                }

                viewModel.uiState.observe(viewLifecycleOwner) { creators ->
                    adapter.submitList(creators)
                    updateVisibility()
                }
            }
        }

        viewModel.loadSavedCreators()
    }
}

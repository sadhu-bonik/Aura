package com.aura.app.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.aura.app.databinding.FragmentDiscoverBinding

class DiscoverFragment : Fragment() {

    private var _binding: FragmentDiscoverBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDiscoverBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Section Header
        binding.incSectionHeader.tvSectionTitle.text = "Component Gallery"
        
        // Stat Pill
        binding.incStatPill.tvStatValue.text = "42"
        binding.incStatPill.tvStatLabel.text = "COMPONENTS"
        
        // Creator Card Horizontal
        binding.incCreatorCard.tvCreatorName.text = "Jessica Vogue"
        binding.incCreatorCard.tvCreatorNiche.text = "Fashion"
        // (Glide can be added for image later)
        
        // Empty State
        view.findViewById<android.widget.TextView>(com.aura.app.R.id.tv_empty_title)?.text = "Nothing here yet"
        view.findViewById<android.widget.TextView>(com.aura.app.R.id.tv_empty_subtitle)?.text = "Check back later"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

package com.aura.app.utils

import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.aura.app.R

fun Fragment.rootNavController(): NavController =
    NavHostFragment.findNavController(
        requireActivity().supportFragmentManager.findFragmentById(R.id.nav_host_fragment)!!
    )

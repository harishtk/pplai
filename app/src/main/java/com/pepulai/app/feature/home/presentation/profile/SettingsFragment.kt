package com.pepulai.app.feature.home.presentation.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.pepulai.app.R
import com.pepulai.app.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentSettingsBinding.bind(view)

        binding.bindState()
    }

    private fun FragmentSettingsBinding.bindState() {
        toolbarIncluded.apply {
            toolbarNavigationIcon.isVisible = true
            toolbarNavigationIcon.setOnClickListener {
                try { findNavController().navigateUp() }
                catch (ignore: Exception) {}
            }

            toolbarTitle.text = getString(R.string.label_settings)
        }
    }



}
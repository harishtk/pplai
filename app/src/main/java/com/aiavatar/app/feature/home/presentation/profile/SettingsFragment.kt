package com.aiavatar.app.feature.home.presentation.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.aiavatar.app.MainActivity
import com.aiavatar.app.R
import com.aiavatar.app.databinding.FragmentSettingsBinding
import com.aiavatar.app.di.ApplicationDependencies
import com.aiavatar.app.feature.home.presentation.util.SettingsAdapter
import com.aiavatar.app.feature.home.presentation.util.SettingsItem
import com.aiavatar.app.feature.home.presentation.util.SettingsListType
import com.aiavatar.app.showToast

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

        val settingsListData = listOf<SettingsItem>(
            SettingsItem(settingsListType = SettingsListType.SIMPLE, id = 0, title = "Logout", null, null)
        )

        val settingsCallback = object : SettingsAdapter.Callback {
            override fun onItemClick(position: Int) {
                when (settingsListData[position].id) {
                    0 -> {
                        ApplicationDependencies.getPersistentStore().logout()
                        context?.showToast("Logged out!")
                        (activity as? MainActivity)?.restart()
                    }
                }
            }
        }

        val settingsAdapter = SettingsAdapter(callback = settingsCallback)

        settingsList.adapter = settingsAdapter
        settingsAdapter.submitList(settingsListData)
    }

}
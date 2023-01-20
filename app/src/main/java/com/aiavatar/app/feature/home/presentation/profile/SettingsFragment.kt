package com.aiavatar.app.feature.home.presentation.profile

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
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
            SettingsItem(settingsListType = SettingsListType.SIMPLE, id = 0, title = "FAQs", R.drawable.ic_faq_outline, "Frequently asked questions", true),
            SettingsItem(settingsListType = SettingsListType.SIMPLE, id = 1, title = "Feedback", R.drawable.ic_feedback_outline, "Tell us something you like", true),
            SettingsItem(settingsListType = SettingsListType.SIMPLE, id = 2, title = "Help & Support", R.drawable.ic_helpline_outline, "Our experts will guide you", true),
            SettingsItem(settingsListType = SettingsListType.SIMPLE, id = 3, title = "About", R.drawable.ic_info_outline, "Some little help", true),
            SettingsItem(settingsListType = SettingsListType.SIMPLE, id = 4, title = "Delete my account", R.drawable.ic_info_outline, "Want out? But We will miss you.", true),
            SettingsItem(settingsListType = SettingsListType.SIMPLE, id = 5, title = "Logout", R.drawable.ic_logout_outline, null)
        )

        val settingsCallback = object : SettingsAdapter.Callback {
            override fun onItemClick(position: Int) {
                when (settingsListData[position].id) {
                    5 -> {
                        ApplicationDependencies.getPersistentStore().logout()
                        context?.showToast("Logged out!")
                        (activity as? MainActivity)?.restart()
                    }
                }
            }
        }

        val settingsAdapter = SettingsAdapter(callback = settingsCallback)

        DividerItemDecoration(requireContext(), RecyclerView.VERTICAL).apply {
            AppCompatResources.getDrawable(requireContext(), R.drawable.list_divider_padded)?.also {
                setDrawable(it)
            }
        }.also { settingsList.addItemDecoration(it) }

        settingsList.adapter = settingsAdapter
        settingsAdapter.submitList(settingsListData)
    }

}
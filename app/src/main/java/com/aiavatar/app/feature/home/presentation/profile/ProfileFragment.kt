package com.aiavatar.app.feature.home.presentation.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.aiavatar.app.R
import com.aiavatar.app.databinding.FragmentProfileBinding
import com.aiavatar.app.defaultNavOptsBuilder
import com.aiavatar.app.di.ApplicationDependencies
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class ProfileFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentProfileBinding.bind(view)

        binding.bindState()
    }

    private fun FragmentProfileBinding.bindState() {
        appbarLayout.addOnOffsetChangedListener { appBarLayout, verticalOffset ->
            Timber.d("Offset: $verticalOffset total: ${appBarLayout.totalScrollRange}")
        }

        toolbarSettings.setOnClickListener { gotoSettings() }
        toolbarNavigationIcon.setOnClickListener {
            try {
                findNavController().navigateUp()
            } catch (ignore: Exception) {}
        }
        textUsernameExpanded.text = getString(R.string.username_with_prefix,
            ApplicationDependencies.getPersistentStore().username)
    }

    private fun gotoSettings() {
        findNavController().apply {
            val navOpts = NavOptions.Builder()
                .setEnterAnim(R.anim.slide_in_right)
                .setExitAnim(R.anim.slide_out_right)
                .setPopExitAnim(R.anim.slide_out_right)
                .build()
            navigate(R.id.action_profile_to_settings, null, navOpts)
        }
    }
}
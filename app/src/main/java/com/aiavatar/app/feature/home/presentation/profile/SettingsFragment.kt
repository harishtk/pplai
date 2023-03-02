package com.aiavatar.app.feature.home.presentation.profile

import android.content.DialogInterface
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.res.ResourcesCompat
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import com.aiavatar.app.*
import com.aiavatar.app.analytics.Analytics
import com.aiavatar.app.analytics.AnalyticsLogger
import com.aiavatar.app.commons.presentation.dialog.WebViewPresenterFragment
import com.aiavatar.app.commons.util.setSpinning
import com.aiavatar.app.databinding.FragmentSettingsBinding
import com.aiavatar.app.di.ApplicationDependencies
import com.aiavatar.app.feature.home.presentation.util.SettingsAdapter
import com.aiavatar.app.feature.home.presentation.util.SettingsItem
import com.aiavatar.app.feature.home.presentation.util.SettingsListType
import com.aiavatar.app.viewmodels.UserViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class SettingsFragment : Fragment() {

    @Inject
    lateinit var analyticsLogger: AnalyticsLogger

    private val viewModel: SettingsViewModel by viewModels()
    private val userViewModel: UserViewModel by activityViewModels()

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
                safeCall {
                    findNavController().navigateUp()
                }
                analyticsLogger.logEvent(Analytics.Event.SETTIGNS_BACK_ACTION_CLICK)
            }

            toolbarTitle.text = getString(R.string.label_settings)
        }

        val appName = getString(R.string.app_name)
        val version = "v${BuildConfig.VERSION_NAME}"
        tvVersionInfo.text = getString(R.string.version_info, appName, version)
        tvVersionInfo.setOnClickListener { gotoMarket() }

        val settingsListData = listOf<SettingsItem>(
            SettingsItem(settingsListType = SettingsListType.SIMPLE, id = 0, title = "FAQs", R.drawable.ic_faq_outline, "Frequently asked questions", true),
            SettingsItem(settingsListType = SettingsListType.SIMPLE, id = 1, title = "Feedback", R.drawable.ic_feedback_outline, "Tell us something you like", true),
            SettingsItem(settingsListType = SettingsListType.SIMPLE, id = 2, title = "Help & Support", R.drawable.ic_helpline_outline, "Our experts will guide you", true),
            SettingsItem(settingsListType = SettingsListType.SIMPLE, id = 3, title = "About", R.drawable.ic_info_outline, "Some little help", true),
            // SettingsItem(settingsListType = SettingsListType.SIMPLE, id = 4, title = "Delete my account", R.drawable.ic_info_outline, "Want out? But We will miss you.", true),
            SettingsItem(settingsListType = SettingsListType.SIMPLE, id = 5, title = "Logout", R.drawable.ic_logout_outline, null, hasMore = false)
        )

        val settingsCallback = object : SettingsAdapter.Callback {
            override fun onItemClick(position: Int) {
                when (settingsListData[position].id) {
                    0 -> {
                        openWebPage(FAQ_URL)
                    }
                    1 -> {
                        gotoFeedback()
                    }
                    2 -> {
                        openWebPage(SUPPORT_URL)
                    }
                    4 -> {
                        gotoDeleteAccount()
                    }
                    5 -> {
                        confirmLogout {
                            analyticsLogger.logEvent(Analytics.Event.SETTINGS_LOGOUT_CLICK)
                            analyticsLogger.setUserId(null)

                            userViewModel.logout()
                            ApplicationDependencies.getPersistentStore().logout()
                            context?.showToast("Logged out!")
                            gotoUploadStep1()
                        }
                    }
                    else -> {
                        openWebPage(LANDING_URL)
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

    private fun confirmLogout(cont: () -> Unit) {
        val clickListener: DialogInterface.OnClickListener =
            DialogInterface.OnClickListener { dialog, which ->
                when (which) {
                    DialogInterface.BUTTON_POSITIVE -> {
                        cont()
                    }
                }
                dialog.dismiss()
            }
        MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_App_MaterialDialog)
            .setMessage("Are you sure?")
            .setPositiveButton("YES", clickListener)
            .setNegativeButton("NO", clickListener)
            .show()
    }

    private fun gotoUploadStep1() {
        try {
            findNavController().apply {
                val navOpts = NavOptions.Builder()
                    .setExitAnim(R.anim.slide_bottom)
                    .setEnterAnim(R.anim.slide_up)
                    .setLaunchSingleTop(true)
                    .setPopUpTo(R.id.main_nav_graph, inclusive = true, saveState = false)
                    .build()
                navigate(R.id.upload_step_1, null, navOpts)
            }
        } catch (e: Exception) {
            Timber.e(e)
            (activity as? MainActivity)?.restart()
        }
    }

    private fun gotoWalkThrough() {
        try {
            findNavController().apply {
                val navOpts = NavOptions.Builder()
                    .setExitAnim(R.anim.slide_bottom)
                    .setEnterAnim(R.anim.slide_up)
                    .setLaunchSingleTop(true)
                    .setPopUpTo(R.id.main_nav_graph, inclusive = true, saveState = false)
                    .build()
                navigate(R.id.walkthrough_fragment, null, navOpts)
            }
        } catch (e: Exception) {
            Timber.e(e)
            (activity as? MainActivity)?.restart()
        }
    }

    private fun gotoFeedback() {
        safeCall {
            findNavController().apply {
                val navOptions = defaultNavOptsBuilder()
                    .build()
                navigate(R.id.feedback_form, null, navOptions)
            }
        }
    }

    private fun gotoDeleteAccount() {
        safeCall {
            findNavController().apply {
                val navOptions = defaultNavOptsBuilder()
                    .build()
                navigate(R.id.delete_account, null, navOptions)
            }
        }
    }

    private fun openWebPage(url: String) = safeCall {
        findNavController().apply {
            val args = bundleOf(WebViewPresenterFragment.EXTRA_URL to url)
            val navOpts = defaultNavOptsBuilder()
                .build()

            navigate(R.id.web_view, args, navOpts)
        }
    }

    private fun gotoMarket() {
        safeCall {
            Intent(Intent.ACTION_VIEW, MARKET_URI.toUri()).also { intent ->
                if (intent.resolveActivity(requireActivity().packageManager) != null) {
                    startActivity(intent)
                } else {
                    throw IllegalStateException("Cannot perform this action!")
                }
            }
        }
    }

    companion object {
        private const val FAQ_URL = "https://aiavatars.ai/#FAQ"
        private const val LANDING_URL = "https://aiavatars.ai"
        private const val SUPPORT_URL = "https://aiavatars.ai/support"

        private val MARKET_URI: String = "market://details?id=${BuildConfig.APPLICATION_ID}"
    }

}
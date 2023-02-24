package com.aiavatar.app.feature.home.presentation.landing

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.SpannedString
import android.text.style.UnderlineSpan
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.PopupWindow
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.vectordrawable.graphics.drawable.AnimationUtilsCompat
import com.aiavatar.app.*
import com.aiavatar.app.analytics.Analytics
import com.aiavatar.app.analytics.AnalyticsLogger
import com.aiavatar.app.commons.util.AnimationUtil
import com.aiavatar.app.databinding.FragmentLandingPageBinding
import com.aiavatar.app.di.ApplicationDependencies
import com.aiavatar.app.feature.onboard.presentation.login.LoginFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class LandingPageFragment : Fragment() {

    @Inject
    lateinit var analyticsLogger: AnalyticsLogger

    private var pendingPopupWindow: PopupWindow? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_landing_page, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentLandingPageBinding.bind(view)

        binding.bindState()
        setupObservers()
        handleBackPressed()

        if (!ApplicationDependencies.getPersistentStore().isLandingUserGuideShown) {
            showGuidedSteps(view)
            analyticsLogger.logEvent(Analytics.Event.LANDING_PAGE_PRESENTED)
        }
    }

    private fun FragmentLandingPageBinding.bindState() {

        val sb = SpannableStringBuilder("Explore More")
        sb.setSpan(UnderlineSpan(), 0, sb.length, SpannedString.SPAN_INCLUSIVE_EXCLUSIVE)
        tvExploreMore.setText(sb, TextView.BufferType.SPANNABLE)

        val sb2 = SpannableStringBuilder("Already have an account?")
        sb2.setSpan(UnderlineSpan(), 0, sb2.length, SpannedString.SPAN_INCLUSIVE_EXCLUSIVE)
        btnAlreadyHaveAccount.setText(sb2, TextView.BufferType.SPANNABLE)

        tvExploreMore.setOnClickListener {
            ApplicationDependencies.getPersistentStore().setUploadStepSkipped(true)
            gotoHome()
            analyticsLogger.logEvent(Analytics.Event.LANDING_EXPLORE_MORE_CLICK)
        }

        btnCreateMasterPiece.setOnClickListener {
            ApplicationDependencies.getPersistentStore().apply {
                safeCall {
                    gotoUpload()
                }
                analyticsLogger.logEvent(Analytics.Event.LANDING_CREATE_BTN_CLICK)
            }
        }

        btnAlreadyHaveAccount.setOnClickListener {
            gotoLogin()
            analyticsLogger.logEvent(Analytics.Event.LANDING_ALREADY_HAVE_AN_ACCOUNT_CLICK)
        }
        profileContainer.setOnClickListener {
            gotoLogin()
            analyticsLogger.logEvent(Analytics.Event.LANDING_MENU_ACCOUNT_CLICK)
        }

    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                getNavigationResultFlow<Boolean>(LoginFragment.LOGIN_RESULT)?.collectLatest { isLoggedIn ->
                    if (isLoggedIn != null && isLoggedIn == true) {
                        safeCall { findNavController().navigateUp() }
                    }
                }
            }
        }
    }

    private fun gotoUpload() {
        findNavController().apply {
            val navOpts = defaultNavOptsBuilder()
                .setPopUpTo(R.id.landingPage, inclusive = false, saveState = false)
                .build()
            navigate(R.id.upload_step_1, null, navOpts)
        }
    }

    private fun gotoLogin(from: String? = null) {
        findNavController().apply {
            val args = bundleOf(
                Constant.EXTRA_FROM to from
            )
            val navOpts = defaultNavOptsBuilder()
                .setPopUpTo(R.id.landingPage, inclusive = false, saveState = false)
                .build()
            navigate(R.id.login_fragment, args, navOpts)
        }
    }

    private fun gotoHome() = safeCall {
        findNavController().apply {
            val navOptions = NavOptions.Builder()
                .setPopUpTo(R.id.main_nav_graph, inclusive = true, saveState = false)
                .build()
            navigate(R.id.catalog_list, null, navOptions)
        }
    }

    private fun handleBackPressed() {
        activity?.onBackPressedDispatcher?.addCallback(viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    // Do nothing. The page automatically closes
                    if (pendingPopupWindow?.isShowing == true) {
                        pendingPopupWindow?.dismiss()
                        pendingPopupWindow = null
                    } else {
                        findNavController().apply {
                            if (!navigateUp()) {
                                if (!popBackStack()) {
                                    activity?.finishAffinity()
                                }
                            }
                        }
                    }
                }
            })
    }

    private fun showGuidedSteps(anchorView: View?) {
        showProfileIconGuidedStep(anchorView)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun showProfileIconGuidedStep(anchorView: View?) {
        val layoutInflater = LayoutInflater.from(context)
        val popupView1 = layoutInflater.inflate(R.layout.profile_icon_guide, null)
        popupView1.findViewById<View>(R.id.profile_icon_pulsator).startAnimation(
            AnimationUtils.loadAnimation(requireContext(), R.anim.pulsator)
        )

        val popupWindow = PopupWindow(popupView1, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            .also { pendingPopupWindow = it }
        popupWindow.showAtLocation(anchorView, Gravity.CENTER, 0, 0)

        popupView1.setOnTouchListener { _, _ ->
            popupWindow.dismiss()
            pendingPopupWindow = null
            showExploreMoreGuidedStep(anchorView)
            true
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun showExploreMoreGuidedStep(anchorView: View?) {
        val layoutInflater = LayoutInflater.from(context)
        val popupView1 = layoutInflater.inflate(R.layout.explore_more_guide, null)
        popupView1.findViewById<View>(R.id.guide_explore_more_title).startAnimation(
            AnimationUtils.loadAnimation(requireContext(), R.anim.pulsator_x)
        )

        val popupWindow = PopupWindow(popupView1, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            .also { pendingPopupWindow = it }
        popupWindow.showAtLocation(anchorView, Gravity.CENTER, 0, 0)

        popupView1.setOnTouchListener { _, _ ->
            popupWindow.dismiss()
            pendingPopupWindow = null
            ApplicationDependencies.getPersistentStore().setLandingUserGuideShown()
            true
        }
    }
}
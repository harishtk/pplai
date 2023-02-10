package com.aiavatar.app.feature.home.presentation.catalog

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.PopupWindow
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.aiavatar.app.*
import com.aiavatar.app.commons.util.AnimationUtil.shakeNow
import com.aiavatar.app.commons.util.AnimationUtil.touchInteractFeedback
import com.aiavatar.app.commons.util.HapticUtil
import com.aiavatar.app.core.domain.util.BuenoCacheException
import com.aiavatar.app.databinding.FragmentCatalogBinding
import com.aiavatar.app.di.ApplicationDependencies
import com.aiavatar.app.feature.home.domain.model.Category
import com.aiavatar.app.feature.home.presentation.util.AvatarsAdapter
import com.aiavatar.app.viewmodels.UserViewModel
import com.bumptech.glide.Glide
import com.aiavatar.app.commons.util.loadstate.LoadState
import com.aiavatar.app.core.data.source.local.AppDatabase
import com.aiavatar.app.feature.onboard.presentation.walkthrough.LegalsBottomSheet
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class CatalogFragment : Fragment() {

    private val userViewModel: UserViewModel by activityViewModels()
    private val viewModel: CatalogViewModel by viewModels()

    private var _binding: FragmentCatalogBinding? = null
    private val binding: FragmentCatalogBinding
        get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_catalog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentCatalogBinding.bind(view)

        binding.bindState(
            uiState = viewModel.uiState,
            uiAction = viewModel.accept,
            uiEvent = viewModel.uiEvent
        )

        setupObservers()
        if (!ApplicationDependencies.getPersistentStore().isHomeUserGuideShown) {
            showGuidedSteps(view.rootView)
        }
    }

    private fun FragmentCatalogBinding.bindState(
        uiState: StateFlow<CatalogState>,
        uiAction: (CatalogUiAction) -> Unit,
        uiEvent: SharedFlow<CatalogUiEvent>
    ) {
        viewLifecycleOwner.lifecycleScope.launch {
            uiEvent.collectLatest { event ->
                when (event) {
                    is CatalogUiEvent.ShowToast -> {
                        context?.showToast(event.message.asString(requireContext()))
                    }
                }
            }
        }

        val avatarsAdapterCallback = object : AvatarsAdapter.Callback {
            override fun onItemClick(position: Int, category: Category) {
                gotoCatalogDetail(category)
            }

        }

        val staggeredGridLayoutManager = StaggeredGridLayoutManager(2, RecyclerView.VERTICAL).apply {
            gapStrategy = StaggeredGridLayoutManager.GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS
        }
        catalogList.layoutManager = staggeredGridLayoutManager

        /*val flexBoxLayoutManager = FlexboxLayoutManager(requireContext(), FlexDirection.ROW, FlexWrap.WRAP)*/

        val avatarsAdapter = AvatarsAdapter(
            layoutManager = staggeredGridLayoutManager,
            avatarsAdapterCallback
        )

        val notLoadingFlow = uiState.map { it.loadState }
            .map { it.refresh !is LoadState.Loading }
        val hasErrorsFlow = uiState.map { it.exception != null }
        viewLifecycleOwner.lifecycleScope.launch {
            combine(
                notLoadingFlow,
                hasErrorsFlow,
                Boolean::and
            ).collectLatest { hasError ->
                if (hasError) {
                    val (e, uiErr) = uiState.value.exception to uiState.value.uiErrorText
                    if (e != null) {
                        ifDebug { Timber.e(e) }
                        when (e) {
                            is BuenoCacheException -> {
                                val hours = TimeUnit.MINUTES.toHours(e.minutesAgo).toInt()
                                val minutes = hours - e.minutesAgo.toInt()
                                val timeAgoString = if (hours > 0) {
                                    resources.getQuantityString(R.plurals.hours_ago, hours, hours)
                                } else {
                                    resources.getQuantityString(R.plurals.minutes_ago, minutes, minutes).also { minutesAgoString ->
                                        context?.showToast(
                                            getString(
                                                R.string.cannot_refresh_message,
                                                minutesAgoString
                                            )
                                        )
                                    }
                                }
                                if (BuildConfig.DEBUG) {
                                    getString(R.string.cannot_refresh_message, timeAgoString).apply {
                                        Timber.d("Refresh: $this")
                                    }
                                }
                            }
                            else -> {
                                if (BuildConfig.DEBUG) {
                                    Timber.e(e)
                                }
                                if (uiErr != null) {
                                    context?.showToast(uiErr.asString(requireContext()))
                                }
                            }
                        }
                        uiAction(CatalogUiAction.ErrorShown(e))
                    }
                }
            }
        }

        val loadStateFlow = uiState.map { it.loadState }
            .distinctUntilChangedBy { it.refresh }
        viewLifecycleOwner.lifecycleScope.launch {
            loadStateFlow.collectLatest { loadState ->
                Timber.d("Load state: $loadState")
                progressBar.isVisible = loadState.refresh is LoadState.Loading &&
                        avatarsAdapter.itemCount <= 0
                if (swipeRefreshLayout.isRefreshing) {
                    if (loadState.refresh !is LoadState.Loading) {
                        swipeRefreshLayout.isRefreshing = false
                    }
                }
                if (loadState.refresh is LoadState.Error) {
                    if (loadState.refresh.error !is BuenoCacheException) {
                        retryButton.isVisible = avatarsAdapter.itemCount <= 0
                        HapticUtil.createError(requireContext())
                        retryButton.shakeNow()
                    }
                }
            }
        }

        swipeRefreshLayout.setOnRefreshListener {
            viewModel.refresh(true)
        }

        bindList(
            adapter = avatarsAdapter,
            uiState = uiState
        )

        bindClick()

        bindToolbar()
    }

    private fun FragmentCatalogBinding.bindList(
        adapter: AvatarsAdapter,
        uiState: StateFlow<CatalogState>
    ) {
        catalogList.adapter = adapter

        val catalogListFlow = uiState.map { it.catalogList }
            .distinctUntilChanged()
        viewLifecycleOwner.lifecycleScope.launch {
            catalogListFlow.collectLatest { catalogList ->
                adapter.submitList(catalogList)
            }
        }
    }

    private fun FragmentCatalogBinding.bindClick() {
        btnCreateMasterPiece.setOnClickListener {
            safeCall {
                findNavController().apply {
                    navigate(CatalogFragmentDirections.actionCatalogListToUploadStep1())
                }
            }
        }

        toolbarTitle.setOnClickListener {
            toolbarTitle.touchInteractFeedback()
            safeCall {
                findNavController().apply {
                    navigate(CatalogFragmentDirections.actionCatalogListToUploadStep1())
                }
            }
        }

        retryButton.setOnClickListener { viewModel.refresh(true) }
    }

    private fun FragmentCatalogBinding.bindToolbar() {
        toolbarNavigationIcon.isVisible = false
        toolbarTitle.text = "AI Avatar"

        viewLifecycleOwner.lifecycleScope.launch {
            userViewModel.loginUser.collectLatest { loginUser ->
                if (loginUser != null) {
                    ApplicationDependencies.getPersistentStore().apply {
                        if (isLogged) {
                            /*val initialLetter = ApplicationDependencies.getPersistentStore().username[0].toString().uppercase()
                            profileName.setText(initialLetter)*/
                            profileName.setText(null)
                            Glide.with(profileImage)
                                .load(socialImage)
                                .placeholder(R.drawable.profile_placeholder)
                                .error(R.drawable.profile_placeholder)
                                .into(profileImage)
                        } else {
                            profileName.setText(null)
                            profileImage.setImageResource(R.drawable.ic_account_outline)
                        }
                    }
                } else {
                    profileName.setText(null)
                    profileImage.setImageResource(R.drawable.ic_account_outline)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            ApplicationDependencies.getPersistentStore().apply {
                MainActivity.THEME_MAP[userPreferredTheme]?.let { resId ->
                    ivTheme.setImageResource(resId)
                }
            }
        }

        ivTheme.isVisible = BuildConfig.DEBUG
        ivTheme.setOnClickListener {
            toggleTheme()?.let { resId ->
                ivTheme.setImageResource(resId)
            }
        }

        profileContainer.setOnClickListener {
            gotoProfile()
        }
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            userViewModel.authenticationState.collectLatest { state ->
                Timber.d("Login: authentication state = $state")
            }
        }
    }

    private fun toggleTheme(): Int? {
        ApplicationDependencies.getPersistentStore().apply {
            val newTheme = (userPreferredTheme + 1) % MainActivity.THEME_MAP.size
            setUserPreferredTheme(newTheme)
            val mode = when (newTheme) {
                MainActivity.THEME_MODE_AUTO -> {
                    context?.showToast("Auto")
                    AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                }
                MainActivity.THEME_MODE_DARK -> {
                    context?.showToast("Dark mode")
                    AppCompatDelegate.MODE_NIGHT_YES
                }
                MainActivity.THEME_MODE_LIGHT -> {
                    context?.showToast("Light mode")
                    AppCompatDelegate.MODE_NIGHT_NO
                }
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
            AppCompatDelegate.setDefaultNightMode(mode)
            Timber.d("Theme: $mode")

            return MainActivity.THEME_MAP[newTheme]
        }
    }

    private fun gotoProfile() {
        findNavController().apply {
            val navOpts = NavOptions.Builder()
                .setEnterAnim(R.anim.slide_in_right)
                .setExitAnim(R.anim.slide_out_right)
                .setPopExitAnim(R.anim.slide_out_right)
                .build()
            navigate(R.id.action_catalog_list_to_profile, null, navOpts)
        }
    }

    private fun gotoCatalogDetail(category: Category, cardClickPosition: Int = -1) {
        findNavController().apply {
            val args = Bundle().apply {
                putString(MoreCatalogFragment.ARG_CATALOG_NAME, category.categoryName)
            }
            // args.putParcelable(Constant.EXTRA_DATA, category)
            navigate(R.id.action_catalog_list_to_more_catalog, args)
        }
    }

    private fun showGuidedSteps(anchorView: View?) {
        showProfileIconGuidedStep(anchorView)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun showProfileIconGuidedStep(anchorView: View?) = anchorView?.post {
        val layoutInflater = LayoutInflater.from(context)
        val popupView1 = layoutInflater.inflate(R.layout.profile_icon_guide, null)
        popupView1.findViewById<View>(R.id.profile_icon_pulsator).startAnimation(
            AnimationUtils.loadAnimation(requireContext(), R.anim.pulsator)
        )

        val popupWindow = PopupWindow(popupView1, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        popupWindow.showAtLocation(anchorView, Gravity.CENTER, 0, 0)

        popupView1.findViewById<View>(R.id.guide_profile_container).setOnClickListener {
            popupWindow.dismiss()
            gotoProfile()
        }

        popupView1.setOnTouchListener { _, _ ->
            popupWindow.dismiss()
            ApplicationDependencies.getPersistentStore().setHomeUserGuideShown()
            true
        }
    }

    companion object {
        private const val UI_RENDER_WAIT_TIME = 50L
    }
}
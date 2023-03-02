package com.aiavatar.app.feature.home.presentation.profile

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.PopupWindow
import androidx.activity.OnBackPressedCallback
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.*
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DiffUtil.ItemCallback
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.aiavatar.app.*
import com.aiavatar.app.analytics.Analytics
import com.aiavatar.app.analytics.AnalyticsLogger
import com.aiavatar.app.commons.util.AnimationUtil.shakeNow
import com.aiavatar.app.commons.util.HapticUtil
import com.aiavatar.app.databinding.FragmentProfileBinding
import com.aiavatar.app.databinding.ItemAvatarStatusBinding
import com.aiavatar.app.databinding.ItemModelListBinding
import com.aiavatar.app.di.ApplicationDependencies
import com.aiavatar.app.eventbus.NewNotificationEvent
import com.aiavatar.app.feature.home.presentation.catalog.ModelDetailFragment
import com.aiavatar.app.feature.onboard.presentation.login.LoginFragment
import com.aiavatar.app.viewmodels.UserViewModel
import com.bumptech.glide.Glide
import com.aiavatar.app.commons.util.loadstate.LoadState
import com.aiavatar.app.commons.util.net.NoInternetException
import com.google.android.material.floatingactionbutton.FloatingActionButton
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class ProfileFragment : Fragment() {

    @Inject
    lateinit var analyticsLogger: AnalyticsLogger

    private val viewModel: ProfileViewModel by viewModels()
    private val userViewModel: UserViewModel by activityViewModels()

    private var pendingPopupWindow: PopupWindow? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentProfileBinding.bind(view)

        binding.bindState(
            uiState = viewModel.uiState,
            uiAction = viewModel.accept,
            uiEvent = viewModel.uiEvent
        )

        setupObservers()
        handleBackPressed()

        viewLifecycleOwner.lifecycle.addObserver(object : LifecycleEventObserver {
            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                Timber.d("Profile lifecycle state: $event")
            }
        })
        analyticsLogger.logEvent(Analytics.Event.PROFILE_PAGE_PRESENTED)
    }

    private fun FragmentProfileBinding.bindState(
        uiState: StateFlow<ProfileState>,
        uiAction: (ProfileUiAction) -> Unit,
        uiEvent: SharedFlow<ProfileUiEvent>,
    ) {
        viewLifecycleOwner.lifecycleScope.launch {
            uiEvent.collectLatest { event ->
                when (event) {
                    is ProfileUiEvent.ShowToast -> {
                        context?.showToast(event.message.asString(requireContext()))
                    }
                    is ProfileUiEvent.ShowUserGuide -> {
                        if (event.show) {
                            val shownCount = ApplicationDependencies.getPersistentStore()
                                .profileCreateGuideShownCount
                            if (pendingPopupWindow == null && shownCount < 3) {
                                showProfileCreateModelGuidedStep(root)
                            }
                        } else {
                            hideUserGuideIfShown()
                        }
                    }
                }
            }
        }

        val notLoadingFlow = uiState.map { it.loadState }
            .map { it.refresh !is LoadState.Loading }
            .distinctUntilChanged()

        val hasErrorsFlow = uiState.map { it.exception != null }
            .distinctUntilChanged()
        viewLifecycleOwner.lifecycleScope.launch {
            combine(
                notLoadingFlow,
                hasErrorsFlow,
                Boolean::and
            ).collectLatest { hasError ->
                if (hasError) {
                    val (e, uiErr) = uiState.value.exception to uiState.value.uiErrorText
                    if (e != null) {
                        Timber.d(e)
                        if (uiErr != null) {
                            context?.showToast(uiErr.asString(requireContext()))
                        }
                        uiAction(ProfileUiAction.ErrorShown(e))
                    }
                }
            }
        }

        val callback = object : ModelListAdapter.Callback {
            override fun onItemClick(position: Int, data: ProfileListUiModel.Item) {
                analyticsLogger.logEvent(Analytics.Event.PROFILE_MODEL_ITEM_CLICK)
                val statusId = data.modelListWithModel.modelListItem.statusId
                if (statusId != "0") {
                    gotoAvatarStatus(statusId)
                } else {
                    gotoModelListResult(data.modelListWithModel.model?.id!!)
                }
            }
        }

        swipeRefreshLayout.setOnRefreshListener {
            viewModel.refresh(true)
        }

        val adapter = ModelListAdapter(callback)

        val loadStateFlow = uiState.map { it.loadState }
            .distinctUntilChanged()
        viewLifecycleOwner.lifecycleScope.launch {
            loadStateFlow.collectLatest { loadState ->
                Timber.d("Load state: ${loadState.refresh}")
                // swipeRefreshLayout.isEnabled = loadState.refresh !is LoadState.Loading
                if (loadState.refresh is LoadState.Loading) {
                    errorContainer.isVisible = false
                    emptyListContainer.isVisible = false
                    progressBar.isVisible = adapter.itemCount <= 0
                } else {
                    swipeRefreshLayout.isRefreshing = false
                    // TODO: Show empty list container
                    // emptyListContainer.isVisible = adapter.itemCount <= 0
                    if (loadState.refresh is LoadState.Error) {
                        errorContainer.isVisible = adapter.itemCount <= 0
                        if (loadState.refresh.error !is NoInternetException) {
                            HapticUtil.createError(requireContext())
                        }
                        if (retryButton.isVisible) {
                            retryButton.shakeNow()
                        }
                    }
                    progressBar.isVisible = false
                }

                /*if (swipeRefreshLayout.isRefreshing) {
                    if (loadState.refresh !is LoadState.Loading) {
                        swipeRefreshLayout.isRefreshing = false
                    }
                }*/

                retryButton.isVisible = loadState.refresh is LoadState.Error &&
                        adapter.itemCount <= 0
            }
        }

        bindList(
            adapter = adapter,
            uiState = uiState
        )

        bindClick(
            uiState = uiState
        )

        bindAppbar(
            uiState = uiState
        )
    }

    private fun FragmentProfileBinding.bindList(
        adapter: ModelListAdapter,
        uiState: StateFlow<ProfileState>,
    ) {
        modelListView.adapter = adapter

        val modelListUiModelsFlow = uiState.map { it.profileListUiModels }
            .distinctUntilChanged()
        viewLifecycleOwner.lifecycleScope.launch {
            modelListUiModelsFlow.collectLatest { modelList ->
                adapter.submitList(modelList)

                // emptyListContainer.isVisible = modelList.isEmpty()
            }
        }

        val emptyModelListFlow = modelListUiModelsFlow
            .map { it.isEmpty() }
            .distinctUntilChanged()

        val loadStateFlow = uiState.map { it.loadState }
            .distinctUntilChangedBy { it.refresh }
            .map { it.refresh !is LoadState.Loading }

        viewLifecycleOwner.lifecycleScope.launch {
            combine(
                emptyModelListFlow,
                loadStateFlow,
                Boolean::and
            ).collectLatest { isEmpty ->
                if (isEmpty) {
                    // showProfileCreateModelGuidedStep(view)
                }
            }
        }
    }

    private fun FragmentProfileBinding.bindClick(uiState: StateFlow<ProfileState>) {
        retryButton.setOnClickListener {
            viewModel.refresh(true)
        }

        btnCreate.setOnClickListener {
            gotoUploadSteps()
            analyticsLogger.logEvent(Analytics.Event.PROFILE_CREATE_CLICK)

            hideUserGuideIfShown()
        }

        fabCreate.setOnClickListener {
            gotoUploadSteps()
            analyticsLogger.logEvent(Analytics.Event.PROFILE_CREATE_CLICK)

            hideUserGuideIfShown()
        }
    }

    private fun FragmentProfileBinding.bindAppbar(uiState: StateFlow<ProfileState>) {
        appbarLayout.addOnOffsetChangedListener { appBarLayout, verticalOffset ->
            val delta = (kotlin.math.abs(verticalOffset)) / appBarLayout.totalScrollRange.toFloat()
            // Timber.d("Offset: $verticalOffset total: ${appBarLayout.totalScrollRange} delta = $delta")

            toolbarSettings.rotation = (90F * delta)

            (1.0F - delta).let { scale ->
                profileImageExpanded.scaleX = scale
                profileImageExpanded.scaleY = scale
            }

            if (delta > 0.91f) {
                toolbarTitle.isVisible = true
                textUsernameExpanded.visibility = View.INVISIBLE
            } else {
                toolbarTitle.isVisible = false
                textUsernameExpanded.visibility = View.VISIBLE
            }
        }

        toolbarSettings.setOnClickListener {
            gotoSettings()
            analyticsLogger.logEvent(Analytics.Event.PROFILE_MENU_SETTINGS_BTN_CLICK)

            hideUserGuideIfShown()
        }
        toolbarNavigationIcon.setOnClickListener {
            safeCall {
                findNavController().navigateUp()
            }
            analyticsLogger.logEvent(Analytics.Event.PROFILE_BACK_ACTION_CLICK)

            hideUserGuideIfShown()
        }
        viewLifecycleOwner.lifecycleScope.launch {
            getString(
                R.string.username_with_prefix,
                ApplicationDependencies.getPersistentStore().username
            ).let { formattedUsername ->
                textUsernameExpanded.text = formattedUsername
                toolbarTitle.text = formattedUsername
            }
            ApplicationDependencies.getPersistentStore().apply {
                if (isLogged) {
                    Glide.with(profileImageExpanded)
                        .load(socialImage)
                        .placeholder(R.drawable.profile_placeholder)
                        .error(R.drawable.profile_placeholder)
                        .into(profileImageExpanded)
                } else {
                    profileImageExpanded.setImageResource(R.drawable.profile_placeholder)
                }
            }
        }
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            userViewModel.loginUser.collectLatest { loginUser ->
                Timber.d("Login user: $loginUser")
                if (loginUser == null) {
                    gotoLogin()
                } else {
                    if (loginUser.userId != null) {
                        viewModel.refresh(true)
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                getNavigationResultFlow<Boolean>(LoginFragment.LOGIN_RESULT)?.collectLatest { isLoggedIn ->
                    Timber.d("Login result: $isLoggedIn")
                    if (isLoggedIn != null) {
                        /*if (isLoggedIn != true) {
                            safeCall { findNavController().navigateUp() }
                        }*/
                        if (isLoggedIn == true) {
                            clearNavigationResult<Boolean>(LoginFragment.LOGIN_RESULT)
                        }
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            userViewModel.authenticationState.collectLatest { state ->
                Timber.d("Login: (profile) authentication state = $state")
            }
        }
    }

    private fun handleBackPressed() {
        activity?.onBackPressedDispatcher?.addCallback(viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    // Do nothing. The page automatically closes
                    findNavController().apply {
                        navigateUp()
                    }
                }
            })
    }

    private fun gotoLogin() {
        Timber.d("User login: opening login..")
        findNavController().apply {
            val navOpts = defaultNavOptsBuilder().build()
            val args = Bundle().apply {
                /* 'popup' means previous page, the one who fired it expects the result */
                putString(Constant.EXTRA_FROM, "popup")
                putInt(Constant.EXTRA_POP_ID, R.id.profile)
            }
            navigate(R.id.login_fragment, args, navOpts)
        }
    }

    private fun gotoSettings() {
        findNavController().apply {
            val navOpts = NavOptions.Builder()
                .build()
            navigate(ProfileFragmentDirections.actionProfileToSettings())
        }
    }

    private fun gotoModelListResult(modelId: String) = safeCall {
        findNavController().apply {
            val navOptions = defaultNavOptsBuilder()
                .build()
            val args = Bundle().apply {
                putString(Constant.ARG_MODEL_ID, modelId)
            }
            navigate(R.id.model_list, args, navOptions)
        }
    }

    private fun gotoAvatarStatus(statusId: String) = safeCall {
        findNavController().apply {
            val navOptions = defaultNavOptsBuilder().build()
            val args = Bundle().apply {
                putString(Constant.ARG_STATUS_ID, statusId)
            }
            navigate(R.id.avatar_status, args, navOptions)
        }
    }

    private fun gotoModelDetail(from: String, position: Int, data: ProfileListUiModel.Item) {
        val modelId = data.modelListWithModel.model?.id!!
        try {
            findNavController().apply {
                val navOpts = defaultNavOptsBuilder()
                    .setPopExitAnim(R.anim.slide_out_right)
                    .build()
                val args = Bundle().apply {
                    putString(Constant.EXTRA_FROM, from)
                    putString(ModelDetailFragment.ARG_MODEL_ID, modelId)
                    // putString(ModelDetailFragment.ARG_STATUS_ID, data.modelList.statusId)
                    // putInt(ModelDetailFragment.ARG_JUMP_TO_POSITION, position)
                }
                navigate(R.id.model_detail, args, navOpts)
            }
        } catch (ignore: Exception) {
        }
    }

    private fun gotoUploadSteps() = safeCall {
        findNavController().apply {
            navigate(ProfileFragmentDirections.actionProfileToUploadStep1())
        }
    }

    override fun onStart() {
        super.onStart()
        if (Build.VERSION.SDK_INT > 23) {
            EventBus.getDefault().register(this)
        }
    }

    override fun onResume() {
        super.onResume()
        if (Build.VERSION.SDK_INT <= 23) {
            if (!EventBus.getDefault().isRegistered(this)) {
                EventBus.getDefault().register(this)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        if (Build.VERSION.SDK_INT <= 23) {
            EventBus.getDefault().unregister(this)
        }
    }

    override fun onStop() {
        super.onStop()
        if (Build.VERSION.SDK_INT > 23) {
            EventBus.getDefault().unregister(this)
        }
    }

    @Subscribe
    public fun onNewNotificationEvent(event: NewNotificationEvent) {
        if (event.hint == "avatar_status") {
            viewModel.refresh(true)
        }
    }

    private fun hideUserGuideIfShown() {
        pendingPopupWindow?.dismiss()
        pendingPopupWindow = null
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun showProfileCreateModelGuidedStep(anchorView: View?) = anchorView?.post {
        val layoutInflater = LayoutInflater.from(context)
        val popupView1 = layoutInflater.inflate(R.layout.profile_create_model_guide, null)
        popupView1.findViewById<View>(R.id.profile_icon_pulsator).startAnimation(
            AnimationUtils.loadAnimation(requireContext(), R.anim.pulsator)
        )
        popupView1.findViewById<FloatingActionButton>(R.id.guide_fab_create).setOnClickListener {
            gotoUploadSteps()
            pendingPopupWindow?.dismiss()
            pendingPopupWindow = null
        }

        val popupWindow = PopupWindow(popupView1, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            .also { pendingPopupWindow = it }
        popupWindow.showAtLocation(anchorView, Gravity.CENTER, 0, 0)

        popupView1.setOnTouchListener { _, _ ->
            popupWindow.dismiss()
            pendingPopupWindow = null
            ApplicationDependencies.getPersistentStore().setProfileCreateGuideShown()
            true
        }
    }


    companion object {
        private const val UI_RENDER_WAIT_TIME = 50L
    }
}

class ModelListAdapter(
    private val callback: Callback,
) : ListAdapter<ProfileListUiModel, ViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return when (viewType) {
            VIEW_TYPE_ITEM -> ItemViewHolder.from(parent)
            VIEW_TYPE_THINKING -> AvatarStatusViewHolder.from(parent)
            else -> {
                throw IllegalStateException("Unknown viewType $viewType")
            }
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val model = getItem(position)
        when (model) {
            is ProfileListUiModel.Item -> {
                when (holder) {
                    is ModelListAdapter.ItemViewHolder -> {
                        holder.bind(data = model, callback)
                    }
                    is AvatarStatusViewHolder -> {
                        holder.bind(data = model, callback)
                    }
                }
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (val model = getItem(position)) {
            is ProfileListUiModel.Item -> {
                if (model.modelListWithModel.modelListItem.statusId != "0") {
                    VIEW_TYPE_THINKING
                } else {
                    VIEW_TYPE_ITEM
                }
            }
            else -> {
                throw IllegalStateException("Can't decide a view type for $position")
            }
        }
    }

    class AvatarStatusViewHolder private constructor(
        private val binding: ItemAvatarStatusBinding,
    ) : ViewHolder(binding.root) {

        fun bind(data: ProfileListUiModel.Item, callback: Callback) = with(binding) {
            title.text = "Generating.."
            description.isVisible = false

            itemView.setOnClickListener { callback.onItemClick(adapterPosition, data) }
        }

        companion object {
            fun from(parent: ViewGroup): AvatarStatusViewHolder {
                val itemView = LayoutInflater.from(parent.context).inflate(
                    R.layout.item_avatar_status,
                    parent,
                    false
                )
                val binding = ItemAvatarStatusBinding.bind(itemView)
                return AvatarStatusViewHolder(binding)
            }
        }
    }

    class ItemViewHolder private constructor(
        private val binding: ItemModelListBinding,
    ) : ViewHolder(binding.root) {

        fun bind(data: ProfileListUiModel.Item, callback: Callback) = with(binding) {
            title.text = data.modelListWithModel.model?.name
            description.text = "${data.modelListWithModel.model?.totalCount} creations"
            Glide.with(imageView)
                .load(data.modelListWithModel.model?.latestImage)
                .placeholder(R.drawable.loading_animation)
                .error(R.color.white)
                .into(imageView)

            // toggleSelection(selected)

            imageView.setOnClickListener { callback.onItemClick(adapterPosition, data) }
        }

        companion object {
            fun from(parent: ViewGroup): ItemViewHolder {
                val itemView = LayoutInflater.from(parent.context).inflate(
                    R.layout.item_model_list,
                    parent,
                    false
                )
                val binding = ItemModelListBinding.bind(itemView)
                return ItemViewHolder(binding)
            }
        }
    }

    interface Callback {
        fun onItemClick(position: Int, data: ProfileListUiModel.Item)
    }

    companion object {
        private const val VIEW_TYPE_ITEM = 0
        private const val VIEW_TYPE_THINKING = 1

        private val DIFF_CALLBACK = object : ItemCallback<ProfileListUiModel>() {
            override fun areItemsTheSame(
                oldItem: ProfileListUiModel,
                newItem: ProfileListUiModel,
            ): Boolean {
                return (oldItem is ProfileListUiModel.Item && newItem is ProfileListUiModel.Item &&
                        oldItem.modelListWithModel.model?.id == newItem.modelListWithModel.model?.id)
            }

            override fun areContentsTheSame(
                oldItem: ProfileListUiModel,
                newItem: ProfileListUiModel,
            ): Boolean {
                return false
            }
        }
    }

}
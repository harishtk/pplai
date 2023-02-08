package com.aiavatar.app.feature.home.presentation.create

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ShareCompat
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.CompositePageTransformer
import androidx.viewpager2.widget.MarginPageTransformer
import androidx.viewpager2.widget.ViewPager2
import com.aiavatar.app.*
import com.aiavatar.app.commons.presentation.dialog.SimpleDialog
import com.aiavatar.app.commons.util.HapticUtil
import com.aiavatar.app.commons.util.recyclerview.Recyclable
import com.aiavatar.app.databinding.FragmentAvatarPreviewBinding
import com.aiavatar.app.databinding.ItemScrollerListBinding
import com.aiavatar.app.databinding.LargePresetPreviewBinding
import com.aiavatar.app.feature.home.domain.model.ModelAvatar
import com.aiavatar.app.feature.home.presentation.dialog.EditFolderNameDialog
import com.aiavatar.app.feature.home.presentation.util.AutoCenterLayoutManger
import com.bumptech.glide.Glide
import com.aiavatar.app.commons.util.loadstate.LoadState
import com.aiavatar.app.viewmodels.UserViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.math.abs

@AndroidEntryPoint
class AvatarPreviewFragment : Fragment() {

    private val userViewModel: UserViewModel by viewModels()
    private val viewModel: AvatarPreviewViewModel by viewModels()

    private val storagePermissions: Array<String> = arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    private lateinit var storagePermissionLauncher: ActivityResultLauncher<Array<String>>
    private var mStoragePermissionContinuation: Continuation? = null

    private var isSettingsLaunched = false

    private var jumpToId: Long? = null
    private var jumpToPosition: Int? = null

    private var previousPosition: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        storagePermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result: Map<String, Boolean> ->
                val deniedList: List<String> = result.filter { !it.value }.map { it.key }
                when {
                    deniedList.isNotEmpty() -> {
                        val map = deniedList.groupBy { permission ->
                            if (shouldShowRequestPermissionRationale(permission)) {
                                Constant.PERMISSION_DENIED
                            } else {
                                Constant.PERMISSION_PERMANENTLY_DENIED
                            }
                        }
                        map[Constant.PERMISSION_DENIED]?.let {
                            requireContext().showToast("Storage permission is required to upload photos")
                            // TODO: show storage rationale
                            showStoragePermissionRationale(false)
                        }
                        map[Constant.PERMISSION_PERMANENTLY_DENIED]?.let {
                            requireContext().showToast("Storage permission is required to upload photos")
                            // TODO: show storage rationale permanent
                            showStoragePermissionRationale(true)
                        }
                    }

                    else -> {
                        mStoragePermissionContinuation?.invoke()
                        mStoragePermissionContinuation = null
                    }
                }
            }

        arguments?.apply {
            val modelId = getString(AvatarPreviewFragment.ARG_MODEL_ID, null)
            val statusId = getString(AvatarPreviewFragment.ARG_STATUS_ID, null)
            jumpToId = getLong(AvatarPreviewFragment.ARG_JUMP_TO_ID, -1L)
            // jumpToImageName = getString(ModelDetailFragment.ARG_JUMP_TO_IMAGE_NAME, null)

            Timber.d("Args: model id = $modelId status id = $statusId jumpTo = $jumpToId")

            if (statusId?.isNotBlank() == true) {
                viewModel.setStatusId(statusId)
            }

            viewModel.refresh()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_avatar_preview, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentAvatarPreviewBinding.bind(view)

        // TODO: bind view
        binding.bindState(
            uiState = viewModel.uiState,
            uiAction = viewModel.accept,
            uiEvent = viewModel.uiEvent
        )

        handleBackPressed()
    }

    private fun FragmentAvatarPreviewBinding.bindState(
        uiState: StateFlow<AvatarPreviewState>,
        uiAction: (AvatarPreviewUiAction) -> Unit,
        uiEvent: SharedFlow<AvatarPreviewUiEvent>
    ) {
        viewLifecycleOwner.lifecycleScope.launch {
            uiEvent.collectLatest { event ->
                when (event) {
                    is AvatarPreviewUiEvent.ShowToast -> {
                        context?.showToast(event.message.asString(requireContext()))
                    }
                    is AvatarPreviewUiEvent.ShareLink -> {
                        handleShareLink(event.link)
                    }
                }
            }
        }

        val catalogPresetAdapter = AvatarPreviewPagerAdapter(requireContext())

        val pageTransformer = CompositePageTransformer().apply {
            addTransformer(MarginPageTransformer(80))
            addTransformer(ViewPager2.PageTransformer { page, position ->
                val r = 1 - abs(position)
                page.scaleY = 0.85f + (r * 0.15f)
            })
        }

        catalogPreviewPager.apply {
            adapter = catalogPresetAdapter
            overScrollMode = RecyclerView.OVER_SCROLL_NEVER
            offscreenPageLimit = 3
            clipChildren = false
            clipToPadding = false

            setPageTransformer(pageTransformer)
        }

        /*circleIndicator.setViewPager(catalogPreviewPager)
        catalogPresetAdapter.registerAdapterDataObserver(circleIndicator.adapterDataObserver)*/

        val indicatorSizePx = resources.getDimensionPixelSize(R.dimen.tab_indicator_size)
        val normalColor = resources.getColor(R.color.grey_divider, null)
        val checkedColor = resources.getColor(R.color.white, null)

        // TODO: get catalog detail list
        val autoCenterLayoutManger = AutoCenterLayoutManger(
            context = avatarScrollerList.context,
            orientation = RecyclerView.HORIZONTAL,
            reverseLayout = false
        )
        avatarScrollerList.layoutManager = autoCenterLayoutManger

        catalogPreviewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                // setUpCurrentIndicator(position)
                // indicatorView.onPageSelected(position)
                viewModel.toggleSelection(position)
                if (abs(previousPosition - position) <= SMOOTH_SCROLL_THRESHOLD) {
                    avatarScrollerList.smoothScrollToPosition(position)
                } else {
                    avatarScrollerList.scrollToPosition(position)
                }

                if (position == jumpToPosition) {
                    jumpToPosition = -1
                    jumpToId = -1
                }
                Timber.d("Jump to Id: $jumpToPosition")
                previousPosition = position
            }

            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int,
            ) {
                super.onPageScrolled(position, positionOffset, positionOffsetPixels)
                // indicatorView.onPageScrolled(position, positionOffset, positionOffsetPixels)
            }

            override fun onPageScrollStateChanged(state: Int) {
                super.onPageScrollStateChanged(state)
                // indicatorView.onPageScrollStateChanged(state)
            }
        })

        val loadStateFlow = uiState.map { it.loadState }
            .distinctUntilChanged()
        viewLifecycleOwner.lifecycleScope.launch {
            loadStateFlow.collectLatest { loadState ->
                progressBar.isVisible = loadState.refresh is LoadState.Loading &&
                        catalogPresetAdapter.itemCount <= 0
            }
        }

        val scrollerAdapter = AvatarScrollAdapter { clickedPosition ->
            if (previousPosition != clickedPosition) {
                HapticUtil.createOneShot(requireContext())
            }
            catalogPreviewPager.setCurrentItem(clickedPosition, true)
        }
        avatarScrollerList.adapter = scrollerAdapter

        val avatarListFlow = uiState.map { it.avatarList }
            .distinctUntilChanged()
        viewLifecycleOwner.lifecycleScope.launch {
            avatarListFlow.collectLatest { avatarList ->
                scrollerAdapter.submitList(avatarList)
                catalogPresetAdapter.submitList(avatarList)

                catalogPreviewPager.post {
                    // TODO: refactor
                    Timber.d("Jump to Id: $jumpToId")
                    if (jumpToId != null) {
                        avatarList.mapIndexed { index, avatarUiModel ->
                            val id =
                                (avatarUiModel as? SelectableAvatarUiModel.Item)?.modelAvatar?._id
                            Timber.d("Compare: 1 id = $id  == jump = $jumpToId")
                            if (avatarUiModel is SelectableAvatarUiModel.Item && avatarUiModel.modelAvatar._id == jumpToId) {
                                index
                            } else {
                                -1
                            }
                        }.filterNot {
                            Timber.d("Filter: $it")
                            it == -1
                        }
                            .lastOrNull()?.let { _jumpToPosition ->
                                jumpToPosition = _jumpToPosition
                                Timber.d("Jump to position: $jumpToPosition")
                                try {
                                    catalogPreviewPager.setCurrentItem(_jumpToPosition, false)
                                } catch (e: Exception) {
                                    Timber.d(e)
                                }
                            }
                    }
                }
            }
        }
        btnNext.text = getString(R.string.label_download)
        icDownload.isVisible = false
        icShare.isVisible = true

        bindShareProgress(
            uiState = uiState,
            uiAction = uiAction
        )

        bindClick(
            uiState = uiState,
            uiAction = uiAction
        )

        bindToolbar(
            uiState = uiState
        )
    }

    private fun FragmentAvatarPreviewBinding.bindClick(
        uiState: StateFlow<AvatarPreviewState>,
        uiAction: (AvatarPreviewUiAction) -> Unit
    ) {
        icShare.isVisible = true
        icShare.setOnClickListener {
            // TODO: check if login is necessary
            if (userViewModel.authenticationState.value.isAuthenticated()) {
                if (uiState.value.shareLinkData != null) {
                    handleShareLink(uiState.value.shareLinkData!!.shortLink)
                } else {
                    uiAction(AvatarPreviewUiAction.GetShareLink)
                }
            } else {
                gotoLogin()
            }
        }

        icDownload.isVisible = false

        btnNext.text = getString(R.string.label_download)
        btnNext.setOnClickListener {
            val avatarStatus = uiState.value.avatarStatusWithFiles?.avatarStatus ?: return@setOnClickListener
            if (avatarStatus.paid) {
                // TODO: get folder name
                if (avatarStatus.modelRenamedByUser) {
                    // TODO: if model is renamed directly save the photos
                    // TODO: [severity-10] check storage permissions if required
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                        if (!checkStoragePermission()) {
                            askStoragePermission()
                        } else {
                            viewModel.createDownloadSession(avatarStatus.modelName ?: avatarStatus.modelId)
                        }
                    } else {
                        viewModel.createDownloadSession(avatarStatus.modelName ?: avatarStatus.modelId)
                    }
                } else {
                    context?.debugToast("Getting folder name")
                    EditFolderNameDialog { typedName ->
                        if (typedName.isBlank()) {
                            return@EditFolderNameDialog "Name cannot be empty!"
                        }
                        if (typedName.length < 4) {
                            return@EditFolderNameDialog "Name too short"
                        }
                        // TODO: move 'save to gallery' to a foreground service
                        context?.showToast("Saving to $typedName")
                        viewModel.saveModelName(typedName)
                        null
                    }.show(childFragmentManager, "folder-name-dialog")
                }
            } else {
                // TODO: goto payment
                gotoPlans(avatarStatus.modelId)
            }
        }

        icShare.isVisible = true
        icShare.setOnClickListener { }
    }

    private fun FragmentAvatarPreviewBinding.bindToolbar(uiState: StateFlow<AvatarPreviewState>) {
        /*val catalogTitleFlow = uiState.mapNotNull { it.category?.categoryName }
        viewLifecycleOwner.lifecycleScope.launch {
            catalogTitleFlow.collectLatest { catalogTitle ->
                toolbarIncluded.toolbarTitle.text = catalogTitle
            }
        }*/

        val avatarStatusWithFilesFlow = uiState.map { it.avatarStatusWithFiles }
        viewLifecycleOwner.lifecycleScope.launch {
            avatarStatusWithFilesFlow.collectLatest { avatarStatusWithFiles ->
                Timber.d("status: $avatarStatusWithFiles")
                if (avatarStatusWithFiles != null) {
                    toolbarIncluded.toolbarTitle.apply {
                        isVisible = true
                        text = avatarStatusWithFiles.avatarStatus.modelName
                    }
                } else {
                    toolbarIncluded.toolbarTitle.apply {
                        isVisible = true
                        text = null
                    }
                }
            }
        }

        toolbarIncluded.toolbarNavigationIcon.setOnClickListener {
            try {
                findNavController().navigateUp()
            } catch (ignore: Exception) {
            }
        }
    }

    private fun FragmentAvatarPreviewBinding.bindShareProgress(
        uiState: StateFlow<AvatarPreviewState>,
        uiAction: (AvatarPreviewUiAction) -> Unit
    ) {
        val loadStateFlow = uiState.map { it.shareLoadState }
            .distinctUntilChangedBy { it.refresh }
        viewLifecycleOwner.lifecycleScope.launch {
            loadStateFlow.collectLatest { loadState ->
                if (loadState.refresh is LoadState.Loading) {
                    icShare.setImageResource(R.drawable.ic_share)
                    icShare.isEnabled = false
                    shareProgressBar.isVisible = true
                } else {
                    icShare.setImageResource(R.drawable.ic_share_outline)
                    icShare.isEnabled = true
                    shareProgressBar.isVisible = false
                }
            }
        }
    }

    private fun handleShareLink(link: String) {
        val shareIntent = ShareCompat.IntentBuilder(requireContext())
            .setText(link)
            .setType("text/plain")
            .setChooserTitle("Share with")
            .intent

        startActivity(Intent.createChooser(shareIntent, "Share with"))
    }

    private fun checkStoragePermission(): Boolean {
        return storagePermissions.all {
            ContextCompat.checkSelfPermission(requireContext(), it) ==
                    PackageManager.PERMISSION_GRANTED
        }
    }

    private fun showStoragePermissionRationale(openSettings: Boolean) {
        /* Simple permission rationale dialog */
        SimpleDialog(
            context = requireContext(),
            popupIcon = R.drawable.ic_files_permission,
            titleText = getString(R.string.permissions_required),
            message = getString(R.string.files_permission_des),
            positiveButtonText = "Settings",
            positiveButtonAction = {
                if (openSettings) {
                    /* go to settings */ openSettings()
                } else {
                    askStoragePermission()
                }
            },
            cancellable = true,
            showCancelButton = true
        ).show()
    }

    private fun openSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val uri: Uri = Uri.fromParts("package", requireActivity().packageName, null)
        intent.data = uri
        startActivity(intent)
        isSettingsLaunched = true
    }

    private fun askStoragePermission() {
        storagePermissionLauncher.launch(storagePermissions)
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


    private fun gotoPlans(modelId: String) = safeCall {
        findNavController().apply {
            val navOpts = defaultNavOptsBuilder()
                .setPopUpTo(R.id.login_fragment, inclusive = true, saveState = true)
                .build()
            val args = Bundle().apply {
                putString(Constant.EXTRA_FROM, "avatar_result")
                putString(Constant.ARG_MODEL_ID, modelId)
            }
            navigate(R.id.subscription_plans, args, navOpts)
        }
    }

    private fun handleBackPressed() {
        activity?.onBackPressedDispatcher?.addCallback(viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    safeCall {
                        findNavController().apply {
                            if (!navigateUp()) {
                                popBackStack()
                            }
                        }
                    }
                }
            }
        )
    }

    override fun onResume() {
        super.onResume()
        if (isSettingsLaunched) {
            // Do something
            isSettingsLaunched = false
        }
    }

    companion object {
        private const val SMOOTH_SCROLL_THRESHOLD = 20

        const val ARG_MODEL_ID = "com.aiavatar.app.args.MODEL_ID"
        const val ARG_STATUS_ID = "com.aiavatar.app.args.STATUS_ID"
        const val ARG_JUMP_TO_ID = "com.aiavatar.app.args.JUMP_TO_ID"
        const val ARG_JUMP_TO_IMAGE_NAME = "com.aiavatar.app.JUMP_TO_IMAGE_NAME"
    }
}

class AvatarScrollAdapter(
    private val onCardClick: (position: Int) -> Unit = { },
) : ListAdapter<SelectableAvatarUiModel, AvatarScrollAdapter.ItemViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        return ItemViewHolder.from(parent)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val model = getItem(position)
        model as SelectableAvatarUiModel.Item
        holder.bind(model.modelAvatar, model.selected, onCardClick)
    }

    override fun onBindViewHolder(
        holder: ItemViewHolder,
        position: Int,
        payloads: MutableList<Any>,
    ) {
        if (payloads.isNotEmpty()) {
            if (isValidPayload(payloads)) {
                val bundle = (payloads.firstOrNull() as? Bundle) ?: kotlin.run {
                    super.onBindViewHolder(holder, position, payloads); return
                }
                if (bundle.containsKey(SELECTION_TOGGLE_PAYLOAD)) {
                    (holder as? ItemViewHolder)?.toggleSelection(
                        bundle.getBoolean(
                            SELECTION_TOGGLE_PAYLOAD, false
                        )
                    )
                }
            } else {
                super.onBindViewHolder(holder, position, payloads)
            }
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    private fun isValidPayload(payloads: MutableList<Any>?): Boolean {
        return (payloads?.firstOrNull() as? Bundle)?.keySet()?.any {
            it == SELECTION_TOGGLE_PAYLOAD
        } ?: false
    }

    class ItemViewHolder private constructor(
        private val binding: ItemScrollerListBinding,
    ) : RecyclerView.ViewHolder(binding.root), Recyclable {

        fun bind(listAvatar: ModelAvatar, selected: Boolean, onCardClick: (position: Int) -> Unit) =
            with(binding) {
                title.text = listAvatar.remoteFile
                Glide.with(previewImage)
                    .load(listAvatar.remoteFile)
                    .placeholder(R.drawable.loading_animation)
                    .error(R.color.white)
                    .into(previewImage)

                toggleSelection(selected)

                previewImage.setOnClickListener { onCardClick(adapterPosition) }
            }

        fun toggleSelection(selected: Boolean) = with(binding) {
            previewImage.alpha = if (selected) {
                1.0F
            } else {
                0.5F
            }
        }

        override fun onViewRecycled() {
            binding.previewImage.let { imageView ->
                Glide.with(imageView).clear(null)
                imageView.setImageDrawable(null)
            }
        }

        companion object {
            fun from(parent: ViewGroup): ItemViewHolder {
                val itemView = LayoutInflater.from(parent.context).inflate(
                    R.layout.item_scroller_list,
                    parent,
                    false
                )
                val binding = ItemScrollerListBinding.bind(itemView)
                return ItemViewHolder(binding)
            }
        }
    }

    companion object {
        private const val SELECTION_TOGGLE_PAYLOAD = "selection_toggle"

        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<SelectableAvatarUiModel>() {
            override fun areItemsTheSame(
                oldItem: SelectableAvatarUiModel,
                newItem: SelectableAvatarUiModel,
            ): Boolean {
                return (oldItem is SelectableAvatarUiModel.Item && newItem is SelectableAvatarUiModel.Item &&
                        oldItem.modelAvatar._id == newItem.modelAvatar._id)
            }

            override fun areContentsTheSame(
                oldItem: SelectableAvatarUiModel,
                newItem: SelectableAvatarUiModel,
            ): Boolean {
                return (oldItem is SelectableAvatarUiModel.Item && newItem is SelectableAvatarUiModel.Item &&
                        oldItem.modelAvatar == newItem.modelAvatar && oldItem.selected == newItem.selected)
            }

            override fun getChangePayload(
                oldItem: SelectableAvatarUiModel,
                newItem: SelectableAvatarUiModel,
            ): Any {
                val updatePayload = bundleOf()
                when {
                    oldItem is SelectableAvatarUiModel.Item && newItem is SelectableAvatarUiModel.Item -> {
                        if (oldItem.selected != newItem.selected) {
                            updatePayload.putBoolean(SELECTION_TOGGLE_PAYLOAD, newItem.selected)
                        }
                    }
                }
                return updatePayload
            }

        }
    }
}

class AvatarPreviewPagerAdapter(
    private val context: Context,
    private val onCardClick: (position: Int) -> Unit = { }
): ListAdapter<SelectableAvatarUiModel, RecyclerView.ViewHolder>(DIFF_CALLBACK) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return ItemViewHolder.from(parent)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val model = getItem(position)
        if (holder is ItemViewHolder) {
            model as SelectableAvatarUiModel.Item
            holder.bind(model.modelAvatar, model.selected, onCardClick)
        }
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.isNotEmpty()) {
            if (isValidPayload(payloads)) {
                val bundle = (payloads.firstOrNull() as? Bundle) ?: kotlin.run {
                    super.onBindViewHolder(holder, position, payloads); return
                }
                if (bundle.containsKey(SELECTION_TOGGLE_PAYLOAD)) {
                    (holder as? ItemViewHolder)?.toggleSelection(bundle.getBoolean(
                        SELECTION_TOGGLE_PAYLOAD, false))
                }
            } else {
                super.onBindViewHolder(holder, position, payloads)
            }
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    private fun isValidPayload(payloads: MutableList<Any>?): Boolean {
        return (payloads?.firstOrNull() as? Bundle)?.keySet()?.any {
            it == SELECTION_TOGGLE_PAYLOAD
        } ?: false
    }

    class ItemViewHolder private constructor(
        private val binding: LargePresetPreviewBinding
    ) : RecyclerView.ViewHolder(binding.root), Recyclable {

        fun bind(listAvatar: ModelAvatar, selected: Boolean, onCardClick: (position: Int) -> Unit) = with(binding) {
            title.text = listAvatar.remoteFile
            Glide.with(previewImage)
                .load(listAvatar.remoteFile)
                .placeholder(R.drawable.loading_animation)
                .error(R.color.white)
                .into(previewImage)

            // toggleSelection(selected)

            previewImage.setOnClickListener { onCardClick(adapterPosition) }
        }

        fun toggleSelection(selected: Boolean) = with(binding) {
            previewImage.alpha = if (selected) {
                1.0F
            } else {
                0.0F
            }
        }

        override fun onViewRecycled() {
            binding.previewImage.let { imageView ->
                Glide.with(imageView).clear(null)
                imageView.setImageDrawable(null)
            }
        }

        companion object {
            fun from(parent: ViewGroup): ItemViewHolder {
                val itemView = LayoutInflater.from(parent.context).inflate(
                    R.layout.large_preset_preview,
                    parent,
                    false
                )
                val binding = LargePresetPreviewBinding.bind(itemView)
                return ItemViewHolder(binding)
            }
        }
    }

    companion object {
        private const val SELECTION_TOGGLE_PAYLOAD = "selection_toggle"

        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<SelectableAvatarUiModel>() {
            override fun areItemsTheSame(
                oldItem: SelectableAvatarUiModel,
                newItem: SelectableAvatarUiModel,
            ): Boolean {
                return (oldItem is SelectableAvatarUiModel.Item && newItem is SelectableAvatarUiModel.Item &&
                        oldItem.modelAvatar._id == newItem.modelAvatar._id)
            }

            override fun areContentsTheSame(
                oldItem: SelectableAvatarUiModel,
                newItem: SelectableAvatarUiModel,
            ): Boolean {
                return (oldItem is SelectableAvatarUiModel.Item && newItem is SelectableAvatarUiModel.Item &&
                        oldItem.modelAvatar == newItem.modelAvatar && oldItem.selected == newItem.selected)
            }

            override fun getChangePayload(
                oldItem: SelectableAvatarUiModel,
                newItem: SelectableAvatarUiModel
            ): Any? {
                /*val updatePayload = bundleOf()
                when {
                    oldItem is SelectableAvatarUiModel.Item && newItem is SelectableAvatarUiModel.Item -> {
                        if (oldItem.selected != newItem.selected) {
                            updatePayload.putBoolean(SELECTION_TOGGLE_PAYLOAD, newItem.selected)
                        }
                    }
                }
                return updatePayload*/
                return null
            }
        }
    }
}
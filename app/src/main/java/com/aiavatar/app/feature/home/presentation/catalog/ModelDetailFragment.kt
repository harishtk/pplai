package com.aiavatar.app.feature.home.presentation.catalog

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
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
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.aiavatar.app.*
import com.aiavatar.app.commons.presentation.dialog.SimpleDialog
import com.aiavatar.app.commons.util.HapticUtil
import com.aiavatar.app.commons.util.recyclerview.Recyclable
import com.aiavatar.app.core.data.source.local.entity.DownloadSessionStatus
import com.aiavatar.app.databinding.FragmentModelDetailBinding
import com.aiavatar.app.databinding.FragmentModelListBinding
import com.aiavatar.app.databinding.ItemScrollerListBinding
import com.aiavatar.app.feature.home.domain.model.ModelAvatar
import com.aiavatar.app.feature.home.presentation.dialog.EditFolderNameDialog
import com.aiavatar.app.feature.home.presentation.util.AutoCenterLayoutManger
import com.aiavatar.app.feature.home.presentation.util.CatalogPagerAdapter
import com.aiavatar.app.work.WorkUtil
import com.bumptech.glide.Glide
import com.pepulnow.app.data.LoadState
import com.zhpan.indicator.enums.IndicatorSlideMode
import com.zhpan.indicator.enums.IndicatorStyle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.math.abs

@AndroidEntryPoint
class ModelDetailFragment : Fragment() {

    private var _binding: FragmentModelDetailBinding? = null
    private val binding: FragmentModelDetailBinding
        get() = _binding!!

    private val viewModel: ModelDetailViewModel by viewModels()

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
            val modelId = getString(ARG_MODEL_ID, null)
            jumpToId = getLong(ARG_JUMP_TO_ID, -1L)

            Timber.d("Args: model id = $modelId jumpTo = $jumpToId")

            if (modelId?.isNotBlank() == true) {
                viewModel.setModelId(modelId)
            }

            viewModel.refresh()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return inflater.inflate(R.layout.fragment_model_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentModelDetailBinding.bind(view)

        binding.bindState(
            uiState = viewModel.uiState,
            uiAction = viewModel.accept,
            uiEvent = viewModel.uiEvent
        )

        handleBackPressed()
    }

    private fun FragmentModelDetailBinding.bindState(
        uiState: StateFlow<ModelDetailState>,
        uiAction: (ModelDetailUiAction) -> Unit,
        uiEvent: SharedFlow<ModelDetailUiEvent>,
    ) {
        viewLifecycleOwner.lifecycleScope.launch {
            uiEvent.collectLatest { event ->
                when (event) {
                    is ModelDetailUiEvent.ShowToast -> {
                        context?.showToast(event.message.asString(requireContext()))
                    }
                    is ModelDetailUiEvent.StartDownload -> {
                        checkPermissionAndScheduleWorker(event.downloadSessionId)
                    }
                    is ModelDetailUiEvent.DownloadComplete -> {
                        handleDownloadComplete(event.savedUri)
                    }
                }
            }
        }

        val catalogPresetAdapter = CatalogPagerAdapter(requireContext())

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
        binding.indicatorView.apply {
            setSliderColor(checkedColor, checkedColor)
            // setCheckedSlideWidth((indicatorSizePx * 2).toFloat())
            // setSliderWidth(indicatorSizePx.toFloat())
            setSliderWidth(indicatorSizePx.toFloat(), (indicatorSizePx * 3).toFloat())
            setSliderHeight(indicatorSizePx.toFloat())
            setSlideMode(IndicatorSlideMode.SCALE)
            setIndicatorStyle(IndicatorStyle.ROUND_RECT)
            notifyDataChanged()
        }

        // TODO: get catalog detail list
        val autoCenterLayoutManger = AutoCenterLayoutManger(
            context = avatarScrollerList.context,
            orientation = RecyclerView.HORIZONTAL,
            reverseLayout = false
        )
        avatarScrollerList.layoutManager = autoCenterLayoutManger

        catalogPreviewPager.registerOnPageChangeCallback(object : OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                // setUpCurrentIndicator(position)
                indicatorView.onPageSelected(position)
                viewModel.toggleSelection(position)
                if (abs(previousPosition - position) <= SMOOTH_SCROLL_THRESHOLD) {
                    avatarScrollerList.smoothScrollToPosition(position)
                } else {
                    avatarScrollerList.scrollToPosition(position)
                }

                if (position == jumpToPosition) {
                    jumpToPosition = null
                    jumpToId = null
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
                indicatorView.onPageScrolled(position, positionOffset, positionOffsetPixels)
            }

            override fun onPageScrollStateChanged(state: Int) {
                super.onPageScrollStateChanged(state)
                indicatorView.onPageScrollStateChanged(state)
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
                catalogPresetAdapter.submitList(avatarList) {
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
                setUpIndicator(avatarList.size)
            }
        }

        btnNext.text = getString(R.string.label_recreate)
        icDownload.isVisible = true
        icShare.isVisible = true

        bindDownloadProgress(
            uiState = uiState,
            uiAction = uiAction
        )

        bindClick(
            uiState = uiState
        )

        bindToolbar(
            uiState = uiState
        )
    }

    private fun FragmentModelDetailBinding.bindClick(uiState: StateFlow<ModelDetailState>) {
        icShare.setOnClickListener {
            context?.showToast("Coming soon!")
        }

        icDownload.setOnClickListener {
            val modelData = uiState.value.modelData
            Timber.d("avatarStatus: $modelData")
            modelData ?: return@setOnClickListener
            if (modelData.renamed) {
                // TODO: if model is renamed directly save the photos
                viewModel.downloadCurrentAvatar(requireContext())
            } else {
                context?.showToast("Getting folder name")
                EditFolderNameDialog { typedName ->
                    if (typedName.isBlank()) {
                        return@EditFolderNameDialog "Name cannot be empty!"
                    }
                    if (typedName.length < 4) {
                        return@EditFolderNameDialog "Name too short"
                    }
                    // TODO: move 'save to gallery' to a foreground service
                    viewModel.saveModelName(typedName) {
                        viewModel.downloadCurrentAvatar(requireContext())
                    }
                    null
                }.show(childFragmentManager, "folder-name-dialog")
                // checkPermissionAndScheduleWorker(uiState.value.modelId!!)
            }
        }

        btnNext.setOnClickListener {
            context?.showToast("Coming soon!")
        }
    }

    private fun FragmentModelDetailBinding.bindToolbar(uiState: StateFlow<ModelDetailState>) {
        /*val catalogTitleFlow = uiState.mapNotNull { it.category?.categoryName }
        viewLifecycleOwner.lifecycleScope.launch {
            catalogTitleFlow.collectLatest { catalogTitle ->
                toolbarIncluded.toolbarTitle.text = catalogTitle
            }
        }*/

        val modelDataFlow = uiState.map { it.modelData }
        viewLifecycleOwner.lifecycleScope.launch {
            modelDataFlow.collectLatest { modelData ->
                if (modelData != null) {
                    toolbarIncluded.toolbarTitle.apply {
                        isVisible = true
                        text = modelData.name
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

    private fun FragmentModelDetailBinding.bindDownloadProgress(
        uiState: StateFlow<ModelDetailState>,
        uiAction: (ModelDetailUiAction) -> Unit
    ) {
        val downloadProgressFlow = uiState.map { it.currentDownloadProgress }
        viewLifecycleOwner.lifecycleScope.launch {
            downloadProgressFlow.collectLatest { downloadProgress ->
                Timber.d("downloadProgress: $downloadProgress")
                if (downloadProgress != null) {
                    icDownload.setImageResource(R.drawable.ic_download)
                    icDownload.isEnabled = false
                    downloadProgressBar.isVisible = true
                } else {
                    icDownload.setImageResource(R.drawable.ic_download_outline)
                    icDownload.isEnabled = true
                    downloadProgressBar.isVisible = false
                }
            }
        }
    }

    private fun FragmentModelDetailBinding.handleDownloadComplete(
        savedUri: Uri
    ) {
        sdkBelowQ {
            MediaScannerConnection.scanFile(
                requireContext(),
                arrayOf(savedUri.toString()),
                arrayOf(Constant.MIME_TYPE_IMAGE)
            ) { path, uri ->
                Timber.v("Gallery refreshed path = $path uri = $uri")
            }
        }
        root.showSnack(
            "Saved to Gallery",
            actionTitle = "View",
            isLong = true,
            actionCallback = {
                openGallery(savedUri)
            }
        )
    }

    private fun setUpIndicator(count: Int) {
        binding.indicatorView.setPageSize(count)
        binding.indicatorView.notifyDataChanged()
        /*val indicators = arrayOfNulls<View>(count)
        *//*val displayMetrics = Resources.getSystem().displayMetrics
        val tabIndicatorWidth = displayMetrics.widthPixels * 0.1
        val tabIndicatorHeight = tabIndicatorWidth * 0.1*//*
        val tabIndicatorWidth = resources.getDimensionPixelSize(R.dimen.tab_indicator_size)
        val tabIndicatorHeight = resources.getDimensionPixelSize(R.dimen.tab_indicator_size)
        val layoutParams: LinearLayout.LayoutParams =
            LinearLayout.LayoutParams(tabIndicatorWidth.toInt(), tabIndicatorHeight.toInt(), 1f)
        if (binding.pagerIndicators.orientation == LinearLayout.HORIZONTAL) {
            layoutParams.setMargins(10, 0, 10, 0)
        } else {
            layoutParams.setMargins(0, 10, 0, 10)
        }

//        View(requireContext()).apply {
//            this.background = ResourcesCompat.getDrawable(resources, R.drawable.grey_curved_bg, null)
//            this.backgroundTintList = ColorStateList.valueOf(resources.getColor(R.color.white, null))
//            this.layoutParams = layoutParams
//            this.layoutParams.width = tabIndicatorWidth * 2
//        }.also { maskedIndicator ->
//            binding.pagerIndicators.addView(maskedIndicator)
//        }

        for (i in indicators.indices) {
            indicators[i] = View(requireContext())
            indicators[i]?.apply {
                // this.setImageResource(R.drawable.grey_curved_bg)
                this.background = ResourcesCompat.getDrawable(resources, R.drawable.grey_curved_bg, null)
                this.backgroundTintList = (resources.getColorStateList(R.color.selector_indicator, null))
                this.layoutParams = layoutParams
            }
            binding.pagerIndicators.addView(indicators[i])
        }*/
    }

    private fun checkPermissionAndScheduleWorker(downloadSessionId: Long) {
        val cont: Continuation = {
            WorkUtil.scheduleDownloadWorker(requireContext(), downloadSessionId)
            val modelName = viewModel.uiState.value.modelData?.name
            if (modelName != null) {
                context?.showToast("Saving to $modelName")
            } else {
                context?.showToast("Saving..")
            }
            Timber.d("Download scheduled: $downloadSessionId")
        }

        if (checkStoragePermission()) {
            cont()
        } else {
            mStoragePermissionContinuation = cont
            askStoragePermission()
        }
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

    private fun openGallery(uri: Uri) {
        try {
            val galleryIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, Constant.MIME_TYPE_IMAGE)
            }
            if (galleryIntent.resolveActivity(requireActivity().packageManager) != null) {
                startActivity(galleryIntent)
            } else {
                throw IllegalStateException("Unable to open downloaded file!")
            }
        } catch (e: Exception) {
            context?.showToast("Unable to perform this action!")
            Timber.e(e)
        }
    }

    override fun onResume() {
        super.onResume()
        if (isSettingsLaunched) {
            // gotoCamera()
            isSettingsLaunched = false
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

    companion object {
        val TAG = ModelDetailFragment::class.java.simpleName

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
                    .placeholder(R.color.transparent_black)
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
                        modelAvatarEquals(oldItem.modelAvatar, newItem.modelAvatar) && oldItem.selected == newItem.selected)
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

            private fun modelAvatarEquals(old: ModelAvatar, new: ModelAvatar): Boolean {
                return (old._id == new._id &&
                        old.remoteFile == new.remoteFile)
            }
        }
    }
}
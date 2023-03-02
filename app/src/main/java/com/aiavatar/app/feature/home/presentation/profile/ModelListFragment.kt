package com.aiavatar.app.feature.home.presentation.profile

import android.Manifest
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.storage.StorageManager
import android.provider.Settings
import android.text.format.Formatter
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.PopupMenu
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.app.ShareCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.aiavatar.app.*
import com.aiavatar.app.analytics.Analytics
import com.aiavatar.app.analytics.AnalyticsLogger
import com.aiavatar.app.commons.presentation.dialog.SimpleDialog
import com.aiavatar.app.commons.util.AnimationUtil.shakeNow
import com.aiavatar.app.commons.util.ByteUnit
import com.aiavatar.app.commons.util.HapticUtil
import com.aiavatar.app.commons.util.ServiceUtil
import com.aiavatar.app.commons.util.imageloader.GlideImageLoader.Companion.disposeGlideLoad
import com.aiavatar.app.commons.util.imageloader.GlideImageLoader.Companion.newGlideBuilder
import com.aiavatar.app.commons.util.loadstate.LoadState
import com.aiavatar.app.commons.util.net.NoInternetException
import com.aiavatar.app.commons.util.recyclerview.Recyclable
import com.aiavatar.app.core.SourceCategory
import com.aiavatar.app.core.URLProvider
import com.aiavatar.app.core.data.source.local.entity.DownloadSessionStatus
import com.aiavatar.app.databinding.FragmentModelListBinding
import com.aiavatar.app.databinding.ItemSquareImageBinding
import com.aiavatar.app.feature.home.presentation.catalog.ModelDetailFragment
import com.aiavatar.app.feature.home.presentation.dialog.EditFolderNameDialog
import com.aiavatar.app.work.WorkUtil
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.internal.format
import org.thoughtcrime.securesms.util.CachedInflater
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import kotlin.properties.Delegates
import kotlin.reflect.KProperty

/**
 * TODO: -done- show download progress
 */
@AndroidEntryPoint
class ModelListFragment : Fragment() {

    @Inject
    lateinit var analyticsLogger: AnalyticsLogger

    private val viewModel: ModelListViewModel by viewModels()

    private val storagePermissions: Array<String> = arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    private lateinit var storagePermissionLauncher: ActivityResultLauncher<Array<String>>
    private var mStoragePermissionContinuation: Continuation? = null

    private var isSettingsLaunched = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.apply {
            getString(Constant.ARG_MODEL_ID, null)?.let { modelId ->
                Timber.d("Args: modelId = $modelId")
                viewModel.setModelId(modelId)
            }
        }
        viewModel.refresh()

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
                            requireContext().showToast("Storage permission is required to download photos")
                            // TODO: show storage rationale
                            showStoragePermissionRationale(false)
                        }
                        map[Constant.PERMISSION_PERMANENTLY_DENIED]?.let {
                            requireContext().showToast("Storage permission is required to download photos")
                            // TODO: show storage rationale permanent
                            showStoragePermissionRationale(openSettings = true)
                        }
                    }

                    else -> {
                        mStoragePermissionContinuation?.invoke()
                        mStoragePermissionContinuation = null
                    }
                }
            }
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_model_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentModelListBinding.bind(view)

        binding.bindState(
            uiState = viewModel.uiState,
            uiAction = viewModel.accept,
            uiEvent = viewModel.uiEvent
        )
        analyticsLogger.logEvent(Analytics.Event.MODEL_LIST_PRESENTED)
    }

    private fun FragmentModelListBinding.bindState(
        uiState: StateFlow<ModelListState>,
        uiAction: (ModelListUiAction) -> Unit,
        uiEvent: SharedFlow<ModelListUiEvent>,
    ) {
        viewLifecycleOwner.lifecycleScope.launch {
            uiEvent.collectLatest { event ->
                when (event) {
                    is ModelListUiEvent.ShowToast -> {
                        context?.showToast(event.message.asString(requireContext()))
                    }
                    is ModelListUiEvent.StartDownload -> {
                        checkPermissionAndScheduleWorker(event.downloadSessionId)
                    }
                    is ModelListUiEvent.ShareLink -> {
                        handleShareLink(event.link)
                    }
                }
            }
        }

        val notLoadingFlow = uiState.map { it.loadState }
            .distinctUntilChanged { old, new ->
                old.refresh == new.refresh && old.action == new.action
            }
            .map { it.refresh !is LoadState.Loading && it.action !is LoadState.Loading }
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
                        ifDebug { Timber.e(e) }
                        uiErr?.let { uiText -> context?.showToast(uiText.asString(requireContext())) }
                        uiAction(ModelListUiAction.ErrorShown(e))
                    }
                }
            }
        }

        val callback = object : ModelListAdapter2.Callback {
            override fun onItemClick(position: Int, data: ModelListUiModel2.AvatarItem) {
                uiState.value.modelData?.let { modelData ->
                    if (modelData.paid) {
                        gotoModelDetail(position, data)
                    } else {
                        gotoPlans(modelData.id)
                    }
                    analyticsLogger.logEvent(Analytics.Event.MODEL_LIST_ITEM_CLICK)
                }

            }
        }

        val adapter = ModelListAdapter2(
            glide = initGlide(),
            callback = callback
        )

        val avatarResultListFlow = uiState.map { it.modelResultList }
            .distinctUntilChanged()
        viewLifecycleOwner.lifecycleScope.launch {
            avatarResultListFlow.collectLatest { avatarResultList ->
                adapter.submitList(avatarResultList)
            }
        }

        val modelPaidStatusFlow = uiState.map { it.modelData }
            .map { it?.paid == true }
            .distinctUntilChanged()
        viewLifecycleOwner.lifecycleScope.launch {
            modelPaidStatusFlow.collectLatest { modelPaidStatus ->
                adapter.enableWaterMark = !modelPaidStatus
            }
        }

        avatarPreviewList.adapter = adapter

        val loadStateFlow = uiState.map { it.loadState }
            .distinctUntilChanged { old, new ->
                old.refresh == new.refresh && old.action == new.action
            }
        viewLifecycleOwner.lifecycleScope.launch {
            loadStateFlow.collectLatest { loadState ->
                Timber.d("Load state: $loadState")
                val emptyList = adapter.itemCount <= 0
                if (loadState.refresh is LoadState.Loading) {
                    listPlaceholder.isVisible = emptyList
                }

                if (loadState.refresh !is LoadState.Loading) {
                    if (listPlaceholder.isVisible) {
                        listPlaceholder.postDelayed({
                            listPlaceholder.isVisible = false
                        }, 100L)
                    }
                }

                /*listPlaceholder.isVisible = loadState.refresh is LoadState.Loading &&
                        adapter.itemCount <= 0*/
                /*progressBar.isVisible = loadState.refresh is LoadState.Loading &&
                        adapter.itemCount <= 0*/
                retryButton.isVisible = loadState.refresh is LoadState.Error &&
                        adapter.itemCount <= 0
                if (loadState.refresh is LoadState.Error) {
                    if (loadState.refresh.error !is NoInternetException) {
                        HapticUtil.createError(requireContext())
                    }
                    retryButton.shakeNow()
                }
                /*if (loadState.action is LoadState.Loading) {
                    btnNext.setSpinning()
                } else {
                    btnNext.cancelSpinning()
                    if (loadState.action is LoadState.Error) {
                        btnNext.shakeNow()
                        HapticUtil.createError(requireContext())
                    }
                }*/
            }
        }

        bindToolbar(
            uiState = uiState
        )

        bindDownloadProgress(
            uiState = uiState,
            uiAction = uiAction
        )

        bindShareProgress(
            uiState = uiState,
            uiAction = uiAction
        )

        bindClick(
            uiState = uiState,
            uiAction = uiAction
        )
    }

    private fun FragmentModelListBinding.bindClick(
        uiState: StateFlow<ModelListState>,
        uiAction: (ModelListUiAction) -> Unit,
    ) {
        icDownload.isVisible = true
        icDownload.setOnClickListener {
            val modelData = uiState.value.modelData ?: return@setOnClickListener
            if (modelData.paid) {
                // TODO: -done- get folder name
                if (modelData.renamed) {
                    analyticsLogger.logEvent(Analytics.Event.MODEL_LIST_DOWNLOAD_CLICK)
                    // TODO: -done- if model is renamed directly save the photos
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                        if (!checkStoragePermission()) {
                            askStoragePermission()
                        } else {
                            confirmDownload { viewModel.createDownloadSession(modelData.name ?: modelData.id) }
                        }
                    } else {
                        confirmDownload { viewModel.createDownloadSession(modelData.name ?: modelData.id) }
                    }
                } else {
                    context?.debugToast("Getting folder name")
                    showEditModelNameDialog(modelData.name) {
                        confirmDownload { viewModel.createDownloadSession(modelData.name) }
                        analyticsLogger.logEvent(Analytics.Event.MODEL_LIST_FOLDER_NAME_CHANGE)
                    }
                }
            } else {
                // TODO: goto payment
                gotoPlans(modelData.id)
                analyticsLogger.logEvent(Analytics.Event.MODEL_LIST_RECREATE_CLICK)
            }
        }

        btnNext.text = getString(R.string.label_recreate)
        btnNext.setOnClickListener {
            uiState.value.modelId?.let { modelId ->
                gotoPlans(modelId)
                analyticsLogger.logEvent(Analytics.Event.MODEL_LIST_RECREATE_CLICK)
            }
        }

        icShare.isVisible = true
        icShare.setOnClickListener {
            if (uiState.value.shareLinkData != null) {
                handleShareLink(uiState.value.shareLinkData!!.shortLink)
                analyticsLogger.logEvent(Analytics.Event.MODEL_LIST_SHARE_CLICK)
            } else {
                uiAction(ModelListUiAction.GetShareLink)
            }
        }

        btnClose.setOnClickListener {
            findNavController().navigateUp()
            analyticsLogger.logEvent(Analytics.Event.MODEL_LIST_BACK_ACTION_CLICK)
        }

        retryButton.setOnClickListener { viewModel.refresh() }

        toolbarIncluded.toolbarTitle.setOnClickListener {
            showModelOptions(it)
        }
    }

    private fun FragmentModelListBinding.bindDownloadProgress(
        uiState: StateFlow<ModelListState>,
        uiAction: (ModelListUiAction) -> Unit
    ) {
        val downloadStatusFlow = uiState.map { it.downloadStatus }
        viewLifecycleOwner.lifecycleScope.launch {
            downloadStatusFlow.collectLatest { downloadStatus ->
                when (downloadStatus) {
                    DownloadSessionStatus.NOT_STARTED,
                    DownloadSessionStatus.PARTIALLY_DONE,
                    DownloadSessionStatus.UNKNOWN -> {
                        icDownload.setImageResource(R.drawable.ic_download)
                        icDownload.isEnabled = false
                        downloadProgressBar.isVisible = true
                    }
                    DownloadSessionStatus.COMPLETE,
                    DownloadSessionStatus.FAILED -> {
                        icDownload.setImageResource(R.drawable.ic_download_outline)
                        icDownload.isEnabled = true
                        downloadProgressBar.isVisible = false
                    }
                    null -> {
                        icDownload.setImageResource(R.drawable.ic_download_outline)
                        icDownload.isEnabled = true
                        downloadProgressBar.isVisible = false
                    }
                }
            }
        }
    }

    private fun FragmentModelListBinding.bindShareProgress(
        uiState: StateFlow<ModelListState>,
        uiAction: (ModelListUiAction) -> Unit
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

    private fun FragmentModelListBinding.bindToolbar(uiState: StateFlow<ModelListState>) {
        val modelDataFlow = uiState.map { it.modelData }
        viewLifecycleOwner.lifecycleScope.launch {
            modelDataFlow.collectLatest { modelData ->
                toolbarIncluded.apply {
                    if (modelData != null) {
                        toolbarTitle.apply {
                            isVisible = true
                            text = modelData.name.ifBlank { getString(R.string.label_result) }
                        }
                    } else {
                        toolbarTitle.apply {
                            isVisible = true
                            text = getString(R.string.label_result)
                        }
                    }
                }
            }
        }

        toolbarIncluded.toolbarNavigationIcon.setOnClickListener { findNavController().navigateUp() }
    }

    private fun checkFolderName(name: String) {

    }

    private fun handleShareLink(link: String) {
        val shareIntent = ShareCompat.IntentBuilder(requireContext())
            .setText(link)
            .setType("text/plain")
            .setChooserTitle("Share with")
            .intent

        startActivity(Intent.createChooser(shareIntent, "Share with"))
    }

    private fun checkPermissionAndScheduleWorker(downloadSessionId: Long) {
        val cont: Continuation = {
            WorkUtil.scheduleDownloadWorker(requireContext(), downloadSessionId)
            Timber.d("Download scheduled: $downloadSessionId")
            context?.debugToast("Downloading.. check notifications")
            viewModel.observeDownloadStatus(downloadSessionId)
            /*if (!ApplicationDependencies.getPersistentStore().isLogged) {
                gotoHome()
            }*/
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (checkStoragePermission()) {
                cont()
            } else {
                mStoragePermissionContinuation = cont
                askStoragePermission()
            }
        } else {
            cont()
        }
     }

    private fun gotoHome() = safeCall {
        findNavController().apply {
            val navOptions = defaultNavOptsBuilder()
                .setPopUpTo(R.id.main_nav_graph, inclusive = true, saveState = false)
                .build()
            navigate(R.id.catalog_list, null, navOptions)
        }
    }

    private fun gotoModelDetail(
        position: Int,
        data: ModelListUiModel2.AvatarItem
    ) {
        Timber.d("gotoModelDetail: ${data.avatar._id}")
        val modelId = viewModel.getModelId()

        try {
            findNavController().apply {
                val navOpts = defaultNavOptsBuilder()
                    .setPopExitAnim(R.anim.slide_out_right)
                    .build()
                val args = Bundle().apply {
                    modelId?.let { putString(ModelDetailFragment.ARG_MODEL_ID, it) }
                    data.avatar._id?.let { putLong(ModelDetailFragment.ARG_JUMP_TO_ID, it) }
                }
                navigate(R.id.model_detail, args, navOpts)
            }
        } catch (ignore: Exception) {
        }
    }

    private fun gotoPlans(modelId: String) = safeCall {
        findNavController().apply {
            val navOpts = defaultNavOptsBuilder()
                .setPopUpTo(R.id.login_fragment, inclusive = true, saveState = true)
                .build()
            val args = Bundle().apply {
                putString(Constant.ARG_MODEL_ID, modelId)
            }
            navigate(R.id.subscription_plans, args, navOpts)
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

    private fun showModelOptions(anchor: View?) {
        val wrapper = ContextThemeWrapper(requireContext(), R.style.Widget_App_PopupMenu)
        PopupMenu(wrapper, anchor, Gravity.BOTTOM).apply {
            inflate(R.menu.model_options_menu)
            setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.menu_option_rename_model -> {
                        val modelData = viewModel.uiState.value.modelData
                            ?: return@setOnMenuItemClickListener false
                        showEditModelNameDialog(modelData.name) {
                            analyticsLogger.logEvent(Analytics.Event.MODEL_DETAIL_FOLDER_NAME_CHANGE)
                        }
                    }
                }
                false
            }
        }.also {
            it.show()
        }
    }

    private fun showEditModelNameDialog(
        modelName: String,
        successContinuation: () -> Unit
    ) {
        EditFolderNameDialog(previousModelName = modelName) { /* onSave */ typedName ->
            if (typedName.isBlank()) {
                return@EditFolderNameDialog "Name cannot be empty!"
            }
            if (typedName.length < 4) {
                return@EditFolderNameDialog "Name too short"
            }
            viewModel.saveModelName(typedName, successContinuation)
            null
        }.show(childFragmentManager, "folder-name-dialog")
    }

    private fun openSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val uri: Uri = Uri.fromParts("package", requireActivity().packageName, null)
        intent.data = uri
        startActivity(intent)
        isSettingsLaunched = true
    }

    private fun checkStoragePermission(): Boolean {
        return storagePermissions.all {
            ContextCompat.checkSelfPermission(requireContext(), it) ==
                    PackageManager.PERMISSION_GRANTED
        }
    }

    private fun confirmDownload(
        successContinuation: () -> Unit
    ) {
        viewLifecycleOwner.lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                val clickListener: DialogInterface.OnClickListener =
                    DialogInterface.OnClickListener { dialog, which ->
                        when (which) {
                            DialogInterface.BUTTON_POSITIVE -> {
                                successContinuation.invoke()
                            }
                        }
                        dialog.dismiss()
                    }

                val downloadSizeBytes = viewModel.getEstimatedDownloadSize()

                withContext(Dispatchers.IO) {
                    ServiceUtil.getStorageManager(requireContext()).let { storageManager ->
                        runCatching {
                            // TODO: try with [StorageManager#allocateBytes(java.io.FileDescriptor, long)]
                            storageManager.allocateBytes(StorageManager.UUID_DEFAULT, downloadSizeBytes)
                        }
                            .onSuccess {
                                val _sizeMb = ByteUnit.BYTES.toMegabytes(downloadSizeBytes)
                                Timber.d("Storage space is available for ${_sizeMb}MB.")
                            }
                            .onFailure { t ->
                                val _sizeMb = ByteUnit.BYTES.toMegabytes(downloadSizeBytes)
                                Timber.d(t, "Storage space is not available for ${_sizeMb}MB.")
                            }
                    }
                }
                val formattedFileSize = Formatter.formatShortFileSize(requireContext(), downloadSizeBytes)
                withContext(Dispatchers.Main) {
                    MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_App_MaterialDialog)
                        .setMessage("Download all avatars to gallery? About ${formattedFileSize}.")
                        .setPositiveButton("YES", clickListener)
                        .setNegativeButton("CANCEL", clickListener)
                        .show()
                }
            }
        }
    }

    private fun initGlide(): RequestManager {
        val options: RequestOptions = RequestOptions()
        return Glide.with(this@ModelListFragment)
            .setDefaultRequestOptions(options)
    }

    override fun onStart() {
        prepare(requireContext())
        super.onStart()
    }

    override fun onResume() {
        super.onResume()
        if (isSettingsLaunched) {
            // gotoCamera()
            isSettingsLaunched = false
        }
    }

    private fun askStoragePermission() {
        storagePermissionLauncher.launch(storagePermissions)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        Timber.d("onSaveInstanceState")
    }

    companion object {
        fun prepare(context: Context) {
            val parent = FrameLayout(context)
            parent.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

            CachedInflater.from(context).cacheUntilLimit(R.layout.item_square_image, parent, 12)
        }
    }
}

class ModelListAdapter2(
    private val glide: RequestManager,
    private val callback: Callback,
) : ListAdapter<ModelListUiModel2, RecyclerView.ViewHolder>(DIFF_CALLBACK) {

    var enableWaterMark: Boolean by Delegates.observable(true)
    { property: KProperty<*>, oldValue: Boolean, newValue: Boolean ->
        if (oldValue != newValue) {
            notifyDataSetChanged()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return ItemViewHolder.from(parent)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val model = getItem(position)
        if (holder is ItemViewHolder) {
            model as ModelListUiModel2.AvatarItem
            holder.bind(model, glide, callback, enableWaterMark)
        }
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        super.onViewRecycled(holder)
        (holder as? Recyclable)?.onViewRecycled()
    }

    class ItemViewHolder(
        private val binding: ItemSquareImageBinding,
    ) : RecyclerView.ViewHolder(binding.root), Recyclable {

        fun bind(data: ModelListUiModel2.AvatarItem, glide: RequestManager, callback: Callback, showWaterMark: Boolean) = with(binding) {
            view1.apply {
                newGlideBuilder(glide)
                    .thumbnail(
                        URLProvider.avatarThumbUrl(data.avatar.thumbnail).also {
                            Timber.d("Thumbnail: $it")
                        }
                    )
                    .originalImage(data.avatar.remoteFile)
                    .placeholder(R.drawable.loading_animation)
                    .error(R.color.grey_900)
                    .start()
            }

            tvWatermarkBrand.isVisible = showWaterMark
            root.setOnClickListener { callback.onItemClick(adapterPosition, data) }
        }

        override fun onViewRecycled() = with(binding) {
            view1.disposeGlideLoad()
        }

        companion object {
            fun from(parent: ViewGroup): ItemViewHolder {
                val itemView = CachedInflater.from(parent.context).inflate<View>(
                    R.layout.item_square_image,
                    parent,
                    false
                )
                val binding = ItemSquareImageBinding.bind(itemView)
                return ItemViewHolder(binding)
            }
        }

    }

    interface Callback {
        fun onItemClick(position: Int, data: ModelListUiModel2.AvatarItem)
    }

    companion object {

        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<ModelListUiModel2>() {
            override fun areItemsTheSame(
                oldItem: ModelListUiModel2,
                newItem: ModelListUiModel2,
            ): Boolean {
                return (oldItem is ModelListUiModel2.AvatarItem && newItem is ModelListUiModel2.AvatarItem &&
                        oldItem.avatar._id == newItem.avatar._id)
            }

            override fun areContentsTheSame(
                oldItem: ModelListUiModel2,
                newItem: ModelListUiModel2,
            ): Boolean {
                return true
            }
        }
    }
}
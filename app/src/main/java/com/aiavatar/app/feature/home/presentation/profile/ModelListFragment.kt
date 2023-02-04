package com.aiavatar.app.feature.home.presentation.profile

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigator
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.aiavatar.app.*
import com.aiavatar.app.commons.util.AnimationUtil.shakeNow
import com.aiavatar.app.commons.util.HapticUtil
import com.aiavatar.app.core.data.source.local.entity.DownloadSessionStatus
import com.aiavatar.app.databinding.FragmentModelListBinding
import com.aiavatar.app.databinding.ItemSquareImageBinding
import com.aiavatar.app.feature.home.presentation.catalog.ModelDetailFragment
import com.aiavatar.app.feature.home.presentation.dialog.EditFolderNameDialog
import com.aiavatar.app.work.WorkUtil
import com.bumptech.glide.Glide
import com.pepulnow.app.data.LoadState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * TODO: show download progress
 */
@AndroidEntryPoint
class ModelListFragment : Fragment() {

    private val viewModel: ModelListViewModel by viewModels()

    private val storagePermissions: Array<String> = arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    private lateinit var storagePermissionLauncher: ActivityResultLauncher<Array<String>>
    private var mStoragePermissionContinuation: Continuation? = null

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
                            requireContext().showToast("Storage permission is required to upload photos")
                            // TODO: show storage rationale
                        }
                        map[Constant.PERMISSION_PERMANENTLY_DENIED]?.let {
                            requireContext().showToast("Storage permission is required to upload photos")
                            // TODO: show storage rationale permanent
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
                        Timber.e(e)
                        uiErr?.let { uiText -> context?.showToast(uiText.asString(requireContext())) }
                        uiAction(ModelListUiAction.ErrorShown(e))
                    }
                }
            }
        }

        val callback = object : ModelListAdapter2.Callback {
            override fun onItemClick(position: Int, data: ModelListUiModel2.AvatarItem) {
                gotoModelDetail(position, data)
            }
        }

        val adapter = ModelListAdapter2(callback)

        val avatarResultListFlow = uiState.map { it.modelResultList }
            .distinctUntilChanged()
        viewLifecycleOwner.lifecycleScope.launch {
            avatarResultListFlow.collectLatest { avatarResultList ->
                adapter.submitList(avatarResultList)
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
                    HapticUtil.createError(requireContext())
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
                // TODO: get folder name
                if (modelData.renamed) {
                    // TODO: if model is renamed directly save the photos
                    viewModel.createDownloadSession(modelData.name)
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
                        viewModel.saveModelName(typedName)
                        null
                    }.show(childFragmentManager, "folder-name-dialog")
                }
            } else {
                // TODO: goto payment
                gotoPlans(modelData.id)
            }
        }

        btnNext.text = getString(R.string.label_recreate)
        btnNext.setOnClickListener {
            uiState.value.modelId?.let { modelId ->
                gotoPlans(modelId)
            }
        }

        icShare.isVisible = true
        icShare.setOnClickListener { }

        btnClose.setOnClickListener { findNavController().navigateUp() }

        retryButton.setOnClickListener { viewModel.refresh() }
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

    private fun FragmentModelListBinding.bindToolbar(uiState: StateFlow<ModelListState>) {
        val modelDataFlow = uiState.map { it.modelData }
        viewLifecycleOwner.lifecycleScope.launch {
            modelDataFlow.collectLatest { modelData ->
                toolbarIncluded.apply {
                    if (modelData != null) {
                        toolbarTitle.apply {
                            isVisible = true
                            text = modelData.name.ifEmpty { getString(R.string.label_result) }
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

        if (checkStoragePermission()) {
            cont()
        } else {
            mStoragePermissionContinuation = cont
            askStoragePermission()
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
                    putString(ModelDetailFragment.ARG_JUMP_TO_IMAGE_NAME, data.avatar.remoteFile)
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

    private fun checkStoragePermission(): Boolean {
        return storagePermissions.all {
            ContextCompat.checkSelfPermission(requireContext(), it) ==
                    PackageManager.PERMISSION_GRANTED
        }
    }

    private fun askStoragePermission() {
        storagePermissionLauncher.launch(storagePermissions)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        Timber.d("onSaveInstanceState")
    }
}

class ModelListAdapter2(
    private val callback: Callback,
) : ListAdapter<ModelListUiModel2, RecyclerView.ViewHolder>(DIFF_CALLBACK) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return ItemViewHolder.from(parent)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val model = getItem(position)
        if (holder is ItemViewHolder) {
            model as ModelListUiModel2.AvatarItem
            holder.bind(model, callback)
        }
    }

    class ItemViewHolder(
        private val binding: ItemSquareImageBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(data: ModelListUiModel2.AvatarItem, callback: Callback) = with(binding) {
            Glide.with(view1)
                .load(data.avatar.remoteFile)
                // .load(R.drawable.image_placeholder_animation)
                .placeholder(R.drawable.loading_animation)
                .into(view1)

            root.setOnClickListener { callback.onItemClick(adapterPosition, data) }
        }

        companion object {
            fun from(parent: ViewGroup): ItemViewHolder {
                val itemView = LayoutInflater.from(parent.context).inflate(
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
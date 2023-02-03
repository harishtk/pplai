package com.aiavatar.app.feature.home.presentation.create

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DiffUtil.ItemCallback
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.aiavatar.app.*
import com.aiavatar.app.commons.util.HapticUtil
import com.aiavatar.app.commons.util.cancelSpinning
import com.aiavatar.app.commons.util.setSpinning
import com.aiavatar.app.commons.util.shakeNow
import com.aiavatar.app.databinding.FragmentAvatarResultBinding
import com.aiavatar.app.databinding.ItemSquareImageBinding
import com.aiavatar.app.di.ApplicationDependencies
import com.aiavatar.app.feature.home.presentation.dialog.EditFolderNameDialog
import com.aiavatar.app.viewmodels.SharedViewModel
import com.aiavatar.app.work.WorkUtil
import com.bumptech.glide.Glide
import com.pepulnow.app.data.LoadState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * TODO: 1. If storage permission is not granted, show a blocking notification to get the same.
 */
@AndroidEntryPoint
class AvatarResultFragment : Fragment() {

    private val sharedViewModel: SharedViewModel by activityViewModels()
    private val viewModel: AvatarResultViewModel by viewModels()

    private val storagePermissions: Array<String> = arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    private lateinit var storagePermissionLauncher: ActivityResultLauncher<Array<String>>
    private var mStoragePermissionContinuation: Continuation? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.apply {
            getString(Constant.ARG_STATUS_ID, null)?.let { statusId ->
                viewModel.setAvatarStatusId(statusId)
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
        savedInstanceState: Bundle?,
    ): View {
        return inflater.inflate(R.layout.fragment_avatar_result, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentAvatarResultBinding.bind(view)

        binding.bindState(
            uiState = viewModel.uiState,
            uiAction = viewModel.accept,
            uiEvent = viewModel.uiEvent
        )
    }

    private fun FragmentAvatarResultBinding.bindState(
        uiState: StateFlow<AvatarResultState>,
        uiAction: (AvatarResultUiAction) -> Unit,
        uiEvent: SharedFlow<AvatarResultUiEvent>,
    ) {
        viewLifecycleOwner.lifecycleScope.launch {
            uiEvent.collectLatest { event ->
                when (event) {
                    is AvatarResultUiEvent.ShowToast -> {
                        context?.showToast(event.message.asString(requireContext()))
                    }
                    is AvatarResultUiEvent.StartDownload -> {
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
                        uiAction(AvatarResultUiAction.ErrorShown(e))
                    }
                }
            }
        }

        val callback = object : AvatarResultAdapter.Callback {
            override fun onItemClick(position: Int, data: AvatarResultUiModel.AvatarItem) {
                gotoAvatarPreview(position, data)
            }
        }

        val adapter = AvatarResultAdapter(callback)

        val avatarResultListFlow = uiState.map { it.avatarResultList }
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
                progressBar.isVisible = loadState.refresh is LoadState.Loading &&
                        adapter.itemCount <= 0
                if (loadState.action is LoadState.Loading) {
                    btnNext.setSpinning()
                } else {
                    btnNext.cancelSpinning()
                    if (loadState.action is LoadState.Error) {
                        btnNext.shakeNow()
                        HapticUtil.createError(requireContext())
                    }
                }
            }
        }

        bindClick(
            uiState = uiState,
            uiAction = uiAction
        )
    }

    private fun FragmentAvatarResultBinding.bindClick(
        uiState: StateFlow<AvatarResultState>,
        uiAction: (AvatarResultUiAction) -> Unit,
    ) {
        icDownload.isVisible = false

        btnNext.idleText = getString(R.string.label_download)
        btnNext.setOnClickListener {
            val avatarStatus = uiState.value.avatarStatus ?: return@setOnClickListener
            if (avatarStatus.paid) {
                // TODO: get folder name
                if (avatarStatus.modelRenamedByUser) {
                    // TODO: if model is renamed directly save the photos
                    viewModel.createDownloadSession(avatarStatus.modelName ?: avatarStatus.modelId)
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
                gotoPlans(avatarStatus.modelId)
            }
        }

        icShare.isVisible = true
        icShare.setOnClickListener { }

        btnClose.setOnClickListener { findNavController().navigateUp() }
    }

    private fun checkFolderName(name: String) {

    }

    private fun checkPermissionAndScheduleWorker(downloadSessionId: Long) {
        val cont: Continuation = {
            WorkUtil.scheduleDownloadWorker(requireContext(), downloadSessionId)
            Timber.d("Download scheduled: $downloadSessionId")
            context?.debugToast("Downloading.. check notifications")
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

    private fun gotoAvatarPreview(position: Int, data: AvatarResultUiModel.AvatarItem) {
        Timber.d("gotoAvatarPreview: ${data.avatar._id}")
        val statusId = viewModel.getStatusId()
        try {
            findNavController().apply {
                val navOpts = defaultNavOptsBuilder()
                    .setPopExitAnim(R.anim.slide_out_right)
                    .build()
                val args = Bundle().apply {
                    putString(Constant.EXTRA_FROM, "result_preview")
                    statusId?.let { putString(AvatarPreviewFragment.ARG_STATUS_ID, it) }
                    data.avatar._id?.let { putLong(AvatarPreviewFragment.ARG_JUMP_TO_ID, it) }
                    putString(AvatarPreviewFragment.ARG_JUMP_TO_IMAGE_NAME, data.avatar.remoteFile)
                }
                navigate(R.id.avatar_preview, args, navOpts)
            }
        } catch (ignore: Exception) {
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

}

class AvatarResultAdapter(
    private val callback: Callback,
) : ListAdapter<AvatarResultUiModel, RecyclerView.ViewHolder>(DIFF_CALLBACK) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return ItemViewHolder.from(parent)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val model = getItem(position)
        if (holder is ItemViewHolder) {
            model as AvatarResultUiModel.AvatarItem
            holder.bind(model, callback)
        }
    }

    class ItemViewHolder(
        private val binding: ItemSquareImageBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(data: AvatarResultUiModel.AvatarItem, callback: Callback) = with(binding) {
            Glide.with(view1)
                .load(data.avatar.remoteFile)
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
        fun onItemClick(position: Int, data: AvatarResultUiModel.AvatarItem)
    }

    companion object {

        val DIFF_CALLBACK = object : ItemCallback<AvatarResultUiModel>() {
            override fun areItemsTheSame(
                oldItem: AvatarResultUiModel,
                newItem: AvatarResultUiModel,
            ): Boolean {
                return (oldItem is AvatarResultUiModel.AvatarItem && newItem is AvatarResultUiModel.AvatarItem &&
                        oldItem.avatar._id == newItem.avatar._id)
            }

            override fun areContentsTheSame(
                oldItem: AvatarResultUiModel,
                newItem: AvatarResultUiModel,
            ): Boolean {
                return true
            }
        }
    }
}
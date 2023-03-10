package com.aiavatar.app.feature.home.presentation.create

import android.Manifest
import android.animation.ObjectAnimator
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
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
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.aiavatar.app.*
import com.aiavatar.app.Constant.MIME_TYPE_JPEG
import com.aiavatar.app.commons.presentation.dialog.SimpleDialog
import com.aiavatar.app.commons.util.AnimationUtil.touchInteractFeedback
import com.aiavatar.app.commons.util.HapticUtil
import com.aiavatar.app.commons.util.cancelSpinning
import com.aiavatar.app.commons.util.setSpinning
import com.aiavatar.app.databinding.FragmentUploadStep2Binding
import com.aiavatar.app.databinding.ItemExamplePhotoBinding
import com.aiavatar.app.databinding.ItemUploadPreviewBinding
import com.aiavatar.app.databinding.ItemUploadPreviewPlaceholderBinding
import com.aiavatar.app.feature.home.domain.model.SelectedMediaItem
import com.aiavatar.app.viewmodels.SharedViewModel
import com.bumptech.glide.Glide
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.pepulnow.app.data.LoadState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class UploadStep2Fragment : Fragment() {

    private val viewModel: UploadStep2ViewModel by viewModels()
    private val sharedViewModel: SharedViewModel by activityViewModels()

    private var isFaceDetectionRunning = false
    private var faceDetectionJob: Job? = null

    private val photoPickerLauncher = registerForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(MAX_IMAGES)
    ) { pickedUris ->
        Timber.d("Picked Uris: $pickedUris")
        if (pickedUris.isNotEmpty()) {
            val maxPick = viewModel.getMaxImages()
            if (pickedUris.size > maxPick) {
                context?.showToast("Up to $maxPick images are allowed")
                HapticUtil.createError(requireContext())
            } else {
                preprocessUris(pickedUris)
            }
            // viewModel.setPickedUris(pickedUris)
        }
    }

    private val photoPickerGenericLauncher = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { pickedUris ->
        Timber.d("Picked Uris Generic: $pickedUris")
        if (pickedUris.isNotEmpty()) {
            val maxPick = viewModel.getMaxImages()
            if (pickedUris.size > maxPick) {
                context?.showToast("Up to $maxPick images are allowed")
                HapticUtil.createError(requireContext())
            } else {
                preprocessUris(pickedUris)
            }
            // viewModel.setPickedUris(pickedUris)
        }
    }

    private val storagePermissions: Array<String> = arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
    )

    private lateinit var storagePermissionLauncher: ActivityResultLauncher<Array<String>>
    private var mStoragePermissionContinuation: Continuation? = null

    private var isSettingsLaunched = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (arguments?.containsKey(ARG_CACHED_SESSION_ID) == true) {
            val cachedSessionId = arguments?.getLong(ARG_CACHED_SESSION_ID)
            if (cachedSessionId != null) {
                viewModel.setCachedSessionId(cachedSessionId)
                viewModel.restoreUploadSession()
            }
        }

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
                            showStoragePermissionRationale(false)
                        }
                        map[Constant.PERMISSION_PERMANENTLY_DENIED]?.let {
                            requireContext().showToast("Storage permission is required to upload photos")
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
        savedInstanceState: Bundle?,
    ): View {
        return inflater.inflate(R.layout.fragment_upload_step2, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentUploadStep2Binding.bind(view)

        binding.bindState(
            uiState = viewModel.uiState,
            uiEvent = viewModel.uiEvent
        )
    }

    private fun FragmentUploadStep2Binding.bindState(
        uiState: StateFlow<UploadStep2State>,
        uiEvent: SharedFlow<UploadStep2UiEvent>,
    ) {
        viewLifecycleOwner.lifecycleScope.launch {
            uiEvent.collectLatest { event ->
                when (event) {
                    is UploadStep2UiEvent.PrepareUpload -> {
                        sharedViewModel.setCurrentUploadSessionId(event.sessionId)
                        gotoNextScreen()
                    }
                }
            }
        }

        val shouldShowExamplesFlow =  uiState.map {
            it.previewModelList.filterIsInstance<UploadPreviewUiModel.Item>()
        }.map { it.isEmpty() }
            .distinctUntilChanged()
        viewLifecycleOwner.lifecycleScope.launch {
            shouldShowExamplesFlow.collectLatest { shouldShowExamples ->
                examplesContainer.isVisible = shouldShowExamples
                privacyDisclosureContainer.isVisible = shouldShowExamples
                listContainer.isVisible = !shouldShowExamples
            }
        }

        val uploadPreviewAdapterCallback = object : UploadPreviewAdapter.Callback {
            override fun onItemClick(position: Int, model: UploadPreviewUiModel.Item) {
                try {
                    model as UploadPreviewUiModel.Item
                    // viewModel.toggleSelectionInternal(model.selectedMediaItem.uri)
                } catch (ignore: Exception) {
                }
            }

            override fun onPlaceholderClick(position: Int) {
                launchPhotoPicker()
            }

            override fun onDeleteClick(position: Int, model: UploadPreviewUiModel.Item) {
                viewModel.deletePhoto(model.selectedMediaItem.uri)
            }

        }

        val adapter = UploadPreviewAdapter(
            callback = uploadPreviewAdapterCallback,

            )

        previewList.adapter = adapter

        val previewModelListFlow = uiState.map { it.previewModelList }
            .distinctUntilChanged()
        viewLifecycleOwner.lifecycleScope.launch {
            previewModelListFlow.collectLatest { previewModelList ->
                adapter.submitList(previewModelList)
            }
        }

        val loadStateFlow = uiState.map { it.loadState }
            .distinctUntilChanged()
        viewLifecycleOwner.lifecycleScope.launch {
            loadStateFlow.collectLatest { loadState ->
                if (loadState.action is LoadState.Loading) {
                    btnNext.setSpinning()
                } else {
                    btnNext.cancelSpinning()
                }
            }
        }

        bindExamplesAdapter()

        btnNext.setOnClickListener {
            /*if (uiState.value.remainingPhotoCount >= MIN_IMAGES || BuildConfig.DEBUG) {
                viewModel.startUpload(requireContext())
            } else {
                launchPhotoPicker()
            }*/
            val picked = uiState.value.previewModelList
                .filterIsInstance<UploadPreviewUiModel.Item>()
                .count { it.selected }
            if (picked > 0) {
                viewModel.startUpload(requireContext())
            } else {
                launchPhotoPicker()
            }
        }

        val remainingPhotosCountFlow = uiState.map { it.remainingPhotoCount }
            .distinctUntilChanged()
        viewLifecycleOwner.lifecycleScope.launch {
            remainingPhotosCountFlow.collectLatest { remainingPhotos ->
                Timber.d("Remaining photos: $remainingPhotos")
                val btnTextString = when {
                    remainingPhotos > MIN_IMAGES -> "Select 10 - 20 selfies"
                    else -> "Continue"
                }
                btnNext.setText(btnTextString)
                if (remainingPhotos != MAX_IMAGES) {
                    previewListTitle.isVisible = true
                    previewListTitle.text = "${MAX_IMAGES - remainingPhotos} photos selected"
                } else {
                    previewListTitle.isVisible = false
                    previewListTitle.text = null
                }
            }
        }

        /*viewLifecycleOwner.lifecycleScope.launch {
            uiState.map { it.pickedUris }.collectLatest { uris ->
                if (!isFaceDetectionRunning) {
                    // detectFacesInternal(uris)
                }
            }
        }*/

        val faceDetectionRunningFlow = uiState.map { it.detectingFaces }
            .distinctUntilChanged()
        viewLifecycleOwner.lifecycleScope.launch {
            faceDetectionRunningFlow.collectLatest { isRunning ->
                // fullscreenLoader.isVisible = isRunning
                if (isRunning) {
                    btnNext.setSpinning()
                } else {
                    btnNext.cancelSpinning()
                }
            }
        }
    }

    private fun FragmentUploadStep2Binding.bindExamplesAdapter() {
        val goodPhotoSamples = listOf(
            R.drawable.good_photo_1,
            R.drawable.good_photo_2,
            R.drawable.good_photo_3,
            R.drawable.good_photo_4,
        )
        val goodExamplesAdapter = ExamplePhotoAdapter(ExamplePhotoType.GOOD).also {
            it.submitList(goodPhotoSamples)
        }
        goodExamplesList.adapter = goodExamplesAdapter

        val badPhotoSamples = listOf(
            R.drawable.bad_photo_1,
            R.drawable.bad_photo_2,
            R.drawable.bad_photo_3,
            R.drawable.bad_photo_4,
        )
        val badExamplesAdapter = ExamplePhotoAdapter(ExamplePhotoType.BAD).also {
            it.submitList(badPhotoSamples)
        }
        badExamplesList.adapter = badExamplesAdapter
    }

    private fun preprocessUris(pickedUris: List<Uri>) {
        viewModel.removeDuplicates(pickedUris) { duplicateCount, newUris ->
            Timber.d("Duplicate result: $duplicateCount $newUris")
            if (duplicateCount > 0) {
                SimpleDialog(
                    requireContext(),
                    message = "$duplicateCount duplicate photos have been removed.",
                    positiveButtonText = "Okay"
                ).show()
                HapticUtil.createOneShot(requireContext())
            }
            faceDetectionJob = detectFacesInternal(newUris)
        }
    }

    private fun detectFacesInternal(pickedUris: List<Uri>) =
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            // TODO: Clean this sheet
            isFaceDetectionRunning = true
            viewModel.setFaceDetectionRunning(isFaceDetectionRunning)
            val result = HashMap<String, Boolean>()
            val faceDetectorOpts = FaceDetectorOptions.Builder()
                .setMinFaceSize(0.5F)
                .build()
            val faceDetector = FaceDetection.getClient(faceDetectorOpts)
            val countDownLatch = CountDownLatch(pickedUris.size)
            val taskList: List<Task<List<Face>>> = pickedUris.mapIndexed { index, uri ->
                val ft = faceDetector.process(InputImage.fromFilePath(requireContext(), uri))
                    .addOnCompleteListener { countDownLatch.countDown() }
                    .addOnFailureListener { countDownLatch.countDown() }
                ft
            }
            runBlocking(Dispatchers.IO) {
                countDownLatch.await(1, TimeUnit.MINUTES)
                val previewModelList = taskList.mapIndexed { index, task ->
                    val trackedIds = task.result.mapNotNull { it.trackingId }.distinct()
                    result[pickedUris[index].toString()] = task.result.isNotEmpty()/* &&
                        trackedIds.size == 1*/
                    Timber.d("Detecting: id = $index faces $trackedIds")
                    UploadPreviewUiModel.Item(
                        SelectedMediaItem(pickedUris[index]),
                        selected = task.result.isNotEmpty()
                    )
                }
                viewModel.setPickedUris(previewModelList)
                Timber.d("Detected Faces: $result")
            }
            isFaceDetectionRunning = false
            viewModel.setFaceDetectionRunning(isFaceDetectionRunning)
        }

    private fun launchPhotoPicker() {
        val maxPick = viewModel.getMaxImages()
        Timber.d("Mx pick: $maxPick")
        if (BuildConfig.DEBUG) {
            context?.showToast("Pick up to $maxPick selfies")
        }
        if (ActivityResultContracts.PickVisualMedia.isPhotoPickerAvailable()) {
            photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        } else {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                if (!checkStoragePermission()) {
                    askStoragePermission()
                } else {
                    photoPickerGenericLauncher.launch(MIME_TYPE_JPEG)
                }
            } else {
                photoPickerGenericLauncher.launch(MIME_TYPE_JPEG)
            }
        }
    }

    private fun gotoNextScreen() = safeCall {
        findNavController().apply {
            navigate(UploadStep2FragmentDirections.actionUploadStep2ToUploadStep3())
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

    private fun checkStoragePermission(): Boolean {
        return storagePermissions.all {
            ContextCompat.checkSelfPermission(requireContext(), it) ==
                    PackageManager.PERMISSION_GRANTED
        }
    }

    private fun askStoragePermission() {
        storagePermissionLauncher.launch(storagePermissions)
    }

    override fun onResume() {
        super.onResume()
        if (isSettingsLaunched) {
            // gotoCamera()
            isSettingsLaunched = false
        }
    }

    companion object {
        const val MAX_IMAGES = 20
        const val MIN_IMAGES = 10

        const val ARG_CACHED_SESSION_ID = "cached_session_id"
    }
}

class UploadPreviewAdapter(
    private val callback: Callback,
) : ListAdapter<UploadPreviewUiModel, ViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return when (viewType) {
            VIEW_TYPE_PLACEHOLDER -> PlaceholderVH.from(parent)
            VIEW_TYPE_ITEM -> ItemViewHolder.from(parent)
            else -> throw IllegalStateException("Unknown view type $viewType")
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val model = getItem(position)
        when (holder) {
            is PlaceholderVH -> {
                model as UploadPreviewUiModel.Placeholder
                holder.bind(model, callback)
            }
            is ItemViewHolder -> {
                model as UploadPreviewUiModel.Item
                holder.bind(model, model.selected, callback)
            }
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isNotEmpty()) {
            if (isValidPayload(payloads)) {
                val bundle = (payloads.firstOrNull() as? Bundle) ?: kotlin.run {
                    super.onBindViewHolder(holder, position, payloads); return
                }
                Timber.d("Chat Adapter: payloads ${bundle.keySet().joinToString()}")
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
        private val binding: ItemUploadPreviewBinding,
    ) : ViewHolder(binding.root) {

        fun bind(data: UploadPreviewUiModel.Item, selected: Boolean, callback: Callback) =
            with(binding) {
                Glide.with(view1)
                    .load(data.selectedMediaItem.uri)
                    .placeholder(R.color.white_grey)
                    .into(view1)
                view1.strokeColor = null
                view1.strokeWidth = 0f

                ivClose.isVisible = true

                toggleSelection(selected)

                view1.setOnClickListener { callback.onItemClick(adapterPosition, data) }
                ivClose.setOnClickListener { callback.onDeleteClick(adapterPosition, data) }
            }

        fun toggleSelection(selected: Boolean) = with(binding) {
            selectionIndicator.isVisible = true
            if (selected) {
                selectionIndicator.setImageResource(R.drawable.ic_thumbs_up)
                view1.alpha = 1.0F
            } else {
                selectionIndicator.setImageResource(R.drawable.ic_thumbs_down)
                view1.alpha = 0.5F
            }
            /*if (selected) {
                // profileImage.isVisible = false
                flip(selectionIndicator, true)
            } else {
                // profileImage.isVisible = true
                // flip(selectionIndicator, false)
                selectionIndicator.isVisible = false
            }*/
        }

        private fun flip(v: View, show: Boolean) {
            val start = if (show) 180f else 0f
            val end = if (show) -180f else 0f
            ObjectAnimator.ofFloat(v, View.ROTATION_Y, 90f, 0f).apply {
                duration = 200
                interpolator = AccelerateInterpolator()
                doOnStart { if (show) v.isVisible = true }
                doOnEnd { if (!show) v.isVisible = false }
                start()
            }
        }

        companion object {
            fun from(parent: ViewGroup): ItemViewHolder {
                val itemView = LayoutInflater.from(parent.context).inflate(
                    R.layout.item_upload_preview,
                    parent,
                    false
                )
                val binding = ItemUploadPreviewBinding.bind(itemView)
                return ItemViewHolder(binding)
            }
        }
    }

    class PlaceholderVH private constructor(
        private val binding: ItemUploadPreviewPlaceholderBinding,
    ) : ViewHolder(binding.root) {

        fun bind(data: UploadPreviewUiModel.Placeholder, callback: Callback) = with(binding) {
            view1.setImageResource(R.drawable.ic_add_placeholder)

            root.setOnClickListener {
                view1.touchInteractFeedback()
                callback.onPlaceholderClick(adapterPosition)
            }
        }

        companion object {
            fun from(parent: ViewGroup): PlaceholderVH {
                val itemView = LayoutInflater.from(parent.context).inflate(
                    R.layout.item_upload_preview_placeholder,
                    parent,
                    false
                )
                val binding = ItemUploadPreviewPlaceholderBinding.bind(itemView)
                return PlaceholderVH(binding)
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (val model = getItem(position)) {
            is UploadPreviewUiModel.Placeholder -> VIEW_TYPE_PLACEHOLDER
            is UploadPreviewUiModel.Item -> VIEW_TYPE_ITEM
            else -> throw IllegalStateException("Unable to determine a view type. Unknown model $model")
        }
    }

    interface Callback {
        fun onItemClick(position: Int, model: UploadPreviewUiModel.Item)
        fun onDeleteClick(position: Int, model: UploadPreviewUiModel.Item)
        fun onPlaceholderClick(position: Int)
    }

    companion object {
        private const val VIEW_TYPE_ITEM = 0
        private const val VIEW_TYPE_PLACEHOLDER = 1

        const val SELECTION_TOGGLE_PAYLOAD = "selection_toggle"

        val DIFF_CALLBACK = object : ItemCallback<UploadPreviewUiModel>() {
            override fun areItemsTheSame(
                oldItem: UploadPreviewUiModel,
                newItem: UploadPreviewUiModel,
            ): Boolean {
                return when {
                    oldItem is UploadPreviewUiModel.Item && newItem is UploadPreviewUiModel.Item -> {
                        oldItem.selectedMediaItem.uri == newItem.selectedMediaItem.uri
                    }
                    oldItem is UploadPreviewUiModel.Placeholder && newItem is UploadPreviewUiModel.Placeholder -> {
                        true
                    }
                    else -> false
                }
            }

            override fun areContentsTheSame(
                oldItem: UploadPreviewUiModel,
                newItem: UploadPreviewUiModel,
            ): Boolean {
                return (oldItem is UploadPreviewUiModel.Item && newItem is UploadPreviewUiModel.Item &&
                        oldItem.selectedMediaItem.uri == newItem.selectedMediaItem.uri &&
                        oldItem.selected == newItem.selected)
            }

            override fun getChangePayload(
                oldItem: UploadPreviewUiModel,
                newItem: UploadPreviewUiModel,
            ): Any {
                val updatePayload = bundleOf()
                when {
                    oldItem is UploadPreviewUiModel.Item && newItem is UploadPreviewUiModel.Item -> {
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

class ExamplePhotoAdapter(
    val examplePhotoType: ExamplePhotoType,
) : ListAdapter<Int, ExamplePhotoAdapter.ItemViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(
            R.layout.item_example_photo,
            parent,
            false
        )
        return ItemViewHolder(ItemExamplePhotoBinding.bind(itemView))
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val model = getItem(position)
        holder.bind(model)
    }

    inner class ItemViewHolder(
        private val binding: ItemExamplePhotoBinding,
    ) : ViewHolder(binding.root) {

        fun bind(@DrawableRes drawableRes: Int) = with(binding) {
            view1.setImageResource(drawableRes)
            when (examplePhotoType) {
                ExamplePhotoType.GOOD -> emoji.setImageResource(R.drawable.ic_thumbs_up)
                ExamplePhotoType.BAD -> emoji.setImageResource(R.drawable.ic_thumbs_down)
            }
        }

    }

    companion object {
        val DIFF_CALLBACK = object : ItemCallback<Int>() {
            override fun areItemsTheSame(oldItem: Int, newItem: Int): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(oldItem: Int, newItem: Int): Boolean {
                return true
            }

        }
    }
}

enum class ExamplePhotoType {
    GOOD, BAD
}
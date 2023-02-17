package com.aiavatar.app.feature.home.presentation.create

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.app.NotificationCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavDeepLinkBuilder
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.work.ForegroundInfo
import com.aiavatar.app.*
import com.aiavatar.app.analytics.Analytics
import com.aiavatar.app.analytics.AnalyticsLogger
import com.aiavatar.app.viewmodels.SharedViewModel
import com.aiavatar.app.commons.util.ServiceUtil
import com.aiavatar.app.commons.util.cancelSpinning
import com.aiavatar.app.commons.util.net.NoInternetException
import com.aiavatar.app.commons.util.setSpinning
import com.aiavatar.app.commons.util.shakeNow
import com.aiavatar.app.core.data.source.local.entity.UploadFileStatus
import com.aiavatar.app.core.data.source.local.entity.UploadSessionStatus
import com.aiavatar.app.core.data.source.local.model.UploadSessionWithFilesEntity
import com.aiavatar.app.core.domain.model.ModelStatus
import com.aiavatar.app.databinding.FragmentAvatarStatusBinding
import com.aiavatar.app.di.ApplicationDependencies
import com.aiavatar.app.eventbus.NewNotificationEvent
import com.aiavatar.app.service.MyFirebaseMessagingService
import com.aiavatar.app.viewmodels.UserViewModel
import com.aiavatar.app.work.UploadWorker
import com.aiavatar.app.commons.util.loadstate.LoadState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import timber.log.Timber
import java.util.concurrent.CancellationException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

/**
 * TODO: handle offline status, retry upload.
 */
@AndroidEntryPoint
class AvatarStatusFragment : Fragment() {

    @Inject
    lateinit var analyticsLogger: AnalyticsLogger

    private val viewModel: AvatarStatusViewModel by viewModels()
    private val sharedViewModel: SharedViewModel by activityViewModels()
    private val userViewModel: UserViewModel by activityViewModels()

    private var countDownJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.apply {
            getString(Constant.ARG_STATUS_ID, null)?.let { statusId ->
                viewModel.setAvatarStatusId(statusId)
            }
            getLong(Constant.ARG_UPLOAD_SESSION_ID, -1L).let { uploadSessionId ->
                if (uploadSessionId != -1L) {
                    viewModel.beginUpload(uploadSessionId)
                    createForegroundInfo(requireContext(), 0)
                }
            }
        }

        /*ApplicationDependencies.getPersistentStore().currentAvatarStatusId?.let {
            viewModel.setAvatarStatusId(it)
        }*/
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return inflater.inflate(R.layout.fragment_avatar_status, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentAvatarStatusBinding.bind(view)

        // TODO: bind state
        binding.bindState(
            uiState = viewModel.uiState,
            uiAction = viewModel.accept,
            uiEvent = viewModel.uiEvent
        )
        setupObservers()
        handleBackPressed()
        analyticsLogger.logEvent(Analytics.Event.AVATAR_STATUS_PAGE_PRESENTED)
    }

    private fun FragmentAvatarStatusBinding.bindState(
        uiState: StateFlow<AvatarStatusState>,
        uiAction: (AvatarStatusUiAction) -> Unit,
        uiEvent: SharedFlow<AvatarStatusUiEvent>,
    ) {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                uiEvent.collectLatest { event ->
                    when (event) {
                        is AvatarStatusUiEvent.ShowToast -> {
                            context?.showToast(event.message.asString(requireContext()))
                        }
                        is AvatarStatusUiEvent.NotifyUploadProgress -> {
                            if (event.isComplete) {
                                notifyUploadComplete(requireContext(), event.progress)
                            }
                        }
                    }
                }
            }
        }

        // TODO: combine with avatar status
        val sessionStatusFlow = uiState.map { it.sessionStatus }
            .distinctUntilChanged()
        /*viewLifecycleOwner.lifecycleScope.launch {
            sessionStatusFlow.collectLatest { sessionStatus ->
                Timber.d("Session status: $sessionStatus")
                when (sessionStatus) {
                    UploadSessionStatus.PARTIALLY_DONE -> {
                        *//*description.text = "Uploading photos.."
                        btnCreateAvatar.isVisible = false
                        progressIndicator.isVisible = true
                        textProgressHint.isVisible = false
                        cbNotifyMe.isVisible = false*//*
                    }
                    UploadSessionStatus.UPLOAD_COMPLETE -> {
                        *//*description.text = "Yay! Your photos for creating avatar!"
                        btnCreateAvatar.isVisible = true
                        progressIndicator.isVisible = false
                        textProgressHint.isVisible = false
                        cbNotifyMe.isVisible = false*//*
                    }
                    else -> {
                        // Noop
                    }
                }
            }
        }*/

        // TODO: combine with session status
        val avatarStatusWithFilesFlow = uiState.map { it.avatarStatusWithFiles }
            .distinctUntilChanged()
        /*viewLifecycleOwner.lifecycleScope.launch {
            avatarStatusWithFilesFlow.collectLatest { avatarStatusWithFiles ->
                Timber.d("Avatar status: $avatarStatusWithFiles")
            }
        }*/

        viewLifecycleOwner.lifecycleScope.launch {
            combine(
                sessionStatusFlow,
                avatarStatusWithFilesFlow,
                ::Pair
            ).collectLatest { (sessionStatus, avatarStatusWithFiles) ->
                Timber.d("Avatar status: $sessionStatus $avatarStatusWithFiles")
                if (avatarStatusWithFiles != null) {
                    // Avatar status
                    when (avatarStatusWithFiles.avatarStatus.modelStatus) {
                        ModelStatus.TRAINING_PROCESSING -> {
                            description.text =
                                "We're pouring out hearts and souls into this project, \nwe ask for a bit more time"

                            logo.isVisible = false
                            thinking.isVisible = true

                            btnCreateAvatar.isVisible = false
                            progressIndicator.isVisible = true
                            progressIndicator.isIndeterminate = true
                            textProgressHint.isVisible = true
                            cbNotifyMe.isVisible = true

                            val etaTimeString = getFormattedTime(avatarStatusWithFiles.avatarStatus.eta)
                            // textProgressHint.text = "ETA $etaTime"
                            val futureTimeMillis = System.currentTimeMillis() +
                                    avatarStatusWithFiles.avatarStatus.eta * 1000L
                            etaCountdownView.isVisible = true
                            etaCountdownView.startCountDownTo(futureTimeMillis)
                           /* countDownJob?.cancel(CancellationException("New request"))
                            countDownJob = countDownFlow(
                                avatarStatusWithFiles.avatarStatus.eta.seconds
                            )
                                .onStart {
                                    textProgressHint.isVisible = true
                                }
                                .onEach { elapsed ->
                                    textProgressHint.text =
                                        getFormattedTime(elapsed.toInt())
                                }
                                .onCompletion {
                                    textProgressHint.isVisible = false
                                }
                                .launchIn(viewLifecycleOwner.lifecycleScope)*/

                            dismissUploadStatusNotification()
                        }
                        ModelStatus.AVATAR_PROCESSING -> {
                            description.text = "Generating your awesome photos!"

                            logo.isVisible = false
                            thinking.isVisible = true

                            btnCreateAvatar.isVisible = false
                            progressIndicator.isVisible = true
                            etaCountdownView.isVisible = false
                            progressIndicator.isIndeterminate = false
                            textProgressHint.isVisible = true
                            textProgressHint.text =
                                "${avatarStatusWithFiles.avatarStatus.generatedAiCount}/" +
                                        "${avatarStatusWithFiles.avatarStatus.totalAiCount}"
                            val progress =
                                (avatarStatusWithFiles.avatarStatus.generatedAiCount.toFloat() / avatarStatusWithFiles.avatarStatus.totalAiCount)
                                    .coerceIn(0.0F, 1.0F)
                            progressIndicator.progress = (progress * 100).toInt()

                            cbNotifyMe.isVisible = true

                            dismissUploadStatusNotification()
                        }
                        ModelStatus.COMPLETED -> {

                            logo.isVisible = true
                            thinking.isVisible = false

                            progressIndicator.isVisible = false
                            textProgressHint.isVisible = false
                            cbNotifyMe.isVisible = false
                            etaCountdownView.isVisible = false

                            description.text = "Yay! Your avatars are ready!"
                            btnCreateAvatar.isVisible = true
                            btnCreateAvatar.text = "View Results"
                            progressIndicator.isVisible = false
                            textProgressHint.isVisible = false
                            cbNotifyMe.isVisible = false

                            // dismissModelStatusNotification()
                        }
                        ModelStatus.TRAINING_FAILED -> {
                            // TODO: retry uploading fresh images
                            logo.isVisible = true
                            thinking.isVisible = false

                            progressIndicator.isVisible = false
                            textProgressHint.isVisible = false
                            cbNotifyMe.isVisible = false
                            etaCountdownView.isVisible = false

                            description.text = "Something went wrong! Please try again."
                            btnCreateAvatar.isVisible = true
                            btnCreateAvatar.idleText = "Retry"
                            progressIndicator.isVisible = false
                            textProgressHint.isVisible = false
                            cbNotifyMe.isVisible = false

                            dismissUploadStatusNotification()
                        }
                        else -> {
                            logo.isVisible = true
                            thinking.isVisible = false
                            btnCreateAvatar.isVisible = false
                            progressIndicator.isVisible = false
                            textProgressHint.isVisible = false
                            cbNotifyMe.isVisible = false
                            etaCountdownView.isVisible = false
                        }
                    }

                } else if (sessionStatus.status <= UploadSessionStatus.FAILED.status) {
                    when (UploadSessionStatus.fromRawValue(sessionStatus.status)) {
                        UploadSessionStatus.PARTIALLY_DONE -> {
                            // val uploadingStatusString = getUploadingStatusString(uiState.value.uploadSessionWithFilesEntity)

                            // TODO: fix realtime updates
                            description.text = getString(R.string.uploading_photos_, null)
                            btnCreateAvatar.isVisible = false
                            progressIndicator.isVisible = true
                            progressIndicator.isIndeterminate = true
                            textProgressHint.isVisible = false
                            cbNotifyMe.isVisible = false
                            etaCountdownView.isVisible = false
                        }
                        UploadSessionStatus.UPLOAD_COMPLETE -> {
                            // description.text = "Yay! Your photos are ready for creating avatar!"
                            description.text = "Please wait.."
                            btnCreateAvatar.isVisible = false
                            progressIndicator.isVisible = false
                            textProgressHint.isVisible = false
                            etaCountdownView.isVisible = false
                            cbNotifyMe.isVisible = false
                        }
                        UploadSessionStatus.FAILED -> {
                            val uploadSessionWithFiles = uiState.value.uploadSessionWithFilesEntity
                            if (uploadSessionWithFiles != null) {
                                val failedUploads =
                                    uploadSessionWithFiles.uploadFilesEntity.map { it.uploadedFileName == null }
                                        .count()
                                description.text =
                                    "Some photos are failed to upload. Please upload again."
                                btnCreateAvatar.idleText = "Retry Upload"
                                progressIndicator.isVisible = false
                                textProgressHint.isVisible = false
                                etaCountdownView.isVisible = false
                                cbNotifyMe.isVisible = false
                            } else {
                                description.text = "Oops! something went wrong"
                                btnCreateAvatar.isVisible = false
                                progressIndicator.isVisible = false
                                textProgressHint.isVisible = false
                                etaCountdownView.isVisible = false
                                cbNotifyMe.isVisible = false
                            }
                        }
                        else -> {
                            // Noop yet
                            etaCountdownView.isVisible = false
                        }
                    }
                } else {

                }
            }
        }

        val loadStateFlow = uiState.map { it.loadState }
            .distinctUntilChanged()
        viewLifecycleOwner.lifecycleScope.launch {
            loadStateFlow.collectLatest { loadState ->
                // TODO: show loader
                if (loadState.action is LoadState.Loading) {
                    btnCreateAvatar.setSpinning()
                } else {
                    btnCreateAvatar.cancelSpinning()
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
                        if (e !is NoInternetException) {
                            if (btnCreateAvatar.isVisible) {
                                btnCreateAvatar.shakeNow()
                            }
                        }
                        retryButton.isVisible = e is NoInternetException
                        uiErr?.let { uiText -> context?.showToast(uiText.asString(requireContext())) }
                        uiAction(AvatarStatusUiAction.ErrorShown(e))
                    }
                }
            }
        }

        val notifyMeToggleStateFlow = uiState.map { it.toggleStateNotifyMe }
            .distinctUntilChanged()
        viewLifecycleOwner.lifecycleScope.launch {
            notifyMeToggleStateFlow.collectLatest { toggleStateNotifyMe ->
                cbNotifyMe.isChecked = toggleStateNotifyMe
                analyticsLogger.logEvent(Analytics.Event.AVATAR_STATUS_NOTIFY_ME_TOGGLE)
            }
        }

        cbNotifyMe.setOnCheckedChangeListener { _, isChecked ->
            uiAction(AvatarStatusUiAction.ToggleNotifyMe(isChecked))
        }

        val progressHintFlow = uiState.map { it.progressHint }
            .distinctUntilChanged()
        viewLifecycleOwner.lifecycleScope.launch {
            progressHintFlow.collectLatest { progressHint ->
                textProgressHint.text = progressHint
            }
        }

        bindUploadStatus(
            uiState = uiState,
            uiAction = uiAction
        )

        bindClick(
            uiState = uiState,
            uiAction = uiAction
        )
    }

    private fun FragmentAvatarStatusBinding.bindClick(
        uiState: StateFlow<AvatarStatusState>,
        uiAction: (AvatarStatusUiAction) -> Unit,
    ) {
        btnCreateAvatar.setOnClickListener {
            val modelStatus = uiState.value.avatarStatusWithFiles?.avatarStatus?.modelStatus
            val sessionStatus = uiState.value.sessionStatus
            if (modelStatus == ModelStatus.COMPLETED) {
                // TODO: View results
                ApplicationDependencies.getPersistentStore().apply {
                    if (isLogged) {
                        setProcessingModel(false)
                    }
                }
                gotoAvatarResult(uiState.value.avatarStatusId!!)
                analyticsLogger.logEvent(Analytics.Event.AVATAR_STATUS_VIEW_RESULTS_CLICK)
            } else if (modelStatus == ModelStatus.TRAINING_FAILED) {
                gotoUploads(null)
            } else if (sessionStatus == UploadSessionStatus.FAILED) {
                val cachedSessionId = uiState.value.sessionId
                gotoUploads(cachedSessionId)
            } else {
                ServiceUtil.getNotificationManager(requireContext())
                    .cancel(UploadWorker.STATUS_NOTIFICATION_ID)
                uiAction(AvatarStatusUiAction.CreateModel)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            userViewModel.loginUser.collectLatest { loginUser ->
                if (loginUser?.userId != null) {
                    btnClose.isVisible = true
                    btnClose.setOnClickListener {
                        activity?.onBackPressedDispatcher?.onBackPressed()
                        analyticsLogger.logEvent(Analytics.Event.AVATAR_STATUS_CLOSE_BTN_CLICK)
                    }
                } else {
                    // If the user isn't logged in, then this is a blocker page
                    btnClose.isVisible = false
                }
            }
        }

        retryButton.setOnClickListener { viewModel.refresh() }
    }

    private fun FragmentAvatarStatusBinding.bindUploadStatus(
        uiState: StateFlow<AvatarStatusState>,
        uiAction: (AvatarStatusUiAction) -> Unit
    ) {
        val uploadStatusStringFlow = uiState.map { it.uploadStatusString }
            .distinctUntilChanged()
        viewLifecycleOwner.lifecycleScope.launch {
            uploadStatusStringFlow.collectLatest { uploadStatusString ->
                uploadStatusString?.let {
                    description.text = it.asString(requireContext())
                }
            }
        }

    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            sharedViewModel.currentUploadSessionId.collectLatest { sessionId ->
                Timber.d("Session id: $sessionId")
                if (sessionId != null) {
                    viewModel.setSessionId(sessionId)
                }
            }
        }

        val isGuestUserFlow = userViewModel.loginUser
            .map { it == null }
        viewLifecycleOwner.lifecycleScope.launch {
            combine(
                viewModel.runningTrainingsFlow,
                isGuestUserFlow,
                ::Pair
            ).collectLatest { (runningStatus, isGuestUser) ->
                Timber.d("Avatar status: $runningStatus guest = $isGuestUser")
                if (runningStatus.isNotEmpty()) {
                    if (isGuestUser) {
                        viewModel.setAvatarStatusId(runningStatus.last().avatarStatusId.toString())
                    } else {
                        /*runningStatus
                            .find { it.modelStatus != ModelStatus.COMPLETED.statusString }
                            ?.avatarStatusId?.let { statusId ->
                            viewModel.setAvatarStatusId(statusId)
                        }*/
                    }
                }
            }
        }
    }

    private fun handleBackPressed() {
        activity?.onBackPressedDispatcher?.addCallback(viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    // Do nothing. The page automatically closes
                    safeCall {
                        findNavController().apply {
                            if (!navigateUp()) {
                                // TODO: clean this up!
                                if (ApplicationDependencies.getPersistentStore().isLogged) {
                                    gotoHome()
                                } else {
                                    // If the user isn't logged in, then is is a blocker page
                                    // Nothing much to do.
                                    activity?.finishAffinity()
                                }
                            }
                        }
                    }
                }
            })
    }

    private fun gotoAvatarResult(avatarStatusId: String) = safeCall {
        findNavController().apply {
            val navOpts = defaultNavOptsBuilder()
                .build()
            val args = Bundle().apply {
                putString(Constant.ARG_STATUS_ID, avatarStatusId)
            }

            navigate(R.id.avatar_result, args, navOpts)
        }
    }

    private fun gotoUploads(cachedSessionId: Long?) = safeCall {
        findNavController().apply {
            val navOpts = NavOptions.Builder()
                .setEnterAnim(R.anim.slide_in_right)
                .setExitAnim(R.anim.slide_out_right)
                .setPopEnterAnim(R.anim.slide_in_right)
                .setPopExitAnim(R.anim.slide_out_right)
                .setPopUpTo(R.id.main_nav_graph, inclusive = true, saveState = false)
                .build()
            val args = Bundle().apply {
                /*
                * TODO: IMPORTANT - To restore previous upload session, uncomment the cachedSessionId
                * */
                /*if (cachedSessionId != null) {
                    putLong(UploadStep2Fragment.ARG_CACHED_SESSION_ID, cachedSessionId)
                }*/
            }
            navigate(R.id.upload_step_2, args, navOpts)
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
            viewModel.refresh()
        }
    }

    private fun getUploadingStatusString(uploadSessionWithFiles: UploadSessionWithFilesEntity?): String {
        uploadSessionWithFiles ?: return ""
        val uploadingFiles = uploadSessionWithFiles.uploadFilesEntity
        val finishedUploads = uploadingFiles.count { it.status == UploadFileStatus.COMPLETE.status }
        return "$finishedUploads of ${uploadingFiles.size}"
    }

    private fun notifyUploadComplete(context: Context, photosCount: Int) {
        ServiceUtil.getNotificationManager(context).cancel(UPLOAD_ONGOING_NOTIFICATION_ID)
        val channelId = context.getString(R.string.upload_notification_channel_id)

        val contentIntent = NavDeepLinkBuilder(context)
            .setGraph(R.navigation.home_nav_graph)
            .setDestination(R.id.avatar_status)
            .setComponentName(MainActivity::class.java)
            .createPendingIntent()

        val notificationBuilder = NotificationCompat.Builder(context, channelId)
        val notification = notificationBuilder
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationManager.IMPORTANCE_HIGH)
            .setCategory(Notification.CATEGORY_STATUS)
            .setOngoing(false)
            .setAutoCancel(true)
            .setContentTitle("Upload Complete!")
            .setContentText("$photosCount Photos uploaded. Tap here to check status!")
            .setContentIntent(contentIntent)
            .build()
        ServiceUtil.getNotificationManager(context)
            .notify(UPLOAD_STATUS_NOTIFICATION_ID, notification)
    }

    private fun createForegroundInfo(context: Context, progress: Int): ForegroundInfo {
        createUploadNotificationChannel(context)

        val channelId = context.getString(R.string.upload_notification_channel_id)

        val contentIntent = NavDeepLinkBuilder(context)
            .setGraph(R.navigation.home_nav_graph)
            .setDestination(R.id.avatar_status)
            .setComponentName(MainActivity::class.java)
            .createPendingIntent()

        val notificationBuilder = NotificationCompat.Builder(context, channelId)
        val notification = notificationBuilder
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationManager.IMPORTANCE_DEFAULT)
            .setCategory(Notification.CATEGORY_PROGRESS)
            .setContentTitle("Preparing upload")
            .setProgress(100, progress, true)
            .setOngoing(true)
            .setContentIntent(contentIntent)
            .build()
        return ForegroundInfo(UPLOAD_STATUS_NOTIFICATION_ID, notification)
    }

    private fun createUploadNotificationChannel(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as
                NotificationManager
        val channelId = context.getString(R.string.upload_notification_channel_id)
        val channelName = context.getString(R.string.title_upload_notifications)
        val channelDesc = context.getString(R.string.desc_upload_notifications)
        val channel =
            NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT)
        channel.description = channelDesc

        notificationManager.createNotificationChannel(channel)
    }

    private fun dismissUploadStatusNotification() {
        ServiceUtil.getNotificationManager(requireContext())
            .cancel(UPLOAD_STATUS_NOTIFICATION_ID)
    }

    private fun dismissModelStatusNotification() {
        ServiceUtil.getNotificationManager(requireContext())
            .cancel(MyFirebaseMessagingService.AVATAR_STATUS_NOTIFICATION_ID)
    }

    private fun getFormattedTime(etaSeconds: Int): String {
        val millisUntilFinished = etaSeconds * 1000L
        var millisUntilFinished2 = millisUntilFinished
        var secondInMillis = millisUntilFinished / 60
        val minuteInMillis = secondInMillis * 60
        val hourInMillis = minuteInMillis * 60

        val elapsedHours: Long = TimeUnit.MILLISECONDS.toHours(millisUntilFinished2) % 60
        // millisUntilFinished2 %= hourInMillis

        val elapsedMinutes: Long = TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished2) % 60
        //millisUntilFinished2 %= minuteInMillis

        val elapsedSeconds: Long = TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished2) % 60

        val uiText = String.format(
            "%02d:%02d:%02d",
            elapsedHours,
            elapsedMinutes,
            elapsedSeconds
        )
        Timber.d("Timer: $uiText")
        return uiText
    }

    companion object {
        const val UPLOAD_ONGOING_NOTIFICATION_ID    = 1001
        const val UPLOAD_COMPLETE_NOTIFICATION_ID   = 1002
        const val UPLOAD_STATUS_NOTIFICATION_ID     = 3001
    }
}
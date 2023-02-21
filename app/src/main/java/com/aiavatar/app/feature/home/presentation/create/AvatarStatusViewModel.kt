package com.aiavatar.app.feature.home.presentation.create

import android.net.Uri
import androidx.core.net.toFile
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiavatar.app.BuildConfig
import com.aiavatar.app.Constant
import com.aiavatar.app.R
import com.aiavatar.app.commons.util.Result
import com.aiavatar.app.commons.util.UiText
import com.aiavatar.app.commons.util.loadstate.LoadType
import com.aiavatar.app.commons.util.net.ApiException
import com.aiavatar.app.commons.util.net.NoInternetException
import com.aiavatar.app.commons.util.net.ProgressRequestBody
import com.aiavatar.app.core.data.source.local.AppDatabase
import com.aiavatar.app.core.data.source.local.entity.UploadFileStatus
import com.aiavatar.app.core.data.source.local.entity.UploadSessionStatus
import com.aiavatar.app.core.data.source.local.entity.toEntity
import com.aiavatar.app.core.data.source.local.model.UploadSessionWithFilesEntity
import com.aiavatar.app.core.data.source.local.model.toAvatarStatusWithFiles
import com.aiavatar.app.core.domain.model.*
import com.aiavatar.app.core.domain.model.request.AvatarStatusRequest
import com.aiavatar.app.core.domain.model.request.CreateModelRequest
import com.aiavatar.app.core.domain.repository.AppRepository
import com.aiavatar.app.di.ApplicationDependencies
import com.aiavatar.app.feature.onboard.domain.model.UploadImageData
import com.aiavatar.app.commons.util.loadstate.LoadState
import com.aiavatar.app.commons.util.loadstate.LoadStates
import com.aiavatar.app.commons.util.net.UnAuthorizedException
import com.aiavatar.app.feature.home.presentation.util.UploadUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import okhttp3.MultipartBody
import timber.log.Timber
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AvatarStatusViewModel @Inject constructor(
    @Deprecated("move to repo")
    private val appDatabase: AppDatabase,
    private val appRepository: AppRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow<AvatarStatusState>(AvatarStatusState())
    val uiState: StateFlow<AvatarStatusState> = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<AvatarStatusUiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    val accept: (AvatarStatusUiAction) -> Unit

    val runningTrainingsFlow = appDatabase.avatarStatusDao()
        .getRunningTraining()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private var createModelJob: Job? = null
    private var avatarStatusJob: Job? = null
    private var uploadPhotosJob: Job? = null
    private var uploadStatusJob: Job? = null
    private var scheduledAvatarStatusJob: Job? = null

    init {
        restoreFromSavedStateInternal(savedStateHandle)

        uiState.mapNotNull { it.sessionId }
            .flatMapLatest { sessionId ->
                appDatabase.uploadSessionDao().getUploadSession(sessionId)
            }
            .onEach { uploadSessionWithFilesEntity ->
                if (uploadSessionWithFilesEntity != null) {
                    _uiState.update { state ->
                        state.copy(
                            uploadSessionWithFilesEntity = uploadSessionWithFilesEntity,
                            sessionStatus = UploadSessionStatus.fromRawValue(
                                uploadSessionWithFilesEntity
                                    .uploadSessionEntity.status
                            )
                        )
                    }
                }
            }
            .launchIn(viewModelScope)

        uiState.mapNotNull { it.avatarStatusId }
            .flatMapLatest { statusId ->
                appDatabase.avatarStatusDao().getAvatarStatus(statusId = statusId)
            }.onEach { avatarStatusWithFilesEntity ->
                if (avatarStatusWithFilesEntity != null) {
                    _uiState.update { state ->
                        state.copy(
                            avatarStatusWithFiles = avatarStatusWithFilesEntity.toAvatarStatusWithFiles()
                        )
                    }
                }
            }
            .launchIn(viewModelScope)

        accept = { uiAction -> onUiAction(uiAction) }
    }

    private fun onUiAction(action: AvatarStatusUiAction) {
        when (action) {
            is AvatarStatusUiAction.ErrorShown -> {
                _uiState.update { state ->
                    state.copy(
                        exception = null,
                        uiErrorText = null
                    )
                }
            }
            is AvatarStatusUiAction.ToggleNotifyMe -> {
                toggleNotifyMeInternal(action.checked)
            }
            is AvatarStatusUiAction.CreateModel -> {
                createModelInternal(uiState.value.sessionId!!)
            }
        }
    }

    fun refresh() {
        getAvatarStatusInternal()
    }

    fun setSessionId(sessionId: Long) {
        _uiState.update { state ->
            state.copy(
                sessionId = sessionId
            )
        }
    }

    fun setAvatarStatusId(statusId: String) {
        _uiState.update { state ->
            state.copy(
                avatarStatusId = statusId
            )
        }
        getAvatarStatusInternal()
    }

    private fun scheduleAvatarStatusJob(forceNew: Boolean = true) {
        if (scheduledAvatarStatusJob?.isActive == true && forceNew) {
            scheduledAvatarStatusJob?.cancel(CancellationException("New request"))
        }
        scheduledAvatarStatusJob = viewModelScope.launch {
            delay(15000)
            getAvatarStatusInternal()
        }
    }

    private fun createModelInternal(sessionId: Long) = viewModelScope.launch {
        Timber.d( "createModelInternal() called")
        val uploadSessionWithFilesEntity = appDatabase.uploadSessionDao().getUploadSessionSync(sessionId)
        val fileNameArray: List<String> = uploadSessionWithFilesEntity?.uploadFilesEntity?.mapNotNull { it.uploadedFileName }
            ?: emptyList()
        if (uploadSessionWithFilesEntity != null) {
            val request = CreateModelRequest(
                folderName = uploadSessionWithFilesEntity.uploadSessionEntity.folderName,
                trainingType = uploadSessionWithFilesEntity.uploadSessionEntity.trainingType,
                files = fileNameArray,
                fcm = ApplicationDependencies.getPersistentStore().fcmToken
            )
            createModel(request)
        } else {
            val cause = IllegalStateException("session data not found")
            _uiState.update { state ->
                state.copy(
                    exception = cause,
                    uiErrorText = UiText.somethingWentWrong
                )
            }
        }
    }

    private fun createModel(request: CreateModelRequest) {
        if (createModelJob?.isActive == true) {
            val t = IllegalStateException("A login request is already in progress. Ignoring request")
            Timber.d(t)
            return
        }
        createModelJob?.cancel(CancellationException("New request")) // just in case
        createModelJob = viewModelScope.launch {
            appRepository.createModel(request).collectLatest { result ->
                when (result) {
                    is Result.Loading -> setLoading(LoadType.ACTION, LoadState.Loading())
                    is Result.Error -> {
                        when (result.exception) {
                            is ApiException -> {
                                when (result.exception.cause) {
                                    is UnAuthorizedException -> { /* Noop */ }
                                    else -> {
                                        _uiState.update { state ->
                                            state.copy(
                                                exception = result.exception,
                                                uiErrorText = UiText.somethingWentWrong
                                            )
                                        }
                                    }
                                }
                            }
                            is NoInternetException -> {
                                _uiState.update { state ->
                                    state.copy(
                                        exception = result.exception,
                                        uiErrorText = UiText.noInternet
                                    )
                                }
                            }
                        }
                        setLoading(LoadType.ACTION, LoadState.Error(result.exception))
                    }
                    is Result.Success -> {
                        setLoading(LoadType.ACTION, LoadState.NotLoading.Complete)
                        ApplicationDependencies.getPersistentStore().apply {
                            setProcessingModel(true)
                            result.data.guestUserId?.let { setGuestUserId(it) }
                        }
                        appDatabase.uploadSessionDao().apply {
                            appDatabase.avatarStatusDao().apply {
                                val newAvatarStatus = AvatarStatus.emptyStatus(result.data.modelId).apply {
                                    avatarStatusId = result.data.statusId
                                }
                                insert(newAvatarStatus.toEntity())
                            }
                        }
                        // TODO: get avatar status
                        setAvatarStatusId(result.data.statusId.toString())
                        getAvatarStatusInternal()
                    }
                }
            }
        }

    }

    private fun toggleNotifyMeInternal(checked: Boolean) {
        _uiState.update { state ->
            state.copy(
                toggleStateNotifyMe = checked
            )
        }
        savedStateHandle[TOGGLE_STATE_NOTIFY_ME] = checked
        ApplicationDependencies.getPersistentStore().setNotifyUponCompletion(checked)
    }

    private fun getAvatarStatusInternal() = viewModelScope.launch {
        try {
            val currentStatusId: String? = uiState.value.avatarStatusId
            if (currentStatusId != null) {
                val status = appDatabase.avatarStatusDao().getAvatarStatusSync(currentStatusId)
                if (status?.avatarStatusEntity?.modelStatus != ModelStatus.COMPLETED.statusString) {
                    val request = AvatarStatusRequest(currentStatusId)
                    getStatus(request)
                } else {
                    // Status is already in completed state
                }
            } else {
                val t = IllegalStateException("Failed to parse status id")
                Timber.e(t)
            }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Timber.e(e)
            }
        }
    }

    private fun getStatus(request: AvatarStatusRequest) {
        if (avatarStatusJob?.isActive == true) {
            val t = IllegalStateException("A status request job is already active. Ignoring request.")
            Timber.e(t)
            return
        }
        avatarStatusJob?.cancel(CancellationException("New request")) // just in case
        avatarStatusJob = viewModelScope.launch {
            appRepository.avatarStatus(request).collectLatest { result ->
                when (result) {
                    is Result.Loading -> setLoading(LoadType.ACTION, LoadState.Loading())
                    is Result.Error -> {
                        when (result.exception) {
                            is UnAuthorizedException -> { /* Noop */ }
                            is ApiException -> {
                                _uiState.update { state ->
                                    state.copy(
                                        exception = result.exception,
                                        uiErrorText = UiText.somethingWentWrong
                                    )
                                }
                            }
                            is NoInternetException -> {
                                _uiState.update { state ->
                                    state.copy(
                                        exception = result.exception,
                                        uiErrorText = UiText.noInternet
                                    )
                                }
                            }
                        }
                        setLoading(LoadType.ACTION, LoadState.Error(result.exception))

                        scheduleAvatarStatusJob(forceNew = true)
                    }
                    is Result.Success -> {
                        setLoading(LoadType.ACTION, LoadState.NotLoading.Complete)

                        if (result.data.avatarStatus.modelStatus != ModelStatus.COMPLETED) {
                            scheduleAvatarStatusJob(forceNew = true)
                        }
                        /*val avatarStatus = result.data.avatarStatus
                        when (avatarStatus.modelStatus) {
                            "training_processing" -> {
                                // TODO: update UI
                            }
                            "avatar_processing" -> {
                                val progress = (avatarStatus.generatedAiCount.toFloat() / avatarStatus.totalAiCount)
                                    .coerceIn(0.0F, 1.0F)
                                *//*_uiState.update { state ->
                                    state.copy(
                                        progressHint = "${avatarStatus.generatedAiCount}/${avatarStatus.totalAiCount}"
                                    )
                                }*//*
                            }
                            "completed" -> {
                                *//*ApplicationDependencies.getPersistentStore().apply {
                                    setProcessingModel(false)
                                    setGuestUserId("")
                                }
                                appDatabase.uploadSessionDao().apply {
                                    updateUploadSessionStatus(
                                        uiState.value.sessionId!!,
                                        UploadSessionStatus.CREATING_MODEL.status
                                    )
                                    updateUploadSessionModelId(
                                        uiState.value.sessionId!!,
                                        result.data.statusId
                                    )
                                }*//*
                            }
                        }*/
                        // sendEvent(Step3UiEvent.NextScreen)
                    }
                }
            }
        }
    }

    fun beginUpload(sessionId: Long) {
        uploadPhotosJob = beginUploadInternal(sessionId)
    }

    /* Upload related */
    private fun beginUploadInternal(sessionId: Long) = viewModelScope.launch {
        /*val sessionId = workerParameters.inputData.getLong(UploadWorker.KEY_SESSION_ID, -1L)
        if (sessionId == -1L) {
            return abortWork("No session id, aborting photo upload.")
        }*/

        val uploadSessionWithFiles = appDatabase.uploadSessionDao().getUploadSessionSync(sessionId)
            ?: return@launch uploadFailed("No upload session data found for session $sessionId")

        appDatabase.uploadSessionDao().updateUploadSessionStatus(
            uploadSessionWithFiles.uploadSessionEntity._id!!,
            UploadSessionStatus.PARTIALLY_DONE.status
        )
        val uploadResultList: List<Deferred<Result<UploadImageData>>> =
            uploadSessionWithFiles.uploadFilesEntity
                .filter { it.uploadedFileName == null }
                .map { uploadFilesEntity ->
                    val task = viewModelScope.async(Dispatchers.IO) {
                        Timber.d("Preparing upload ${uploadFilesEntity.fileUriString}")
                        val file = Uri.parse(uploadFilesEntity.fileUriString).toFile()
                        val progressRequestBody = ProgressRequestBody(
                            file,
                            Constant.MIME_TYPE_IMAGE,
                            object : ProgressRequestBody.ProgressCallback {
                                override fun onProgressUpdate(percentage: Int) {
                                    Timber.d("Uploading: ${file.name} PROGRESS $percentage")
                                    // progressCallback((percentage / 10f).coerceIn(0.0F, 1.0F))
                                    runBlocking {
                                        appDatabase.uploadFilesDao()
                                            .updateFileUploadProgress(uploadFilesEntity._id!!, percentage)
                                    }
                                }

                                override fun onError() {

                                }
                            }
                        )
                        val filePart: MultipartBody.Part =
                            MultipartBody.Part.createFormData(
                                "files",
                                file.name,
                                progressRequestBody
                            )
                        val folderNamePart: MultipartBody.Part =
                            MultipartBody.Part.createFormData(
                                "folderName",
                                uploadSessionWithFiles.uploadSessionEntity.folderName
                            )
                        val fileNamePart: MultipartBody.Part =
                            MultipartBody.Part.createFormData(
                                "fileName",
                                file.name
                            )
                        val typePart = MultipartBody.Part.createFormData(
                            "type",
                            "photo_sample"
                        )

                        val result = appRepository.uploadFileSync(
                            folderName = folderNamePart,
                            type = typePart,
                            fileName = fileNamePart,
                            files = filePart
                        )
                        when (result) {
                            is com.aiavatar.app.commons.util.Result.Loading -> {
                                appDatabase.uploadFilesDao().updateFileStatus(
                                    uploadFilesEntity._id!!,
                                    UploadFileStatus.UPLOADING.status
                                )
                            }

                            is com.aiavatar.app.commons.util.Result.Success -> {
                                appDatabase.uploadFilesDao().updateUploadedFileName(
                                    uploadFilesEntity._id!!,
                                    result.data.imageName,
                                    System.currentTimeMillis()
                                )
                                val affectedRows = appDatabase.uploadFilesDao().updateFileStatus(
                                    uploadFilesEntity._id!!,
                                    UploadFileStatus.COMPLETE.status
                                )
                                Timber.d("Upload file status: affected = $affectedRows")
                            }

                            is com.aiavatar.app.commons.util.Result.Error -> {
                                appDatabase.uploadFilesDao().updateFileStatus(
                                    uploadFilesEntity._id!!,
                                    UploadFileStatus.FAILED.status
                                )
                            }
                        }
                        result
                    }
                    task
                }

        observeUploadStatusInternal()

        uploadResultList.awaitAll()
        uploadResultList.map { it.getCompleted() }
            .forEachIndexed { index, result ->
                if (result is com.aiavatar.app.commons.util.Result.Success) {
                    Timber.d("Upload result: ${index + 1} ${result.data.imageName} success")
                }
            }

        val totalUploads = appDatabase.uploadFilesDao().getAllUploadFilesSync(sessionId)
            .mapNotNull { it.uploadedFileName }.count()
        if (totalUploads < UploadUtil.getMinUploadImageCount() /* Min upload size */) {
            appDatabase.uploadSessionDao().updateUploadSessionStatus(
                uploadSessionWithFiles.uploadSessionEntity._id!!,
                UploadSessionStatus.FAILED.status
            )
            uploadFailed("Unable to complete upload.")
        } else {
            appDatabase.uploadSessionDao().updateUploadSessionStatus(
                uploadSessionWithFiles.uploadSessionEntity._id!!,
                UploadSessionStatus.UPLOAD_COMPLETE.status
            )

            /*if (uploadResultList.isNotEmpty() && !ApplicationDependencies.getAppForegroundObserver().isForegrounded) {
                notifyUploadComplete(context, uploadResultList.size)
            }*/
            sendEvent(AvatarStatusUiEvent.NotifyUploadProgress(uploadResultList.size, true))
            createModelInternal(sessionId)

            /* when (val result = createModelSyncInternal(uploadSessionWithFiles.uploadSessionEntity._id!!).await()) {
                is com.aiavatar.app.commons.util.Result.Success -> {
                    ApplicationDependencies.getPersistentStore().apply {
                        setProcessingModel(true)
                        result.data.guestUserId?.let { setGuestUserId(it) }
                    }
                    appDatabase.uploadSessionDao().apply {
                        appDatabase.avatarStatusDao().apply {
                            val newAvatarStatus = AvatarStatus.emptyStatus(result.data.modelId).apply {
                                avatarStatusId = result.data.statusId
                            }
                            insert(newAvatarStatus.toEntity())
                        }
                    }
                    /*// TODO: get avatar status
                    val request = AvatarStatusRequest(result.data.statusId.toString())
                    getStatus(request)*/
                }
                else -> {
                    uploadFailed("Create model request failed")
                }
            } */
        }
        uploadStatusJob?.cancel(CancellationException("Upload is complete!"))
        _uiState.update { state ->
            state.copy(
                uploadStatusString = null
            )
        }
    }

    private fun uploadFailed(message: String) {
        // TODO: handle upload failure
    }

    private fun observeUploadStatusInternal() {
        val uploadSessionWithFilesEntityFlow =
            uiState.map { it.uploadSessionWithFilesEntity }
                .distinctUntilChanged()
        uploadStatusJob = viewModelScope.launch {
            uploadSessionWithFilesEntityFlow.collectLatest {
                getUploadingStatusString(it)?.let { statusString ->
                    _uiState.update { state ->
                        state.copy(
                            uploadStatusString = UiText.StringResource(R.string.uploading_photos_, statusString)
                        )
                    }
                }

            }
        }
    }

    private fun getUploadingStatusString(uploadSessionWithFiles: UploadSessionWithFilesEntity?): String? {
        uploadSessionWithFiles ?: return null
        val uploadingFiles = uploadSessionWithFiles.uploadFilesEntity
        val finishedUploads = uploadingFiles.count { it.status == UploadFileStatus.COMPLETE.status }
        return "$finishedUploads of ${uploadingFiles.size}"
    }
    /* END - Upload related */

    /* Model creation */
    private fun createModelSyncInternal(sessionId: Long): Deferred<com.aiavatar.app.commons.util.Result<CreateModelData>> = viewModelScope.async {
        Timber.d( "createModelInternal() called")
        val uploadSessionWithFilesEntity = appDatabase.uploadSessionDao().getUploadSessionSync(sessionId)
        val fileNameArray: List<String> = uploadSessionWithFilesEntity?.uploadFilesEntity?.mapNotNull { it.uploadedFileName }
            ?: emptyList()
        if (uploadSessionWithFilesEntity != null) {
            val request = CreateModelRequest(
                folderName = uploadSessionWithFilesEntity.uploadSessionEntity.folderName,
                trainingType = uploadSessionWithFilesEntity.uploadSessionEntity.trainingType,
                files = fileNameArray,
                fcm = ApplicationDependencies.getPersistentStore().fcmToken
            )
            createModelSync(request)
        } else {
            val cause = IllegalStateException("session data not found")
            com.aiavatar.app.commons.util.Result.Error(cause)
        }
    }

    private suspend fun createModelSync(request: CreateModelRequest): com.aiavatar.app.commons.util.Result<CreateModelData> {
        return appRepository.createModelSync(request)
    }
    /* END - Model creation */

    private fun setLoading(
        loadType: LoadType,
        loadState: LoadState
    ) {
        val newLoadState = uiState.value.loadState.modifyState(loadType, loadState)
        _uiState.update { state ->
            state.copy(
                loadState = newLoadState
            )
        }
    }

    private fun restoreFromSavedStateInternal(savedStateHandle: SavedStateHandle) {
        _uiState.update { state ->
            state.copy(
                toggleStateNotifyMe = savedStateHandle[TOGGLE_STATE_NOTIFY_ME] ?: getPreferredToggleState()
            )
        }
    }

    private fun sendEvent(newEvent: AvatarStatusUiEvent) = viewModelScope.launch {
        _uiEvent.emit(newEvent)
    }

    private fun getPreferredToggleState(): Boolean {
        return ApplicationDependencies.getPersistentStore().notifyMe
    }

    override fun onCleared() {
        val t = CancellationException("View model is dead")
        scheduledAvatarStatusJob?.cancel(t)
        avatarStatusJob?.cancel(t)
        createModelJob?.cancel(t)
        uploadPhotosJob?.cancel(t)
        super.onCleared()
    }

}

data class AvatarStatusState(
    val loadState: LoadStates = LoadStates.IDLE,
    val sessionId: Long? = null,
    val sessionStatus: UploadSessionStatus = UploadSessionStatus.UNKNOWN,
    val uploadSessionWithFilesEntity: UploadSessionWithFilesEntity? = null,
    val avatarStatusId: String? = null,
    val avatarStatusWithFiles: AvatarStatusWithFiles? = null,
    val toggleStateNotifyMe: Boolean = DEFAULT_TOGGLE_STATE,
    val progressHint: String? = null,
    val uploadStatusString: UiText? = null,
    val exception: Exception? = null,
    val uiErrorText: UiText? = null
)

interface AvatarStatusUiAction {
    data class ErrorShown(val e: Exception) : AvatarStatusUiAction
    data class ToggleNotifyMe(val checked: Boolean) : AvatarStatusUiAction
    object CreateModel : AvatarStatusUiAction
}

interface AvatarStatusUiEvent {
    data class ShowToast(val message: UiText) : AvatarStatusUiEvent
    data class NotifyUploadProgress(val progress: Int, val isComplete: Boolean) : AvatarStatusUiEvent
}

private const val TOGGLE_STATE_NOTIFY_ME = "toggle_state_notify_me"

private const val DEFAULT_TOGGLE_STATE: Boolean = true
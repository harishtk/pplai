package com.aiavatar.app.feature.home.presentation.create

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiavatar.app.*
import com.aiavatar.app.commons.util.*
import com.aiavatar.app.commons.util.loadstate.LoadType
import com.aiavatar.app.commons.util.net.ApiException
import com.aiavatar.app.commons.util.net.NoInternetException
import com.aiavatar.app.core.data.source.local.AppDatabase
import com.aiavatar.app.core.data.source.local.entity.*
import com.aiavatar.app.core.data.source.local.model.toAvatarStatusWithFiles
import com.aiavatar.app.core.domain.model.AvatarStatusWithFiles
import com.aiavatar.app.core.domain.model.DownloadFile
import com.aiavatar.app.core.domain.model.DownloadSession
import com.aiavatar.app.core.domain.model.request.AvatarStatusRequest
import com.aiavatar.app.core.domain.model.request.RenameModelRequest
import com.aiavatar.app.core.domain.repository.AppRepository
import com.aiavatar.app.feature.home.domain.model.ModelAvatar
import com.aiavatar.app.feature.home.domain.repository.HomeRepository
import com.aiavatar.app.commons.util.loadstate.LoadState
import com.aiavatar.app.commons.util.loadstate.LoadStates
import com.aiavatar.app.commons.util.net.UnAuthorizedException
import com.aiavatar.app.feature.home.presentation.catalog.ModelDetailUiAction
import com.aiavatar.app.feature.home.presentation.catalog.ModelDetailUiEvent
import com.aiavatar.app.feature.onboard.domain.model.ShareLinkData
import com.aiavatar.app.feature.onboard.domain.model.request.GetShareLinkRequest
import com.aiavatar.app.feature.onboard.domain.repository.AccountsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.lang.StringBuilder
import java.util.concurrent.CancellationException
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AvatarPreviewViewModel @Inject constructor(
    private val appRepository: AppRepository,
    private val homeRepository: HomeRepository,
    private val accountsRepository: AccountsRepository,
    private val appDatabase: AppDatabase,
): ViewModel() {

    private val _uiState = MutableStateFlow<AvatarPreviewState>(AvatarPreviewState())
    val uiState: StateFlow<AvatarPreviewState> = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<AvatarPreviewUiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    private var selectedAvatarPosition: Int = 0
    private val selectedToggleFlow = MutableStateFlow(false)

    val accept: (AvatarPreviewUiAction) -> Unit

    private var avatarsFetchJob: Job? = null
    private var avatarStatusJob: Job? = null
    private var modelDetailFetchJob: Job? = null
    private var createDownloadSessionJob: Job? = null
    private var getShareLinkJob: Job? = null

    init {
        val selectableAvatarUiModelListFlow = uiState.map { it.avatarList }
            .distinctUntilChanged()

        combine(
            selectedToggleFlow,
            selectableAvatarUiModelListFlow,
            ::Pair
        ).map { (selectedToggle, selectableAvatarList) ->
            val newSelectableAvatarList = selectableAvatarList.mapIndexed { index, selectableAvatarUiModel ->
                if (selectableAvatarUiModel is SelectableAvatarUiModel.Item) {
                    selectableAvatarUiModel.copy(selected = index == selectedAvatarPosition)
                } else {
                    selectableAvatarUiModel
                }
            }
            newSelectableAvatarList
        }.onEach { selectableAvatarUiModelList ->
            _uiState.update { state ->
                state.copy(
                    avatarList = selectableAvatarUiModelList
                )
            }
        }.launchIn(viewModelScope)

        uiState.mapNotNull { it.statusId }
            .distinctUntilChanged()
            .flatMapLatest { statusId ->
                appDatabase.avatarStatusDao().getAvatarStatus(statusId = statusId)
            }.onEach { avatarStatusWithFilesEntity ->
                Timber.d("onEach: 1")
                if (avatarStatusWithFilesEntity != null) {
                    val avatarResultList = avatarStatusWithFilesEntity.avatarFilesEntity.map {
                        SelectableAvatarUiModel.Item(it.toModelAvatar(), selected = false)
                    }
                    _uiState.update { state ->
                        state.copy(
                            avatarStatusWithFiles = avatarStatusWithFilesEntity.toAvatarStatusWithFiles(),
                            avatarList = avatarResultList
                        )
                    }
                }
            }
            .launchIn(viewModelScope)

        accept = { uiAction -> onUiAction(uiAction) }
    }

    private fun onUiAction(action: AvatarPreviewUiAction) {
        when (action) {
            is AvatarPreviewUiAction.ErrorShown -> {
                _uiState.update { state ->
                    state.copy(
                        exception = null,
                        uiErrorText = null
                    )
                }
            }
            is AvatarPreviewUiAction.GetShareLink -> {
                uiState.value.avatarStatusWithFiles?.avatarStatus?.modelId?.let { getShareLinkInternal(it) }
            }
        }
    }

    fun refresh() {
        uiState.value.statusId?.let { statusId ->
            val request = AvatarStatusRequest(id = statusId)
            getStatus(request)
            // getAvatarsForStatus(statusId)
        }
    }

    fun setStatusId(statusId: String) {
        _uiState.update { state ->
            state.copy(
                statusId = statusId
            )
        }
    }

    fun createDownloadSession(modelName: String) {
        createDownloadSessionInternal(modelName)
    }

    fun saveModelName(modelName: String) = viewModelScope.launch {
        val modelId = getModelId()
        if (modelId != null) {
            val request = RenameModelRequest(
                modelId = modelId, modelName = modelName
            )
            appRepository.renameModel(request).collectLatest { result ->
                when (result) {
                    is Result.Loading -> { setLoading(LoadType.ACTION, LoadState.Loading()) }
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
                    }
                    is Result.Success -> {
                        val affectedRows = appDatabase.avatarStatusDao().updateModelNameForModelId(modelId, modelName, true)
                        Timber.d("Update model name: affected $affectedRows rows")
                        setLoading(LoadType.ACTION, LoadState.NotLoading.Complete)
                        createDownloadSessionInternal(modelName, LoadType.ACTION)
                    }
                }
            }
        } else {
            val t = IllegalStateException("Failed to get model id")
            _uiState.update { state ->
                state.copy(
                    exception = ResolvableException(t),
                    uiErrorText =  UiText.somethingWentWrong
                )
            }
        }
    }

    fun getModelId(): String? {
        return uiState.value.avatarStatusWithFiles?.avatarStatus?.modelId
    }

    fun toggleSelection(position: Int) {
        selectedAvatarPosition = position
        // Signals the flow
        selectedToggleFlow.update { selectedToggleFlow.value.not() }
    }

    fun downloadCurrentAvatar(context: Context) {
        downloadCurrentAvatarInternal(context)
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
                    }
                    is Result.Success -> {
                        setLoading(LoadType.ACTION, LoadState.NotLoading.Complete)

                        /*_uiState.update { state ->
                            state.copy(
                                avatarStatusWithFiles = result.data
                            )
                        }*/

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

    private fun getAvatarsForStatus(statusId: String) {
        avatarStatusJob?.cancel(CancellationException("Refresh"))
        avatarStatusJob = uiState.mapNotNull { it.statusId }
            .flatMapLatest { statusId ->
                appDatabase.avatarStatusDao().getAvatarStatus(statusId = statusId)
            }.onEach { avatarStatusWithFilesEntity ->
                Timber.d("onEach: 1")
                if (avatarStatusWithFilesEntity != null) {
                    val avatarResultList = avatarStatusWithFilesEntity.avatarFilesEntity.map {
                        SelectableAvatarUiModel.Item(it.toModelAvatar(), selected = false)
                    }
                    _uiState.update { state ->
                        state.copy(
                            avatarStatusWithFiles = avatarStatusWithFilesEntity.toAvatarStatusWithFiles(),
                            avatarList = avatarResultList
                        )
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    /* Download session related */
    private fun downloadCurrentAvatarInternal(context: Context) = viewModelScope.launch {
        val modelAvatar = uiState.value.avatarList
            .filterIsInstance<com.aiavatar.app.feature.home.presentation.catalog.SelectableAvatarUiModel.Item>()
            .find { uiModel -> uiModel.selected }
            ?.modelAvatar
        if (modelAvatar == null) {
            sendEvent(AvatarPreviewUiEvent.ShowToast(UiText.DynamicString("Cannot download right now. Try later")))
            return@launch
        }

        val modelName = uiState.value.avatarStatusWithFiles?.avatarStatus?.modelName
            ?: uiState.value.avatarStatusWithFiles?.avatarStatus?.modelId

        val relativeDownloadPath = StringBuilder()
            .append(context.getString(R.string.app_name))
            .append(File.separator)
            .append(modelName)
            .toString()

        sendEvent(AvatarPreviewUiEvent.ShowToast(UiText.DynamicString("Downloading..")))
        _uiState.update { state ->
            state.copy(
                currentDownloadProgress = 0
            )
        }

        val mimeType = getMimeType(context, modelAvatar.remoteFile.toUri())
            ?: Constant.MIME_TYPE_JPEG
        val savedUri = StorageUtil.saveFile(
            context = context,
            url = modelAvatar.remoteFile,
            relativePath = relativeDownloadPath,
            mimeType = mimeType,
            displayName = Commons.getFileNameFromUrl(modelAvatar.remoteFile),
        ) { progress, bytesDownloaded ->
            Timber.d("Download: ${modelAvatar.remoteFile} progress = $progress downloaded = $bytesDownloaded")
            _uiState.update { state ->
                state.copy(
                    currentDownloadProgress = progress
                )
            }
            viewModelScope.launch {
                appDatabase.modelAvatarDao().apply {
                    updateDownloadProgress(modelAvatar._id!!, progress)
                    if (progress == 100) {
                        updateDownloadStatus(
                            id = modelAvatar._id!!,
                            downloaded = true,
                            downloadedAt = System.currentTimeMillis(),
                            downloadSize = bytesDownloaded
                        )
                    }
                }
            }
        }
        if (savedUri != null) {
            sendEvent(AvatarPreviewUiEvent.DownloadComplete(savedUri))
        } else {
            appDatabase.modelAvatarDao().apply {
                updateDownloadStatus(
                    id = modelAvatar._id!!,
                    downloaded = false,
                    downloadedAt = 0,
                    downloadSize = 0
                )
            }
            sendEvent(AvatarPreviewUiEvent.ShowToast(UiText.DynamicString("Cannot download right now. Try later")))
        }

        _uiState.update { state ->
            state.copy(
                currentDownloadProgress = null
            )
        }
    }

    private fun createDownloadSessionInternal(modelName: String, loadType: LoadType = LoadType.ACTION) {
        createDownloadSessionJob = viewModelScope.launch {
            setLoading(loadType, LoadState.Loading())
            val modelId  = uiState.value.avatarStatusWithFiles?.avatarStatus?.modelId

            val downloadSession = DownloadSession(
                createdAt = System.currentTimeMillis(),
                status = DownloadSessionStatus.NOT_STARTED,
                folderName = modelName,
                modelId = modelId.nullAsEmpty()
            )
            appDatabase.downloadSessionDao().apply {
                insert(downloadSession.toEntity()).let { id ->
                    downloadSession.id = id
                }
            }

            val downloadFiles: List<DownloadFile> = if (modelId != null) {
                appDatabase.modelAvatarDao().getModelAvatarsSync(modelId)
                    .map { modelAvatarEntity ->
                        DownloadFile(
                            sessionId = downloadSession.id!!,
                            fileUri = modelAvatarEntity.remoteFile.toUri(),
                            status = DownloadFileStatus.NOT_STARTED
                        )
                    }
            } else {
                emptyList()
            }

            if (downloadFiles.isNotEmpty()) {
                // TODO: add download files and schedule worker
                appDatabase.downloadFilesDao().insertAll(
                    downloadFiles.map(DownloadFile::toEntity)
                )
                sendEvent(AvatarPreviewUiEvent.StartDownload(downloadSessionId = downloadSession.id!!))
                setLoading(loadType, LoadState.NotLoading.Complete)
            } else {
                val t = IllegalStateException("Nothing to download")
                setLoading(loadType, LoadState.Error(t))
            }
        }
    }
    /* END - Download session related */

    /* Share link */
    private fun getShareLinkInternal(modelId: String) {
        val request = GetShareLinkRequest(
            modelId = modelId,
            avatarId = "",
            folderName = "",
            fileName = ""
        )

        getShareLink(request)
    }

    private fun getShareLink(request: GetShareLinkRequest) {
        if (getShareLinkJob?.isActive == true) {
            val t = IllegalStateException("A share link fetch job is already active. Ignoring..")
            ifDebug { Timber.w(t) }
            return
        }

        getShareLinkJob?.cancel(CancellationException("New request")) // just in case
        getShareLinkJob = viewModelScope.launch {
            accountsRepository.getShareLink(request).collectLatest { result ->
                when (result) {
                    is Result.Loading -> {
                        val newLoadState = uiState.value.shareLoadState.modifyState(LoadType.REFRESH, LoadState.Loading())
                        _uiState.update { state -> state.copy(shareLoadState = newLoadState) }
                    }
                    is Result.Error -> {
                        when (result.exception) {
                            is NoInternetException -> {
                                _uiState.update { state ->
                                    state.copy(
                                        exception = result.exception,
                                        uiErrorText = UiText.noInternet
                                    )
                                }
                            }
                            else -> {
                                _uiState.update { state ->
                                    state.copy(
                                        exception = result.exception,
                                        uiErrorText = UiText.somethingWentWrong
                                    )
                                }
                            }
                        }
                        val newLoadState = uiState.value.shareLoadState.modifyState(LoadType.REFRESH, LoadState.Error(result.exception))
                        _uiState.update { state -> state.copy(shareLoadState = newLoadState) }
                    }
                    is Result.Success -> {
                        val newLoadState = uiState.value.shareLoadState.modifyState(LoadType.REFRESH, LoadState.NotLoading.Complete)
                        _uiState.update { state ->
                            state.copy(
                                shareLoadState = newLoadState,
                                shareLinkData = result.data
                            )
                        }
                        sendEvent(AvatarPreviewUiEvent.ShareLink(result.data.shortLink))
                    }
                }
            }
        }
    }
    /* END - Share link */

    @Suppress("SameParameterValue")
    private fun setLoading(
        loadType: LoadType,
        loadState: LoadState
    ) {
        val newLoadState = uiState.value.loadState.modifyState(loadType, loadState)
        _uiState.update { it.copy(loadState = newLoadState) }
    }

    private fun sendEvent(newEvent: AvatarPreviewUiEvent) = viewModelScope.launch {
        _uiEvent.emit(newEvent)
    }

    override fun onCleared() {
        val t = CancellationException("View model is dead")
        avatarStatusJob?.cancel(t)
        avatarsFetchJob?.cancel(t)
        createDownloadSessionJob?.cancel(t)
        getShareLinkJob?.cancel(t)
        super.onCleared()
    }

}

data class AvatarPreviewState(
    val loadState: LoadStates = LoadStates.IDLE,
    val statusId: String? = null,
    val avatarStatusWithFiles: AvatarStatusWithFiles? = null,
    val avatarList: List<SelectableAvatarUiModel> = emptyList(),
    val currentDownloadProgress: Int? = null,
    val shareLoadState: LoadStates = LoadStates.IDLE,
    val shareLinkData: ShareLinkData? = null,
    val exception: Exception? = null,
    val uiErrorText: UiText? = null
)

interface AvatarPreviewUiAction {
    data class ErrorShown(val e: Exception) : AvatarPreviewUiAction
    object GetShareLink : AvatarPreviewUiAction
}

interface AvatarPreviewUiEvent {
    data class ShowToast(val message: UiText) : AvatarPreviewUiEvent
    data class StartDownload(val downloadSessionId: Long) : AvatarPreviewUiEvent
    data class DownloadComplete(val savedUri: Uri) : AvatarPreviewUiEvent
    data class ShareLink(val link: String) : AvatarPreviewUiEvent
}

interface SelectableAvatarUiModel {
    data class Item(val modelAvatar: ModelAvatar, val selected: Boolean) : SelectableAvatarUiModel
}
package com.aiavatar.app.feature.home.presentation.catalog

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiavatar.app.*
import com.aiavatar.app.commons.util.ResolvableException
import com.aiavatar.app.commons.util.Result
import com.aiavatar.app.commons.util.StorageUtil
import com.aiavatar.app.commons.util.UiText
import com.aiavatar.app.commons.util.loadstate.LoadType
import com.aiavatar.app.commons.util.net.ApiException
import com.aiavatar.app.commons.util.net.NoInternetException
import com.aiavatar.app.core.data.source.local.AppDatabase
import com.aiavatar.app.core.data.source.local.entity.*
import com.aiavatar.app.core.domain.model.DownloadFile
import com.aiavatar.app.core.domain.model.DownloadSession
import com.aiavatar.app.core.domain.model.request.RenameModelRequest
import com.aiavatar.app.core.domain.repository.AppRepository
import com.aiavatar.app.feature.home.domain.model.ModelAvatar
import com.aiavatar.app.feature.home.domain.model.ModelData
import com.aiavatar.app.feature.home.domain.model.request.GetAvatarsRequest
import com.aiavatar.app.feature.home.domain.repository.HomeRepository
import com.pepulnow.app.data.LoadState
import com.pepulnow.app.data.LoadStates
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
class ModelDetailViewModel @Inject constructor(
    private val appRepository: AppRepository,
    private val homeRepository: HomeRepository,
    private val appDatabase: AppDatabase,
): ViewModel() {

    private val _uiState = MutableStateFlow<ModelDetailState>(ModelDetailState())
    val uiState: StateFlow<ModelDetailState> = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<ModelDetailUiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    private var selectedAvatarPosition: Int = 0
    private val selectedToggleFlow = MutableStateFlow(false)

    val accept: (ModelDetailUiAction) -> Unit

    private var avatarsFetchJob: Job? = null
    private var avatarStatusJob: Job? = null
    private var modelDetailFetchJob: Job? = null
    private var createDownloadSessionJob: Job? = null

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

        accept = { uiAction -> onUiAction(uiAction) }
    }

    private fun onUiAction(action: ModelDetailUiAction) {
        when (action) {
            is ModelDetailUiAction.ErrorShown -> {
                _uiState.update { state ->
                    state.copy(
                        exception = null,
                        uiErrorText = null
                    )
                }
            }
        }
    }

    fun refresh() {
        uiState.value.modelId?.let { modelId ->
            val request = GetAvatarsRequest(modelId = modelId)
            getModelDetail(modelId)
            getAvatarsForModel(request)
        }
    }

    fun setModelId(modelId: String) {
        _uiState.update { state ->
            state.copy(
                modelId = modelId
            )
        }
    }

    fun getSelectedAvatarPosition(): Int {
        return selectedAvatarPosition
    }

    fun createDownloadSession(modelName: String) {
        TODO("Not supported")
        // createDownloadSessionInternal(modelName)
    }

    fun downloadCurrentAvatar(context: Context) {
        downloadCurrentAvatarInternal(context)
    }

    fun saveModelName(modelName: String, cont: Continuation = {}) = viewModelScope.launch {
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
                        cont()
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
        return uiState.value.modelId
    }

    fun toggleSelection(position: Int) {
        selectedAvatarPosition = position
        // Signals the flow
        selectedToggleFlow.update { selectedToggleFlow.value.not() }
    }

    private fun getModelDetail(modelId: String) {
        modelDetailFetchJob?.cancel(CancellationException("New request"))
        modelDetailFetchJob = viewModelScope.launch {
            homeRepository.getModel2(modelId).collectLatest { result ->
                when (result) {
                    is Result.Loading -> { /* Noop */ }
                    is Result.Error -> {
                        // Something went wrong
                    }
                    is Result.Success -> {
                        _uiState.update { state ->
                            state.copy(
                                modelData = result.data
                            )
                        }
                    }
                }
            }
        }
    }

    private fun getAvatarsForModel(request: GetAvatarsRequest) {
        if (avatarsFetchJob?.isActive == true) {
            val t = IllegalStateException("A fetch job is already active. Ignoring request")
            if (BuildConfig.DEBUG) {
                Timber.w(t)
            }
            return
        }

        avatarsFetchJob?.cancel(CancellationException("New request")) // just in case
        avatarsFetchJob = viewModelScope.launch {
            homeRepository.getAvatars2(request, forceRefresh = false).collectLatest { result ->
                when (result) {
                    is Result.Loading -> setLoading(LoadType.REFRESH, LoadState.Loading())
                    is Result.Error -> {
                        when (result.exception) {
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
                        setLoading(LoadType.REFRESH, LoadState.Error(result.exception))
                    }
                    is Result.Success -> {
                        setLoading(LoadType.REFRESH, LoadState.NotLoading.Complete)
                        _uiState.update { state ->
                            state.copy(
                                avatarList = result.data.map { SelectableAvatarUiModel.Item(it, false) }
                            )
                        }
                    }
                }
            }
        }
    }

    /* Download session related */
    private fun downloadCurrentAvatarInternal(context: Context) = viewModelScope.launch {
        val modelAvatar = uiState.value.avatarList
            .filterIsInstance<SelectableAvatarUiModel.Item>()
            .find { uiModel -> uiModel.selected }
            ?.modelAvatar
        if (modelAvatar == null) {
            sendEvent(ModelDetailUiEvent.ShowToast(UiText.DynamicString("Cannot download right now. Try later")))
            return@launch
        }

        val modelName = uiState.value.modelData?.name ?: modelAvatar.modelId
        val relativeDownloadPath = StringBuilder()
            .append(context.getString(R.string.app_name))
            .append(File.separator)
            .append(modelName)
            .toString()

        sendEvent(ModelDetailUiEvent.ShowToast(UiText.DynamicString("Downloading..")))
        _uiState.update { state ->
            state.copy(
                currentDownloadProgress = 0
            )
        }

        val savedUri = StorageUtil.saveFile(
            context = context,
            url = modelAvatar.remoteFile,
            relativePath = relativeDownloadPath,
            mimeType = Constant.MIME_TYPE_JPEG,
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
            sendEvent(ModelDetailUiEvent.DownloadComplete(savedUri))
        } else {
            appDatabase.modelAvatarDao().apply {
                updateDownloadStatus(
                    id = modelAvatar._id!!,
                    downloaded = false,
                    downloadedAt = 0,
                    downloadSize = 0
                )
            }
            sendEvent(ModelDetailUiEvent.ShowToast(UiText.DynamicString("Cannot download right now. Try later")))
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
            val modelId  = uiState.value.modelId

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
                sendEvent(ModelDetailUiEvent.StartDownload(downloadSessionId = downloadSession.id!!))
                setLoading(loadType, LoadState.NotLoading.Complete)
            } else {
                val t = IllegalStateException("Nothing to download")
                setLoading(loadType, LoadState.Error(t))
            }
        }
    }
    /* END - Download session related */

    @Suppress("SameParameterValue")
    private fun setLoading(
        loadType: LoadType,
        loadState: LoadState
    ) {
        val newLoadState = uiState.value.loadState.modifyState(loadType, loadState)
        _uiState.update { it.copy(loadState = newLoadState) }
    }

    private fun sendEvent(newEvent: ModelDetailUiEvent) = viewModelScope.launch {
        _uiEvent.emit(newEvent)
    }

    override fun onCleared() {
        val t = CancellationException("View model is dead")
        avatarStatusJob?.cancel(t)
        avatarsFetchJob?.cancel(t)
        createDownloadSessionJob?.cancel(t)
        super.onCleared()
    }

}

data class ModelDetailState(
    val loadState: LoadStates = LoadStates.IDLE,
    val modelId: String? = null,
    val modelData: ModelData? = null,
    val avatarList: List<SelectableAvatarUiModel> = emptyList(),
    val currentDownloadProgress: Int? = null,
    val exception: Exception? = null,
    val uiErrorText: UiText? = null
)

interface ModelDetailUiAction {
    data class ErrorShown(val e: Exception) : ModelDetailUiAction
}

interface ModelDetailUiEvent {
    data class ShowToast(val message: UiText) : ModelDetailUiEvent
    data class StartDownload(val downloadSessionId: Long) : ModelDetailUiEvent
    data class DownloadComplete(val savedUri: Uri) : ModelDetailUiEvent
}

interface SelectableAvatarUiModel {
    data class Item(val modelAvatar: ModelAvatar, val selected: Boolean) : SelectableAvatarUiModel
}
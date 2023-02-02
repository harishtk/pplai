package com.aiavatar.app.feature.home.presentation.catalog

import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiavatar.app.BuildConfig
import com.aiavatar.app.commons.util.ResolvableException
import com.aiavatar.app.commons.util.Result
import com.aiavatar.app.commons.util.UiText
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
import com.aiavatar.app.feature.home.domain.model.request.GetAvatarsRequest
import com.aiavatar.app.feature.home.domain.repository.HomeRepository
import com.aiavatar.app.nullAsEmpty
import com.pepulnow.app.data.LoadState
import com.pepulnow.app.data.LoadStates
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
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
            getAvatarsForModel(request)
        }
        uiState.value.statusId?.let { statusId ->
            val request = AvatarStatusRequest(id = statusId)
            getStatus(request)
        }
    }

    fun setModelId(modelId: String) {
        _uiState.update { state ->
            state.copy(
                modelId = modelId
            )
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

    /**
     * Populates the model details from local cache.
     */
    private fun getModelResult(statusId: String) {
        uiState.map { it.modelId }
            .distinctUntilChanged()
            .onStart {
                val request = AvatarStatusRequest(
                    id = statusId
                )
                getStatus(request)
            }
            .flatMapLatest { modelId ->
                Timber.d("Model id: $modelId")
                if (modelId != null) {
                    appDatabase.avatarStatusDao().getAvatarStatusForModelId(modelId = modelId).also {
                        Timber.d("Local result: $it")
                    }
                } else {
                    emptyFlow()
                }
            }.onEach { avatarStatusWithFilesEntity ->
                Timber.d("Local result: $avatarStatusWithFilesEntity")
                if (avatarStatusWithFilesEntity != null) {
                    _uiState.update { state ->
                        state.copy(
                            avatarStatusWithFiles = avatarStatusWithFilesEntity.toAvatarStatusWithFiles(),
                            avatarList = avatarStatusWithFilesEntity.toAvatarStatusWithFiles()
                                .avatarFiles
                                .map { SelectableAvatarUiModel.Item(it.toModelAvatar(), false) }
                        )
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    fun setFrom(from: String) {
        _uiState.update { state ->
            state.copy(
                from = from
            )
        }
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

                        _uiState.update { state ->
                            state.copy(
                                avatarStatusWithFiles = result.data
                            )
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

    /* Download session related */
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
    val from: String? = null,
    val modelId: String? = null,
    val statusId: String? = null,
    val avatarStatusWithFiles: AvatarStatusWithFiles? = null,
    val avatarList: List<SelectableAvatarUiModel> = emptyList(),
    val exception: Exception? = null,
    val uiErrorText: UiText? = null
)

interface ModelDetailUiAction {
    data class ErrorShown(val e: Exception) : ModelDetailUiAction
}

interface ModelDetailUiEvent {
    data class ShowToast(val message: UiText) : ModelDetailUiEvent
    data class StartDownload(val downloadSessionId: Long) : ModelDetailUiEvent
}

interface SelectableAvatarUiModel {
    data class Item(val modelAvatar: ModelAvatar, val selected: Boolean) : SelectableAvatarUiModel
}
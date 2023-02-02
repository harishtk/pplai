package com.aiavatar.app.feature.home.presentation.create

import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
class AvatarPreviewViewModel @Inject constructor(
    private val appRepository: AppRepository,
    private val homeRepository: HomeRepository,
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
        super.onCleared()
    }

}

data class AvatarPreviewState(
    val loadState: LoadStates = LoadStates.IDLE,
    val statusId: String? = null,
    val avatarStatusWithFiles: AvatarStatusWithFiles? = null,
    val avatarList: List<SelectableAvatarUiModel> = emptyList(),
    val exception: Exception? = null,
    val uiErrorText: UiText? = null
)

interface AvatarPreviewUiAction {
    data class ErrorShown(val e: Exception) : AvatarPreviewUiAction
}

interface AvatarPreviewUiEvent {
    data class ShowToast(val message: UiText) : AvatarPreviewUiEvent
    data class StartDownload(val downloadSessionId: Long) : AvatarPreviewUiEvent
}

interface SelectableAvatarUiModel {
    data class Item(val modelAvatar: ModelAvatar, val selected: Boolean) : SelectableAvatarUiModel
}
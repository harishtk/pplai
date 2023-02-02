package com.aiavatar.app.feature.home.presentation.create

import androidx.core.net.toUri
import androidx.lifecycle.SavedStateHandle
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
import com.aiavatar.app.core.domain.model.DownloadFile
import com.aiavatar.app.core.domain.model.DownloadSession
import com.aiavatar.app.core.domain.model.request.RenameModelRequest
import com.aiavatar.app.core.domain.repository.AppRepository
import com.aiavatar.app.feature.home.domain.model.ModelAvatar
import com.aiavatar.app.feature.home.domain.model.ModelData
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
class AvatarResultViewModel @Inject constructor(
    private val appRepository: AppRepository,
    private val homeRepository: HomeRepository,
    @Deprecated("move to repo")
    private val appDatabase: AppDatabase,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow<AvatarResultState>(AvatarResultState())
    val uiState: StateFlow<AvatarResultState> = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<AvatarResultUiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    val accept: (AvatarResultUiAction) -> Unit

    private var avatarsFetchJob: Job? = null
    private var avatarStatusJob: Job? = null
    private var modelDetailFetchJob: Job? = null
    private var createDownloadSessionJob: Job? = null

    init {
        accept = { uiAction -> onUiAction(uiAction) }
    }

    private fun onUiAction(uiAction: AvatarResultUiAction) {
        when (uiAction) {
            is AvatarResultUiAction.ErrorShown -> {
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
        refreshInternal()
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
                        appDatabase.modelDao().updateModelNameForModelId(modelId, modelName, true)
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

    fun setAvatarStatusId(statusId: String) {
        _uiState.update { state ->
            state.copy(
                avatarStatusId = statusId
            )
        }
    }

    fun setModelId(modelId: String) {
        _uiState.update { state ->
            state.copy(
                modelId = modelId
            )
        }
    }

    fun getModelId(): String? {
        return uiState.value.modelData?.id
    }

    fun getStatusId(): String?  {
        return uiState.value.avatarStatusId
    }

    fun createDownloadSession(modelName: String) {
        createDownloadSessionInternal(modelName)
    }

    private fun refreshInternal() {
        // TODO: get data from server
        uiState.value.modelId?.let { modelId ->
            val request = GetAvatarsRequest(modelId = modelId)
            // TODO: get model detail
            getModelDetail(modelId)
            getAvatarsForModel(request)
        }
        uiState.value.avatarStatusId?.let { statusId ->
            getAvatarsForStatus(statusId)
        }
    }

    private fun getAvatarsForStatus(statusId: String) {
        avatarStatusJob?.cancel(CancellationException("Refresh"))
        avatarStatusJob = uiState.mapNotNull { it.avatarStatusId }
            .flatMapLatest { statusId ->
                appDatabase.avatarStatusDao().getAvatarStatus(statusId = statusId)
            }.onEach { avatarStatusWithFilesEntity ->
                if (avatarStatusWithFilesEntity != null) {
                    val avatarResultList = avatarStatusWithFilesEntity.avatarFilesEntity.map {
                        AvatarResultUiModel.AvatarItem(it.toModelAvatar())
                    }
                    val modelData = with(avatarStatusWithFilesEntity.avatarStatusEntity) {
                        ModelData(
                            id = modelId,
                            name = modelName.nullAsEmpty(),
                            latestImage = "",
                            totalCount = this.totalAiCount,
                            paid = this.paid,
                            renamed = this.modelRenamed
                        )
                    }
                    _uiState.update { state ->
                        state.copy(
                            modelData = modelData,
                            avatarResultList = avatarResultList
                        )
                    }
                }
            }
            .launchIn(viewModelScope)
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
            setLoading(LoadType.REFRESH, LoadState.Loading())
            homeRepository.getAvatars2(request, forceRefresh = true).collectLatest { result ->
                Timber.d("getAvatars: $result")
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

                        val avatarResultList = result.data.map {
                            AvatarResultUiModel.AvatarItem(it)
                        }

                        _uiState.update { state ->
                            state.copy(
                                avatarResultList = avatarResultList
                            )
                        }
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
            val statusId = uiState.value.avatarStatusId

            if (modelId != null) {
                val runningDownloadSession = appDatabase.downloadSessionDao().getDownloadSessionSyncForModelIdSync(
                    modelId
                )
                if (runningDownloadSession?.downloadSessionEntity?.status ==
                    DownloadSessionStatus.PARTIALLY_DONE.status) {
                    val t = IllegalStateException("A download for this model is already in progress")
                    setLoading(loadType, LoadState.Error(t))
                    _uiState.update { state ->
                        state.copy(
                            exception = t,
                            uiErrorText = UiText.DynamicString("Please wait.. a download is already in progress.")
                        )
                    }
                    return@launch
                }
            }

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
            } else if (statusId != null) {
                appDatabase.avatarStatusDao().getAvatarStatusSync(uiState.value.avatarStatusId!!)
                    ?.avatarFilesEntity?.map { avatarFilesEntity ->
                        DownloadFile(
                            sessionId = downloadSession.id!!,
                            fileUri = avatarFilesEntity.remoteFile.toUri(),
                            status = DownloadFileStatus.NOT_STARTED,
                        )
                    } ?: emptyList()
            } else {
                emptyList()
            }

            if (downloadFiles.isNotEmpty()) {
                // TODO: add download files and schedule worker
                appDatabase.downloadFilesDao().insertAll(
                    downloadFiles.map(DownloadFile::toEntity)
                )
                sendEvent(AvatarResultUiEvent.StartDownload(downloadSessionId = downloadSession.id!!))
                setLoading(loadType, LoadState.NotLoading.Complete)
            } else {
                val t = IllegalStateException("Nothing to download")
                setLoading(loadType, LoadState.Error(t))
            }
        }
    }
    /* END - Download session related */

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

    private fun sendEvent(newEvent: AvatarResultUiEvent) = viewModelScope.launch {
        _uiEvent.emit(newEvent)
    }

    override fun onCleared() {
        val t = CancellationException("View model is dead")
        avatarStatusJob?.cancel(t)
        avatarsFetchJob?.cancel(t)
        modelDetailFetchJob?.cancel(t)
        createDownloadSessionJob?.cancel(t)
        super.onCleared()
    }
}

data class AvatarResultState(
    val loadState: LoadStates = LoadStates.IDLE,
    val avatarStatusId: String? = null,
    val modelId: String? = null,
    val modelData: ModelData? = null,
    val avatarResultList: List<AvatarResultUiModel> = emptyList(),
    val exception: Exception? = null,
    val uiErrorText: UiText? = null
)

interface AvatarResultUiAction {
    data class ErrorShown(val e: Exception) : AvatarResultUiAction
}

interface AvatarResultUiEvent {
    data class ShowToast(val message: UiText) : AvatarResultUiEvent
    data class StartDownload(val downloadSessionId: Long) : AvatarResultUiEvent
}

interface AvatarResultUiModel {
    data class AvatarItem(val avatar: ModelAvatar) : AvatarResultUiModel
}
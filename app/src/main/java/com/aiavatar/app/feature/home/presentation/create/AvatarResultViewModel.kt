package com.aiavatar.app.feature.home.presentation.create

import androidx.core.net.toUri
import androidx.lifecycle.SavedStateHandle
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
import com.aiavatar.app.core.domain.model.AvatarStatus
import com.aiavatar.app.core.domain.model.DownloadFile
import com.aiavatar.app.core.domain.model.DownloadSession
import com.aiavatar.app.core.domain.model.request.RenameModelRequest
import com.aiavatar.app.core.domain.repository.AppRepository
import com.aiavatar.app.feature.home.domain.model.ModelAvatar
import com.aiavatar.app.feature.home.domain.model.ModelData
import com.aiavatar.app.feature.home.domain.repository.HomeRepository
import com.aiavatar.app.nullAsEmpty
import com.aiavatar.app.commons.util.loadstate.LoadState
import com.aiavatar.app.commons.util.loadstate.LoadStates
import com.aiavatar.app.feature.home.presentation.catalog.ModelDetailUiAction
import com.aiavatar.app.feature.home.presentation.catalog.ModelDetailUiEvent
import com.aiavatar.app.feature.onboard.domain.model.ShareLinkData
import com.aiavatar.app.feature.onboard.domain.model.request.GetShareLinkRequest
import com.aiavatar.app.feature.onboard.domain.repository.AccountsRepository
import com.aiavatar.app.ifDebug
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
    private val accountsRepository: AccountsRepository,
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
    private var getShareLinkJob: Job? = null

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
            is AvatarResultUiAction.GetShareLink -> {
                uiState.value.avatarStatus?.modelId?.let { getShareLinkInternal(it) }
            }
        }
    }

    fun refresh() {
        refreshInternal()
    }

    fun saveModelName(modelName: String) = viewModelScope.launch {
        val modelId = uiState.value.avatarStatus?.modelId
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

    fun getStatusId(): String?  {
        return uiState.value.avatarStatusId
    }

    fun createDownloadSession(modelName: String) {
        createDownloadSessionInternal(modelName)
    }

    private fun refreshInternal() {
        // TODO: get data from server
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
                    val ids = avatarStatusWithFilesEntity.avatarFilesEntity.map { it._id }
                    Timber.d("Avatar status: $ids")
                    _uiState.update { state ->
                        state.copy(
                            avatarStatus = avatarStatusWithFilesEntity.avatarStatusEntity.toAvatarStatus(),
                            avatarResultList = avatarResultList
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
            val avatarStatus = uiState.value.avatarStatus ?: return@launch

            uiState.value.avatarStatus?.modelId?.let { modelId ->
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
                modelId = avatarStatus.modelId
            )
            appDatabase.downloadSessionDao().apply {
                insert(downloadSession.toEntity()).let { id ->
                    downloadSession.id = id
                }
            }

            val downloadFiles: List<DownloadFile> = if (avatarStatus.avatarStatusId != null) {
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
                        sendEvent(AvatarResultUiEvent.ShareLink(result.data.shortLink))
                    }
                }
            }
        }
    }
    /* END - Share link */

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
        getShareLinkJob?.cancel(t)
        super.onCleared()
    }
}

data class AvatarResultState(
    val loadState: LoadStates = LoadStates.IDLE,
    val avatarStatusId: String? = null,
    val avatarStatus: AvatarStatus? = null,
    val avatarResultList: List<AvatarResultUiModel> = emptyList(),
    val shareLoadState: LoadStates = LoadStates.IDLE,
    val shareLinkData: ShareLinkData? = null,
    val exception: Exception? = null,
    val uiErrorText: UiText? = null
)

interface AvatarResultUiAction {
    data class ErrorShown(val e: Exception) : AvatarResultUiAction
    object GetShareLink : AvatarResultUiAction
}

interface AvatarResultUiEvent {
    data class ShowToast(val message: UiText) : AvatarResultUiEvent
    data class StartDownload(val downloadSessionId: Long) : AvatarResultUiEvent
    data class ShareLink(val link: String) : AvatarResultUiEvent
}

interface AvatarResultUiModel {
    data class AvatarItem(val avatar: ModelAvatar) : AvatarResultUiModel
}
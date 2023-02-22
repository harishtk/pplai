package com.aiavatar.app.feature.home.presentation.profile

import androidx.core.net.toUri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiavatar.app.BuildConfig
import com.aiavatar.app.commons.util.ResolvableException
import com.aiavatar.app.commons.util.Result
import com.aiavatar.app.commons.util.UiText
import com.aiavatar.app.commons.util.loadstate.LoadState
import com.aiavatar.app.commons.util.loadstate.LoadStates
import com.aiavatar.app.commons.util.loadstate.LoadType
import com.aiavatar.app.commons.util.net.ApiException
import com.aiavatar.app.commons.util.net.NoInternetException
import com.aiavatar.app.commons.util.net.UnAuthorizedException
import com.aiavatar.app.core.data.source.local.AppDatabase
import com.aiavatar.app.core.data.source.local.entity.DownloadFileStatus
import com.aiavatar.app.core.data.source.local.entity.DownloadSessionStatus
import com.aiavatar.app.core.data.source.local.entity.toEntity
import com.aiavatar.app.core.data.source.local.model.toDownloadSessionWithFiles
import com.aiavatar.app.core.domain.model.DownloadFile
import com.aiavatar.app.core.domain.model.DownloadSession
import com.aiavatar.app.core.domain.model.DownloadSessionWithFiles
import com.aiavatar.app.core.domain.model.request.RenameModelRequest
import com.aiavatar.app.core.domain.repository.AppRepository
import com.aiavatar.app.feature.home.domain.model.ModelAvatar
import com.aiavatar.app.feature.home.domain.model.ModelData
import com.aiavatar.app.feature.home.domain.model.request.GetAvatarsRequest
import com.aiavatar.app.feature.home.domain.repository.HomeRepository
import com.aiavatar.app.feature.onboard.domain.model.ShareLinkData
import com.aiavatar.app.feature.onboard.domain.model.request.GetShareLinkRequest
import com.aiavatar.app.feature.onboard.domain.repository.AccountsRepository
import com.aiavatar.app.ifDebug
import com.aiavatar.app.nullAsEmpty
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.concurrent.CancellationException
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class ModelListViewModel @Inject constructor(
    private val appRepository: AppRepository,
    private val accountsRepository: AccountsRepository,
    private val homeRepository: HomeRepository,
    @Deprecated("migrate to repo")
    private val appDatabase: AppDatabase,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow<ModelListState>(ModelListState())
    val uiState: StateFlow<ModelListState> = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<ModelListUiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    val accept: (ModelListUiAction) -> Unit

    private var avatarsFetchJob: Job? = null
    private var modelDetailFetchJob: Job? = null
    private var createDownloadSessionJob: Job? = null
    private var downloadProgressJob: Job? = null
    private var getShareLinkJob: Job? = null

    init {
        accept = { uiAction -> onUiAction(uiAction) }
    }

    private fun onUiAction(uiAction: ModelListUiAction) {
        when (uiAction) {
            is ModelListUiAction.ErrorShown -> {
                _uiState.update { state ->
                    state.copy(
                        exception = null,
                        uiErrorText = null
                    )
                }
            }
            is ModelListUiAction.GetShareLink -> {
                uiState.value.modelId?.let { getShareLinkInternal(it) }
            }
        }
    }

    fun refresh() {
        refreshInternal()
    }

    fun saveModelName(
        modelName: String,
        continuation: () -> Unit
    ) = viewModelScope.launch {
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
                        val affectedRows = appDatabase.modelDao().updateModelNameForModelId(modelId, modelName, true)
                        appDatabase.modelDao().updateModelNameForModelId(modelId, modelName, true)
                        Timber.d("Update model name: affected $affectedRows rows")
                        setLoading(LoadType.ACTION, LoadState.NotLoading.Complete)
                        continuation.invoke()
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

    fun createDownloadSession(modelName: String) {
        createDownloadSessionInternal(modelName)
    }

    private fun refreshInternal() {
        // TODO: get data from server
        Timber.d("refreshInternal")
        uiState.value.modelId?.let { modelId ->
            val request = GetAvatarsRequest(modelId = modelId)
            // TODO: get model detail
            getModelDetail(modelId)
            getAvatarsForModel(request)
        }
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
            // setLoading(LoadType.REFRESH, LoadState.Loading())
            homeRepository.getAvatars2(request, forceRefresh = true).collectLatest { result ->
                Timber.d("getAvatars: $result")
                when (result) {
                    is Result.Loading -> setLoading(LoadType.REFRESH, LoadState.Loading())
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
                            else -> {
                                _uiState.update { state ->
                                    state.copy(
                                        exception = result.exception,
                                        uiErrorText = UiText.somethingWentWrong
                                    )
                                }
                            }
                        }
                        setLoading(LoadType.REFRESH, LoadState.Error(result.exception))
                    }
                    is Result.Success -> {
                        setLoading(LoadType.REFRESH, LoadState.NotLoading.Complete)

                        val avatarResultList = result.data.map {
                            ModelListUiModel2.AvatarItem(it)
                        }

                        _uiState.update { state ->
                            state.copy(
                                modelResultList = avatarResultList
                            )
                        }
                    }
                }
            }
        }
    }

    /* Download session related */

    fun getDownloadSession(sessionId: Long): Flow<DownloadSessionWithFiles?> {
        return appDatabase.downloadSessionDao().getDownloadSession(id = sessionId)
            .map { it?.toDownloadSessionWithFiles() }
    }

    private fun createDownloadSessionInternal(modelName: String, loadType: LoadType = LoadType.ACTION) {
        createDownloadSessionJob = viewModelScope.launch {
            setLoading(loadType, LoadState.Loading())
            val modelId  = uiState.value.modelId

            if (modelId != null) {
                val runningDownloadSession = appDatabase.downloadSessionDao().getDownloadSessionSyncForModelIdSync(
                    modelId
                )

                Timber.d("Current download session: ${runningDownloadSession?.downloadSessionEntity}")
                if (runningDownloadSession?.downloadSessionEntity?.status ==
                    DownloadSessionStatus.PARTIALLY_DONE.status) {
                    val createdAt = runningDownloadSession.downloadSessionEntity.createdAt
                    val delta = (System.currentTimeMillis() - createdAt).coerceAtLeast(0)
                    if (delta > TimeUnit.HOURS.toMillis(1)) {
                        Timber.e("Current download session: running more than an hour!")
                    }

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
            } /*else if (statusId != null) {
                appDatabase.avatarStatusDao().getAvatarStatusSync(uiState.value.avatarStatusId!!)
                    ?.avatarFilesEntity?.map { avatarFilesEntity ->
                        DownloadFile(
                            sessionId = downloadSession.id!!,
                            fileUri = avatarFilesEntity.remoteFile.toUri(),
                            status = DownloadFileStatus.NOT_STARTED,
                        )
                    } ?: emptyList()
            }*/ else {
                emptyList()
            }

            if (downloadFiles.isNotEmpty()) {
                // TODO: add download files and schedule worker
                appDatabase.downloadFilesDao().insertAll(
                    downloadFiles.map(DownloadFile::toEntity)
                )
                sendEvent(ModelListUiEvent.StartDownload(downloadSessionId = downloadSession.id!!))
                setLoading(loadType, LoadState.NotLoading.Complete)
            } else {
                val t = IllegalStateException("Nothing to download")
                setLoading(loadType, LoadState.Error(t))
            }
        }
    }

    fun observeDownloadStatus(downloadSessionId: Long) {
        downloadProgressJob = viewModelScope.launch {
            getDownloadSession(sessionId = downloadSessionId).collectLatest { downloadSessionWithFiles ->
                _uiState.update { state ->
                    state.copy(
                        downloadStatus = downloadSessionWithFiles?.downloadSession?.status
                    )
                }
                /*when (downloadSessionWithFiles?.downloadSession?.status) {
                    DownloadSessionStatus.NOT_STARTED -> {

                    }
                    DownloadSessionStatus.PARTIALLY_DONE -> TODO()
                    DownloadSessionStatus.COMPLETE -> TODO()
                    DownloadSessionStatus.FAILED -> TODO()
                    DownloadSessionStatus.UNKNOWN -> TODO()
                    null -> TODO()
                }*/
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
                        sendEvent(ModelListUiEvent.ShareLink(result.data.shortLink))
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

    private fun sendEvent(newEvent: ModelListUiEvent) = viewModelScope.launch {
        _uiEvent.emit(newEvent)
    }

    override fun onCleared() {
        val t = CancellationException("View model is dead")
        avatarsFetchJob?.cancel(t)
        modelDetailFetchJob?.cancel(t)
        createDownloadSessionJob?.cancel(t)
        downloadProgressJob?.cancel(t)
        getShareLinkJob?.cancel(t)
        super.onCleared()
    }
}

data class ModelListState(
    val loadState: LoadStates = LoadStates.IDLE,
    val modelId: String? = null,
    val modelData: ModelData? = null,
    val modelResultList: List<ModelListUiModel2> = emptyList(),
    val downloadStatus: DownloadSessionStatus? = null,
    val shareLoadState: LoadStates = LoadStates.IDLE,
    val shareLinkData: ShareLinkData? = null,
    val exception: Exception? = null,
    val uiErrorText: UiText? = null
)

interface ModelListUiAction {
    data class ErrorShown(val e: Exception) : ModelListUiAction
    object GetShareLink : ModelListUiAction
}

interface ModelListUiEvent {
    data class ShowToast(val message: UiText) : ModelListUiEvent
    data class StartDownload(val downloadSessionId: Long) : ModelListUiEvent
    data class ShareLink(val link: String) : ModelListUiEvent
}

interface ModelListUiModel2 {
    data class AvatarItem(val avatar: ModelAvatar) : ModelListUiModel2
}



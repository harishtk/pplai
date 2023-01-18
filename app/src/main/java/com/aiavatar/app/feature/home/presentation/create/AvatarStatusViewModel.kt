package com.aiavatar.app.feature.home.presentation.create

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiavatar.app.commons.util.Result
import com.aiavatar.app.commons.util.UiText
import com.aiavatar.app.commons.util.loadstate.LoadType
import com.aiavatar.app.commons.util.net.ApiException
import com.aiavatar.app.commons.util.net.NoInternetException
import com.aiavatar.app.core.data.source.local.AppDatabase
import com.aiavatar.app.core.data.source.local.entity.UploadSessionStatus
import com.aiavatar.app.core.domain.model.request.AvatarStatusRequest
import com.aiavatar.app.core.domain.model.request.CreateModelRequest
import com.aiavatar.app.core.domain.repository.AppRepository
import com.aiavatar.app.di.ApplicationDependencies
import com.pepulnow.app.data.LoadState
import com.pepulnow.app.data.LoadStates
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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

    val accept: (AvatarStatusUiAction) -> Unit

    private var createModelJob: Job? = null
    private var avatarStatusJob: Job? = null

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
                            sessionStatus = UploadSessionStatus.fromRawValue(
                                uploadSessionWithFilesEntity
                                    .uploadSessionEntity.status
                            )
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

    fun setSessionId(sessionId: Long) {
        _uiState.update { state ->
            state.copy(
                sessionId = sessionId
            )
        }
        getAvatarStatusInternal()
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
                        ApplicationDependencies.getPersistentStore().apply {
                            setProcessingModel(true)
                            setGuestUserId(result.data.guestUserId)
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
                        }
                        // sendEvent(Step3UiEvent.NextScreen)
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
    }

    private fun getAvatarStatusInternal() = viewModelScope.launch {
        val uploadSessionWithFilesEntity = appDatabase.uploadSessionDao().getUploadSessionSync(
            uiState.value.sessionId!!
        )
        if (uploadSessionWithFilesEntity != null &&
            uploadSessionWithFilesEntity.uploadSessionEntity.modelId?.isNotBlank() == true) {
            val request = AvatarStatusRequest(uploadSessionWithFilesEntity.uploadSessionEntity.modelId!!)
            getStatus(request)
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
                        when (result.data.modelStatus) {
                            "processing" -> {

                            }
                            "completed" -> {
                                /*ApplicationDependencies.getPersistentStore().apply {
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
                                }*/
                            }
                        }
                        // sendEvent(Step3UiEvent.NextScreen)
                    }
                }
            }
        }
    }

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
                toggleStateNotifyMe = savedStateHandle[TOGGLE_STATE_NOTIFY_ME] ?: DEFAULT_TOGGLE_STATE
            )
        }
    }

}

data class AvatarStatusState(
    val loadState: LoadStates = LoadStates.IDLE,
    val sessionId: Long? = null,
    val sessionStatus: UploadSessionStatus = UploadSessionStatus.UNKNOWN,
    val toggleStateNotifyMe: Boolean = DEFAULT_TOGGLE_STATE,
    val exception: Exception? = null,
    val uiErrorText: UiText? = null
)

interface AvatarStatusUiAction {
    data class ErrorShown(val e: Exception) : AvatarStatusUiAction
    data class ToggleNotifyMe(val checked: Boolean) : AvatarStatusUiAction
    object CreateModel : AvatarStatusUiAction
}

private const val TOGGLE_STATE_NOTIFY_ME = "toggle_state_notify_me"

private const val DEFAULT_TOGGLE_STATE: Boolean = true
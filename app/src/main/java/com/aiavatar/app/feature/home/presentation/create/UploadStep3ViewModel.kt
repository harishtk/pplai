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
import com.aiavatar.app.core.data.source.local.entity.toEntity
import com.aiavatar.app.core.domain.model.AvatarStatus
import com.aiavatar.app.core.domain.model.request.CreateModelRequest
import com.aiavatar.app.core.domain.repository.AppRepository
import com.aiavatar.app.di.ApplicationDependencies
import com.aiavatar.app.feature.home.presentation.util.GenderModel
import com.aiavatar.app.commons.util.loadstate.LoadState
import com.aiavatar.app.commons.util.loadstate.LoadStates
import com.aiavatar.app.commons.util.net.UnAuthorizedException
import com.aiavatar.app.commons.util.runtimetest.MockApiDataProvider
import com.aiavatar.app.feature.home.presentation.create.util.CreateCreditExhaustedException
import com.aiavatar.app.feature.onboard.domain.model.CreateCheckData
import com.aiavatar.app.feature.onboard.domain.model.request.CreateCheckRequest
import com.aiavatar.app.feature.onboard.domain.repository.AccountsRepository
import com.aiavatar.app.ifDebug
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class UploadStep3ViewModel @Inject constructor(
    @Deprecated("move to repo")
    private val appDatabase: AppDatabase,
    private val appRepository: AppRepository,
    private val accountsRepository: AccountsRepository,
    private val savedStateHandle: SavedStateHandle,
    private val mockApiDataProvider: MockApiDataProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow<Step3State>(Step3State())
    val uiState: StateFlow<Step3State> = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<Step3UiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    val accept: (Step3UiAction) -> Unit

    private var selectedGenderPosition: Int = 0
    private val selectedToggleFlow = MutableStateFlow(false)

    private var createModelJob: Job? = null
    private var createCheckJob: Job? = null

    init {
        val genderModelList = mutableListOf<GenderModel>(
            GenderModel("Male"),
            GenderModel("Female"),
            GenderModel("Others")
        )

        val genderListFlow = uiState.map { it.genderList }

        _uiState.update { state ->
            state.copy(
                genderList = genderModelList
            )
        }

        combine(
            selectedToggleFlow,
            genderListFlow,
            ::Pair
        ).map {(selectedToggle, genderList) ->
            val newGenderList = genderList.mapIndexed { index, genderModel ->
                genderModel.copy(selected = index == selectedGenderPosition) }
            newGenderList
        }.onEach { genderList ->
            _uiState.update { state ->
                state.copy(
                    genderList = genderList
                )
            }
        }.launchIn(viewModelScope)

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

    private fun onUiAction(action: Step3UiAction) {
        when (action) {
            is Step3UiAction.ErrorShown -> {
                _uiState.update { state ->
                    state.copy(
                        exception = null,
                        uiErrorText = null
                    )
                }
            }
        }
    }

    fun toggleSelection(position: Int) {
        selectedGenderPosition = position
        // Signals the flow
        selectedToggleFlow.update { selectedToggleFlow.value.not() }
    }

    fun checkCreate(onCompletion: (kotlin.Result<CreateCheckData>) -> Unit) {
        val request = CreateCheckRequest(
            timestamp = System.currentTimeMillis()
        )
        checkCreateInternal(request, onCompletion)
    }

    fun updateTrainingType(sessionId: Long) {
        val selectedGender = uiState.value.genderList[selectedGenderPosition]
        viewModelScope.launch {
            appDatabase.uploadSessionDao().updateUploadSessionTrainingType(
                sessionId,
                selectedGender.title
            )
            sendEvent(Step3UiEvent.NextScreen(false))
            /*if (uiState.value.sessionStatus == UploadSessionStatus.UPLOAD_COMPLETE) {
                createModelInternal(sessionId)
            } else {

            }*/
        }
    }

    fun setSessionId(sessionId: Long) {
        _uiState.update { state ->
            state.copy(
                sessionId = sessionId
            )
        }
    }

    private fun checkCreateInternal(
        request: CreateCheckRequest,
        onCompletion: (kotlin.Result<CreateCheckData>) -> Unit
    ) {
        if (createCheckJob?.isActive == true) {
            val cause = IllegalStateException("A create check job is already active. Ignoring request.")
            ifDebug { Timber.w(cause) }
            return
        }

        createCheckJob?.cancel(CancellationException("New request")) // just in case
        createCheckJob = viewModelScope.launch {
            accountsRepository.createCheck(request).collectLatest { result ->
                when (result) {
                    is Result.Loading -> {
                        setLoading(LoadType.ACTION, LoadState.Loading())
                    }
                    is Result.Error -> {
                        onCompletion(kotlin.Result.failure(result.exception))
                        setLoading(LoadType.ACTION, LoadState.Error(result.exception))
                    }
                    is Result.Success -> {
                        if (result.data.allowModelCreate) {
                            setLoading(LoadType.ACTION, LoadState.NotLoading.Complete)
                            onCompletion(kotlin.Result.success(result.data))
                        } else {
                            setLoading(LoadType.ACTION, LoadState.NotLoading.InComplete)
                            val t = CreateCreditExhaustedException()
                            onCompletion(kotlin.Result.failure(t))
                            _uiState.update { state ->
                                state.copy(
                                    exception = t,
                                )
                            }
                        }
                    }
                }
            }
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
                trainingType = uploadSessionWithFilesEntity.uploadSessionEntity.trainingType.lowercase(),
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
                        ApplicationDependencies.getPersistentStore().apply {
                            setProcessingModel(true)
                            result.data.guestUserId?.let { setGuestUserId(it) }
                        }
                        appDatabase.avatarStatusDao().apply {
                            val newAvatarStatus = AvatarStatus.emptyStatus(result.data.modelId).apply {
                                avatarStatusId = result.data.statusId
                            }
                            insert(newAvatarStatus.toEntity())
                        }
                        sendEvent(Step3UiEvent.NextScreen(true))
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

    private fun sendEvent(newEvent: Step3UiEvent) = viewModelScope.launch {
        _uiEvent.emit(newEvent)
    }

}

data class Step3State(
    val loadState: LoadStates = LoadStates.IDLE,
    val selectedGenderPosition: Int = 0,
    val genderList: List<GenderModel> = emptyList(),
    val sessionId: Long? = null,
    val sessionStatus: UploadSessionStatus = UploadSessionStatus.UNKNOWN,
    val exception: Exception? = null,
    val uiErrorText: UiText? = null
)

interface Step3UiAction {
    data class ErrorShown(val e: Exception) : Step3UiAction
}

interface Step3UiEvent {
    data class NextScreen(val restart: Boolean) : Step3UiEvent
}
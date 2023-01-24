package com.aiavatar.app.feature.home.presentation.create

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiavatar.app.commons.util.Result
import com.aiavatar.app.commons.util.ResolvableException
import com.aiavatar.app.commons.util.UiText
import com.aiavatar.app.commons.util.loadstate.LoadType
import com.aiavatar.app.commons.util.net.ApiException
import com.aiavatar.app.commons.util.net.NoInternetException
import com.aiavatar.app.core.data.source.local.AppDatabase
import com.aiavatar.app.core.data.source.local.entity.toAvatarFile
import com.aiavatar.app.core.data.source.local.model.toAvatarStatusWithFiles
import com.aiavatar.app.core.domain.model.AvatarFile
import com.aiavatar.app.core.domain.model.AvatarStatusWithFiles
import com.aiavatar.app.core.domain.model.request.RenameModelRequest
import com.aiavatar.app.core.domain.repository.AppRepository
import com.pepulnow.app.data.LoadState
import com.pepulnow.app.data.LoadStates
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class AvatarResultViewModel @Inject constructor(
    private val appRepository: AppRepository,
    @Deprecated("move to repo")
    private val appDatabase: AppDatabase,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow<AvatarResultState>(AvatarResultState())
    val uiState: StateFlow<AvatarResultState> = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<AvatarResultUiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    val accept: (AvatarResultUiAction) -> Unit

    init {
        accept = { uiAction -> onUiAction(uiAction) }

        uiState.mapNotNull { it.avatarStatusId }
            .flatMapLatest { statusId ->
                appDatabase.avatarStatusDao().getAvatarStatus(statusId = statusId.toLong())
            }.onEach { avatarStatusWithFilesEntity ->
                if (avatarStatusWithFilesEntity != null) {
                    val avatarResultList = avatarStatusWithFilesEntity.avatarFilesEntity.map {
                        AvatarResultUiModel.AvatarItem(it.toAvatarFile())
                    }
                    _uiState.update { state ->
                        state.copy(
                            avatarStatusWithFiles = avatarStatusWithFilesEntity.toAvatarStatusWithFiles(),
                            avatarResultList = avatarResultList
                        )
                    }
                }
            }
            .launchIn(viewModelScope)

        refreshInternal()
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
                        sendEvent(AvatarResultUiEvent.StartDownload(modelId))
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

    fun getModelId(): String? {
        return uiState.value.avatarStatusWithFiles?.avatarStatus?.modelId
    }

    private fun refreshInternal() {
        // TODO: get data from server
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

    private fun sendEvent(newEvent: AvatarResultUiEvent) = viewModelScope.launch {
        _uiEvent.emit(newEvent)
    }
}

data class AvatarResultState(
    val loadState: LoadStates = LoadStates.IDLE,
    val avatarStatusId: String? = null,
    val avatarStatusWithFiles: AvatarStatusWithFiles? = null,
    val avatarResultList: List<AvatarResultUiModel> = emptyList(),
    val exception: Exception? = null,
    val uiErrorText: UiText? = null
)

interface AvatarResultUiAction {
    data class ErrorShown(val e: Exception) : AvatarResultUiAction
}

interface AvatarResultUiEvent {
    data class ShowToast(val message: UiText) : AvatarResultUiEvent
    data class StartDownload(val modelId: String) : AvatarResultUiEvent
}

interface AvatarResultUiModel {
    data class AvatarItem(val avatar: AvatarFile) : AvatarResultUiModel
}
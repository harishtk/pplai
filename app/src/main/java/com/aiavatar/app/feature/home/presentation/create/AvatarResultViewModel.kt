package com.aiavatar.app.feature.home.presentation.create

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiavatar.app.commons.util.UiText
import com.aiavatar.app.commons.util.loadstate.LoadType
import com.aiavatar.app.core.data.source.local.AppDatabase
import com.aiavatar.app.core.data.source.local.entity.toAvatarFile
import com.aiavatar.app.core.data.source.local.model.toAvatarStatusWithFiles
import com.aiavatar.app.core.domain.model.AvatarFile
import com.aiavatar.app.core.domain.model.AvatarStatusWithFiles
import com.pepulnow.app.data.LoadState
import com.pepulnow.app.data.LoadStates
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class AvatarResultViewModel @Inject constructor(
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
                Timber.d("flatMapLatest: $statusId")
                appDatabase.avatarStatusDao().getAvatarStatus(id = statusId.toLong())
            }.onEach { avatarStatusWithFilesEntity ->
                Timber.d("flatMapLatest: 2 $avatarStatusWithFilesEntity")
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
}

interface AvatarResultUiModel {
    data class AvatarItem(val avatar: AvatarFile) : AvatarResultUiModel
}
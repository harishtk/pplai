package com.aiavatar.app.feature.home.presentation.create

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiavatar.app.commons.util.UiText
import com.aiavatar.app.commons.util.loadstate.LoadType
import com.aiavatar.app.core.data.source.local.AppDatabase
import com.aiavatar.app.core.data.source.local.entity.UploadSessionStatus
import com.pepulnow.app.data.LoadState
import com.pepulnow.app.data.LoadStates
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AvatarStatusViewModel @Inject constructor(
    @Deprecated("move to repo")
    private val appDatabase: AppDatabase,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow<AvatarStatusState>(AvatarStatusState())
    val uiState: StateFlow<AvatarStatusState> = _uiState.asStateFlow()

    val accept: (AvatarStatusUiAction) -> Unit

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
        }
    }

    fun setSessionId(sessionId: Long) {
        _uiState.update { state ->
            state.copy(
                sessionId = sessionId
            )
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

    private fun setLoadState(
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
}

private const val TOGGLE_STATE_NOTIFY_ME = "toggle_state_notify_me"

private const val DEFAULT_TOGGLE_STATE: Boolean = true
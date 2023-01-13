package com.pepulai.app.feature.home.presentation.create

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pepulai.app.commons.util.UiText
import com.pepulai.app.commons.util.loadstate.LoadType
import com.pepulai.app.feature.home.domain.model.Avatar
import com.pepulai.app.feature.home.presentation.catalog.AvatarUiModel
import com.pepulnow.app.data.LoadState
import com.pepulnow.app.data.LoadStates
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AvatarResultViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow<AvatarResultState>(AvatarResultState())
    val uiState: StateFlow<AvatarResultState> = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<AvatarResultUiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    val accept: (AvatarResultUiAction) -> Unit

    init {
        accept = { uiAction -> onUiAction(uiAction) }

        refreshInternal()
    }

    private fun onUiAction(uiAction: AvatarResultUiAction) {
        when (uiAction) {
            is AvatarResultUiAction.ErrorShown -> {

            }
        }
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
    val avatarResultList: List<AvatarUiModel> = emptyList(),
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
    data class AvatarItem(val avatar: Avatar) : AvatarUiModel
}
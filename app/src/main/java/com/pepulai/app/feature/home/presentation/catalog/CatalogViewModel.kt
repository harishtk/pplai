package com.pepulai.app.feature.home.presentation.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pepulai.app.commons.util.UiText
import com.pepulai.app.commons.util.loadstate.LoadType
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

@HiltViewModel
class CatalogViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<CatalogState>(CatalogState())
    val uiState: StateFlow<CatalogState> = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<CatalogUiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    val accept: (CatalogUiAction) -> Unit

    init {

        accept = { uiAction -> onUiAction(action = uiAction) }
    }

    private fun onUiAction(action: CatalogUiAction) {
        when (action) {
            is CatalogUiAction.ErrorShown -> {
                // TODO: reset error state
            }
        }
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

    private fun sendEvent(newEvent: CatalogUiEvent) = viewModelScope.launch {
        _uiEvent.emit(newEvent)
    }
}

data class CatalogState(
    val loadState: LoadStates = LoadStates.IDLE,
    val catalogList: List<Any> = emptyList(),
    val exception: Exception? = null,
    val uiErrorText: UiText? = null
)

interface CatalogUiAction {
    data class ErrorShown(val e: Exception) : CatalogUiAction
}

interface CatalogUiEvent {
    data class ShowToast(val message: UiText) : CatalogUiEvent
}
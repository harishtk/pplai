package com.aiavatar.app.feature.home.presentation.profile

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiavatar.app.BuildConfig
import com.aiavatar.app.commons.util.Result
import com.aiavatar.app.commons.util.UiText
import com.aiavatar.app.commons.util.loadstate.LoadType
import com.aiavatar.app.commons.util.net.ApiException
import com.aiavatar.app.commons.util.net.NoInternetException
import com.aiavatar.app.databinding.FragmentForceUpdateBinding
import com.aiavatar.app.feature.home.domain.model.ModelList
import com.aiavatar.app.feature.home.domain.repository.HomeRepository
import com.aiavatar.app.nullAsEmpty
import com.pepulnow.app.data.LoadState
import com.pepulnow.app.data.LoadStates
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.CancellationException
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val homeRepository: HomeRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProfileState>(ProfileState())
    val uiState: StateFlow<ProfileState> = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<ProfileUiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    val accept: (ProfileUiAction) -> Unit

    private var modelsFetchJob: Job? = null

    init {
        accept = { uiAction -> onUiAction(uiAction) }
    }

    private fun onUiAction(action: ProfileUiAction) {
        when (action) {
            is ProfileUiAction.ErrorShown -> {
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
        refreshInternal(forceRefresh = true)
    }

    private fun refreshInternal(forceRefresh: Boolean) {
        getModels(forceRefresh = forceRefresh)
    }

    private fun getModels(loadType: LoadType = LoadType.REFRESH, forceRefresh: Boolean = false) {
        /*if (modelsFetchJob?.isActive == true) {
            val t = IllegalStateException("A model fetch job is already active. Ignoring request")
            if (BuildConfig.DEBUG) {
                Timber.w(t)
            }
            return
        }*/

        modelsFetchJob?.cancel(CancellationException("New request")) // just in case
        modelsFetchJob = viewModelScope.launch {
            setLoading(loadType, LoadState.Loading())
            homeRepository.getMyModels().collectLatest { result ->
                when (result) {
                    is Result.Loading -> setLoading(loadType, LoadState.Loading())
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
                        setLoading(loadType, LoadState.Error(result.exception))
                    }
                    is Result.Success -> {
                        setLoading(loadType, LoadState.NotLoading.Complete)
                        result.data.let {
                            _uiState.update { state ->
                                state.copy(
                                    modelListUiModels = it.map { modelData ->
                                        ModelListUiModel.Item(modelList = modelData)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun setLoading(
        loadType: LoadType,
        loadState: LoadState,
    ) {
        val newLoadState = uiState.value.loadState.modifyState(loadType, loadState)
        _uiState.update { state -> state.copy(loadState = newLoadState) }
    }

    private fun sendEvent(newEvent: ProfileUiEvent) = viewModelScope.launch {
        _uiEvent.emit(newEvent)
    }

}

data class ProfileState(
    val loadState: LoadStates = LoadStates.IDLE,
    val modelListUiModels: List<ModelListUiModel> = emptyList(),
    val exception: Exception? = null,
    val uiErrorText: UiText? = null
)

interface ProfileUiAction {
    data class ErrorShown(val e: Exception) : ProfileUiAction
}

interface ProfileUiEvent {
    data class ShowToast(val message: UiText) : ProfileUiEvent
}

interface ModelListUiModel {
    data class Item(val modelList: ModelList) : ModelListUiModel
}
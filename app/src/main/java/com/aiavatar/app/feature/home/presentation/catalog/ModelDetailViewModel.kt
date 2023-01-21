package com.aiavatar.app.feature.home.presentation.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiavatar.app.BuildConfig
import com.aiavatar.app.commons.util.Result
import com.aiavatar.app.commons.util.UiText
import com.aiavatar.app.commons.util.loadstate.LoadType
import com.aiavatar.app.commons.util.net.ApiException
import com.aiavatar.app.commons.util.net.NoInternetException
import com.aiavatar.app.feature.home.domain.model.ListAvatar
import com.aiavatar.app.feature.home.domain.model.request.GetAvatarsRequest
import com.aiavatar.app.feature.home.domain.repository.HomeRepository
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
class ModelDetailViewModel @Inject constructor(
    private val homeRepository: HomeRepository
): ViewModel() {

    private val _uiState = MutableStateFlow<ModelDetailState>(ModelDetailState())
    val uiState: StateFlow<ModelDetailState> = _uiState.asStateFlow()

    private var selectedAvatarPosition: Int = 0
    private val selectedToggleFlow = MutableStateFlow(false)

    val accept: (ModelDetailUiAction) -> Unit

    private var avatarsFetchJob: Job? = null

    init {
        val selectableAvatarUiModelListFlow = uiState.map { it.avatarList }
            .distinctUntilChanged()

        combine(
            selectedToggleFlow,
            selectableAvatarUiModelListFlow,
            ::Pair
        ).map { (selectedToggle, selectableAvatarList) ->
            val newSelectableAvatarList = selectableAvatarList.mapIndexed { index, selectableAvatarUiModel ->
                if (selectableAvatarUiModel is SelectableAvatarUiModel.Item) {
                    selectableAvatarUiModel.copy(selected = index == selectedAvatarPosition)
                } else {
                    selectableAvatarUiModel
                }
            }
            newSelectableAvatarList
        }.onEach { selectableAvatarUiModelList ->
            _uiState.update { state ->
                state.copy(
                    avatarList = selectableAvatarUiModelList
                )
            }
        }.launchIn(viewModelScope)

        accept = { uiAction -> onUiAction(uiAction) }
    }

    private fun onUiAction(action: ModelDetailUiAction) {
        when (action) {
            is ModelDetailUiAction.ErrorShown -> {
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
        uiState.value.modelId?.let {
            val request = GetAvatarsRequest(it)
            getAvatarsForModel(request)
        }
    }

    fun setModelId(modelId: String) {
        _uiState.update { state ->
            state.copy(
                modelId = modelId
            )
        }
    }

    fun toggleSelection(position: Int) {
        selectedAvatarPosition = position
        // Signals the flow
        selectedToggleFlow.update { selectedToggleFlow.value.not() }
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
            homeRepository.getAvatars(request).collectLatest { result ->
                when (result) {
                    is Result.Loading -> setLoading(LoadType.REFRESH, LoadState.Loading())
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
                        setLoading(LoadType.REFRESH, LoadState.Error(result.exception))
                    }
                    is Result.Success -> {
                        setLoading(LoadType.REFRESH, LoadState.NotLoading.Complete)
                        _uiState.update { state ->
                            state.copy(
                                avatarList = result.data.map { SelectableAvatarUiModel.Item(it, false) }
                            )
                        }
                    }
                }
            }
        }
    }

    @Suppress("SameParameterValue")
    private fun setLoading(
        loadType: LoadType,
        loadState: LoadState
    ) {
        val newLoadState = uiState.value.loadState.modifyState(loadType, loadState)
        _uiState.update { it.copy(loadState = newLoadState) }
    }

}

data class ModelDetailState(
    val loadState: LoadStates = LoadStates.IDLE,
    val modelId: String? = null,
    val avatarList: List<SelectableAvatarUiModel> = emptyList(),
    val exception: Exception? = null,
    val uiErrorText: UiText? = null
)

interface ModelDetailUiAction {
    data class ErrorShown(val e: Exception) : ModelDetailUiAction
}

interface SelectableAvatarUiModel {
    data class Item(val listAvatar: ListAvatar, val selected: Boolean) : SelectableAvatarUiModel
}
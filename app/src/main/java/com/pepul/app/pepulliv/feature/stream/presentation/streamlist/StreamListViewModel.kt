package com.pepul.app.pepulliv.feature.stream.presentation.streamlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pepul.app.pepulliv.commons.util.Result
import com.pepul.app.pepulliv.commons.util.UiText
import com.pepul.app.pepulliv.commons.util.net.ApiException
import com.pepul.app.pepulliv.commons.util.net.NoInternetException
import com.pepul.app.pepulliv.feature.stream.domain.repository.StreamRepository
import com.pepulnow.app.data.LoadState
import com.pepulnow.app.data.LoadStates
import com.pepul.app.pepulliv.commons.util.loadstate.LoadType
import com.pepul.app.pepulliv.feature.stream.data.source.remote.dto.StreamItemDto
import com.pepul.app.pepulliv.feature.stream.data.source.remote.model.StreamDto
import com.pepul.app.pepulliv.feature.stream.data.source.remote.model.WOWZStreamDto
import com.pepul.app.pepulliv.feature.stream.domain.model.request.StreamIdRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StreamListViewModel @Inject constructor(
    private val repository: StreamRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StreamListState())
    val uiState: StateFlow<StreamListState> = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<StreamListUiEvent>()
    val uiEvent: SharedFlow<StreamListUiEvent> = _uiEvent.asSharedFlow()

    val accept: (StreamListUiAction) -> Unit

    private var streamListFetchJob: Job? = null
    private var streamScheduledJob: Job? = null
    private var streamKeyFetchJob: Job? = null

    init {
        accept = { uiAction -> onUiAction(uiAction) }

        refreshInternal(true)
    }

    private fun onUiAction(action: StreamListUiAction) {
        when (action) {
            is StreamListUiAction.ErrorShown -> {
                _uiState.update { state ->
                    state.copy(
                        exception = null,
                        uiErrorMessage = null
                    )
                }
            }
            is StreamListUiAction.Refresh -> {
                refreshInternal(true)
            }
            is StreamListUiAction.GetStreamKey -> {
                getStreamKeyInternal()
            }
            is StreamListUiAction.DeleteStream -> {
                deleteStreamInternal(action.id)
            }
        }
    }

    private fun refreshInternal(force: Boolean = false) {
        if (force) {
            streamListFetchJob?.cancel(CancellationException("Force refresh"))
            getStreamListInternal()
        } else {
            getStreamListInternal()
            scheduleNewJob()
        }
    }

    private fun getStreamListInternal() {
        streamListFetchJob?.cancel(CancellationException("New Request"))
        streamListFetchJob = viewModelScope.launch {
            repository.getStreams().collectLatest { result ->
                when (result) {
                    is Result.Loading -> setLoading(LoadState.Loading(), LoadType.REFRESH)
                    is Result.Error -> {
                        when (result.exception) {
                            is ApiException -> {
                                _uiState.update { state ->
                                    state.copy(
                                        exception = result.exception,
                                        uiErrorMessage = UiText.somethingWentWrong
                                    )
                                }
                            }
                            is NoInternetException -> {
                                _uiState.update { state ->
                                    state.copy(
                                        exception = result.exception,
                                        uiErrorMessage = UiText.noInternet
                                    )
                                }
                            }
                        }
                        setLoading(LoadState.Error(result.exception), LoadType.REFRESH)
                    }
                    is Result.Success -> {
                        // TODO: parse result
                        setLoading(LoadState.NotLoading.Complete, LoadType.REFRESH)
                        _uiState.update { state ->
                            state.copy(
                                streamList = result.data.map { StreamUiModel.Item(it) }
                            )
                        }

                        scheduleNewJob()
                    }
                }
            }
        }
    }

    private fun scheduleNewJob() {
        streamScheduledJob?.cancel(CancellationException("Force refresh"))
        streamScheduledJob = viewModelScope.launch {
            delay(SCHEDULE_DELAY)
            getStreamListInternal()
        }
    }

    private fun getStreamKeyInternal() {
        if (streamKeyFetchJob?.isActive == true) {
            sendEvent(StreamListUiEvent.ShowToast(UiText.DynamicString("Already a request in progress. Please wait..")))
            return
        }
        // just in case
        streamKeyFetchJob?.cancel(CancellationException("New request"))
        streamKeyFetchJob = viewModelScope.launch {
            repository.getStreamKey().collectLatest { result ->
                when (result) {
                    is Result.Loading -> setLoading(LoadState.Loading(), LoadType.ACTION)
                    is Result.Error -> {
                        when (result.exception) {
                            is ApiException -> {
                                _uiState.update { state ->
                                    state.copy(
                                        exception = result.exception,
                                        uiErrorMessage = UiText.somethingWentWrong
                                    )
                                }
                            }
                            is NoInternetException -> {
                                _uiState.update { state ->
                                    state.copy(
                                        exception = result.exception,
                                        uiErrorMessage = UiText.noInternet
                                    )
                                }
                            }
                        }
                        setLoading(LoadState.Error(result.exception), LoadType.ACTION)
                    }
                    is Result.Success -> {
                        // TODO: parse result
                        setLoading(LoadState.NotLoading.Complete, LoadType.ACTION)
                        sendEvent(StreamListUiEvent.GotoPublish(result.data))
                    }
                }
            }
        }
    }

    private fun deleteStreamInternal(id: String) {
        val request = StreamIdRequest(id)
        repository.deleteStream(request)
            .onEach { result ->
                when (result) {
                    is Result.Error -> {
                        // Noop
                    }
                    Result.Loading -> {
                        // Noop
                    }
                    is Result.Success -> {
                        sendEvent(StreamListUiEvent.StreamDeleted)
                        refreshInternal(true)
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    private fun setLoading(
        loadState: LoadState,
        loadType: LoadType
    ) {
        val newLoadState = uiState.value.loadState.modifyState(loadType, loadState)
        _uiState.update { it.copy(loadState = newLoadState) }
    }

    private fun sendEvent(newEvent: StreamListUiEvent) = viewModelScope.launch {
        _uiEvent.emit(newEvent)
    }

    override fun onCleared() {
        val t = CancellationException("View model is dead")
        streamScheduledJob?.cancel(t)
        streamListFetchJob?.cancel(t)
        super.onCleared()
    }
}

data class StreamListState(
    val loadState: LoadStates = LoadStates.IDLE,
    val streamList: List<StreamUiModel> = emptyList(),
    val exception: Exception? = null,
    val uiErrorMessage: UiText? = null
)

interface StreamListUiAction {
    data class ErrorShown(val e: Exception) : StreamListUiAction
    data class DeleteStream(val id: String) : StreamListUiAction
    object Refresh : StreamListUiAction
    object GetStreamKey : StreamListUiAction
}

interface StreamListUiEvent {
    data class ShowToast(val message: UiText) : StreamListUiEvent
    data class GotoPublish(val stream: WOWZStreamDto) : StreamListUiEvent
    object StreamDeleted : StreamListUiEvent
}

interface StreamUiModel {
    data class Item(val streamItem: StreamDto) : StreamUiModel
}

private const val SCHEDULE_DELAY = 30000L
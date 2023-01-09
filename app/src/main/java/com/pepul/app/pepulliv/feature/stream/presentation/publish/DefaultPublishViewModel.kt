package com.pepul.app.pepulliv.feature.stream.presentation.publish

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pepul.app.pepulliv.Constant.STREAM_STATE_STARTING
import com.pepul.app.pepulliv.commons.util.Result
import com.pepul.app.pepulliv.commons.util.UiText
import com.pepul.app.pepulliv.feature.stream.domain.model.request.StreamIdRequest
import com.pepul.app.pepulliv.feature.stream.domain.repository.StreamRepository
import com.pepul.app.pepulliv.view.LiveRecordButton
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class DefaultPublishViewModel @Inject constructor(
    private val repository: StreamRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<DefaultPublishState>(DefaultPublishState())
    val uiState: StateFlow<DefaultPublishState> = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<DefaultPublishUiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    val accept: (DefaultPublishUiAction) -> Unit

    private var startPublishJob: Job? = null
    private var stopPublishJob: Job? = null

    private var streamStateFetchJob: Job? = null

    init {
        accept = { uiAction -> onUiAction(uiAction) }
    }

    private fun onUiAction(action: DefaultPublishUiAction) {
        when (action) {
            is DefaultPublishUiAction.ErrorShown -> {
                _uiState.update { state ->
                    state.copy(
                        exception = null,
                        uiErrorMessage = null
                    )
                }
            }
        }
    }

    fun startPublish(id: String) {
        if (startPublishJob?.isActive == true) {
            val cause = IllegalStateException("A start publish job is already active. Ignoring request")
            Timber.d(cause)
            return
        }
        startPublishJob?.cancel(CancellationException("New Request")) // just in case
        val request = StreamIdRequest(id)
        startPublishJob = repository.startStream(request)
            .onEach { result ->
                Timber.d("Start Stream: $result")
                when (result) {
                    is Result.Success -> {
                        scheduleStreamStateInternal(id)
                    }
                    is Result.Error -> {
                        // Noop
                    }
                    is Result.Loading -> {
                        // Noop
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    fun stopPublish(id: String) {
        if (stopPublishJob?.isActive == true) {
            val cause = IllegalStateException("A stop publish job is already active. Ignoring request")
            Timber.d(cause)
            return
        }
        streamStateFetchJob?.cancel(CancellationException("Stream stop request received"))
        stopPublishJob?.cancel(CancellationException("New Request")) // just in case
        val request = StreamIdRequest(id)
        stopPublishJob = repository.stopStream(request)
            .onEach { result ->
                Timber.d("Stop Stream: $result")
            }
            .launchIn(viewModelScope)
    }

    private fun scheduleStreamStateInternal(id: String) {
        if (streamStateFetchJob?.isActive == true) {
            val cause = IllegalStateException("A state fetch job is already active. Ignoring request")
            Timber.d(cause)
            return
        }
        streamStateFetchJob?.cancel(CancellationException("New request")) // just in case
        val request = StreamIdRequest(id)
        getStreamState(request)
        scheduleNewStateFetchJob(id)
    }

    private fun getStreamState(request: StreamIdRequest) {
        repository.getStreamState(request)
            .onEach { result ->
                Timber.d("Stream state: result $result")
                when (result) {
                    is Result.Success -> {
                        Timber.d("Stream state: ${result.data}")
                        _uiState.update { state ->
                            state.copy(streamState = result.data.state)
                        }
                        if (result.data.state == STREAM_STATE_STARTING) {
                            scheduleNewStateFetchJob(request.id)
                        }
                    }
                    else -> {
                        // Noop
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    private fun scheduleNewStateFetchJob(id: String) {
        streamStateFetchJob?.cancel(CancellationException("New request")) // just in case
        val request = StreamIdRequest(id)
        streamStateFetchJob = viewModelScope.launch {
            delay(1000)
            getStreamState(request)
        }
    }

    val STREAM_STATE_FETCH_DELAY = 1000L
}

data class DefaultPublishState(
    val streamState: String = DEFAULT_STREAM_STATE,
    val recordButtonState: LiveRecordButton.LiveRecordButtonState = LiveRecordButton.LiveRecordButtonState.IDLE,
    val exception: Exception? = null,
    val uiErrorMessage: UiText? = null
)

interface DefaultPublishUiAction {
    data class ErrorShown(val e: Exception) : DefaultPublishUiAction
}

interface DefaultPublishUiEvent {
    data class ShowToast(val message: UiText) : DefaultPublishUiEvent
}

private const val DEFAULT_STREAM_STATE = "new"
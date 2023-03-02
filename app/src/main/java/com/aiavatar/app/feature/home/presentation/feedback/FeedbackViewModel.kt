package com.aiavatar.app.feature.home.presentation.feedback

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiavatar.app.commons.util.Result
import com.aiavatar.app.commons.util.UiText
import com.aiavatar.app.commons.util.loadstate.LoadState
import com.aiavatar.app.commons.util.loadstate.LoadStates
import com.aiavatar.app.commons.util.loadstate.LoadType
import com.aiavatar.app.commons.util.net.NoInternetException
import com.aiavatar.app.core.di.ApplicationCoroutineScope
import com.aiavatar.app.feature.onboard.domain.model.request.FeedbackRequest
import com.aiavatar.app.feature.onboard.domain.repository.AccountsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject


/**
 * @author Hariskumar Kubendran
 * @date 01/03/23
 * Pepul Tech
 * hariskumar@pepul.com
 */
@HiltViewModel
class FeedbackViewModel @Inject constructor(
    private val accountsRepository: AccountsRepository,
    @ApplicationCoroutineScope
    private val externalScope: CoroutineScope,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FeedbackState())
    val uiState: StateFlow<FeedbackState> = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<FeedbackUiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    fun errorShown(@Suppress("UNUSED_PARAMETER") e: Exception) {
        _uiState.update { state ->
            state.copy(
                exception = null,
                uiErrorText = null
            )
        }
    }

    fun sendFeedback(
        rating: String,
        tags: String,
        comment: String
    ) {
        val request = FeedbackRequest(
            rating = rating,
            tags = tags,
            comment = comment,
        )
        sendFeedbackInternal(request)
    }

    private fun sendFeedbackInternal(request: FeedbackRequest) {
        externalScope.launch {
            accountsRepository.feedback(request).collectLatest { result ->
                when (result) {
                    is Result.Loading -> {
                        setLoadingInternal(LoadType.ACTION, LoadState.Loading())
                    }
                    is Result.Error -> {
                        when (result.exception) {
                            is NoInternetException -> {
                                _uiState.update { state ->
                                    state.copy(
                                        exception = result.exception,
                                        uiErrorText = UiText.noInternet
                                    )
                                }
                            }
                            else -> {
                                _uiState.update { state ->
                                    state.copy(
                                        exception = result.exception,
                                        uiErrorText = UiText.somethingWentWrong
                                    )
                                }
                            }
                        }
                        setLoadingInternal(LoadType.ACTION, LoadState.Error(result.exception))
                    }
                    is Result.Success -> {
                        setLoadingInternal(LoadType.ACTION, LoadState.NotLoading.Complete)
                        sendEvent(FeedbackUiEvent.ShowToast(UiText.DynamicString("Thanks for your feedback!")))
                        sendEvent(FeedbackUiEvent.FeedbackSubmitted)
                    }
                }
            }
        }
    }

    @Suppress("SameParameterValue")
    private fun setLoadingInternal(
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

    private fun sendEvent(newEvent: FeedbackUiEvent) = viewModelScope.launch {
        _uiEvent.emit(newEvent)
    }
}

data class FeedbackState(
    val loadState: LoadStates = LoadStates.IDLE,
    val exception: Exception? = null,
    val uiErrorText: UiText? = null
)

interface FeedbackUiEvent {
    data class ShowToast(val message: UiText) : FeedbackUiEvent
    object FeedbackSubmitted : FeedbackUiEvent
}
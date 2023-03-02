package com.aiavatar.app.feature.home.presentation.deleteaccount

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiavatar.app.commons.util.UiText
import com.aiavatar.app.commons.util.loadstate.LoadState
import com.aiavatar.app.commons.util.loadstate.LoadStates
import com.aiavatar.app.commons.util.loadstate.LoadType
import com.aiavatar.app.mapButReplace
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DeleteAccountViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private var _uiState = MutableStateFlow(DeleteAccountUiState())
    val uiState: StateFlow<DeleteAccountUiState> = _uiState
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DeleteAccountUiState()
        )

    private var _uiEvent = MutableSharedFlow<DeleteAccountUiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    private var deleteAccountTempJob: Job? = null

    val accept: (DeleteAccountUiAction) -> Unit

    private val typingState = MutableSharedFlow<DeleteAccountUiAction>()

    private var recaptchaToken: String = ""

    init {

        typingState
            .filterIsInstance<DeleteAccountUiAction.TypingEmail>()
            .distinctUntilChanged()
            .onEach { action ->
                _uiState.update { it.copy(typedEmail = action.typed) }
            }
            .launchIn(viewModelScope)

        typingState
            .filterIsInstance<DeleteAccountUiAction.TypingTitle>()
            .distinctUntilChanged()
            .onEach { action ->
                _uiState.update { it.copy(title = action.selectedTitle) }
            }
            .launchIn(viewModelScope)

        typingState
            .filterIsInstance<DeleteAccountUiAction.TypingDescription>()
            .distinctUntilChanged()
            .onEach { action ->
                _uiState.update { it.copy(description = action.typedDescription) }
            }
            .launchIn(viewModelScope)

        accept = { uiAction -> onUiAction(uiAction) }
    }

    private fun onUiAction(action: DeleteAccountUiAction) {
        when (action) {
            is DeleteAccountUiAction.ErrorShown -> {
                _uiState.update {
                    it.copy(
                        exception = null,
                        uiErrorMessage = null
                    )
                }
            }
            is DeleteAccountUiAction.TypingDescription,
            is DeleteAccountUiAction.TypingTitle,
            is DeleteAccountUiAction.TypingEmail -> {
                viewModelScope.launch { typingState.emit(action) }
            }
            is DeleteAccountUiAction.Validate -> {
                validateInternal(action.suppressError)
            }
        }
    }

    fun setQuestionnaireOpts(newQuestionnaire: List<DeleteAccountFragment.QuestionnaireOpt>) {
        _uiState.update { state ->
            state.copy(
                questionnaireOpts = newQuestionnaire
            )
        }
    }

    fun onQuestionnaireToggle(data: DeleteAccountFragment.QuestionnaireOpt, checked: Boolean) {
        val newOpts = uiState.value.questionnaireOpts?.toMutableList()?.mapButReplace(data, data.copy(checked = checked))
            ?: return

        _uiState.update { state ->
            state.copy(
                questionnaireOpts = newOpts
            )
        }
    }

    /*private fun deleteAccountTemp(
        request: DeleteAccountRequest
    ) {
        deleteAccountTempJob = viewModelScope.launch {
            repository.deleteAccountTemp(request).collectLatest { result ->
                when (result) {
                    is Result.Loading -> setLoading(LoadType.ACTION, LoadState.Loading())
                    is Result.Error -> {
                        when (result.exception) {
                            is ApiException -> {
                                when (result.exception.cause) {
                                    is InvalidMobileNumberException -> {
                                        _uiState.update {
                                            it.copy(
                                                exception = ResolvableException(result.exception.cause),
                                                uiErrorMessage = UiText.DynamicString("Please enter the mobile number used to login.")
                                            )
                                        }
                                    }
                                    is RecaptchaException -> {
                                        _uiState.update {
                                            it.copy(
                                                exception = ResolvableException(result.exception.cause),
                                                uiErrorMessage = UiText.DynamicString("Complete ReCaptcha to continue")
                                            )
                                        }
                                    }
                                    else -> {
                                        _uiState.update {
                                            it.copy(
                                                exception = result.exception,
                                                uiErrorMessage = UiText.somethingWentWrong
                                            )
                                        }
                                    }
                                }
                            }
                            is NoInternetException -> {
                                _uiState.update {
                                    it.copy(
                                        exception = result.exception,
                                        uiErrorMessage = UiText.noInternet
                                    )
                                }
                            }
                        }
                        setLoading(LoadType.ACTION, LoadState.Error(result.exception))
                    }
                    is Result.Success -> {
                        setLoading(LoadType.ACTION, LoadState.NotLoading.Complete)
                        sendEvent(DeleteAccountUiEvent.ShowToast(UiText.DynamicString(result.data)))
                        sendEvent(DeleteAccountUiEvent.NextScreen(System.currentTimeMillis()))
                    }
                }
            }
        }
    }*/

    private fun validateInternal(suppressError: Boolean = false) = viewModelScope.launch {

        // TODO: Email validation

    }



    private fun sendEvent(newEvent: DeleteAccountUiEvent) = viewModelScope.launch {
        _uiEvent.emit(newEvent)
    }

    private fun setLoading(
        loadType: LoadType,
        loadState: LoadState
    ) {
        val newLoadState = uiState.value.loadState.modifyState(loadType, loadState)
        _uiState.update { it.copy(loadState = newLoadState) }
    }
}

const val CALL_FOR_SEND_OTP = "sendOtp"
const val CALL_FOR_VERIFY_OTP = "verifyOtp"

data class DeleteAccountUiState(
    val loadState: LoadStates = LoadStates.IDLE,
    val typedEmail: String = "",
    val title: String = "",
    val description: String = "",
    val questionnaireOpts: List<DeleteAccountFragment.QuestionnaireOpt>? = null,
    val exception: Exception? = null,
    val uiErrorMessage: UiText? = null
)

interface DeleteAccountUiAction {
    data class ErrorShown(val e: Exception) : DeleteAccountUiAction
    data class TypingEmail(val typed: String) : DeleteAccountUiAction
    data class TypingDescription(val typedDescription: String) : DeleteAccountUiAction
    data class TypingTitle(val selectedTitle: String) : DeleteAccountUiAction
    data class Validate(val suppressError: Boolean) : DeleteAccountUiAction
}

interface DeleteAccountUiEvent {
    data class ShowSnack(val message: UiText) : DeleteAccountUiEvent
    data class ShowToast(val message: UiText) : DeleteAccountUiEvent
    data class NextScreen(val timestamp: Long) : DeleteAccountUiEvent
}
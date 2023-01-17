package com.aiavatar.app.feature.onboard.presentation.login

import androidx.core.util.PatternsCompat
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiavatar.app.Constant
import com.aiavatar.app.commons.util.InvalidOtpException
import com.aiavatar.app.commons.util.ResolvableException
import com.aiavatar.app.commons.util.Result
import com.aiavatar.app.commons.util.UiText
import com.aiavatar.app.commons.util.ValidationResult
import com.aiavatar.app.commons.util.loadstate.LoadType
import com.aiavatar.app.commons.util.net.ApiException
import com.aiavatar.app.commons.util.net.NoInternetException
import com.aiavatar.app.commons.util.succeeded
import com.aiavatar.app.di.ApplicationDependencies
import com.aiavatar.app.feature.onboard.domain.model.request.LoginRequest
import com.aiavatar.app.feature.onboard.domain.repository.AccountsRepository
import com.aiavatar.app.nullAsEmpty
import com.pepulnow.app.data.LoadState
import com.pepulnow.app.data.LoadStates
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    val savedStateHandle: SavedStateHandle,
    val repository: AccountsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginState>(LoginState())
    val uiState: StateFlow<LoginState> = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<LoginUiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    val accept: (LoginUiAction) -> Unit

    private val continuousActions = MutableSharedFlow<LoginUiAction>()

    private var loginJob: Job? = null

    init {
        accept = { uiAction -> onUiAction(uiAction) }

        // TODO: validate username and save to state
        continuousActions.filterIsInstance<LoginUiAction.TypingUsername>()
            .distinctUntilChanged()
            .onEach { action ->
                _uiState.update { state ->
                    state.copy(
                        typedEmail = action.typed
                    )
                }
            }
            .launchIn(viewModelScope)

        continuousActions.filterIsInstance<LoginUiAction.TypingOtp>()
            .distinctUntilChanged()
            .onEach { action ->
                _uiState.update { state ->
                    state.copy(
                        typedOtp = action.typed
                    )
                }
            }
            .launchIn(viewModelScope)

    }

    private fun onUiAction(action: LoginUiAction) {
        when (action) {
            is LoginUiAction.ErrorShown -> {
                _uiState.update { state ->
                    state.copy(
                        exception = null,
                        uiErrorMessage = null
                    )
                }
            }
            is LoginUiAction.TypingUsername,
            is LoginUiAction.TypingOtp -> {
                viewModelScope.launch { continuousActions.emit(action) }
            }
            is LoginUiAction.NextClick -> {
                when (uiState.value.loginSequence) {
                    LoginSequence.TYPING_EMAIL -> { validateLogin() }
                    LoginSequence.OTP_SENT -> { validateOtp() }
                    LoginSequence.OTP_VERIFIED -> { /* Noop */ }
                }
            }

        }
    }

    private fun validateOtp() {
        val otp = uiState.value.typedOtp
        // TODO: validate otp
        if (otp.isBlank()) {
            val message = "Please enter a otp"
            val e = ResolvableException(message)
            _uiState.update { state ->
                state.copy(
                    exception = e,
                    uiErrorMessage = UiText.DynamicString(message)
                )
            }
            return
        }

        if (otp.length < 6) {
            val message = "OTP contains at least 6 digits"
            val e = ResolvableException(message)
            _uiState.update { state ->
                state.copy(
                    exception = e,
                    uiErrorMessage = UiText.DynamicString(message)
                )
            }
            return
        }

        verifyOtpInternal(otp)
    }

    private fun validateLogin() {
        // TODO: validate username
        val email = uiState.value.typedEmail
        if (email.isBlank()) {
            val message = "Please enter a email"
            val e = ResolvableException(message)
            _uiState.update { state ->
                state.copy(
                    exception = e,
                    uiErrorMessage = UiText.DynamicString(message)
                )
            }
            return
        }

        if (email.length < 4) {
            val message = "Email should contain at least 4 characters"
            val e = ResolvableException(message)
            _uiState.update { state ->
                state.copy(
                    exception = e,
                    uiErrorMessage = UiText.DynamicString(message)
                )
            }
            return
        }

        if (!PatternsCompat.EMAIL_ADDRESS.toRegex().matches(email)) {
            val message = "Enter a valid Email Address"
            val e = ResolvableException(message)
            _uiState.update { state ->
                state.copy(
                    exception = e,
                    uiErrorMessage = UiText.DynamicString(message)
                )
            }
            return
        }

        loginUserInternal(email)
    }

    private fun loginUserInternal(email: String) {
        val request = LoginRequest(
            email = email,
            callFor = CALL_FOR_SEND_OTP,
            platform = Constant.PLATFORM,
        )
        login(request)
    }

    private fun verifyOtpInternal(otp: String) {
        val request = LoginRequest(
            email = uiState.value.typedEmail,
            callFor = CALL_FOR_VERIFY_OTP,
            platform = Constant.PLATFORM
        )
        request.otp = otp
        login(request)
    }

    private fun login(loginRequest: LoginRequest) {
        if (loginJob?.isActive == true) {
            val t = IllegalStateException("A login request is already in progress. Ignoring request")
            Timber.d(t)
            return
        }
        loginJob?.cancel(CancellationException("New request")) // just in case
        loginJob = viewModelScope.launch {
            repository.loginUser(loginRequest).collectLatest { result ->
                when (result) {
                    is Result.Loading -> setLoading(LoadState.Loading(), LoadType.ACTION)
                    is Result.Error -> {
                        when (result.exception) {
                            is ApiException -> {
                                when (result.exception.cause) {
                                    is InvalidOtpException -> {
                                        _uiState.update { state ->
                                            state.copy(
                                                exception = ResolvableException(result.exception.cause),
                                                uiErrorMessage = UiText.DynamicString("Please enter a valid OTP!")
                                            )
                                        }
                                    }
                                    else -> {
                                        _uiState.update { state ->
                                            state.copy(
                                                exception = result.exception,
                                                uiErrorMessage = UiText.somethingWentWrong
                                            )
                                        }
                                    }
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
                        when (loginRequest.callFor) {
                            CALL_FOR_SEND_OTP -> {
                                sendEvent(LoginUiEvent.ShowToast(UiText.DynamicString("Otp sent to your Email")))
                                _uiState.update { state ->
                                    state.copy(
                                        loginSequence = LoginSequence.OTP_SENT
                                    )
                                }
                                setLoading(LoadState.NotLoading.Complete, LoadType.ACTION)
                            }
                            CALL_FOR_VERIFY_OTP -> {
                                result.data.let { loginData ->
                                    ApplicationDependencies.getPersistentStore()
                                        .setUserId(loginData.loginUser?.userId.nullAsEmpty())
                                        .setDeviceToken(loginData.deviceToken.nullAsEmpty())
                                        .setUsername(loginData.loginUser?.username.nullAsEmpty())
                                        .setEmail(loginRequest.email)
                                }
                                sendEvent(LoginUiEvent.ShowToast(UiText.DynamicString("Login successful!")))
                                sendEvent(LoginUiEvent.NextScreen)
                            }
                        }
                    }
                }
            }
        }

    }

    @Suppress("SameParameterValue")
    private fun setLoading(
        loadState: LoadState,
        loadType: LoadType
    ) {
        val newLoadState = uiState.value.loadState.modifyState(loadType, loadState)
        _uiState.update { it.copy(loadState = newLoadState) }
    }

    private fun sendEvent(newEvent: LoginUiEvent) = viewModelScope.launch {
        _uiEvent.emit(newEvent)
    }

}

data class LoginState(
    val loadState: LoadStates = LoadStates.IDLE,
    val loginSequence: LoginSequence = LoginSequence.TYPING_EMAIL,
    val typedEmail: String = DEFAULT_EMAIL,
    val typedOtp: String = "",
    val usernameValidationResult: ValidationResult? = null,
    val exception: Exception? = null,
    val uiErrorMessage: UiText? = null
)

interface LoginUiAction {
    data class ErrorShown(val e: Exception) : LoginUiAction
    data class TypingUsername(val typed: String) : LoginUiAction
    data class TypingOtp(val typed: String) : LoginUiAction
    object NextClick : LoginUiAction
}

interface LoginUiEvent {
    data class ShowToast(val message: UiText) : LoginUiEvent
    object NextScreen : LoginUiEvent
}

private const val DEFAULT_EMAIL = ""

private const val CALL_FOR_SEND_OTP = "sendOtp"
private const val CALL_FOR_VERIFY_OTP = "verifyOtp"

enum class LoginSequence(sequence: Int) {
    TYPING_EMAIL(-1), OTP_SENT(0), OTP_VERIFIED(1)
}
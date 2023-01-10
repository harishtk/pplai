package com.pepulai.app.feature.onboard.presentation.login

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pepulai.app.commons.util.ResolvableException
import com.pepulai.app.commons.util.Result
import com.pepulai.app.commons.util.UiText
import com.pepulai.app.commons.util.ValidationResult
import com.pepulai.app.commons.util.loadstate.LoadType
import com.pepulai.app.commons.util.net.ApiException
import com.pepulai.app.commons.util.net.NoInternetException
import com.pepulai.app.di.ApplicationDependencies
import com.pepulai.app.feature.onboard.domain.model.request.LoginRequest
import com.pepulai.app.feature.onboard.domain.repository.AccountsRepository
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
                        typedUsername = action.typed
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
            is LoginUiAction.TypingUsername -> {
                viewModelScope.launch { continuousActions.emit(action) }
            }
            is LoginUiAction.NextClick -> {
                validateLogin()
            }

        }
    }

    private fun validateLogin() {
        // TODO: validate username
        val username = uiState.value.typedUsername
        if (username.isBlank()) {
            val message = "Please enter a username"
            val e = ResolvableException(message)
            _uiState.update { state ->
                state.copy(
                    exception = e,
                    uiErrorMessage = UiText.DynamicString(message)
                )
            }
            return
        }

        if (username.length < 4) {
            val message = "Username should contain at least 4 characters"
            val e = ResolvableException(message)
            _uiState.update { state ->
                state.copy(
                    exception = e,
                    uiErrorMessage = UiText.DynamicString(message)
                )
            }
            return
        }

        loginUserInternal(username)
    }

    private fun loginUserInternal(username: String) {
        val request = LoginRequest(
            userName = username
        )
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
                        result.data.apply {
                            ApplicationDependencies.getPersistentStore()
                                .setDeviceToken(deviceToken)
                                .setUserId(userId)
                                .setUsername(loginRequest.userName)
                        }
                        sendEvent(LoginUiEvent.ShowToast(UiText.DynamicString("Login Successful")))
                        sendEvent(LoginUiEvent.NextScreen)
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
    val typedUsername: String = DEFAULT_USERNAME,
    val usernameValidationResult: ValidationResult? = null,
    val exception: Exception? = null,
    val uiErrorMessage: UiText? = null
)

interface LoginUiAction {
    data class ErrorShown(val e: Exception) : LoginUiAction
    data class TypingUsername(val typed: String) : LoginUiAction
    object NextClick : LoginUiAction
}

interface LoginUiEvent {
    data class ShowToast(val message: UiText) : LoginUiEvent
    object NextScreen : LoginUiEvent
}

private const val DEFAULT_USERNAME = ""
package com.aiavatar.app.feature.home.presentation.subscription

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiavatar.app.BuildConfig
import com.aiavatar.app.commons.util.ResolvableException
import com.aiavatar.app.commons.util.Result
import com.aiavatar.app.commons.util.UiText
import com.aiavatar.app.commons.util.loadstate.LoadType
import com.aiavatar.app.commons.util.net.ApiException
import com.aiavatar.app.commons.util.net.NoInternetException
import com.aiavatar.app.core.data.source.local.AppDatabase
import com.aiavatar.app.di.ApplicationDependencies
import com.aiavatar.app.feature.home.domain.model.request.GenerateAvatarRequest
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
class SubscriptionSuccessViewModel @Inject constructor(
    private val homeRepository: HomeRepository,
    @Deprecated("move to repo")
    private val appDatabase: AppDatabase,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow<SubscriptionSuccessState>(SubscriptionSuccessState())
    val uiState: StateFlow<SubscriptionSuccessState> = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<SubscriptionSuccessUiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    val accept: (SubscriptionSuccessUiAction) -> Unit

    private var generateAvatarJob: Job? = null

    init {
        accept = { uiAction -> onUiAction(uiAction) }
    }

    private fun onUiAction(action: SubscriptionSuccessUiAction) {
        when (action) {
            is SubscriptionSuccessUiAction.ErrorShown -> {
                _uiState.update { state ->
                    state.copy(
                        exception = null,
                        uiErrorText = null
                    )
                }
            }
        }
    }

    fun generateAvatarRequest(planId: String) {
        generateAvatarRequestInternal(planId)
    }

    private fun generateAvatarRequestInternal(planId: String) = viewModelScope.launch {
        val avatarStatusId = ApplicationDependencies.getPersistentStore().currentAvatarStatusId
        if (avatarStatusId != null) {
            val avatarStatus = appDatabase.avatarStatusDao().getAvatarStatusSync(id = avatarStatusId.toLong())
                ?.avatarStatusEntity

            if (avatarStatus != null) {
                val request = GenerateAvatarRequest(
                    id = planId,
                    modelId = avatarStatus.modelId
                )
                generateAvatarRequest(request)
            } else {
                sendEvent(SubscriptionSuccessUiEvent.ShowToast(
                    UiText.DynamicString("Cannot complete your purchase")
                ))
                val t = IllegalStateException("Avatar status not found")
                Timber.e(t)
            }
        } else {
            val t = IllegalStateException("Something went wrong")
            Timber.e(t)
        }
    }

    private fun generateAvatarRequest(request: GenerateAvatarRequest) {
        if (generateAvatarJob?.isActive == true) {
            val t = IllegalStateException("A request is already active. Ignoring request")
            if (BuildConfig.DEBUG) {
                Timber.e(t)
            }
            return
        }
        generateAvatarJob?.cancel(CancellationException("New request"))
        generateAvatarJob = viewModelScope.launch {
            homeRepository.generateAvatar(request).collectLatest { result ->
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
                        ApplicationDependencies.getPersistentStore().apply {
                            setCurrentAvatarStatusId(result.data.toString())
                        }
                        sendEvent(SubscriptionSuccessUiEvent.NextScreen)
                    }
                }
            }
        }
    }

    private fun setLoading(
        loadType: LoadType,
        loadState: LoadState
    ) {
        val newLoadState = uiState.value.loadState.modifyState(loadType, loadState)
        _uiState.update { state -> state.copy(loadState = newLoadState) }
    }

    private fun sendEvent(newEvent: SubscriptionSuccessUiEvent) = viewModelScope.launch {
        _uiEvent.emit(newEvent)
    }
}

data class SubscriptionSuccessState(
    val loadState: LoadStates = LoadStates.IDLE,
    val exception: Exception? = null,
    val uiErrorText: UiText? = null
)

interface SubscriptionSuccessUiAction {
    data class ErrorShown(val e: Exception) : SubscriptionSuccessUiAction
}

interface SubscriptionSuccessUiEvent {
    data class ShowToast(val message: UiText) : SubscriptionSuccessUiEvent
    @Deprecated("event will never be triggered")
    object NextScreen : SubscriptionSuccessUiEvent
}
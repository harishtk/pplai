package com.aiavatar.app.feature.home.presentation.subscription

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiavatar.app.commons.util.Result
import com.aiavatar.app.commons.util.UiText
import com.aiavatar.app.commons.util.loadstate.LoadType
import com.aiavatar.app.commons.util.net.ApiException
import com.aiavatar.app.commons.util.net.NoInternetException
import com.aiavatar.app.feature.home.domain.model.SubscriptionPlan
import com.aiavatar.app.feature.home.domain.repository.HomeRepository
import com.pepulnow.app.data.LoadState
import com.pepulnow.app.data.LoadStates
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class SubscriptionViewModel @Inject constructor(
    private val homeRepository: HomeRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow<SubscriptionState>(SubscriptionState())
    val uiState: StateFlow<SubscriptionState> = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<SubscriptionUiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    val accept: (SubscriptionUiAction) -> Unit

    private var selectedPlanId: Int = -1
    private val selectedToggleFlow = MutableStateFlow(false)

    private var subscriptionPlanFetchJob: Job? = null

    init {
        val subscriptionPlanModelFlow = uiState.map { it.subscriptionPlans }
            .distinctUntilChanged()
        combine(
            selectedToggleFlow,
            subscriptionPlanModelFlow,
            ::Pair
        ).map { (selectedToggle, subscriptionPlanModel) ->
            val newSubscriptionPlanList = subscriptionPlanModel.map { model ->
                if (model is SubscriptionUiModel.Plan) {
                    model.copy(model.subscriptionPlan, selected = selectedPlanId == model.subscriptionPlan.id)
                } else {
                    model
                }
            }
            newSubscriptionPlanList
        }.onEach { subscriptionModelList ->
            _uiState.update { state ->
                state.copy(
                    subscriptionPlans = subscriptionModelList
                )
            }
            Timber.d("Plans: $subscriptionModelList")
        }.launchIn(viewModelScope)

        accept = { uiAction -> onUiAction(uiAction) }

        refreshInternal()
    }

    private fun onUiAction(uiAction: SubscriptionUiAction) {
        when (uiAction) {
            is SubscriptionUiAction.ErrorShown -> {
                _uiState.update { state ->
                    state.copy(
                        exception = null,
                        uiErrorText = null
                    )
                }
            }
            is SubscriptionUiAction.Retry -> {
                refreshInternal()
            }
            is SubscriptionUiAction.ToggleSelectedPlan -> {
                if (selectedPlanId != uiAction.planId) {
                    selectedPlanId = uiAction.planId
                    // Signals the flow
                    selectedToggleFlow.update { selectedToggleFlow.value.not() }
                }
            }
        }
    }

    private fun refreshInternal() {
        getSubscriptionPlans()
    }

    private fun getSubscriptionPlans() {
        if (subscriptionPlanFetchJob?.isActive == true) {
            val t = IllegalStateException("A login request is already in progress. Ignoring request")
            Timber.d(t)
            return
        }
        subscriptionPlanFetchJob?.cancel(CancellationException("New request")) // just in case
        subscriptionPlanFetchJob = viewModelScope.launch {
            homeRepository.getSubscriptionPlans().collectLatest { result ->
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
                        val plans: List<SubscriptionUiModel> = result.data.map { SubscriptionUiModel.Plan(it) }
                        _uiState.update { state ->
                            state.copy(
                                subscriptionPlans = plans
                            )
                        }
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
        _uiState.update { state ->
            state.copy(
                loadState = newLoadState
            )
        }
    }

    private fun sendEvent(newEvent: SubscriptionUiEvent) = viewModelScope.launch {
        _uiEvent.emit(newEvent)
    }

}

data class SubscriptionState(
    val loadState: LoadStates = LoadStates.IDLE,
    val subscriptionPlans: List<SubscriptionUiModel> = emptyList(),
    val exception: Exception? = null,
    val uiErrorText: UiText? = null
)

interface SubscriptionUiAction {
    data class ErrorShown(val e: Exception) : SubscriptionUiAction
    data class ToggleSelectedPlan(val planId: Int) : SubscriptionUiAction
    object Retry : SubscriptionUiAction
}

interface SubscriptionUiEvent {
    data class ShowToast(val message: UiText) : SubscriptionUiEvent
}

interface SubscriptionUiModel {
    data class Plan(val subscriptionPlan: SubscriptionPlan, val selected: Boolean = false) : SubscriptionUiModel
}
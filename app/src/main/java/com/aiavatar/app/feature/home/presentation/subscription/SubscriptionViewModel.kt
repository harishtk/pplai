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
import com.aiavatar.app.core.data.source.local.entity.toEntity
import com.aiavatar.app.core.domain.model.AvatarStatus
import com.aiavatar.app.di.ApplicationDependencies
import com.aiavatar.app.feature.home.domain.model.SubscriptionPlan
import com.aiavatar.app.feature.home.domain.model.request.SubscriptionPurchaseRequest
import com.aiavatar.app.feature.home.domain.repository.HomeRepository
import com.aiavatar.app.commons.util.loadstate.LoadState
import com.aiavatar.app.commons.util.loadstate.LoadStates
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
    private val appDatabase: AppDatabase,
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
    private var subscriptionPurchaseJob: Job? = null

    init {
        val subscriptionPlanModelFlow = uiState.map { it.subscriptionPlansUiModels }
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
                    subscriptionPlansUiModels = subscriptionModelList
                )
            }
            Timber.d("Plans: $subscriptionModelList")
        }.launchIn(viewModelScope)

        accept = { uiAction -> onUiAction(uiAction) }
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
            is SubscriptionUiAction.NextClick -> {
                startPurchaseFlowInternal()
            }
        }
    }

    fun refresh() {
        refreshInternal()
    }

    fun setModelId(modelId: String) {
        _uiState.update { state ->
            state.copy(
                modelId = modelId
            )
        }
    }

    fun setSubscriptionUiModelList(
        newUiModelList: List<SubscriptionUiModel>
    ) {
        _uiState.update { state ->
            state.copy(
                subscriptionPlansUiModels = newUiModelList
            )
        }
    }

    fun setError(
        e: Exception?,
        uiErrorText: UiText?
    ) {
        _uiState.update { state ->
            state.copy(
                exception = e,
                uiErrorText = uiErrorText
            )
        }
    }

    fun setLoading(
        loadType: LoadType,
        loadState: LoadState
    ) {
        setLoadingInternal(loadType, loadState)
    }

    private fun startPurchaseFlowInternal() {
        // TODO: start Google Play purchase
        viewModelScope.launch {
            val modelId = uiState.value.modelId
            if (modelId != null) {
                val selectedPlan = uiState.value.subscriptionPlansUiModels
                    .filterIsInstance<SubscriptionUiModel.Plan>()
                    .find { it.selected }?.subscriptionPlan

                if (selectedPlan != null) {
                    val request = SubscriptionPurchaseRequest(
                        id = selectedPlan.id.toString(),
                        modelId = modelId,
                        transactionId = ""
                    )

                    sendPurchaseDetailToServer(request)

                } else {
                    val t = IllegalStateException("No plans selected")
                    _uiState.update { state ->
                        state.copy(
                            exception = ResolvableException(t),
                            uiErrorText = UiText.DynamicString("Please select a plan.")
                        )
                    }
                }
            } else {
                val t = IllegalStateException("No model id")
                _uiState.update { state ->
                    state.copy(
                        exception = ResolvableException(t),
                        uiErrorText = UiText.DynamicString("Cannot complete your purchase now.")
                    )
                }
            }
        }
    }

    private fun refreshInternal() {
        _uiState.update { state -> state.copy(subscriptionPlansCache = null) }
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
                    is Result.Loading -> setLoadingInternal(LoadType.REFRESH, LoadState.Loading())
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
                        setLoadingInternal(LoadType.REFRESH, LoadState.Error(result.exception))
                    }
                    is Result.Success -> {
                        setLoadingInternal(LoadType.REFRESH, LoadState.NotLoading.InComplete)
                        _uiState.update { state ->
                            state.copy(
                                subscriptionPlansCache = result.data
                            )
                        }
                        /*val plans: List<SubscriptionUiModel> = result.data.mapIndexed { index, subscriptionPlan ->
                            if (subscriptionPlan.bestSeller) {
                                selectedPlanId = subscriptionPlan.id
                            }
                            SubscriptionUiModel.Plan(subscriptionPlan)
                        }
                        _uiState.update { state ->
                            state.copy(
                                subscriptionPlansUiModels = plans
                            )
                        }*/
                    }
                }
            }
        }
    }

    private fun sendPurchaseDetailToServer(request: SubscriptionPurchaseRequest) {
        if (subscriptionPurchaseJob?.isActive == true) {
            val t = IllegalStateException("A purchase request is already active. Ignoring request")
            if (BuildConfig.DEBUG) {
                Timber.e(t)
            }
            return
        }
        subscriptionPurchaseJob?.cancel(CancellationException("New request")) // just in case
        subscriptionPurchaseJob = viewModelScope.launch {
            homeRepository.purchasePlan(request).collectLatest { result ->
                when (result) {
                    is Result.Loading -> setLoadingInternal(LoadType.ACTION, LoadState.Loading())
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
                        setLoadingInternal(LoadType.ACTION, LoadState.Error(result.exception))
                    }
                    is Result.Success -> {
                        setLoadingInternal(LoadType.ACTION, LoadState.NotLoading.Complete)
                        ApplicationDependencies.getPersistentStore().apply {
                            setProcessingModel(true)
                        }
                        appDatabase.uploadSessionDao().apply {
                            appDatabase.avatarStatusDao().apply {
                                val newAvatarStatus = AvatarStatus.emptyStatus(result.data.modelId).apply {
                                    avatarStatusId = result.data.avatarStatusId
                                }
                                insert(newAvatarStatus.toEntity())
                            }
                        }
                        sendEvent(SubscriptionUiEvent.PurchaseComplete(request.id, result.data.avatarStatusId))
                    }
                }
            }
        }
    }

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

    private fun sendEvent(newEvent: SubscriptionUiEvent) = viewModelScope.launch {
        _uiEvent.emit(newEvent)
    }

}

data class SubscriptionState(
    val loadState: LoadStates = LoadStates.IDLE,
    val modelId: String? = null,
    val subscriptionPlansUiModels: List<SubscriptionUiModel> = emptyList(),
    val subscriptionPlansCache: List<SubscriptionPlan>? = null,
    val exception: Exception? = null,
    val uiErrorText: UiText? = null
)

interface SubscriptionUiAction {
    data class ErrorShown(val e: Exception) : SubscriptionUiAction
    data class ToggleSelectedPlan(val planId: Int) : SubscriptionUiAction
    object Retry : SubscriptionUiAction
    object NextClick : SubscriptionUiAction
}

interface SubscriptionUiEvent {
    data class ShowToast(val message: UiText) : SubscriptionUiEvent
    data class PurchaseComplete(val planId: String, val statusId: String) : SubscriptionUiEvent
}

interface SubscriptionUiModel {
    data class Plan(val subscriptionPlan: SubscriptionPlan, val selected: Boolean = false) : SubscriptionUiModel
}
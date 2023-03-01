package com.aiavatar.app.feature.home.presentation.coupon

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiavatar.app.BuildConfig
import com.aiavatar.app.commons.util.ResolvableException
import com.aiavatar.app.commons.util.Result
import com.aiavatar.app.commons.util.UiText
import com.aiavatar.app.commons.util.ValidationResult
import com.aiavatar.app.commons.util.loadstate.LoadState
import com.aiavatar.app.commons.util.loadstate.LoadStates
import com.aiavatar.app.commons.util.loadstate.LoadType
import com.aiavatar.app.commons.util.net.ApiException
import com.aiavatar.app.commons.util.net.NoInternetException
import com.aiavatar.app.commons.util.net.UnAuthorizedException
import com.aiavatar.app.core.data.source.local.AppDatabase
import com.aiavatar.app.core.data.source.local.entity.toEntity
import com.aiavatar.app.core.domain.model.AvatarStatus
import com.aiavatar.app.core.util.ValidateException
import com.aiavatar.app.di.ApplicationDependencies
import com.aiavatar.app.feature.home.domain.model.SubscriptionPlan
import com.aiavatar.app.feature.home.domain.model.request.SubscriptionPurchaseRequest
import com.aiavatar.app.feature.home.domain.model.request.VerifyCouponRequest
import com.aiavatar.app.feature.home.domain.repository.HomeRepository
import com.aiavatar.app.feature.home.presentation.subscription.SubscriptionUiEvent
import com.aiavatar.app.feature.home.presentation.subscription.SubscriptionUiModel
import com.aiavatar.app.feature.home.presentation.util.InvalidCouponCodeException
import com.aiavatar.app.nullAsEmpty
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import timber.log.Timber
import java.util.concurrent.CancellationException
import javax.inject.Inject


/**
 * @author Hariskumar Kubendran
 * @date 27/02/23
 * Pepul Tech
 * hariskumar@pepul.com
 */
@HiltViewModel
class CouponViewModel @Inject constructor(
    private val homeRepository: HomeRepository,
    @Deprecated("move to repo")
    private val appDatabase: AppDatabase,
    private val savedStateHandle: SavedStateHandle,
): ViewModel() {

    private val _uiState = MutableStateFlow<CouponState>(CouponState())
    val uiState: StateFlow<CouponState> = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<CouponUiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    val accept: (CouponUiAction) -> Unit

    private var selectedPlanId: Int = -1
    private val selectedToggleFlow = MutableStateFlow(false)

    private val continuousActions = MutableSharedFlow<CouponUiAction>()

    private var couponValidationJob: Job? = null
    private var subscriptionPurchaseJob: Job? = null

    init {
        val uiModelListFlow = uiState.map { it.uiModelList }
            .distinctUntilChanged()

        combine(
            selectedToggleFlow,
            uiModelListFlow,
            ::Pair
        ).map { (signal, modelList) ->
            val newSubscriptionPlanList = modelList.map { model ->
                if (model is SubscriptionUiModel.Plan) {
                    model.copy(model.subscriptionPlan, selected = selectedPlanId == model.subscriptionPlan.id)
                } else {
                    model
                }
            }
            newSubscriptionPlanList
        }
            .onEach { modelList ->
                _uiState.update { state ->
                    state.copy(
                        uiModelList = modelList
                    )
                }
            }
            .launchIn(viewModelScope)

        continuousActions.filterIsInstance<CouponUiAction.TypingCoupon>()
            .distinctUntilChanged()
            .map { action ->
                _uiState.update { state ->
                    state.copy(
                        typedCoupon = action.typed
                    )
                }
                action.typed
            }
            .onEach { typedCoupon ->
                val prevValidationResult = uiState.value.couponValidationResult
                if (prevValidationResult?.typedValue != typedCoupon) {
                    resetValidationInternal()
                }
                if (typedCoupon.length <= COUPON_CODE_VALIDATE_THRESHOLD_LENGTH) {
                    resetValidationInternal()
                } /*else {
                    // Uncomment me for real-time coupon validation
                    validateCouponCode(typedCoupon)
                }*/
            }
            .launchIn(viewModelScope)

        accept = { uiAction -> onUiAction(uiAction) }
    }

    private fun onUiAction(action: CouponUiAction) {
        when (action) {
            is CouponUiAction.ErrorShown -> {
                _uiState.update { state ->
                    state.copy(
                        exception = null,
                        uiErrorMessage = null
                    )
                }
            }
            is CouponUiAction.TypingCoupon -> {
                viewModelScope.launch {
                    continuousActions.emit(action)
                }
            }
            is CouponUiAction.Validate -> {
                validateInternal()
            }
            is CouponUiAction.NextClick -> {
                handleNextClickInternal()
            }
        }
    }

    fun setModelId(modelId: String) {
        _uiState.update { state ->
            state.copy(
                modelId = modelId
            )
        }
    }

    fun toggleSelection(planId: Int): Boolean {
        val prevPlanId = uiState.value.selectedPlanId
        if (prevPlanId == planId) {
            return false
        }

        _uiState.update { state ->
            state.copy(
                selectedPlanId = planId
            )
        }

        selectedPlanId = planId
        // Signals the flow
        selectedToggleFlow.update { selectedToggleFlow.value.not() }
        return true
    }

    private fun validateInternal() {
        val typedCoupon = uiState.value.typedCoupon

        if (typedCoupon.length <= COUPON_CODE_VALIDATE_THRESHOLD_LENGTH) {
            val cause = ValidateException("Coupon code length is invalid")
            _uiState.update { state ->
                state.copy(
                    exception = ResolvableException(cause),
                    uiErrorMessage = UiText.DynamicString("Enter a valid coupon code"),
                    couponValidationResult = ValidationResult(
                        typedValue = typedCoupon,
                        successful = false,
                        errorMessage = UiText.DynamicString("Enter a valid coupon code.")
                    )
                )
            }
            return
        }

        val request = VerifyCouponRequest(
            couponCode = typedCoupon
        )
        validateCouponCode(request)
    }

    private fun validateCouponCode(request: VerifyCouponRequest) {
        if (couponValidationJob?.isActive == true) {
            couponValidationJob?.cancel(CancellationException("New request"))
        }

        couponValidationJob = viewModelScope.launch {
            homeRepository.verifyCoupon(request).collectLatest { result ->
                Timber.d((result as? Result.Error)?.exception, "Result: $result")
                when (result) {
                    is Result.Loading -> {
                        setLoadingInternal(LoadType.REFRESH, LoadState.Loading())
                    }
                    is Result.Error -> {
                        when (result.exception) {
                            is ApiException -> {
                                when (result.exception.cause) {
                                    is UnAuthorizedException -> { /* Noop */ }
                                    is InvalidCouponCodeException -> {
                                        ValidationResult(
                                            typedValue = request.couponCode,
                                            successful = false,
                                            errorMessage = UiText.DynamicString("Invalid coupon code!")
                                        ).also { _result ->
                                            _uiState.update { state ->
                                                state.copy(
                                                    couponValidationResult = _result
                                                )
                                            }
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
                            else -> {
                                _uiState.update { state ->
                                    state.copy(
                                        exception = result.exception,
                                        uiErrorMessage = UiText.somethingWentWrong
                                    )
                                }
                            }
                        }
                        setLoadingInternal(LoadType.REFRESH, LoadState.Error(result.exception))
                    }
                    is Result.Success -> {
                        setLoadingInternal(LoadType.REFRESH, LoadState.NotLoading.Complete)

                        val uiModelList = result.data.plans.map { SubscriptionUiModel.Plan(subscriptionPlan = it) }
                        selectedPlanId = uiModelList.firstOrNull()?.subscriptionPlan?.id ?: -1

                        ValidationResult(
                            typedValue = request.couponCode,
                            successful = true
                        ).also { _result ->
                            _uiState.update { state ->
                                state.copy(
                                    couponValidationResult = _result,
                                    couponPlansCache = result.data.plans,
                                    uiModelList = uiModelList,
                                    selectedPlanId = selectedPlanId
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun handleNextClickInternal() {
        val (selectedPlanId, couponCode) = uiState.value.selectedPlanId to
                uiState.value.typedCoupon
        val modelId = uiState.value.modelId.nullAsEmpty()

        val request = SubscriptionPurchaseRequest(
            id = selectedPlanId.toString(),
            modelId = modelId,
            couponCode = couponCode
        )
        purchasePlan(request)
    }

    private fun purchasePlan(request: SubscriptionPurchaseRequest) {
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
                Timber.d("Result: subscription/purchase $result")
                when (result) {
                    is Result.Loading -> setLoadingInternal(LoadType.ACTION, LoadState.Loading())
                    is Result.Error -> {
                        when (result.exception) {
                            is UnAuthorizedException -> { /* Noop */ }
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
                        sendEvent(CouponUiEvent.PurchaseComplete(request.id, result.data.avatarStatusId))
                    }
                }
            }
        }
    }

    private fun resetValidationInternal() {
        _uiState.update { state ->
            state.copy(
                couponValidationResult = null,
                uiModelList = emptyList(),
                selectedPlanId = -1
            )
        }
    }

    private fun setLoadingInternal(
        loadType: LoadType,
        loadState: LoadState
    ) {
        val newLoadState = uiState.value.loadState.modifyState(loadType, loadState)
        _uiState.update { state -> state.copy(loadState = newLoadState) }
    }

    private fun sendEvent(newEvent: CouponUiEvent) = viewModelScope.launch {
        _uiEvent.emit(newEvent)
    }

}

data class CouponState(
    val loadState: LoadStates = LoadStates.IDLE,
    val modelId: String? = null,
    val couponPlansCache: List<SubscriptionPlan>? = null,
    val uiModelList: List<SubscriptionUiModel> = emptyList(),
    val typedCoupon: String = DEFAULT_COUPON_VALUE,
    val couponValidationResult: ValidationResult? = null,
    val selectedPlanId: Int = -1,
    val exception: Exception? = null,
    val uiErrorMessage: UiText? = null
)

interface CouponUiAction {
    data class ErrorShown(val e: Exception) : CouponUiAction
    data class TypingCoupon(val typed: String) : CouponUiAction
    object NextClick : CouponUiAction
    object Validate : CouponUiAction
}

interface CouponUiEvent {
    data class ShowToast(val message: UiText) : CouponUiEvent
    data class PurchaseComplete(val planId: String, val statusId: String) : CouponUiEvent
    object NextScreen : CouponUiEvent
}

private const val DEFAULT_POSITION = 0
private const val DEFAULT_COUPON_VALUE = ""
private const val COUPON_CODE_VALIDATE_THRESHOLD_LENGTH = 4

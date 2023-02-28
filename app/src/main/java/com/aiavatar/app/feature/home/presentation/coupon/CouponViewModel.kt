package com.aiavatar.app.feature.home.presentation.coupon

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import com.aiavatar.app.core.util.ValidateException
import com.aiavatar.app.feature.home.domain.model.SubscriptionPlan
import com.aiavatar.app.feature.home.domain.model.request.VerifyCouponRequest
import com.aiavatar.app.feature.home.domain.repository.HomeRepository
import com.aiavatar.app.feature.home.presentation.subscription.SubscriptionUiModel
import com.aiavatar.app.feature.home.presentation.util.InvalidCouponCodeException
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
                    _uiState.update { state ->
                        state.copy(
                            couponValidationResult = null
                        )
                    }
                }
                if (typedCoupon.length <= COUPON_CODE_VALIDATE_THRESHOLD_LENGTH) {
                    _uiState.update { state ->
                        state.copy(
                            couponValidationResult = null
                        )
                    }
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
        }
    }

    fun toggleSelection(planId: Int) {
        val prevPlanId = uiState.value.selectedPlanId
        if (prevPlanId == planId) {
            return
        }

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                _uiState.updateAndGet { state ->
                    state.copy(selectedPlanId = planId)
                }
            }
            selectedPlanId = planId
            // Signals the flow
            selectedToggleFlow.update { selectedToggleFlow.value.not() }
        }
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
        // TODO: validate coupon code
        if (couponValidationJob?.isActive == true) {
            couponValidationJob?.cancel(CancellationException("New request"))
        }

        couponValidationJob = viewModelScope.launch {
            homeRepository.verifyCoupon(request).collectLatest { result ->
                Timber.d((result as? Result.Error)?.exception, "Result: $result")
                when (result) {
                    is Result.Loading -> {
                        setLoading(LoadType.REFRESH, LoadState.Loading())
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
                        setLoading(LoadType.REFRESH, LoadState.Error(result.exception))
                    }
                    is Result.Success -> {
                        setLoading(LoadType.REFRESH, LoadState.NotLoading.Complete)

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

    private fun setLoading(
        loadType: LoadType,
        loadState: LoadState
    ) {
        val newLoadState = uiState.value.loadState.modifyState(loadType, loadState)
        _uiState.update { state -> state.copy(loadState = newLoadState) }
    }

}

data class CouponState(
    val loadState: LoadStates = LoadStates.IDLE,
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
    object NextScreen : CouponUiEvent
}

private const val DEFAULT_POSITION = 0
private const val DEFAULT_COUPON_VALUE = ""
private const val COUPON_CODE_VALIDATE_THRESHOLD_LENGTH = 4

package com.aiavatar.app.feature.home.presentation.coupon

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiavatar.app.commons.util.UiText
import com.aiavatar.app.commons.util.ValidationResult
import com.aiavatar.app.commons.util.loadstate.LoadStates
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
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
    private val savedStateHandle: SavedStateHandle,
): ViewModel() {

    private val _uiState = MutableStateFlow<CouponState>(CouponState())
    val uiState: StateFlow<CouponState> = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<CouponUiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    val accept: (CouponUiAction) -> Unit

    private val continuousActions = MutableSharedFlow<CouponUiAction>()

    private var couponValidationJob: Job? = null

    init {
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
                if (typedCoupon.isNotBlank()) {
                    validateCouponCode(typedCoupon)
                } else {
                    _uiState.update { state ->
                        state.copy(
                            couponValidationResult = null
                        )
                    }
                }
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
        }
    }

    private fun validateCouponCode(couponCode: String) {
        // TODO: validate coupon code
        if (couponValidationJob?.isActive == true) {
            couponValidationJob?.cancel(CancellationException("New request"))
        }

        couponValidationJob = viewModelScope.launch {
            delay(1000)

            if (couponCode == "AVATAR10") {
                ValidationResult(
                    typedValue = couponCode,
                    successful = true
                ).also { result ->
                    _uiState.update { state ->
                        state.copy(
                            couponValidationResult = result
                        )
                    }
                }
            } else {
                // TODO: validate otherwise
                ValidationResult(
                    typedValue = couponCode,
                    successful = false,
                    errorMessage = UiText.DynamicString("Invalid coupon code!")
                ).also { result ->
                    _uiState.update { state ->
                        state.copy(
                            couponValidationResult = result
                        )
                    }
                }
            }

        }
    }

}

data class CouponState(
    val loadState: LoadStates = LoadStates.IDLE,
    val typedCoupon: String = DEFAULT_COUPON_VALUE,
    val couponValidationResult: ValidationResult? = null,
    val exception: Exception? = null,
    val uiErrorMessage: UiText? = null
)

interface CouponUiAction {
    data class ErrorShown(val e: Exception) : CouponUiAction
    data class TypingCoupon(val typed: String) : CouponUiAction
    object NextClick : CouponUiAction
}

private const val DEFAULT_COUPON_VALUE = ""

interface CouponUiEvent {
    data class ShowToast(val message: UiText) : CouponUiEvent
    object NextScreen : CouponUiEvent
}
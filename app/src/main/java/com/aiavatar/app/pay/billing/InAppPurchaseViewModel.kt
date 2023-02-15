package com.aiavatar.app.pay.billing

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiavatar.app.commons.util.UiText
import com.aiavatar.app.commons.util.loadstate.LoadState
import com.aiavatar.app.commons.util.loadstate.LoadStates
import com.aiavatar.app.commons.util.loadstate.LoadType
import com.aiavatar.app.feature.home.domain.model.request.SubscriptionLogRequest
import com.aiavatar.app.feature.home.domain.repository.HomeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InAppPurchaseViewModel @Inject constructor(
    private val homeRepository: HomeRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<InAppPurchaseState>(InAppPurchaseState())
    val uiState: StateFlow<InAppPurchaseState> = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<InAppPurchaseUiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    val accept: (InAppPurchaseUiAction) -> Unit

    init {
        accept = { uiAction -> onUiAction(uiAction) }
    }

    private fun onUiAction(uiAction: InAppPurchaseUiAction) {
        when (uiAction) {
            is InAppPurchaseUiAction.ErrorShown -> {
                _uiState.update { state ->
                    state.copy(
                        exception = null,
                        uiErrorText = null
                    )
                }
            }
        }
    }

    fun updatePaymentLog(
        transactionId: String,
        paymentStatus: String,
        purchaseToken: String = ""
    ) {
        val request = SubscriptionLogRequest(
            transactionId = transactionId,
            purchaseToken = purchaseToken,
            paymentStatus = paymentStatus
        )
        homeRepository.subscriptionLog(request)
            .launchIn(viewModelScope)
    }

    fun setProductSku(sku: String) {
        _uiState.update { state -> state.copy(productSku = sku) }
    }

    fun setPaymentSequence(sequence: PaymentSequence) {
        _uiState.update { state -> state.copy(paymentSequence = sequence) }
    }

    private fun setLoadState(
        loadType: LoadType,
        loadState: LoadState
    ) {
        val newLoadState = uiState.value.loadState.modifyState(loadType, loadState)
        _uiState.update { state -> state.copy(loadState = newLoadState) }
    }

    private fun sendEvent(newEvent: InAppPurchaseUiEvent) = viewModelScope.launch {
        _uiEvent.emit(newEvent)
    }

}

data class InAppPurchaseState(
    val loadState: LoadStates = LoadStates.IDLE,
    val productSku: String? = null,
    val paymentSequence: PaymentSequence = PaymentSequence.Default,
    val description: UiText? = null,
    val exception: Exception? = null,
    val uiErrorText: UiText? = null
)

interface InAppPurchaseUiAction {
    data class ErrorShown(val e: Exception) : InAppPurchaseUiAction
}

interface InAppPurchaseUiEvent {
    data class ShowToast(val message: UiText) : InAppPurchaseUiEvent
}

enum class PaymentSequence(val sequenceNumber: Int) {
    UNKNOWN(-1), CONNECTING_BILLING(0), BILLING_CONNECTED(1),
    BILLING_CONNECTION_FAILED(2), VALIDATING_PRODUCT_SKU(3),
    CONTACTING_STORE(4), PRODUCT_PRESENTED(5),
    PAYMENT_PROCESSING(6), PAYMENT_FAILED(7),
    CONSUMING_PRODUCT(8), CONSUME_FAILED(9),
    PURCHASE_VALIDATION_PENDING(10);

    companion object  {
        internal val Default = PaymentSequence.UNKNOWN
    }
}
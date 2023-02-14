package com.aiavatar.app.pay.billing

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.aiavatar.app.BuildConfig
import com.aiavatar.app.databinding.ActivityInappPurchaseBinding
import com.aiavatar.app.debugToast
import com.aiavatar.app.ifNull
import com.aiavatar.app.showToast
import com.android.billingclient.api.*
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.Purchase.PurchaseState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

@AndroidEntryPoint
class InAppPurchaseActivity : AppCompatActivity() {

    private val viewModel: InAppPurchaseViewModel by viewModels()

    private val purchaseUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        Timber.d("purchaseUpdatedListener: ${billingResult.responseCode} msg = ${billingResult.debugMessage}")
        when (billingResult.responseCode) {
            BillingResponseCode.OK -> {
                Timber.d("purchaseUpdatedListener: purchases = $purchases")
                if (purchases != null) {
                    // TODO: save and send the purchase details to server
                    viewModel.setPaymentSequence(PaymentSequence.PAYMENT_PROCESSING)
                    purchases.firstOrNull()?.let { purchase ->
                        when (purchase.purchaseState) {
                            PurchaseState.PURCHASED -> {
                                handlePurchase(purchase)
                            }
                            PurchaseState.PENDING -> {

                            }
                            PurchaseState.UNSPECIFIED_STATE -> {

                            }
                        }
                    }.ifNull {
                        val msg = "Purchase failed. "
                        setResultInternal(
                            code = ResultCode.FAILED,
                            errorMessage = msg,
                            debugMessage = msg + " ${billingResult.responseCode}: Payment done. But no purchase returned."
                        )
                        finish()
                    }
                } else {
                    viewModel.setPaymentSequence(PaymentSequence.PAYMENT_FAILED)
                    val msg = "Purchase failed. "
                    setResultInternal(
                        code = ResultCode.FAILED,
                        errorMessage = msg,
                        debugMessage = msg + " ${billingResult.responseCode}: Payment done. But no purchase returned."
                    )
                    finish()
                }
            }
            BillingResponseCode.DEVELOPER_ERROR -> {
                viewModel.setPaymentSequence(PaymentSequence.PAYMENT_FAILED)
                debugToast("Purchase failed")
                val msg = "Purchase failed. "
                setResultInternal(
                    code = ResultCode.FAILED,
                    errorMessage = msg,
                    debugMessage = msg + " ${billingResult.responseCode}: Developer error"
                )
                finish()
            }
            else -> {
                // TODO: handle distinguished error
                viewModel.setPaymentSequence(PaymentSequence.PAYMENT_FAILED)
            }
        }
    }

    private val billingClient by lazy {
        BillingClient.newBuilder(this@InAppPurchaseActivity)
            .enablePendingPurchases()
            .setListener(purchaseUpdatedListener)
            .build()
    }

    // Helper flags
    private var isBillingClientConnected: Boolean = false
    private var billingConnectionRetryCount: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkSecureMode()
        val binding = ActivityInappPurchaseBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.bindState(
            uiState = viewModel.uiState,
            uiAction = viewModel.accept,
            uiEvent = viewModel.uiEvent
        )

        parseIntentInternal(intent)
        val msg = "User canceled the payment."
        setResultInternal(ResultCode.USER_CANCELED, errorMessage = msg, debugMessage = msg + " Untouched.")
    }

    private fun ActivityInappPurchaseBinding.bindState(
        uiState: StateFlow<InAppPurchaseState>,
        uiAction: (InAppPurchaseUiAction) -> Unit,
        uiEvent: SharedFlow<InAppPurchaseUiEvent>
    ) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                uiEvent.collectLatest { event ->
                    when (event) {
                        is InAppPurchaseUiEvent.ShowToast -> {
                            showToast(event.message.asString(context = this@InAppPurchaseActivity))
                        }
                    }
                }
            }
        }

        val paymentSequenceFlow = uiState.map { it.paymentSequence }
            .distinctUntilChanged()
        lifecycleScope.launch {
            paymentSequenceFlow.collectLatest { paymentSequence ->
                Timber.d("Payment sequence: $paymentSequence")
                when (paymentSequence) {
                    PaymentSequence.UNKNOWN -> { /* Noop */ }
                    PaymentSequence.CONNECTING_BILLING -> { /* Noop */ }
                    PaymentSequence.BILLING_CONNECTED -> {
                        validateProductSku(uiState.value.productSku!!)
                    }
                    PaymentSequence.BILLING_CONNECTION_FAILED -> {
                        if (billingConnectionRetryCount < MAX_BILLING_CLIENT_CONNECTION_ATTEMPTS) {
                            initBillingClientIfRequired()
                        } else {
                            val msg = "Failed to initiate purchase. "
                            setResultInternal(
                                ResultCode.ERROR,
                                errorMessage = msg,
                                debugMessage = "Failed to initialize billing client."
                            )
                        }
                    }
                    PaymentSequence.VALIDATING_PRODUCT_SKU -> {
                        /* Noop */
                    }
                    PaymentSequence.CONTACTING_STORE -> {
                        /* Noop */
                    }
                    PaymentSequence.PRODUCT_PRESENTED -> {

                    }
                    PaymentSequence.PAYMENT_PROCESSING -> {
                        /* Noop */
                    }
                    PaymentSequence.PAYMENT_FAILED -> {
                        debugToast("Purchase failed")
                        finish()
                    }
                    PaymentSequence.CONSUMING_PRODUCT -> {

                    }
                    PaymentSequence.CONSUME_FAILED -> {

                    }
                    PaymentSequence.PURCHASE_VALIDATION_PENDING -> {

                    }
                }

            }
        }

        bindToolbar(
            uiState = uiState
        )
    }

    private fun ActivityInappPurchaseBinding.bindToolbar(uiState: StateFlow<InAppPurchaseState>) {
        toolbarIncluded.apply {
            toolbarNavigationIcon.isVisible = false
            toolbarTitle.text = "Complete purchase"
        }
    }

    private fun parseIntentInternal(newIntent: Intent) {
        val productSku = newIntent.getStringExtra(EXTRA_PRODUCT_SKU)
        if (productSku == null || productSku.isBlank()) {
            val msg = "No product sku. Nothing to do."
            setResultInternal(ResultCode.ERROR, errorMessage = msg, debugMessage = msg)
            finish()
            return
        }

        initBillingClientIfRequired()
        viewModel.setProductSku(productSku)
    }

    private fun validateProductSku(sku: String) {
        Timber.d("Billing: sku $sku")
        viewModel.setPaymentSequence(PaymentSequence.VALIDATING_PRODUCT_SKU)
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                val productList = ArrayList<QueryProductDetailsParams.Product>()
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(sku)
                    .setProductType(BillingClient.ProductType.INAPP)
                    .build().also {
                        productList.add(it)
                    }
                val params = QueryProductDetailsParams.newBuilder()
                    .setProductList(productList)
                    .build()

                viewModel.setPaymentSequence(PaymentSequence.CONTACTING_STORE)
                val productDetailsResult = withContext(Dispatchers.IO) {
                    billingClient.queryProductDetails(params)
                }

                val productDetailList = productDetailsResult.productDetailsList
                if (productDetailList != null && productDetailList.isNotEmpty()) {
                    // TODO: next
                    Timber.d("Billing: product detail list = $productDetailList")
                    val productDetailsParams = listOf(
                        BillingFlowParams.ProductDetailsParams.newBuilder()
                            .setProductDetails(productDetailList.first())
                                // set offer tokens here
                            .build()
                    )

                    val billingFlowParams = BillingFlowParams.newBuilder()
                        .setProductDetailsParamsList(productDetailsParams)
                        .build()

                    val billingResult = billingClient.launchBillingFlow(
                        this@InAppPurchaseActivity,
                        billingFlowParams
                    )

                    Timber.d("billingResult: ${billingResult.responseCode} msg = ${billingResult.debugMessage}")
                    when (billingResult.responseCode) {
                        BillingResponseCode.OK -> {
                            viewModel.setPaymentSequence(PaymentSequence.PRODUCT_PRESENTED)
                        }
                        BillingResponseCode.BILLING_UNAVAILABLE -> {
                            viewModel.setPaymentSequence(PaymentSequence.PAYMENT_FAILED)
                        }
                        BillingResponseCode.USER_CANCELED -> {
                            viewModel.setPaymentSequence(PaymentSequence.PAYMENT_FAILED)
                        }
                        BillingResponseCode.SERVICE_TIMEOUT,
                        BillingResponseCode.SERVICE_DISCONNECTED,
                        BillingResponseCode.SERVICE_UNAVAILABLE -> {
                            // service error
                            viewModel.setPaymentSequence(PaymentSequence.PAYMENT_FAILED)
                        }
                        BillingResponseCode.ERROR -> {
                            // unknown error
                            viewModel.setPaymentSequence(PaymentSequence.PAYMENT_FAILED)
                        }
                        BillingResponseCode.ITEM_ALREADY_OWNED,
                        BillingResponseCode.ITEM_NOT_OWNED,
                        BillingResponseCode.ITEM_UNAVAILABLE -> {
                            // item error
                            viewModel.setPaymentSequence(PaymentSequence.PAYMENT_FAILED)
                        }
                        BillingResponseCode.DEVELOPER_ERROR -> {
                            // internal config error
                            viewModel.setPaymentSequence(PaymentSequence.PAYMENT_FAILED)
                        }
                        BillingResponseCode.FEATURE_NOT_SUPPORTED -> {
                            // payment method not supported
                            viewModel.setPaymentSequence(PaymentSequence.PAYMENT_FAILED)
                        }
                    }

                } else {
                    val msg = "Failed to fetch product details. "
                    setResultInternal(ResultCode.ERROR,
                        errorMessage = msg,
                        debugMessage = msg)
                    finish()
                }
            }
        }
    }

    /* Play Billing */

    private fun initBillingClientIfRequired() {
        if (!isBillingClientConnected) {
            viewModel.setPaymentSequence(PaymentSequence.CONNECTING_BILLING)
            billingClient.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(p0: BillingResult) {
                    isBillingClientConnected = true
                    billingConnectionRetryCount = 0
                    viewModel.setPaymentSequence(PaymentSequence.BILLING_CONNECTED)
                    val msg = "Billing client setup finished"
                    Timber.v(msg)
                    Timber.d("code = ${p0.responseCode} ${p0.debugMessage} ")
                }

                override fun onBillingServiceDisconnected() {
                    isBillingClientConnected = false
                    billingConnectionRetryCount = billingConnectionRetryCount.plus(1)
                    viewModel.setPaymentSequence(PaymentSequence.BILLING_CONNECTION_FAILED)
                    val msg = "Billing client disconnected"
                    val t = IllegalStateException(msg)
                    Timber.w(t)
                }
            })
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        viewModel.setPaymentSequence(PaymentSequence.CONSUMING_PRODUCT)
        val consumeParams = ConsumeParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        billingClient.consumeAsync(consumeParams
        ) { billingResult, purchaseToken ->
            if (billingResult.responseCode == BillingResponseCode.OK) {
                viewModel.setPaymentSequence(PaymentSequence.PURCHASE_VALIDATION_PENDING)
                val extras = Bundle().apply {
                    putString(EXTRA_PURCHASE_TOKEN, purchaseToken)
                }
                setResultInternal(
                    code = ResultCode.SUCCESS,
                    data = extras,
                    message = "Purchase successful",
                    debugMessage = "Purchase successful for purchaseToken = $purchaseToken",
                )
                finish()
            } else {
                viewModel.setPaymentSequence(PaymentSequence.CONSUME_FAILED)
                // TODO: handle consume failed
                val msg = "Purchase failed"
                setResultInternal(
                    code = ResultCode.FAILED,
                    message = "Purchase failed",
                    debugMessage = msg + " for purchaseToken = $purchaseToken"
                )
                finish()
            }
        }
    }
    /* END - Play Billing */

    private fun checkSecureMode() {
        if (BuildConfig.IS_SECURED) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
            )
        }
    }

    private fun setResultInternal(
        code: Int,
        message: String? = null,
        errorMessage: String? = null,
        data: Bundle? = null,
        debugMessage: String,
    ) {
        val extras = Bundle().apply {
            data?.let { putAll(data) }
            putInt(EXTRA_RESULT_CODE, code)
            message?.let { putString(EXTRA_MESSAGE, message) }
            errorMessage?.let { putString(EXTRA_ERROR_MESSAGE, errorMessage) }
            putString(EXTRA_DEBUG_MESSAGE, debugMessage)
        }
        Intent().apply {
            putExtras(extras)
        }.also { intent ->
            setResult(code, intent)
        }
    }

    companion object {
        const val EXTRA_PRODUCT_SKU = "com.aiavatar.app.extras.PRODUCT_SKU"
        const val EXTRA_MESSAGE = "com.aiavatar.app.extras.MESSAGE"
        const val EXTRA_DEBUG_MESSAGE = "com.aiavatar.app.extras.DEBUG_MESSAGE"
        const val EXTRA_ERROR_MESSAGE = "com.aiavatar.app.extras.ERROR_MESSAGE"
        const val EXTRA_RESULT_CODE = "com.aiavatar.app.extras.RESULT_CODE"
        const val EXTRA_PURCHASE_TOKEN = "com.aiavatar.app.extras.PURCHASE_TOKEN"

        const val MAX_BILLING_CLIENT_CONNECTION_ATTEMPTS = 3
    }

}

object ResultCode {
    const val SUCCESS: Int = 0
    const val USER_CANCELED: Int = -1
    const val FAILED: Int = 1
    const val ERROR: Int = 2
}
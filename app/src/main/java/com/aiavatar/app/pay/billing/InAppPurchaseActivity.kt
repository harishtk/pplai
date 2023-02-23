package com.aiavatar.app.pay.billing

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import android.window.OnBackInvokedDispatcher
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.aiavatar.app.*
import com.aiavatar.app.core.data.source.local.entity.PAYMENT_STATUS_CANCELED
import com.aiavatar.app.core.data.source.local.entity.PAYMENT_STATUS_PROCESSING
import com.aiavatar.app.databinding.ActivityInappPurchaseBinding
import com.aiavatar.app.ifNull
import com.android.billingclient.api.*
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClient.ProductType
import com.android.billingclient.api.Purchase.PurchaseState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import okhttp3.internal.immutableListOf
import timber.log.Timber
import java.util.concurrent.CancellationException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * TODO: refactor
 */
@AndroidEntryPoint
class InAppPurchaseActivity : AppCompatActivity() {

    private val WAKE_LOCK_TAG = "aiavtr::payments-wake-lock"

    private val viewModel: InAppPurchaseViewModel by viewModels()

    private var transactionId: String? = null

    private val purchaseUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        Timber.d("purchaseUpdatedListener: ${billingResult.responseCode} msg = ${billingResult.debugMessage}")
        when (billingResult.responseCode) {
            BillingResponseCode.OK -> {
                Timber.d("purchaseUpdatedListener: purchases = $purchases")
                if (purchases != null) {
                    // TODO: save and send the purchase details to server
                    viewModel.setPaymentSequence(PaymentSequence.PAYMENT_PROCESSING)
                    purchases.firstOrNull()?.let { purchase ->
                        Timber.d("purchaseUpdatedListener: state = ${purchase.purchaseState} token = ${purchase.purchaseToken}")
                        when (purchase.purchaseState) {
                            PurchaseState.PURCHASED -> {
                                lifecycleScope.launch { handlePurchase(purchase) }
                            }
                            in setOf(
                                PurchaseState.PENDING,
                                PurchaseState.UNSPECIFIED_STATE
                            ) -> {
                                // TODO: wait for pending purchase to complete
                                val extras = Bundle().apply {
                                    putString(EXTRA_PURCHASE_TOKEN, purchase.purchaseToken)
                                }
                                showToast("Please wait..")
                                val msg = "Validating purchase. Please wait.."
                                setResultInternal(
                                    code = ResultCode.PENDING,
                                    data = extras,
                                    errorMessage = msg,
                                    debugMessage = msg + " ${billingResult.debugMessage}"
                                )
                                finish()
                            }
                            PURCHASE_STATE_FAILED -> {
                                val extras = Bundle().apply {
                                    putString(EXTRA_PURCHASE_TOKEN, purchase.purchaseToken)
                                }
                                val msg = "The payment was unable to complete."
                                setResultInternal(
                                    code = ResultCode.FAILED,
                                    data = extras,
                                    errorMessage = msg,
                                    debugMessage = msg + " Invalid purchase state $PURCHASE_STATE_FAILED"
                                )
                            }
                            else -> {
                                val msg = "Something went wrong."
                                setResultInternal(
                                    code = ResultCode.FAILED,
                                    errorMessage = msg,
                                    debugMessage = msg + " Invalid purchase state"
                                )
                            }
                        }
                    }.ifNull {
                        val msg = "Purchase failed. "
                        setResultInternal(
                            code = ResultCode.FAILED,
                            errorMessage = msg,
                            debugMessage = msg + " ${billingResult.responseCode}: Payment done. But no purchase returned."
                        )
                        viewModel.setPaymentSequence(PaymentSequence.PAYMENT_FAILED)
                    }
                } else {
                    val msg = "Purchase failed. "
                    setResultInternal(
                        code = ResultCode.FAILED,
                        errorMessage = msg,
                        debugMessage = msg + " ${billingResult.responseCode}: Payment done. But no purchase returned."
                    )
                    viewModel.setPaymentSequence(PaymentSequence.PAYMENT_FAILED)
                }
            }
            BillingResponseCode.ITEM_ALREADY_OWNED,
            BillingResponseCode.ITEM_NOT_OWNED,
            BillingResponseCode.ITEM_UNAVAILABLE -> {
                // item error
                viewModel.setPaymentSequence(PaymentSequence.PAYMENT_FAILED)
            }
            BillingResponseCode.DEVELOPER_ERROR -> {
                debugToast("Purchase failed")
                val msg = "Purchase failed. "
                setResultInternal(
                    code = ResultCode.FAILED,
                    errorMessage = msg,
                    debugMessage = msg + " ${billingResult.responseCode}: Developer error"
                )
                viewModel.setPaymentSequence(PaymentSequence.PAYMENT_FAILED)
            }
            BillingResponseCode.ERROR -> {
                val msg = "Purchase failed. "
                setResultInternal(
                    code = ResultCode.FAILED,
                    errorMessage = msg,
                    debugMessage = msg + " ${billingResult.responseCode}: ${billingResult.debugMessage}"
                )
                viewModel.setPaymentSequence(PaymentSequence.PAYMENT_FAILED)
            }
            BillingResponseCode.USER_CANCELED -> {
                val msg = "Payment canceled"
                setResultInternal(
                    code = ResultCode.USER_CANCELED,
                    errorMessage = msg,
                    debugMessage = msg + " ${billingResult.responseCode}: User ignored the payment flow."
                )
                viewModel.setPaymentSequence(PaymentSequence.PAYMENT_FAILED)
            }
            else -> {
                // TODO: handle distinguished error
                val msg = "Purchase failed."
                setResultInternal(
                    code = ResultCode.FAILED,
                    errorMessage = msg,
                    debugMessage = msg + " ${billingResult.responseCode}"
                )
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
    private var billingConnectionRetries: Int = 0

    private var pendingPurchaseFetchJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkSecureMode()
        val binding = ActivityInappPurchaseBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        binding.bindState(
            uiState = viewModel.uiState,
            uiAction = viewModel.accept,
            uiEvent = viewModel.uiEvent
        )

        parseIntentInternal(intent)
        val msg = "User canceled the payment."
        setResultInternal(ResultCode.USER_CANCELED, errorMessage = msg, debugMessage = msg + " Untouched.")
        setupOnBackPressedDispatcher()
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
                        if (billingConnectionRetries < MAX_BILLING_CLIENT_CONNECTION_ATTEMPTS) {
                            initBillingClientIfRequired()
                        } else {
                            val msg = "Failed to initiate purchase. "
                            setResultInternal(
                                ResultCode.ERROR,
                                errorMessage = msg,
                                debugMessage = "Failed to initialize billing client. Service unavailable"
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
        val transactionId = newIntent.getStringExtra(EXTRA_TRANSACTION_ID)
        if (productSku == null || productSku.isBlank()) {
            val msg = "No product sku. Nothing to do."
            setResultInternal(ResultCode.ERROR, errorMessage = msg, debugMessage = msg)
            finish()
            return
        }

        this.transactionId = transactionId
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
                            transactionId?.let { txnId ->
                                viewModel.updatePaymentLog(
                                    transactionId = txnId,
                                    paymentStatus = PAYMENT_STATUS_PROCESSING
                                )
                            }
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
                            // This causes the billing client not to retry
                            billingConnectionRetries = MAX_BILLING_CLIENT_CONNECTION_ATTEMPTS
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
                    billingConnectionRetries = 0
                    viewModel.setPaymentSequence(PaymentSequence.BILLING_CONNECTED)
                    val msg = "Billing client setup finished"
                    Timber.v(msg)
                    Timber.d("code = ${p0.responseCode} ${p0.debugMessage} ")
                }

                override fun onBillingServiceDisconnected() {
                    isBillingClientConnected = false
                    billingConnectionRetries++
                    viewModel.setPaymentSequence(PaymentSequence.BILLING_CONNECTION_FAILED)
                    val msg = "Billing client disconnected"
                    val t = IllegalStateException(msg)
                    Timber.w(t)
                }
            })
        }
    }

    private suspend fun handlePurchase(purchase: Purchase) {
        viewModel.setPaymentSequence(PaymentSequence.CONSUMING_PRODUCT)

        withContext(Dispatchers.IO) {
            val initialDelay = 0L
            val retries = 3
            val retryFactor = 2

            InAppUtil.consume(billingClient, purchase.purchaseToken).let { consumeResult ->
                val billingResult = consumeResult.billingResult
                val purchaseToken = consumeResult.purchaseToken

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
                    val extras = Bundle().apply {
                        putString(EXTRA_PURCHASE_TOKEN, purchaseToken)
                    }
                    val msg = "Purchase failed"
                    setResultInternal(
                        code = ResultCode.FAILED,
                        data = extras,
                        message = "Purchase failed",
                        debugMessage = msg + " for purchaseToken = $purchaseToken"
                    )
                    finish()
                }
            }
        }
    }

    private fun checkPendingPurchases() {
        if (pendingPurchaseFetchJob?.isActive == true) {
            val t = IllegalStateException("A pending purchase fetch job is already active. Ignoring request.")
            ifDebug { Timber.e(t) }
            return
        }
        pendingPurchaseFetchJob?.cancel(CancellationException("New request")) // just in case
        pendingPurchaseFetchJob = lifecycleScope.launch(Dispatchers.IO) {
            kotlin.runCatching { billingClient.queryPurchases(ProductType.INAPP) }
                .onSuccess { purchases ->
                    val _p = purchases.map {
                        immutableListOf(it.products.joinToString(), it.purchaseState, it.signature)
                    }
                        .joinToString()
                    Timber.d("Purchases: $_p")

                    // TODO: handle pending purchase
                }
                .onFailure { t ->
                    Timber.e(t)
                }
            // checkPurchaseHistory()
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

    /* Util */
    private suspend fun <T> exponentialRetry(
        maxTries: Int = Int.MAX_VALUE,
        initialDelay: Long = Long.MAX_VALUE,
        retryFactor: Int = Int.MAX_VALUE,
        block: suspend () -> T
    ): T? {
        var currentDelay = initialDelay
        var retryAttempt = 1
        do {
            kotlin.runCatching {
                delay(currentDelay)
                block()
            }
                .onSuccess {
                    return@onSuccess;
                }
                .onFailure { t ->
                    ifDebug { Timber.e(t, "Retry Failed") }
                }
            currentDelay *= retryFactor
            retryAttempt++
        } while (retryAttempt < maxTries)

        return block()
    }
    /* END - Util */

    private fun setupOnBackPressedDispatcher() {
        if (VersionCompat.isAtLeastT) {
            onBackInvokedDispatcher.registerOnBackInvokedCallback(OnBackInvokedDispatcher.PRIORITY_DEFAULT) {
                // on back pressed
                handleBackPressed()
            }
        } else {
            onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    // on back pressed
                    handleBackPressed()
                }
            })
        }
    }

    private fun handleBackPressed(): Boolean {
        showToast("Please wait while the payment is processing")
        return true
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

    override fun onResume() {
        super.onResume()

        if (isBillingClientConnected) {
            checkPendingPurchases()
        }
    }

    companion object {
        const val EXTRA_PRODUCT_SKU = "com.aiavatar.app.extras.PRODUCT_SKU"
        const val EXTRA_TRANSACTION_ID = "com.aiavatar.app.extras.TRANSACTION_ID"
        const val EXTRA_MESSAGE = "com.aiavatar.app.extras.MESSAGE"
        const val EXTRA_DEBUG_MESSAGE = "com.aiavatar.app.extras.DEBUG_MESSAGE"
        const val EXTRA_ERROR_MESSAGE = "com.aiavatar.app.extras.ERROR_MESSAGE"
        const val EXTRA_RESULT_CODE = "com.aiavatar.app.extras.RESULT_CODE"
        const val EXTRA_PURCHASE_TOKEN = "com.aiavatar.app.extras.PURCHASE_TOKEN"

        const val MAX_BILLING_CLIENT_CONNECTION_ATTEMPTS = 3
        const val PURCHASE_STATE_FAILED = 4

        val retriableBillingResponses = setOf(
            BillingResponseCode.SERVICE_TIMEOUT,
            BillingResponseCode.SERVICE_DISCONNECTED,
            BillingResponseCode.SERVICE_UNAVAILABLE,
            BillingResponseCode.BILLING_UNAVAILABLE,
            BillingResponseCode.ERROR,
            BillingResponseCode.ITEM_ALREADY_OWNED,
            BillingResponseCode.ITEM_NOT_OWNED
        )

        val nonRetriableBillingResponses = setOf(
            BillingResponseCode.FEATURE_NOT_SUPPORTED,
            BillingResponseCode.USER_CANCELED,
            BillingResponseCode.ITEM_UNAVAILABLE,
            BillingResponseCode.DEVELOPER_ERROR
        )
    }

}

object ResultCode {
    const val SUCCESS: Int = 0
    const val USER_CANCELED: Int = -1
    const val FAILED: Int = 1
    const val ERROR: Int = 2
    const val PENDING: Int = 3
}
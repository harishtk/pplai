/*
package com.aiavatar.app.pay.billing

import android.app.Activity
import androidx.lifecycle.LifecycleCoroutineScope
import com.android.billingclient.api.*
import com.google.common.collect.ImmutableList
import com.pepul.socialnetworking.model.ConsumeRequestData
import com.pepul.socialnetworking.model.payment.PaymentLogDataModel
import com.pepul.socialnetworking.mvvm.local.session.SessionPref
import com.pepul.socialnetworking.mvvm.viewmodel.RewardViewModel
import com.pepul.socialnetworking.pepul_creators.model.LoggingEventDataModel
import com.pepul.socialnetworking.pepul_creators.ui.web_engage.mySubscription.PaymentWebEngageImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

class InAppBillingHelper private constructor(
    private val activity: Activity,
    private val scope: LifecycleCoroutineScope,
    private val rewardViewModel: RewardViewModel,
    private val sessionPref: SessionPref,
    private val callback: InAppBillingListener
) {

    private lateinit var billingClient: BillingClient

    private var purchaseId: Int = -1
    private var title: String = ""
    private var storeId: String = ""
    private var promotionToken: String = ""
    private var creatorId: String = "0"

    private lateinit var paymentWebEngageImpl: PaymentWebEngageImpl

    companion object {
        fun newBuilder(activity: Activity, scope: LifecycleCoroutineScope, rewardViewModel: RewardViewModel, sessionPref: SessionPref, callback: InAppBillingListener) =
            InAppBillingHelper(activity, scope, rewardViewModel, sessionPref, callback)
    }

    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        try {
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {

                // Purchase completed.
                for (purchase in purchases) {
                    scope.launch {
                        handlePurchase(purchase)
                    }
                }
            } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
                // Handle an error caused by a user cancelling the purchase flow.

                handleUserCancelledPurchase()
            } else {
                // Handle any other error codes.

                val errorEvent = when (billingResult.responseCode) {
                    -3 -> "SERVICE_TIMEOUT"
                    -2 -> "FEATURE_NOT_SUPPORTED"
                    -1 -> "SERVICE_DISCONNECTED"
                    1 -> "USER_CANCELED"
                    2 -> "SERVICE_UNAVAILABLE"
                    3 -> "BILLING_UNAVAILABLE"
                    4 -> "ITEM_UNAVAILABLE"
                    5 -> "DEVELOPER_ERROR"
                    6 -> "ERROR"
                    7 -> "ITEM_ALREADY_OWNED"
                    else -> "ITEM_NOT_OWNED"
                }

                unknownPurchaseError(errorEvent)
            }
        } catch (e: Exception) {
            Timber.e(e)
            e.printStackTrace()
        }
    }

    fun enableBilling() {

        paymentWebEngageImpl = PaymentWebEngageImpl()

        //        start in app billing
        billingClient = BillingClient.newBuilder(activity)
            .setListener(purchasesUpdatedListener)
            .enablePendingPurchases()
            .build()

        rewardViewModel.logEvent(LoggingEventDataModel(logEvent = "BILLING_EVENT_ENABLE_BILLING", logContent = "Billing Enabled"))
    }


    fun startBilling(purchaseId: Int, title: String, storeId: String, promotionToken: String, creatorId: String) {
        rewardViewModel.logEvent(LoggingEventDataModel(logEvent = "BILLING_EVENT_ON_START", logContent = "Start Billing"))

        this@InAppBillingHelper.purchaseId = purchaseId
        this@InAppBillingHelper.title = title
        this@InAppBillingHelper.storeId = storeId
        this@InAppBillingHelper.promotionToken = promotionToken
        this@InAppBillingHelper.creatorId = creatorId

        try {
            billingClient.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        // The BillingClient is ready. You can query purchases here.
                        rewardViewModel.logEvent(LoggingEventDataModel(logEvent = "BILLING_EVENT_CONNECT", logContent = "Connection Established"))
                        processPayment(storeId)
                    }
                }

                override fun onBillingServiceDisconnected() {
                    rewardViewModel.logEvent(LoggingEventDataModel(logEvent = "BILLING_EVENT_DISCONNECTED", logContent = "Connection Disconnected"))
                }
            })
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    private fun processPayment(storeId: String) {
        rewardViewModel.logEvent(LoggingEventDataModel(logEvent = "BILLING_EVENT_ON_PROCESS_PAYMENT", logContent = "processPayment storeId: $storeId"))

//         process payment purchase
        val queryProductDetailsParams = QueryProductDetailsParams.newBuilder()
            .setProductList(
                ImmutableList.of(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(storeId)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
                )
            )
            .build()

        val productDetailsParamsList = mutableListOf<BillingFlowParams.ProductDetailsParams>()

        billingClient.queryProductDetailsAsync(queryProductDetailsParams) { billingResult, productDetailsList ->
            // check billingResult
            // process returned productDetailsList

            productDetailsList.forEach {
                val product = BillingFlowParams.ProductDetailsParams.newBuilder().apply {
                    // retrieve a value for "productDetails" by calling queryProductDetailsAsync()
                    setProductDetails(it)
                }.build()

                productDetailsParamsList.add(product)

                val billingFlowParams = BillingFlowParams.newBuilder()
                    .setProductDetailsParamsList(productDetailsParamsList)
                    .setObfuscatedAccountId(sessionPref.getprofileInfodata()?.get(sessionPref.userId)!!.toString())
                    .setObfuscatedProfileId(sessionPref.getprofileInfodata()?.get(sessionPref.userId)!!.toString())
                    .build()

                billingClient.launchBillingFlow(activity, billingFlowParams)
            }
        }
    }


    private fun handlePurchase(purchase: Purchase) {
        rewardViewModel.logEvent(LoggingEventDataModel(logEvent = "BILLING_EVENT_HANDLE_PURCHASE", logContent = "handlePurchase token: ${purchase.purchaseToken}"))

        try {
            onPaymentCompleted(purchase = purchase)
            scope.launch(Dispatchers.IO) {
                rewardViewModel.logPurchase(PaymentLogDataModel(purchaseId = storeId, purchaseToken = purchase.purchaseToken))
            }

            val consumeParams = ConsumeParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()

            scope.launch(Dispatchers.IO) {

                val consumeResult = billingClient.consumePurchase(params = consumeParams)

                if (consumeResult.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    onConsumeSuccess(purchase)
                } else {
                    onConsumeFailed(purchase)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    private fun onPaymentCompleted(purchase: Purchase) {
        // payment is completed but the consume is not not yet completed
        rewardViewModel.logEvent(LoggingEventDataModel(logEvent = "BILLING_EVENT_ON_PAYMENT_COMPLETED", logContent = "onPaymentCompleted: ${purchase.purchaseToken}"))

        // On consume failed update the room db and set the status as the false
        scope.launch(Dispatchers.IO) {
            rewardViewModel.updatePaymentLog(purchase.purchaseToken, storeId, false, creatorId)
        }

        callback.onBillingProcessing()
    }

    private fun onConsumeSuccess(purchase: Purchase) {
        rewardViewModel.logEvent(LoggingEventDataModel(logEvent = "BILLING_EVENT_ON_CONSUME_SUCCESS", logContent = "onConsumeSuccess: ${purchase.purchaseToken}"))


        paymentWebEngageImpl.triggerPurchaseSuccess(
            planText = title, gender = sessionPref.getprofileInfodata()!![sessionPref.gender] ?: "",
            userName = sessionPref.getprofileInfodata()!![sessionPref.userName] ?: "",
            userId = sessionPref.getprofileInfodata()!![sessionPref.webengageId] ?: ""
        )

//            On success purchase update the pepul server
        rewardViewModel.updatePurchaseIdToken(
            ConsumeRequestData(
                purchaseId = purchaseId.toString(),
                packageName = activity.packageName,
                token = purchase.purchaseToken,
                creatorId = creatorId
            )
        )


        scope.launch(Dispatchers.IO) {

//            after the post insert the log in the database
            rewardViewModel.updatePaymentLog(
                token = purchase.purchaseToken,
                purchaseId = purchaseId.toString(),
                status = true,
                creatorId = creatorId
            )
        }

//        Finally call the interface success call
        callback.onBillingCompleted(creatorId = creatorId)
    }

    fun onConsumeFailed(purchase: Purchase) {
        rewardViewModel.logEvent(LoggingEventDataModel(logEvent = "BILLING_EVENT_ON_CONSUME_FAILED", logContent = "onConsumeFailed: ${purchase.purchaseToken}"))

        paymentWebEngageImpl.triggerPurchaseFailed(
            planText = title, gender = sessionPref.getprofileInfodata()!![sessionPref.gender] ?: "",
            userName = sessionPref.getprofileInfodata()!![sessionPref.userName] ?: "",
            userId = sessionPref.getprofileInfodata()!![sessionPref.webengageId] ?: ""
        )

//        Finally call the interface as the billing failed
        callback.onBillingFailed()
    }

    private fun unknownPurchaseError(errorEvent: String) {

        paymentWebEngageImpl.triggerPaymentFailed(
            userName = sessionPref.getprofileInfodata()!![sessionPref.userName] ?: "",
            userId = sessionPref.getprofileInfodata()!![sessionPref.userId] ?: "",
            planText = title, gender = sessionPref.getprofileInfodata()!![sessionPref.gender] ?: ""
        )

        rewardViewModel.logEvent(LoggingEventDataModel(logEvent = "BILLING_EVENT_ERROR_$errorEvent", logContent = errorEvent))

        // on the billing error call the interface as the billing failed
        callback.onBillingError()
    }

    fun handleUserCancelledPurchase() {

        paymentWebEngageImpl.triggerUserCancelledPayment(
            userName = sessionPref.getprofileInfodata()!![sessionPref.userName] ?: "",
            userId = sessionPref.getprofileInfodata()!![sessionPref.userId] ?: "",
            planText = title, gender = sessionPref.getprofileInfodata()!![sessionPref.gender] ?: ""
        )

        rewardViewModel.logEvent(LoggingEventDataModel(logEvent = "BILLING_EVENT_USER_CANCELLED", logContent = "handleUserCancelledPurchase"))

//        when user rejected the payment call the interface and show he user rejected message
        callback.onUserRejected()
    }

    interface InAppBillingListener {
        fun onBillingCompleted(creatorId: String)
        fun onBillingError()
        fun onBillingFailed()
        fun onUserRejected()
        fun onBillingProcessing()
    }
}*/

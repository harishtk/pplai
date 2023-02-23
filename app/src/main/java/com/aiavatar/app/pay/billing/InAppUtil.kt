package com.aiavatar.app.pay.billing

import com.android.billingclient.api.*
import com.android.billingclient.api.BillingClient.ProductType
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object InAppUtil {

    suspend fun consume(billingClient: BillingClient, purchaseToken: String): ConsumeResult {
        val params = ConsumeParams.newBuilder()
            .setPurchaseToken(purchaseToken)
            .build()

        return billingClient.consumePurchase(params)
    }

    suspend fun queryPurchases(
        billingClient: BillingClient,
        @ProductType productType: String
    ): List<Purchase>
            = suspendCancellableCoroutine { continuation ->
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(productType)
            .build()

        if (!billingClient.isReady) {
            val cause = IllegalStateException("Billing client is not ready.")
            continuation.resumeWithException(cause)
        } else {
            billingClient.queryPurchasesAsync(params
            ) { billingResult, purchases ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    continuation.resume(purchases)
                } else {
                    val cause = IllegalStateException("${billingResult.responseCode} ${billingResult.debugMessage}")
                    continuation.resumeWithException(cause)
                }
            }
        }
    }
}

suspend fun BillingClient.queryPurchases(@ProductType productType: String): List<Purchase> {
    return InAppUtil.queryPurchases(this, productType)
}
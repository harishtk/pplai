package com.aiavatar.app.pay.billing

import com.android.billingclient.api.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

object InAppUtil {

    suspend fun consume(billingClient: BillingClient, purchaseToken: String): ConsumeResult {
        val params = ConsumeParams.newBuilder()
            .setPurchaseToken(purchaseToken)
            .build()

        return billingClient.consumePurchase(params)
    }

    suspend fun queryPurchases(billingClient: BillingClient): List<Purchase>
            = suspendCoroutine { continuation ->
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
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
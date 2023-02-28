package com.aiavatar.app.feature.home.data.source.remote.dto

import com.aiavatar.app.feature.home.domain.model.VerifyCouponData
import com.google.gson.annotations.SerializedName

data class VerifyCouponDataDto(
    @SerializedName("plans")
    val plans: List<SubscriptionPlanDto>
)

fun VerifyCouponDataDto.toVerifyCoupon(): VerifyCouponData {
    return VerifyCouponData(
        plans = plans.map(SubscriptionPlanDto::toSubscriptionPlan)
    )
}
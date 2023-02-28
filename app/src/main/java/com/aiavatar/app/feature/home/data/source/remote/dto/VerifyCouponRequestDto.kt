package com.aiavatar.app.feature.home.data.source.remote.dto

import com.aiavatar.app.feature.home.domain.model.request.VerifyCouponRequest
import com.google.gson.annotations.SerializedName

data class VerifyCouponRequestDto(
    @SerializedName("couponCode")
    val couponCode: String
)

fun VerifyCouponRequest.asDto(): VerifyCouponRequestDto {
    return VerifyCouponRequestDto(
        couponCode = couponCode
    )
}
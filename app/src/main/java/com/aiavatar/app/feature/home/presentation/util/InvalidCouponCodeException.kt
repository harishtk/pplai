package com.aiavatar.app.feature.home.presentation.util

import com.aiavatar.app.ifDebug


/**
 * @author Hariskumar Kubendran
 * @date 28/02/23
 * Pepul Tech
 * hariskumar@pepul.com
 */
class InvalidCouponCodeException(
    override val message: String,
    val debugMessage: String? = null
) : Exception() {

    override fun toString(): String {
        return super.toString().also {
            ifDebug { "$it. Reason: $debugMessage" }
        }
    }
}
package com.aiavatar.app.commons.util

import javax.net.ssl.HttpsURLConnection

sealed class NetworkResult<T>(
    val data: T? = null,
    val message: String? = null,
    open val code: Int? = null,
    open val uiMessage: String? = null,
) {
    class Success<T>(data: T, message: String?, override val code: Int = HttpsURLConnection.HTTP_OK) : NetworkResult<T>(data, message)
    class Error<T>(message: String, override val uiMessage: String? = null, override val code: Int = ERROR_CODE_NONE) : NetworkResult<T>(code = code, message = message)
    class Loading<T> : NetworkResult<T>()
    class NoInternet<T>(message: String, data: T? = null) : NetworkResult<T>(data, message)
    class UnAuthorized<T>(message: String, data: T? = null) : NetworkResult<T>(data, message)
}

const val ERROR_CODE_NONE = -1
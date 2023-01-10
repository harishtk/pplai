package com.pepulai.app.commons.data.source.remote

import androidx.annotation.WorkerThread
import com.pepulai.app.commons.util.NetWorkHelper
import com.pepulai.app.commons.util.NetworkResult
import com.pepulai.app.commons.util.net.isConnected
import org.json.JSONObject
import retrofit2.HttpException
import retrofit2.Response
import timber.log.Timber
import java.io.IOException
import java.net.HttpURLConnection

open class BaseRemoteDataSource(
    val netWorkHelper: NetWorkHelper
) {

    private val TAG : String = BaseRemoteDataSource::class.java.simpleName

    @WorkerThread
    protected suspend fun <T> safeApiCall(call : suspend() -> Response<T>) : NetworkResult<T> {
        var networkResult : NetworkResult<T> = NetworkResult.Loading()
        Timber.d("Network Helper: ${netWorkHelper.checkForInternet()} ${netWorkHelper.context.isConnected()}")
        if (netWorkHelper.checkForInternet()) {
            try {
                call.invoke().let { response: Response<T> ->
                    Timber.d("Response: body ${response.body()} errorBody ${response.errorBody()} raw ${response}")
                    if (response.isSuccessful) {
                        response.body()?.let {
                            networkResult = NetworkResult.Success(it, response.message(), code = response.code())
                        }
                    } else {
                        networkResult = apiErrorHandler(response)
                    }
                }
            } catch (e: Exception) {
                networkResult = NetworkResult.Error(e.toString(), null)
            }
        } else {
            networkResult = NetworkResult.NoInternet("No Internet Connection", null)
        }
        return networkResult
    }

    @WorkerThread
    protected suspend fun <T> synchronizedCall(call : suspend() -> T): NetworkResult<T> {
        return try {
            val response = call.invoke() ?: throw IllegalStateException("Failed to make call.")
            Timber.d("Response: body $response")
            NetworkResult.Success(response, null)
        } catch (e: HttpException) {
            NetworkResult.Error(e.message(), code = e.code())
        } catch (e: IOException) {
            NetworkResult.Error(e.message ?: "Unknown Error")
        }
    }

    private fun <T> apiErrorHandler(response: Response<T>) : NetworkResult<T> {
        var code : String = ""
        var message : String = ""
        var errorMessage : String = ""
        try {
            val jsonObject = JSONObject(response.errorBody()!!.string())
            jsonObject.apply {
                if (has("statusCode")) {
                    code = getString("statusCode")
                }
                if (has("message")) {
                    message = getString("message")
                }
            }
            if (message.isNotEmpty()) {
                when (code) {
                    "500" -> {
                        errorMessage = "$code ${HttpURLConnection.HTTP_INTERNAL_ERROR}"
                    }
                    "403" -> {
                        errorMessage = "$code ${HttpURLConnection.HTTP_UNAUTHORIZED}"
                        return NetworkResult.UnAuthorized(errorMessage)
                    }
                    HttpURLConnection.HTTP_UNAUTHORIZED.toString() -> {
                        errorMessage = "UnAuthorized"
                        return NetworkResult.UnAuthorized(errorMessage)
                    }
                    else -> {
                        errorMessage = "$code $message"
                    }
                }
            } else {
                "${response.code()} ${response.message()}"
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, e.toString())
            errorMessage = "${response.code()} ${response.message()}"
        }
        return NetworkResult.Error("Api Error Response: $errorMessage", uiMessage = message, code = response.code())
    }

}
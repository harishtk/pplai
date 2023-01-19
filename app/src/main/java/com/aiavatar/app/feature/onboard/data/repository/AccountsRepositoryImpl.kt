package com.aiavatar.app.feature.onboard.data.repository

import com.aiavatar.app.commons.util.InvalidOtpException
import com.aiavatar.app.commons.util.NetworkResult
import com.aiavatar.app.commons.util.NetworkResultParser
import com.aiavatar.app.commons.util.Result
import com.aiavatar.app.commons.util.net.ApiException
import com.aiavatar.app.commons.util.net.BadResponseException
import com.aiavatar.app.commons.util.net.EmptyResponseException
import com.aiavatar.app.commons.util.net.HttpResponse
import com.aiavatar.app.feature.onboard.data.source.remote.AccountsRemoteDataSource
import com.aiavatar.app.feature.onboard.data.source.remote.dto.asDto
import com.aiavatar.app.feature.onboard.data.source.remote.dto.toLoginData
import com.aiavatar.app.feature.onboard.data.source.remote.model.LoginResponse
import com.aiavatar.app.feature.onboard.domain.model.LoginData
import com.aiavatar.app.feature.onboard.domain.model.request.AutoLoginRequest
import com.aiavatar.app.feature.onboard.domain.model.request.LoginRequest
import com.aiavatar.app.feature.onboard.domain.model.request.LogoutRequest
import com.aiavatar.app.feature.onboard.domain.model.request.SocialLoginRequest
import com.aiavatar.app.feature.onboard.domain.repository.AccountsRepository
import com.aiavatar.app.feature.onboard.presentation.utils.InvalidMobileNumberException
import com.aiavatar.app.feature.onboard.presentation.utils.RecaptchaException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.net.ssl.HttpsURLConnection

class AccountsRepositoryImpl constructor(
    private val remoteDataSource: AccountsRemoteDataSource
) : AccountsRepository, NetworkResultParser {

    override fun loginUser(loginRequest: LoginRequest): Flow<Result<LoginData>> {
        return remoteDataSource.login(loginRequest.asDto()).map(this::parseLoginResult)
    }

    override fun socialLogin(socialLoginRequest: SocialLoginRequest): Flow<Result<LoginData>> {
        return remoteDataSource.socialLogin(socialLoginRequest.asDto()).map(this::parseLoginResult)
    }

    override fun autoLogin(autoLoginRequest: AutoLoginRequest): Flow<Result<String>> {
        return remoteDataSource.autoLogin(autoLoginRequest.asDto()).map { networkResult ->
            when (networkResult) {
                is NetworkResult.Loading -> Result.Loading
                is NetworkResult.Success -> {
                    if (networkResult.data?.statusCode == HttpsURLConnection.HTTP_OK) {
                        val data = networkResult.data?.message ?: "Login Successful. No message"
                        Result.Success(data)
                    } else {
                        val cause = BadResponseException("Unexpected response code ${networkResult.code}")
                        Result.Error(ApiException(cause))
                    }
                }
                else -> parseErrorNetworkResult(networkResult)
            }
        }
    }

    override fun logout(logoutRequest: LogoutRequest): Flow<Result<String>> {
        return remoteDataSource.logout(logoutRequest.asDto()).map { networkResult ->
            when (networkResult) {
                is NetworkResult.Loading -> Result.Loading
                is NetworkResult.Success -> {
                    if (networkResult.data?.statusCode == HttpsURLConnection.HTTP_OK) {
                        val data = networkResult.data?.message ?: "Logout Successful. No message"
                        Result.Success(data)
                    } else {
                        val cause = BadResponseException("Unexpected response code ${networkResult.code}")
                        Result.Error(ApiException(cause))
                    }
                }
                else -> parseErrorNetworkResult(networkResult)
            }
        }
    }

    private fun parseLoginResult(networkResult: NetworkResult<LoginResponse>): Result<LoginData> {
        return when (networkResult) {
            is NetworkResult.Loading -> Result.Loading
            is NetworkResult.Success -> {
                if (networkResult.data?.statusCode == HttpsURLConnection.HTTP_OK) {
                    val data = networkResult.data?.loginDataDto
                    if (data != null) {
                        Result.Success(data.toLoginData())
                    } else {
                        val cause = EmptyResponseException("No data")
                        Result.Error(ApiException(cause))
                    }
                } else {
                    val cause = BadResponseException("Unexpected response code: ${networkResult.code}")
                    Result.Error(ApiException(cause))
                }
            }
            else -> {
                Timber.d("Status code check: signup ${networkResult.data?.statusCode}")
                when (networkResult.code) {
                    HttpsURLConnection.HTTP_NOT_ACCEPTABLE -> {
                        val cause = InvalidOtpException(networkResult.message ?: "Invalid OTP!")
                        Result.Error(ApiException(cause))
                    }
                    HttpsURLConnection.HTTP_BAD_REQUEST -> {
                        val cause = RecaptchaException()
                        Result.Error(ApiException(cause))
                    }
                    HttpsURLConnection.HTTP_PRECON_FAILED,
                    HttpResponse.HTTP_TOO_MANY_REQUESTS -> {
                        val cause = InvalidMobileNumberException()
                        Result.Error(ApiException(cause))
                    }
                    else -> {
                        parseErrorNetworkResult(networkResult)
                    }
                }
            }
        }
    }

}
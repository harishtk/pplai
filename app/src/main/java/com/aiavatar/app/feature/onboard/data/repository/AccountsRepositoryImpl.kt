package com.aiavatar.app.feature.onboard.data.repository

import com.aiavatar.app.commons.util.InvalidOtpException
import com.aiavatar.app.commons.util.NetworkResult
import com.aiavatar.app.commons.util.NetworkResultParser
import com.aiavatar.app.commons.util.Result
import com.aiavatar.app.commons.util.net.ApiException
import com.aiavatar.app.commons.util.net.HttpResponse
import com.aiavatar.app.core.di.ApplicationCoroutineScope
import com.aiavatar.app.di.IoDispatcher
import com.aiavatar.app.feature.onboard.data.source.remote.AccountsRemoteDataSource
import com.aiavatar.app.feature.onboard.data.source.remote.dto.*
import com.aiavatar.app.feature.onboard.data.source.remote.model.LoginResponse
import com.aiavatar.app.feature.onboard.domain.model.AutoLoginData
import com.aiavatar.app.feature.onboard.domain.model.CreateCheckData
import com.aiavatar.app.feature.onboard.domain.model.LoginData
import com.aiavatar.app.feature.onboard.domain.model.ShareLinkData
import com.aiavatar.app.feature.onboard.domain.model.request.*
import com.aiavatar.app.feature.onboard.domain.repository.AccountsRepository
import com.aiavatar.app.feature.onboard.presentation.utils.InvalidMobileNumberException
import com.aiavatar.app.feature.onboard.presentation.utils.RecaptchaException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject
import javax.net.ssl.HttpsURLConnection

class AccountsRepositoryImpl @Inject constructor(
    @ApplicationCoroutineScope
    private val applicationScope: CoroutineScope,
    @IoDispatcher
    private val ioDispatcher: CoroutineDispatcher,
    private val remoteDataSource: AccountsRemoteDataSource
) : AccountsRepository, NetworkResultParser {

    override fun loginUser(loginRequest: LoginRequest): Flow<Result<LoginData>> {
        return remoteDataSource.login(loginRequest.asDto()).map(this::parseLoginResult)
    }

    override fun socialLogin(socialLoginRequest: SocialLoginRequest): Flow<Result<LoginData>> {
        return remoteDataSource.socialLogin(socialLoginRequest.asDto()).map(this::parseLoginResult)
    }

    override fun autoLogin(autoLoginRequest: AutoLoginRequest): Flow<Result<AutoLoginData>> {
        return remoteDataSource.autoLogin(autoLoginRequest.asDto()).map { networkResult ->
            when (networkResult) {
                is NetworkResult.Loading -> Result.Loading
                is NetworkResult.Success -> {
                    if (networkResult.data?.statusCode == HttpsURLConnection.HTTP_OK) {
                        val data = networkResult.data.data
                        if (data != null) {
                            Result.Success(data.toAutoLoginData())
                        } else {
                            emptyResponse(networkResult)
                        }
                    } else {
                        badResponse(networkResult)
                    }
                }
                else -> parseErrorNetworkResult(networkResult)
            }
        }
    }

    override fun logout(logoutRequest: LogoutRequest): Flow<Result<String>> {
        return remoteDataSource.logout(logoutRequest.asDto())
            /* Executes the call in the application scope,
                so that it won't get canceled when the calling scope is dead. *//*
            .flowOn(applicationScope.coroutineContext)*/
            .map { networkResult ->
                when (networkResult) {
                    is NetworkResult.Loading -> Result.Loading
                    is NetworkResult.Success -> {
                        if (networkResult.data?.statusCode == HttpsURLConnection.HTTP_OK) {
                            val data = networkResult.data.message ?: "Logout Successful. No message"
                            Result.Success(data)
                        } else {
                            badResponse(networkResult)
                        }
                    }
                    else -> parseErrorNetworkResult(networkResult)
            }
        }
    }

    override fun getShareLink(getShareLinkRequest: GetShareLinkRequest): Flow<Result<ShareLinkData>> {
        return remoteDataSource.getShareLink(getShareLinkRequest.asDto()).map { networkResult ->
            when (networkResult) {
                is NetworkResult.Loading -> Result.Loading
                is NetworkResult.Success -> {
                    if (networkResult.data?.statusCode == HttpsURLConnection.HTTP_OK) {
                        val shareLinkDataDto = networkResult.data?.data
                        if (shareLinkDataDto != null) {
                            Result.Success(shareLinkDataDto.toShareLinkData())
                        } else {
                            emptyResponse(networkResult)
                        }
                    } else {
                        badResponse(networkResult)
                    }
                }
                else -> parseErrorNetworkResult(networkResult)
            }
        }
            .catch { t ->
                val cause = ApiException(t)
                emit(Result.Error.NonRecoverableError(cause))
            }
    }

    override fun feedback(feedbackRequest: FeedbackRequest): Flow<Result<String>> {
        return remoteDataSource.feedback(feedbackRequest.asDto()).map { networkResult ->
            when (networkResult) {
                is NetworkResult.Loading -> Result.Loading
                is NetworkResult.Success -> {
                    if (networkResult.data?.statusCode == HttpsURLConnection.HTTP_OK) {
                        val message = networkResult.data?.message
                            ?: "Success. No message."
                        Result.Success(message)
                    } else {
                        badResponse(networkResult)
                    }
                }
                else -> parseErrorNetworkResult(networkResult)
            }
        }
    }

    override fun createCheck(createCheckRequest: CreateCheckRequest): Flow<Result<CreateCheckData>> {
        return remoteDataSource.createCheck(createCheckRequest.asDto()).map { networkResult ->
            when (networkResult) {
                is NetworkResult.Loading -> Result.Loading
                is NetworkResult.Success -> {
                    if (networkResult.data?.statusCode == HttpsURLConnection.HTTP_OK) {
                        val dataDto = networkResult.data.data
                        if (dataDto != null) {
                            Result.Success(dataDto.toCreateCheckData())
                        } else {
                            emptyResponse(networkResult)
                        }
                    } else {
                        badResponse(networkResult)
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
                    val data = networkResult.data.loginDataDto
                    if (data != null) {
                        Result.Success(data.toLoginData())
                    } else {
                        emptyResponse(networkResult)
                    }
                } else {
                    badResponse(networkResult)
                }
            }
            else -> {
                Timber.d("Status code check: signup ${networkResult.data?.statusCode}")
                when (networkResult.code) {
                    HttpsURLConnection.HTTP_NOT_ACCEPTABLE -> {
                        val cause = InvalidOtpException(networkResult.message ?: "Invalid OTP!")
                        Result.Error.NonRecoverableError(ApiException(cause))
                    }
                    HttpsURLConnection.HTTP_BAD_REQUEST -> {
                        val cause = RecaptchaException()
                        Result.Error.NonRecoverableError(ApiException(cause))
                    }
                    HttpsURLConnection.HTTP_PRECON_FAILED,
                    HttpResponse.HTTP_TOO_MANY_REQUESTS -> {
                        val cause = InvalidMobileNumberException()
                        Result.Error.NonRecoverableError(ApiException(cause))
                    }
                    else -> {
                        parseErrorNetworkResult(networkResult)
                    }
                }
            }
        }
    }

}
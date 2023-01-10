package com.pepulai.app.feature.onboard.data.repository

import com.pepulai.app.commons.util.NetworkResult
import com.pepulai.app.commons.util.NetworkResultParser
import com.pepulai.app.commons.util.Result
import com.pepulai.app.commons.util.net.ApiException
import com.pepulai.app.commons.util.net.BadResponseException
import com.pepulai.app.commons.util.net.EmptyResponseException
import com.pepulai.app.feature.onboard.data.source.remote.AccountsRemoteDataSource
import com.pepulai.app.feature.onboard.data.source.remote.dto.asDto
import com.pepulai.app.feature.onboard.data.source.remote.dto.toLoginData
import com.pepulai.app.feature.onboard.domain.model.LoginData
import com.pepulai.app.feature.onboard.domain.model.request.AutoLoginRequest
import com.pepulai.app.feature.onboard.domain.model.request.LoginRequest
import com.pepulai.app.feature.onboard.domain.repository.AccountsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.net.ssl.HttpsURLConnection

class AccountsRepositoryImpl constructor(
    private val remoteDataSource: AccountsRemoteDataSource
) : AccountsRepository, NetworkResultParser {

    override fun loginUser(loginRequest: LoginRequest): Flow<Result<LoginData>> {
        return remoteDataSource.login(loginRequest.asDto()).map { networkResult ->
            when (networkResult) {
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
                else -> parseErrorNetworkResult(networkResult)
            }
        }
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

}
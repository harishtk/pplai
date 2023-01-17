package com.aiavatar.app.feature.onboard.domain.repository

import com.aiavatar.app.commons.util.Result
import com.aiavatar.app.feature.onboard.domain.model.LoginData
import com.aiavatar.app.feature.onboard.domain.model.request.AutoLoginRequest
import com.aiavatar.app.feature.onboard.domain.model.request.LoginRequest
import com.aiavatar.app.feature.onboard.domain.model.request.LogoutRequest
import kotlinx.coroutines.flow.Flow

interface AccountsRepository {

    fun loginUser(loginRequest: LoginRequest): Flow<Result<LoginData>>

    fun autoLogin(autoLoginRequest: AutoLoginRequest): Flow<Result<String>>

    fun logout(logoutRequest: LogoutRequest):  Flow<Result<String>>

}
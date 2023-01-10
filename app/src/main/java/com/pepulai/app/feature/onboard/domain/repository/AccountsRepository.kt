package com.pepulai.app.feature.onboard.domain.repository

import com.pepulai.app.commons.util.Result
import com.pepulai.app.feature.onboard.domain.model.LoginData
import com.pepulai.app.feature.onboard.domain.model.request.AutoLoginRequest
import com.pepulai.app.feature.onboard.domain.model.request.LoginRequest
import kotlinx.coroutines.flow.Flow

interface AccountsRepository {

    fun loginUser(loginRequest: LoginRequest): Flow<Result<LoginData>>

    fun autoLogin(autoLoginRequest: AutoLoginRequest): Flow<Result<String>>

}
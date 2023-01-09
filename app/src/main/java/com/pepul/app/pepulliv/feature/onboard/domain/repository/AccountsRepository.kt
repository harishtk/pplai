package com.pepul.app.pepulliv.feature.onboard.domain.repository

import com.pepul.app.pepulliv.commons.util.Result
import com.pepul.app.pepulliv.feature.onboard.domain.model.LoginData
import com.pepul.app.pepulliv.feature.onboard.domain.model.request.AutoLoginRequest
import com.pepul.app.pepulliv.feature.onboard.domain.model.request.LoginRequest
import kotlinx.coroutines.flow.Flow

interface AccountsRepository {

    fun loginUser(loginRequest: LoginRequest): Flow<Result<LoginData>>

    fun autoLogin(autoLoginRequest: AutoLoginRequest): Flow<Result<String>>

}
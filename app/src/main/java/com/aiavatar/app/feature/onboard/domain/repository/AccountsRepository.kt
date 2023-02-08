package com.aiavatar.app.feature.onboard.domain.repository

import com.aiavatar.app.commons.util.Result
import com.aiavatar.app.feature.onboard.domain.model.AutoLoginData
import com.aiavatar.app.feature.onboard.domain.model.LoginData
import com.aiavatar.app.feature.onboard.domain.model.ShareLinkData
import com.aiavatar.app.feature.onboard.domain.model.request.*
import kotlinx.coroutines.flow.Flow

interface AccountsRepository {

    fun loginUser(loginRequest: LoginRequest): Flow<Result<LoginData>>

    fun socialLogin(socialLoginRequest: SocialLoginRequest): Flow<Result<LoginData>>

    fun autoLogin(autoLoginRequest: AutoLoginRequest): Flow<Result<AutoLoginData>>

    fun logout(logoutRequest: LogoutRequest): Flow<Result<String>>

    fun getShareLink(getShareLinkRequest: GetShareLinkRequest): Flow<Result<ShareLinkData>>

}
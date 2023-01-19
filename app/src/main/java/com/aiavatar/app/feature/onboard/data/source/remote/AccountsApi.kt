package com.aiavatar.app.feature.onboard.data.source.remote

import com.aiavatar.app.feature.onboard.data.source.remote.dto.AutoLoginRequestDto
import com.aiavatar.app.feature.onboard.data.source.remote.dto.LoginRequestDto
import com.aiavatar.app.feature.onboard.data.source.remote.dto.LogoutRequestDto
import com.aiavatar.app.feature.onboard.data.source.remote.dto.SocialLoginRequestDto
import com.aiavatar.app.feature.onboard.data.source.remote.model.AutoLoginResponse
import com.aiavatar.app.feature.onboard.data.source.remote.model.BaseResponse
import com.aiavatar.app.feature.onboard.data.source.remote.model.LoginResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AccountsApi {

    @POST("user/signup")
    suspend fun login(@Body loginRequestDto: LoginRequestDto): Response<LoginResponse>

    @POST("user/socialLogin")
    suspend fun socialLogin(@Body socialLoginRequestDto: SocialLoginRequestDto): Response<LoginResponse>

    @POST("user/autologin")
    suspend fun autoLogin(@Body autoRequestDto: AutoLoginRequestDto): Response<AutoLoginResponse>

    @POST("user/logout")
    suspend fun logout(@Body logoutRequestDto: LogoutRequestDto): Response<BaseResponse>

}
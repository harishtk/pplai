package com.aiavatar.app.feature.onboard.data.source.remote

import com.aiavatar.app.feature.onboard.data.source.remote.dto.*
import com.aiavatar.app.feature.onboard.data.source.remote.model.*
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

    @POST("user/share")
    suspend fun getShareLink(@Body getShareLinkRequestDto: GetShareLinkRequestDto): Response<GetShareLinkResponse>

    @POST("user/feedback")
    suspend fun feedback(@Body feedbackRequestDto: FeedbackRequestDto): Response<BaseResponse>

    @POST("user/createCheck")
    suspend fun createCheck(@Body createCheckRequest: CreateCheckRequestDto): Response<CreateCheckResponse>
}
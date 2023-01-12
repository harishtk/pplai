package com.pepulai.app.feature.onboard.data.source.remote

import com.pepulai.app.feature.onboard.data.source.remote.dto.AutoLoginRequestDto
import com.pepulai.app.feature.onboard.data.source.remote.dto.LoginRequestDto
import com.pepulai.app.feature.onboard.data.source.remote.dto.LogoutRequestDto
import com.pepulai.app.feature.onboard.data.source.remote.model.AutoLoginResponse
import com.pepulai.app.feature.onboard.data.source.remote.model.BaseResponse
import com.pepulai.app.feature.onboard.data.source.remote.model.LoginResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AccountsApi {

    @POST("user/signup")
    suspend fun login(@Body loginRequestDto: LoginRequestDto): Response<LoginResponse>

    @POST("user/autologin")
    suspend fun autoLogin(@Body autoRequestDto: AutoLoginRequestDto): Response<AutoLoginResponse>

    @POST("user/logout")
    suspend fun logout(@Body logoutRequestDto: LogoutRequestDto): Response<BaseResponse>

}
package com.aiavatar.app.core.domain.repository

import com.aiavatar.app.commons.util.Result
import com.aiavatar.app.core.domain.model.request.SendFcmTokenRequest
import kotlinx.coroutines.flow.Flow

interface AppRepository {

    fun sendFcmToken(request: SendFcmTokenRequest): Flow<Result<String>>

    suspend fun sendFcmTokenSync(request: SendFcmTokenRequest): Result<String>

}
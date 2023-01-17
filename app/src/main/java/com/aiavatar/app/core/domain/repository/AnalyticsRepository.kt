package com.aiavatar.app.core.domain.repository

import com.aiavatar.app.commons.util.Result
import com.google.gson.JsonObject
import kotlinx.coroutines.flow.Flow

/**
 * TODO: -partially_done- the result should be independent of the local/remote resource, but now is just NetworkResult<BaseResponse> which violates the domain layer
 */
interface AnalyticsRepository {

    fun updateCount(params: JsonObject): Flow<Result<String>>

}
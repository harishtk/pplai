package com.aiavatar.app.core.data.source.remote

import com.google.gson.JsonObject
import javax.inject.Inject

class LoggingRemoteDataSource @Inject constructor(
    private val apiService: AppApi
) {
    suspend fun customLog(jsonObject: JsonObject) = apiService.customLog(jsonObject)
}
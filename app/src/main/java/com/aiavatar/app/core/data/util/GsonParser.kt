package com.aiavatar.app.core.data.util

import com.aiavatar.app.core.domain.util.JsonParser
import com.google.gson.Gson
import java.lang.reflect.Type
import javax.inject.Inject

class GsonParser @Inject constructor(
    private val gson: Gson
) : JsonParser {
    override fun <T> fromJson(json: String, type: Type): T? {
        return gson.fromJson(json, type)
    }

    override fun <T> toJson(obj: T, type: Type): String? {
        return gson.toJson(obj, type)
    }
}
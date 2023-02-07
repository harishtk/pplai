package com.aiavatar.app.core.domain.util

import java.lang.reflect.Type
import kotlin.reflect.KClass

interface JsonParser {

    fun <T> fromJson(json: String, clazz: Class<T>): T?

    fun <T> toJson(obj: T, type: Type): String?

}
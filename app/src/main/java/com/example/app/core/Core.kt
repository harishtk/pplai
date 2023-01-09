package com.example.app.core

import androidx.annotation.StringDef
import com.example.app.Constant

@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY, AnnotationTarget.VALUE_PARAMETER)
@StringDef(Constant.ENV_DEV, Constant.ENV_STAGE, Constant.ENV_PROD, Constant.ENV_SPECIAL)
@Retention(AnnotationRetention.SOURCE)
annotation class AppEnv

enum class Env {
    DEV, STAGE, PROD, SPECIAL
}

fun envForConfig(config: String): Env = when (config) {
    "dev" -> Env.DEV
    "staging" -> Env.STAGE
    "sp" -> Env.SPECIAL
    "prod" -> Env.PROD
    else -> throw IllegalStateException("Unknown Environment $config")
}

const val DEFAULT_PAGE_SIZE: Int = 20
package com.aiavatar.app.core

import com.aiavatar.app.BuildConfig

object URLProvider {

    private const val baseUrl: String = BuildConfig.S3_BUCKET
    private val environment: Env = envForConfig(BuildConfig.ENV)

    fun getImageUrl(
        source: URLSource,
        fileName: String,
        sourceCategory: SourceCategory = SourceCategory.ORIGINAL,
    ): String {
        val prefix = buildPrefix(source, sourceCategory)
        return "$prefix$fileName"
    }

    fun avatarUrl(fileName: String): String {
        return getImageUrl(URLSource.AVATAR, fileName)
    }

    fun avatarThumbUrl(fileName: String): String {
        return getImageUrl(URLSource.AVATAR, fileName, SourceCategory.THUMBNAIL)
    }

    private fun buildPrefix(source: URLSource, sourceCategory: SourceCategory): String {
        return when (environment) {
            Env.DEV -> when (source) {
                URLSource.AVATAR -> {
                    when (sourceCategory) {
                        SourceCategory.THUMBNAIL -> "$baseUrl/aiAvatar/"
                        SourceCategory.ORIGINAL -> "$baseUrl/aiAvatar/"
                    }
                }
            }
            Env.PROD -> when (source) {
                URLSource.AVATAR -> {
                    when (sourceCategory) {
                        SourceCategory.THUMBNAIL -> "$baseUrl/aiAvatar/"
                        SourceCategory.ORIGINAL -> "$baseUrl/aiAvatar/"
                    }
                }
            }
            Env.STAGE,
            Env.SPECIAL -> {
                ""
            }
        }
    }
}

enum class URLSource {
    AVATAR
}

enum class SourceCategory {
    THUMBNAIL, ORIGINAL
}

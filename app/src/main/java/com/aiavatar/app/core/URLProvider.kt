package com.aiavatar.app.core

import com.aiavatar.app.BuildConfig
import org.jetbrains.annotations.Contract
import timber.log.Timber

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

    @Contract("null -> null")
    fun avatarThumbUrl(fileName: String?): String? {
        fileName ?: return null
        return getImageUrl(URLSource.AVATAR, fileName, SourceCategory.THUMBNAIL)
    }

    private fun buildPrefix(source: URLSource, sourceCategory: SourceCategory): String {
        return when (environment) {
            Env.DEV -> when (source) {
                URLSource.AVATAR -> {
                    when (sourceCategory) {
                        SourceCategory.THUMBNAIL -> "$baseUrl/thumbnail/"
                        SourceCategory.ORIGINAL -> "$baseUrl/aiAvatar/"
                    }
                }
            }
            Env.PROD, Env.INTERNAL -> when (source) {
                URLSource.AVATAR -> {
                    when (sourceCategory) {
                        SourceCategory.THUMBNAIL -> "$baseUrl/thumbnail/"
                        SourceCategory.ORIGINAL -> "$baseUrl/aiAvatar/"
                    }
                }
            }
            Env.STAGE,
            Env.SPECIAL, -> {
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

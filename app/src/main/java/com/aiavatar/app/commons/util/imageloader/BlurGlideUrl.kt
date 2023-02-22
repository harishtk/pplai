package com.aiavatar.app.commons.util.imageloader

import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.Headers
import timber.log.Timber
import java.net.URL

class BlurGlideUrl : GlideUrl {
    constructor(url: URL?) : super(url)
    constructor(url: String?) : super(url)
    constructor(url: URL?, headers: Headers?) : super(url, headers)
    constructor(url: String?, headers: Headers?) : super(url, headers)

    override fun getCacheKey(): String {
        val newCacheKey = super.getCacheKey().plus("-blur")
        Timber.d("Cache Key: $newCacheKey")
        return newCacheKey
    }
}
package com.aiavatar.app.commons.util.imageloader

import android.content.Context
import android.widget.ImageView
import com.aiavatar.app.core.util.BlurTransformation
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions

interface ImageBlur {
    fun loadBlurImage(url: String, target: ImageView)
}

/*class CoilImageBlur2(
    private val context: Context
) : ImageBlur {
    override fun loadBlurImage(url: String, target: ImageView) {
        target.load(url) {
            *//*allowHardware(false)
            allowConversionToBitmap(true)
            memoryCachePolicy(CachePolicy.ENABLED)*//*
            //size(width = 400, height = 400)
            transformations(com.commit451.coiltransformations.BlurTransformation(context, 25F))
        }
    }
}

class CoilImageBlur(
    private val context: Context
) : ImageBlur {
    private val imageLoader: ImageLoader by lazy { ImageLoader(context) }
    private val IMAGE_CACHE: HashMap<String, MemoryCache.Key> by lazy { HashMap() }

    val hd: Size = Size(720, 1280)

    override fun loadBlurImage(url: String, target: ImageView) {
        if (IMAGE_CACHE[url] != null) {
            Timber.d("From cached: true")
            val cachedBmp = imageLoader.memoryCache?.get(IMAGE_CACHE[url]!!)?.bitmap
            cachedBmp?.let(target::setImageBitmap)
                ?: run {
                    IMAGE_CACHE.remove(url)
                    loadBlurImage(url, target)
                }
            Timber.d("Blur: set 3")
        } else {
            val memoryCacheKey = MemoryCache.Key(key = url)
            IMAGE_CACHE[url] = memoryCacheKey
            imageLoader.enqueue(
                ImageRequest.Builder(context)
                    .data(url)
                    //.placeholder(R.drawable.loading_animation)
                    .allowHardware(false)
                    .allowConversionToBitmap(true)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .memoryCacheKey(memoryCacheKey)
                    .size(hd)
                    .target { d ->
                        Timber.d("From cached: false")
                        val resized = d.toBitmap(width = 400, height = 400)
                        val blurred = Commons.fastblur(resized, 0.50F, 20)
                        target.setImageBitmap(blurred)
                        Timber.d("Blur: set")
                        IMAGE_CACHE[url]?.let { key ->
                            blurred?.let {
                                val value = MemoryCache.Value(blurred)
                                imageLoader.memoryCache?.set(key, value)
                                Timber.d("Blur: cached")
                            }
                        }
                    }
                    .build()
            )
        }
    }
}*/

class GlideImageBlur(
    private val context: Context
) : ImageBlur {

    override fun loadBlurImage(url: String, target: ImageView) {
        Glide.with(target)
            .load(url)
            //.placeholder(R.drawable.loading_animation)

            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .apply(RequestOptions().override(400, 400))
            .transform(BlurTransformation(context, 0.25F, 25F))
            .into(target)
    }
}

/*
class DeepCoilBlur(
    private val context: Context
) : ImageBlur {
    private val imageLoader: ImageLoader by lazy { ImageLoader(context) }
    private val IMAGE_CACHE: HashMap<String, MemoryCache.Key> by lazy { HashMap() }
    override fun loadBlurImage(url: String, target: ImageView) {
        if (IMAGE_CACHE[url] != null) {
            Timber.d("From cached: true")
            val cachedBmp = imageLoader.memoryCache?.get(IMAGE_CACHE[url]!!)?.bitmap
            cachedBmp?.let(target::setImageBitmap)
                ?: run {
                    IMAGE_CACHE.remove(url)
                    loadBlurImage(url, target)
                }
            Timber.d("Blur: set 3")
        } else {
            val memoryCacheKey = MemoryCache.Key(key = url)
            IMAGE_CACHE[url] = memoryCacheKey
            imageLoader.enqueue(
                ImageRequest.Builder(context)
                    .data(url)
                    .allowHardware(false)
                    .allowConversionToBitmap(true)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .memoryCacheKey(memoryCacheKey)
                    .target { d ->
                        Timber.d("From cached: false")
                        val resized = d.toBitmap(width = 400, height = 400)
                        val blurred = Commons.fastblur(resized, 0.25F, 25)
                        target.setImageBitmap(blurred)
                        Timber.d("Blur: set")
                        IMAGE_CACHE[url]?.let { key ->
                            blurred?.let {
                                val value = MemoryCache.Value(blurred)
                                imageLoader.memoryCache?.set(key, value)
                                Timber.d("Blur: cached")
                            }
                        }
                    }
                    .build()
            )
        }
    }

}*/

package com.aiavatar.app.commons.util.imageloader

import android.graphics.drawable.Drawable
import android.transition.TransitionManager
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.annotation.MainThread
import com.aiavatar.app.R
import com.aiavatar.app.core.util.BlurTransformation
import com.aiavatar.app.safeCall
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import timber.log.Timber

interface ImageLoader {

    fun start()

    interface Listener {

        /**
         * Called immediately after [Target.onStart].
         */
        @MainThread
        fun onStart(loader: ImageLoader) {
        }

        /**
         * Called when thumbnail is loaded
         */
        @MainThread
        fun onThumbnailLoaded(loader: ImageLoader) {
        }

        /**
         * Called if the loader is cancelled.
         */
        @MainThread
        fun onCancel(loader: ImageLoader) {
        }

        /**
         * Called if an error occurs while executing the loader.
         */
        @MainThread
        fun onError(loader: ImageLoader, result: ErrorResult) {
        }

        /**
         * Called if the loader completes successfully.
         */
        @MainThread
        fun onSuccess(loader: ImageLoader, result: SuccessResult) {
        }
    }
}

data class SuccessResult(
    val drawable: Drawable
)

data class ErrorResult(
    val exception: Exception
)

/**
 * TODO - Extract request builder boiler-plate
 */
class GlideImageLoader private constructor(
    private val target: ImageView,
    private val glide: RequestManager
) : ImageLoader {

    private var listener: ImageLoader.Listener? = null

    private var thumbnail: String? = null
    private var originalImage: String? = null

    private var placeholder: Int = R.color.image_placeholder_background
    private var errorImage: Int = R.color.image_placeholder_background

    private var isBlurred: Boolean = false
    private var isLoaded: Boolean = false

    private var size: android.util.Size = android.util.Size(512, 512)

    private fun loadThumbnail(onNext: () -> Unit) {
        thumbnail?.let { thumb ->
            glide
                .load(thumb)
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: com.bumptech.glide.request.target.Target<Drawable>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        // TODO: -done- continue to load original as fallback
                        onNext()
                        return false
                    }

                    override fun onResourceReady(
                        resource: Drawable?,
                        model: Any?,
                        target: com.bumptech.glide.request.target.Target<Drawable>?,
                        dataSource: DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        onNext()
                        return false
                    }
                })
                .into(target)
        } ?: kotlin.run {
            onNext()
        }
    }

    private fun getThumbnailRequest(thumbUrl: String?): RequestBuilder<Drawable>? {
        return if (thumbUrl.isNullOrBlank()) {
            null
        } else {
            glide
                .load(thumbUrl)
                .placeholder(placeholder)
                .error(errorImage)
                .dontAnimate()
                .override(size.width, size.height)
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        Timber.d(e, "POST: ERR THUMB")
                        return false
                    }

                    override fun onResourceReady(
                        resource: Drawable?,
                        model: Any?,
                        target: Target<Drawable>?,
                        dataSource: DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        listener?.onThumbnailLoaded(this@GlideImageLoader)
                        Timber.d("Post: THUMB $thumbnail")
                        return false
                    }
                })
        }
    }

    private fun loadOriginalImage() {
        originalImage?.let { orig ->
            glide
                .load(orig)
                .thumbnail(
                    getThumbnailRequest(thumbnail)
                )
                .placeholder(placeholder)
                .error(errorImage)
                .dontAnimate()
                .override(size.width, size.height)
                /*.diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)*/
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: com.bumptech.glide.request.target.Target<Drawable>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        // TODO: continue to load original as fallback
                        listener?.onError(
                            this@GlideImageLoader,
                            ErrorResult(exception = Exception(e))
                        )
                        return false
                    }

                    override fun onResourceReady(
                        resource: Drawable?,
                        model: Any?,
                        target: com.bumptech.glide.request.target.Target<Drawable>?,
                        dataSource: DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        Timber.d("Post: POST $orig")
                        resource?.let {
                            isLoaded = true
                            listener?.onSuccess(
                                this@GlideImageLoader,
                                SuccessResult(drawable = it)
                            )
                        } ?: kotlin.run {
                            listener?.onError(
                                this@GlideImageLoader,
                                ErrorResult(exception = IllegalStateException("Cannot load resource $originalImage"))
                            )
                        }
                        return false
                    }
                })
                .into(target)
        } ?: kotlin.run {
            listener?.onError(
                this,
                ErrorResult(exception = IllegalStateException("No original URL"))
            )
        }
    }

    private fun loadBlurredImage(model: String) {
        glide
            .load(BlurGlideUrl(model))
            //.placeholder(R.drawable.loading_animation)
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .apply(RequestOptions().override(400, 400))
            .transform(BlurTransformation(target.context, 0.50F, 20F))
            .error(
                glide
                    .load(originalImage)
                    .placeholder(placeholder)
                    .error(errorImage)
                    .override(size.width, size.height)
                    .listener(object : RequestListener<Drawable> {
                        override fun onLoadFailed(
                            e: GlideException?,
                            model: Any?,
                            target: Target<Drawable>?,
                            isFirstResource: Boolean
                        ): Boolean {
                            Timber.d(e, "POST: ERR THUMB")
                            return false
                        }

                        override fun onResourceReady(
                            resource: Drawable?,
                            model: Any?,
                            target: Target<Drawable>?,
                            dataSource: DataSource?,
                            isFirstResource: Boolean
                        ): Boolean {
                            listener?.onThumbnailLoaded(this@GlideImageLoader)
                            Timber.d("Post: THUMB $thumbnail")
                            return false
                        }
                    })
            )
            .listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable>?,
                    isFirstResource: Boolean
                ): Boolean {
                    listener?.onError(this@GlideImageLoader,
                        ErrorResult(exception = Exception(e))
                    )
                    return false
                }

                override fun onResourceReady(
                    resource: Drawable?,
                    model: Any?,
                    target: Target<Drawable>?,
                    dataSource: DataSource?,
                    isFirstResource: Boolean
                ): Boolean {
                    resource?.let {
                        isLoaded = true
                        listener?.onSuccess(
                            this@GlideImageLoader,
                            SuccessResult(drawable = it)
                        )
                    } ?: kotlin.run {
                        listener?.onError(
                            this@GlideImageLoader,
                            ErrorResult(exception = IllegalStateException("Cannot load resource ${thumbnail ?: originalImage}"))
                        )
                    }
                    return false
                }

            })
            .into(target)
    }

    override fun start() {
        listener?.onStart(this)
        if (isBlurred) {
            loadBlurredImage(thumbnail ?: originalImage ?: "")
        } else {
            loadOriginalImage()
        }
    }

    class Builder<T : ImageView>(
        private val imageView: T,
        private val glide: RequestManager = Glide.with(imageView),
    ) {
        private val glideImageLoader: GlideImageLoader =
            GlideImageLoader(imageView, glide)

        inline fun listener(
            crossinline onStart: (loader: ImageLoader) -> Unit = {},
            crossinline onCancel: (loader: ImageLoader) -> Unit = {},
            crossinline onThumbnailLoaded: (loader: ImageLoader) -> Unit = {},
            crossinline onError: (loader: ImageLoader, ErrorResult) -> Unit = { _, _ -> },
            crossinline onSuccess: (loader: ImageLoader, SuccessResult) -> Unit = { _, _ -> }
        ) = listener(object : ImageLoader.Listener {
            override fun onStart(loader: ImageLoader) = onStart(loader)
            override fun onCancel(loader: ImageLoader) = onCancel(loader)
            override fun onThumbnailLoaded(loader: ImageLoader) = onThumbnailLoaded(loader)
            override fun onError(loader: ImageLoader, result: ErrorResult) = onError(loader, result)
            override fun onSuccess(loader: ImageLoader, result: SuccessResult) =
                onSuccess(loader, result)
        })

        fun listener(listener: ImageLoader.Listener?) = this.apply {
            this.glideImageLoader.listener = listener
        }

        fun thumbnail(thumbUrl: String?) = this.apply {
            this.glideImageLoader.thumbnail = thumbUrl
        }

        fun originalImage(originalUrl: String?) = this.apply {
            this.glideImageLoader.originalImage = originalUrl
        }

        fun placeholder(@DrawableRes drawable: Int) = this.apply {
            this.glideImageLoader.placeholder = drawable
        }

        fun error(@DrawableRes drawable: Int) = this.apply {
            this.glideImageLoader.errorImage = drawable
        }

        fun setBlur(shouldBlur: Boolean = true) = this.apply {
            this.glideImageLoader.isBlurred = shouldBlur
        }

        fun size(targetSize: android.util.Size) = this.apply {
            this.glideImageLoader.size = targetSize
        }

        fun start(): GlideImageLoader = this.glideImageLoader.also {
            it.start()
        }
    }

    companion object {
        fun <T : ImageView> T.newGlideBuilder(requestManager: RequestManager? = null): Builder<T> =
            if (requestManager != null) {
                Builder(this, requestManager)
            } else {
                Builder(this)
            }

        fun <T : ImageView> T.disposeGlideLoad() {
            try {
                Glide.with(this).clear(this)
                setImageDrawable(null)
            } catch (ignore: Exception) {}
        }
    }
}
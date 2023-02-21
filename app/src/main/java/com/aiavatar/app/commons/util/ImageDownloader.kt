package com.aiavatar.app.commons.util

import androidx.annotation.WorkerThread
import com.aiavatar.app.BuildConfig
import com.aiavatar.app.commons.util.io.ProgressResponseBody
import com.aiavatar.app.commons.util.net.UserAgentInterceptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.*
import okio.buffer
import okio.sink
import timber.log.Timber
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit

class ImageDownloader(
    private val url: String,
    private val outFile: File,
    private val onProgress: (progress: Int, bytes: Long) -> Unit
) {

    private val client: OkHttpClient

    init {
        val progressListener =
            ProgressResponseBody.ProgressListener { bytesRead, contentLength, done ->
                if (contentLength != -1L && !done) {
                    val percent = ((bytesRead * 100 / contentLength))
                        .coerceIn(0, 100).toInt()
                    onProgress(percent, bytesRead)
                } else {
                    onProgress(100, contentLength)
                }
            }

        val progressInterceptor = Interceptor { chain ->
            val originalResponse = chain.proceed(chain.request())
            originalResponse.newBuilder()
                .body(ProgressResponseBody(originalResponse.body, progressListener))
                .build()
        }
        client = OkHttpClient.Builder()
            .addNetworkInterceptor(progressInterceptor)
            .addInterceptor(UserAgentInterceptor())
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(2, TimeUnit.MINUTES)
            .followRedirects(true)
            .build()
    }

    @WorkerThread
    @Throws(IOException::class)
    suspend fun download() = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url = url)
            .cacheControl(CacheControl.FORCE_NETWORK)
            .build()

        if (BuildConfig.DEBUG) {
            Timber.d("Downloading: $url")
        }
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Unexpected response code ${response.code}")
            }

            response.body?.source()?.use {
                BufferedOutputStream(FileOutputStream(outFile)).sink().use { sink ->
                    sink.buffer().writeAll(it)
                }
            } ?: throw IOException("Cannot get input source")

        }
    }
}
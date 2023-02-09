package com.aiavatar.app.feature.home.presentation.create.util

import android.content.Context
import android.net.Uri
import android.os.CancellationSignal
import android.os.SystemClock
import android.text.Html.ImageGetter
import com.bumptech.glide.Glide
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import io.github.devzwy.nsfw.NSFWHelper
import io.github.devzwy.nsfw.NSFWScoreBean
import kotlinx.coroutines.*
import timber.log.Timber
import java.time.Clock
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.suspendCoroutine

@Deprecated("not yet written")
class ImageProcessorPipeline(
    private val context: Context,
    private val imageUris: List<Uri>
) {
    private val exceptionHandler = CoroutineExceptionHandler { _, t ->
        Timber.e(t)
    }
    private val coroutineContext: CoroutineContext =
        Dispatchers.IO + SupervisorJob() + exceptionHandler
    val scope = CoroutineScope(coroutineContext)

    var detectFaces: Boolean = false
        private set
    var detectNsfw: Boolean = false
        private set

    private val faceDetector by lazy { FaceDetection.getClient() }

    fun start(scope: CoroutineScope = this.scope): List<Deferred<Result>> {
        Timber.d("Pipeline: start() detectNsfw = $detectNsfw detectFaces = $detectFaces")
        return imageUris.map { uri ->
            scope.async(Dispatchers.IO) {
                Result(uri).apply {
                    detectNsfw(uri, this)
                    detectFaces(uri, this)
                }
            }
        }
    }

    private suspend fun detectNsfw(uri: Uri, out: Result): Result {
        return if (detectNsfw) {
            val score = NsfwDetectionPipe(context, uri).process()
            out.also { it.nsfwScoreBean = score }
        } else {
            out
        }
    }

    private suspend fun detectFaces(uri: Uri, out: Result): Result {
        return if (detectFaces) {
            var shouldDetect = false
            if (detectNsfw) {
                if (out.nsfwScoreBean != null && out.nsfwScoreBean!!.nsfwScore <= 0.5) {
                    shouldDetect = true
                }
            } else {
                shouldDetect = true
            }
            if (shouldDetect) {
                val result = FaceDetectionPipe(context, imageUri = uri, faceDetector = faceDetector)
                    .process()
                out.also { it.faces = result }
            } else {
                out
            }
        } else {
            out
        }
    }

    data class Result(
        val uri: Uri
    ) {
        var nsfwScoreBean: NSFWScoreBean? = null
        var faces: List<Face>? = null

        override fun toString(): String {
            return "Result[uri=$uri,nsfwScoreBean=$nsfwScoreBean,faces=$faces]"
        }
    }

    interface Pipe<R> {
        suspend fun process(): R
    }

    class Builder(context: Context, imageUris: List<Uri>) {
        val instance = ImageProcessorPipeline(context, imageUris)

        fun detectFaces(faceDetector: FaceDetector): Builder {
            instance.detectFaces = true
            return this
        }

        fun detectNsfw(): Builder {
            instance.detectNsfw = true
            return this
        }

        fun build(): ImageProcessorPipeline {
            return instance
        }
    }
}

class NsfwDetectionPipe(
    private val context: Context,
    private val imageUri: Uri
) : ImageProcessorPipeline.Pipe<NSFWScoreBean> {

    override suspend fun process(): NSFWScoreBean = withContext(Dispatchers.IO) {
        val bmp = Glide.with(context)
            .asBitmap()
            .load(imageUri)
            .submit(512, 512)
            .get()
        return@withContext NSFWHelper.getNSFWScore(bmp)
    }
}

class FaceDetectionPipe(
    private val context: Context,
    private val faceDetector: FaceDetector,
    private val imageUri: Uri
) : ImageProcessorPipeline.Pipe<List<Face>> {

    override suspend fun process(): List<Face> {
        val latch = CountDownLatch(1)
        val task = faceDetector.process(InputImage.fromFilePath(context, imageUri))
            .addOnCompleteListener { latch.countDown() }
            .addOnFailureListener { latch.countDown() }
        withContext(Dispatchers.IO) {
            latch.await()
        }
        return task.result
    }
}
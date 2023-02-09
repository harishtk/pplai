package com.aiavatar.app.feature.home.presentation.create.util

import android.content.Context
import android.net.Uri
import com.bumptech.glide.Glide
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import io.github.devzwy.nsfw.NSFWHelper
import io.github.devzwy.nsfw.NSFWScoreBean
import kotlinx.coroutines.*
import timber.log.Timber
import java.util.concurrent.CountDownLatch
import kotlin.coroutines.CoroutineContext


class ImageProcessorPipeline(
    private val context: Context,
    private val imageUris: List<Uri>
) {
    private val exceptionHandler = CoroutineExceptionHandler { _, t ->
        Timber.e(t)
    }
    private val coroutineContext: CoroutineContext =
        Dispatchers.IO + SupervisorJob() + exceptionHandler
    private val scope = CoroutineScope(coroutineContext)

    var detectFaces: Boolean = false
        private set
    var detectNsfw: Boolean = false
        private set

    private val faceDetector by lazy {
        val faceDetectorOpts = FaceDetectorOptions.Builder()
            .setMinFaceSize(0.5F)
            .build()
        FaceDetection.getClient(faceDetectorOpts)
    }

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

        fun detectFaces(): Builder {
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
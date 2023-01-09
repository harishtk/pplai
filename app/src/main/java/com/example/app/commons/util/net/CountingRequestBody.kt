package com.example.app.commons.util.net

import okhttp3.RequestBody
import okio.*

class CountingRequestBody(
    private val requestBody: RequestBody,
    private val onProgressUpdate: CountingRequestListener
) : RequestBody() {
    override fun contentType() = requestBody.contentType()

    @Throws(IOException::class)
    override fun contentLength(): Long = requestBody.contentLength()

    override fun writeTo(sink: BufferedSink) {
        val countingSink = CountingSink(sink, requestBody, onProgressUpdate)
        val bufferedSink = countingSink.buffer()

        requestBody.writeTo(bufferedSink)
        bufferedSink.flush()
    }

}

typealias CountingRequestListener = (bytesWritten: Long, contentLength: Long) -> Unit

class CountingSink(
    sink: Sink,
    private val requestBody: RequestBody,
    private val onProgressUpdate: CountingRequestListener
) : ForwardingSink(sink) {
    private var bytesWritten = 0L

    override fun write(source: Buffer, byteCount: Long) {
        super.write(source, byteCount)

        bytesWritten += byteCount
        onProgressUpdate(bytesWritten, requestBody.contentLength())
    }
}
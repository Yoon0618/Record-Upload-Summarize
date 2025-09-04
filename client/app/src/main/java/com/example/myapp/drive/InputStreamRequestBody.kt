package com.example.myapp.drive


import okhttp3.MediaType
import okhttp3.RequestBody
import okio.BufferedSink
import okio.source
import java.io.InputStream


class InputStreamRequestBody(
    private val contentType: String,
    private val inputStream: InputStream,
) : RequestBody() {
    override fun contentType(): MediaType? = MediaType.parse(contentType)


    override fun writeTo(sink: BufferedSink) {
        inputStream.source().use { src ->
            sink.writeAll(src)
        }
    }
}
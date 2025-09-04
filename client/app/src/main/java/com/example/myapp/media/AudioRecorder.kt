package com.example.myapp.media
import android.content.ContentValues
import android.content.Context
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class AudioRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var outputUri: Uri? = null


    val isRecording: Boolean get() = recorder != null


    fun startRecording(): Uri {
        check(recorder == null) { "Already recording" }


        val name = "rec_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date()) + ".m4a"
        val values = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, name)
            put(MediaStore.Audio.Media.MIME_TYPE, "audio/m4a")
            if (Build.VERSION.SDK_INT >= 29) {
                put(MediaStore.Audio.Media.RELATIVE_PATH, Environment.DIRECTORY_RECORDINGS + "/myapp")
            }
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)
            ?: error("Failed to create MediaStore item")


        val pfd = resolver.openFileDescriptor(uri, "w") ?: error("PFD null")


        val r = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioEncodingBitRate(128_000)
            setAudioSamplingRate(44_100)
            setOutputFile(pfd.fileDescriptor)
            prepare()
            start()
        }


        recorder = r
        outputUri = uri
        pfd.close() // descriptor는 내부에서 유지
        return uri
    }


    fun stopRecording(): Uri {
        val r = recorder ?: error("Not recording")
        try {
            r.stop()
        } finally {
            r.reset()
            r.release()
            recorder = null
        }
        return outputUri ?: error("No output URI")
    }
}
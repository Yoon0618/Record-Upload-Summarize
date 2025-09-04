package com.example.myapp.media


import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore


object MediaStoreUtils {
    fun listMyAppRecordings(context: Context): List<Uri> {
        val uris = mutableListOf<Uri>()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID
        )
        val selection: String?
        val args: Array<String>?
        if (Build.VERSION.SDK_INT >= 29) {
            selection = MediaStore.Audio.Media.RELATIVE_PATH + " LIKE ?"
            args = arrayOf("Recordings/myapp/%")
        } else {
// API 29 미만은 스캔 생략(샘플 단순화)
            selection = null
            args = null
        }
        val sort = MediaStore.Audio.Media.DATE_ADDED + " DESC"
        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            args,
            sort
        )?.use { c ->
            val idIdx = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            while (c.moveToNext()) {
                val id = c.getLong(idIdx)
                uris += Uri.withAppendedPath(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id.toString())
            }
        }
        return uris
    }
}
package com.example.myapp


import android.content.Context
import android.net.Uri
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.myapp.drive.DriveApi
import com.example.myapp.drive.DriveAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


class DriveUploadWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    companion object {
        const val KEY_URI = "uri"
    }


    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val uriStr = inputData.getString(KEY_URI) ?: return@withContext Result.failure()
        val uri = Uri.parse(uriStr)
        try {
            val token = DriveAuth.getAccessToken(applicationContext)
            val folderId = DriveApi.ensureFolder(token, "myapp")


            val name = applicationContext.contentResolver.query(uri, arrayOf(android.provider.MediaStore.MediaColumns.DISPLAY_NAME), null, null, null)
                ?.use { c -> if (c.moveToFirst()) c.getString(0) else "recording.m4a" } ?: "recording.m4a"


            DriveApi.uploadMultipart(
                token = token,
                folderId = folderId,
                displayName = name,
                mimeType = "audio/m4a",
                contentResolver = applicationContext.contentResolver,
                contentUri = uri,
            )
            Result.success()
        } catch (t: Throwable) {
            t.printStackTrace()
            Result.retry()
        }
    }
}
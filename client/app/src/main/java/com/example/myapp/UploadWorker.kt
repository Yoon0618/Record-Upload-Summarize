package com.example.myapp

import android.app.Notification
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.core.app.NotificationCompat
import androidx.documentfile.provider.DocumentFile
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UploadWorker(appContext: Context, params: WorkerParameters) :
    CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val cr = applicationContext.contentResolver
        val tree = DriveFolderStore.load(applicationContext)
            ?: return@withContext Result.failure()

        val targetRoot = DocumentFile.fromTreeUri(applicationContext, tree)
            ?: return@withContext Result.failure()

        // 이미 존재하는 원격 파일명 목록 수집(중복 업로드 방지)
        val existing = targetRoot.listFiles().mapNotNull { it.name }.toSet()

        // 로컬 MediaStore에서 Recordings/myapp 항목 조회
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.RELATIVE_PATH
        )
        val selection = "${MediaStore.Audio.Media.RELATIVE_PATH} LIKE ?"
        val args = arrayOf("Recordings/myapp%")
        val audioUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        var uploaded = 0
        cr.query(audioUri, projection, selection, args, null)?.use { c ->
            val idCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val nameCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
            val mimeCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)

            while (c.moveToNext()) {
                val id = c.getLong(idCol)
                val name = c.getString(nameCol)
                val mime = c.getString(mimeCol) ?: "audio/mp4"

                if (existing.contains(name)) continue // 이미 업로드됨

                val srcUri = ContentUris.withAppendedId(audioUri, id)
                val dst = targetRoot.createFile(mime, name) ?: continue

                cr.openInputStream(srcUri).use { input ->
                    cr.openOutputStream(dst.uri, "w").use { output ->
                        if (input == null || output == null) return@use
                        input.copyTo(output, bufferSize = 8 * 1024)
                    }
                }
                uploaded++
            }
        }

        // 상태 메시지를 알림으로 표시
        notify("업로드 완료: $uploaded 개 파일")
        return@withContext Result.success()
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val notification = buildNotification("업로드 실행 중…")
        return ForegroundInfo(1001, notification)
    }

    private fun buildNotification(text: String): Notification {
        return NotificationCompat.Builder(applicationContext, BuildConfig.UPLOAD_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setContentTitle("RecorderDrive")
            .setContentText(text)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .build()
    }

    private fun notify(text: String) {
        val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE)
                as android.app.NotificationManager
        nm.notify(1002, NotificationCompat.Builder(applicationContext, BuildConfig.UPLOAD_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_upload_done)
            .setContentTitle("RecorderDrive")
            .setContentText(text)
            .build())
    }
}

package com.example.myapp

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.work.Constraints
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {

    private var recorder: MediaRecorder? = null
    private var currentOutputUri: Uri? = null
    private lateinit var txtStatus: TextView
    private val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)

    // 권한 요청 런처
    private val audioPerms = mutableListOf(Manifest.permission.RECORD_AUDIO).apply {
        if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
        if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.READ_MEDIA_AUDIO)
    }.toTypedArray()
    private val reqPerms = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* no-op */ }

    // 드라이브 폴더 선택 (SAF)
    private val pickTree = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            // 권한 영속화
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            DriveFolderStore.save(this, uri)
            txtStatus.text = "드라이브 폴더 연결 완료"
        } else {
            txtStatus.text = "드라이브 폴더 연결 취소"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val btnRecord = findViewById<Button>(R.id.btnRecord)
        val btnPickDriveFolder = findViewById<Button>(R.id.btnPickDriveFolder)
        val btnUpload = findViewById<Button>(R.id.btnUpload)
        txtStatus = findViewById(R.id.txtStatus)

        // 권한 요청(필요 시)
        ensurePermissions()

        btnRecord.setOnClickListener {
            if (recorder == null) startRecording(btnRecord) else stopRecording(btnRecord)
        }

        btnPickDriveFolder.setOnClickListener {
            pickTree.launch(null)
        }

        btnUpload.setOnClickListener {
            enqueueUpload()
        }
    }

    private fun ensurePermissions() {
        val needed = audioPerms.filter {
            ContextCompat.checkSelfPermission(this, it) != PermissionChecker.PERMISSION_GRANTED
        }
        if (needed.isNotEmpty()) reqPerms.launch(needed.toTypedArray())
    }

    private fun startRecording(btn: Button) {
        // MediaStore에 "Recordings/myapp"으로 m4a 항목 예약
        val filename = "rec_${sdf.format(Date())}.m4a"
        val values = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, filename)
            put(MediaStore.Audio.Media.MIME_TYPE, "audio/mp4")
            // 상위 저장소 기준의 상대경로
            put(MediaStore.Audio.Media.RELATIVE_PATH, "Recordings/myapp")
            if (Build.VERSION.SDK_INT >= 29) {
                put(MediaStore.Audio.Media.IS_PENDING, 1)
            }
        }
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val itemUri = contentResolver.insert(collection, values)
            ?: run { txtStatus.text = "파일 생성 실패"; return }

        val pfd = contentResolver.openFileDescriptor(itemUri, "w") ?: run {
            txtStatus.text = "FD 열기 실패"
            return
        }

        recorder = MediaRecorder().apply {
            if (Build.VERSION.SDK_INT >= 31) setAudioSource(MediaRecorder.AudioSource.VOICE_RECOGNITION)
            else setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioEncodingBitRate(128_000)
            setAudioSamplingRate(44_100)
            setOutputFile(pfd.fileDescriptor)
            prepare()
            start()
        }
        currentOutputUri = itemUri
        btn.text = "녹음 정지"
        txtStatus.text = "녹음 중… $filename"
        pfd.close()
    }

    private fun stopRecording(btn: Button) {
        try {
            recorder?.apply {
                stop()
                reset()
                release()
            }
            // IS_PENDING → 0으로 내려 commit
            currentOutputUri?.let { uri ->
                if (Build.VERSION.SDK_INT >= 29) {
                    val cv = ContentValues().apply {
                        put(MediaStore.Audio.Media.IS_PENDING, 0)
                    }
                    contentResolver.update(uri, cv, null, null)
                }
            }
            txtStatus.text = "저장 완료: ${currentOutputUri}"
        } catch (e: Exception) {
            txtStatus.text = "정지 오류: ${e.message}"
        } finally {
            recorder = null
            currentOutputUri = null
            btn.text = "녹음 시작"
        }
    }

    private fun enqueueUpload() {
        val tree = DriveFolderStore.load(this)
        if (tree == null) {
            txtStatus.text = "먼저 [드라이브 폴더 연결]을 눌러 폴더를 선택하세요."
            return
        }
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
            .build()
        val req = OneTimeWorkRequestBuilder<UploadWorker>()
            .setConstraints(constraints)
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()
        WorkManager.getInstance(this).enqueue(req)
        txtStatus.text = "업로드 작업 대기열에 추가됨"
    }

    override fun onDestroy() {
        super.onDestroy()
        recorder?.release()
        recorder = null
    }
}

package com.example.myapp
setContentView(R.layout.activity_main)


recorder = AudioRecorder(this)


btnSignIn = findViewById(R.id.btnSignIn)
btnRecord = findViewById(R.id.btnRecord)
btnUploadAll = findViewById(R.id.btnUploadAll)
txtStatus = findViewById(R.id.txtStatus)


requestRuntimePermissions()


btnSignIn.setOnClickListener {
    val client = DriveAuth.getGoogleSignInClient(this)
    signInLauncher.launch(client.signInIntent)
}


btnRecord.setOnClickListener {
    if (!recorder.isRecording) {
        lifecycleScope.launch(Dispatchers.IO) {
            val uri = recorder.startRecording()
            currentRecording = uri
            runOnUiThread {
                txtStatus.text = "Recording...\n$uri"
                btnRecord.text = "Stop Recording"
            }
        }
    } else {
        lifecycleScope.launch(Dispatchers.IO) {
            val uri = recorder.stopRecording()
            runOnUiThread {
                txtStatus.text = "Saved: $uri\nUploading..."
                btnRecord.text = "Start Recording"
            }
            enqueueUpload(uri)
        }
    }
}


btnUploadAll.setOnClickListener {
    lifecycleScope.launch(Dispatchers.IO) {
// 간단하게: 최근 녹음(현재 세션)만 업로드 재시도 예시
        currentRecording?.let { enqueueUpload(it) }
        runOnUiThread { txtStatus.text = "Enqueued uploads" }
    }
}
}


private fun enqueueUpload(uri: Uri) {
    val input = Data.Builder()
        .putString(DriveUploadWorker.KEY_URI, uri.toString())
        .build()
    val request = OneTimeWorkRequestBuilder<DriveUploadWorker>()
        .setInputData(input)
        .build()
    WorkManager.getInstance(this)
        .enqueueUniqueWork("upload-${uri}", ExistingWorkPolicy.REPLACE, request)
}


private fun requestRuntimePermissions() {
    val perms = mutableListOf(Manifest.permission.RECORD_AUDIO)
    if (Build.VERSION.SDK_INT >= 33) {
        perms += Manifest.permission.READ_MEDIA_AUDIO
    }
    permissionLauncher.launch(perms.toTypedArray())
}
}
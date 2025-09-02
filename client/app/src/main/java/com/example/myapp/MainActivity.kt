package com.example.myapp // 본인의 패키지 이름 확인

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.Scopes
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.Scope
import java.io.File
import java.io.IOException

class MainActivity : AppCompatActivity() {

    // --- 녹음 관련 변수들 ---
    private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.RECORD_AUDIO)
    private val PERMISSION_REQUEST_CODE = 200
    private var mediaRecorder: MediaRecorder? = null
    private var isRecording = false
    private lateinit var outputFile: String
    private lateinit var recordButton: Button

    // --- 구글 로그인 관련 변수들 ---
    private lateinit var mGoogleSignInClient: GoogleSignInClient
    private lateinit var signInButton: SignInButton
    private lateinit var signOutButton: Button
    private lateinit var statusTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // --- UI 요소 초기화 ---
        recordButton = findViewById(R.id.recordButton)
        signInButton = findViewById(R.id.signInButton)
        signOutButton = findViewById(R.id.signOutButton)
        statusTextView = findViewById(R.id.statusTextView)

        // --- 녹음 버튼 리스너 ---
        recordButton.setOnClickListener {
            if (isRecording) {
                stopRecording()
            } else {
                startRecording()
            }
        }

        // --- 구글 로그인 설정 ---
        // 1. 로그인 옵션 설정 (GSO)
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail() // 사용자의 이메일 주소 요청
            // ▼▼▼ 여기가 핵심! 드라이브에 파일을 생성할 수 있는 권한을 요청합니다. ▼▼▼
            .requestScopes(Scope(Scopes.DRIVE_FILE))
            .build()

        // 2. 구글 로그인 클라이언트 생성
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso)

        // 3. 로그인 버튼 리스너
        signInButton.setOnClickListener {
            signIn()
        }

        // 4. 로그아웃 버튼 리스너
        signOutButton.setOnClickListener {
            signOut()
        }
    }

    override fun onStart() {
        super.onStart()
        // 앱이 시작될 때, 이전에 로그인한 계정이 있는지 확인
        val account = GoogleSignIn.getLastSignedInAccount(this)
        updateUI(account) // UI 상태 업데이트
    }

    // 로그인 결과를 처리하는 부분
    private val signInResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.result
                updateUI(account)
            } catch (e: Exception) {
                // 로그인 실패
                updateUI(null)
            }
        } else {
            // 로그인 취소
            Toast.makeText(this, "Google Sign In cancelled", Toast.LENGTH_SHORT).show()
            updateUI(null)
        }
    }

    // 로그인 시작
    private fun signIn() {
        val signInIntent = mGoogleSignInClient.signInIntent
        signInResultLauncher.launch(signInIntent)
    }

    // 로그아웃 처리
    private fun signOut() {
        mGoogleSignInClient.signOut().addOnCompleteListener(this) {
            updateUI(null) // 로그아웃 후 UI 업데이트
        }
    }

    // UI 상태 업데이트 (로그인/로그아웃 시)
    private fun updateUI(account: GoogleSignInAccount?) {
        if (account != null) {
            // 로그인 성공 상태
            statusTextView.text = "로그인됨: ${account.email}"
            signInButton.visibility = View.GONE
            signOutButton.visibility = View.VISIBLE
            recordButton.isEnabled = true // 녹음 버튼 활성화
        } else {
            // 로그아웃 상태
            statusTextView.text = "로그인이 필요합니다."
            signInButton.visibility = View.VISIBLE
            signOutButton.visibility = View.GONE
            recordButton.isEnabled = false // 녹음 버튼 비활성화
        }
    }

    // --- 녹음 관련 함수들 ---

    private fun startRecording() {
        if (!hasPermissions()) {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, PERMISSION_REQUEST_CODE)
            return
        }

        val recordingsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_RECORDINGS)
        val appDir = File(recordingsDir, "myapp")
        if (!appDir.exists()) {
            appDir.mkdirs()
        }
        outputFile = "${appDir.absolutePath}/${System.currentTimeMillis()}.m4a"

        mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(outputFile)
            try {
                prepare()
                start()
                isRecording = true
                recordButton.text = "녹음 중지"
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private fun stopRecording() {
        mediaRecorder?.apply {
            try {
                stop()
                release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        mediaRecorder = null
        isRecording = false
        recordButton.text = "녹음 시작"
        Toast.makeText(this, "녹음이 저장되었습니다: $outputFile", Toast.LENGTH_LONG).show()

        // TODO: 여기에 녹음된 파일을 구글 드라이브에 업로드하는 코드를 추가할 것입니다.
    }

    private fun hasPermissions(): Boolean {
        return REQUIRED_PERMISSIONS.all {
            ActivityCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                startRecording()
            } else {
                Toast.makeText(this, "권한이 거부되어 녹음을 시작할 수 없습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

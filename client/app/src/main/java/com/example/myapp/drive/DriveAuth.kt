package com.example.myapp.drive


object DriveAuth {
    private val scopes = listOf(DriveScopes.DRIVE_FILE)


    fun getGoogleSignInClient(context: Context) =
        com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(
            context,
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(Scope(DriveScopes.DRIVE_FILE))
                .build()
        )


    /**
     * 토큰을 가져오며, 최초 호출 시 사용자에게 Drive 권한 동의를 요청할 수 있습니다.
     * Worker에서는 상호작용이 불가하므로, Activity에서 먼저 한 번 호출해 두는 것을 권장합니다.
     */
    fun getAccessToken(context: Context): String {
        val account = GoogleSignIn.getLastSignedInAccount(context)
            ?: error("Not signed in")
        val credential = GoogleAccountCredential.usingOAuth2(context, scopes).apply {
            selectedAccount = account.account
        }
        return credential.token // blocking; Worker/IO 스레드에서 호출할 것
    }
}
```kotlin
package com.example.myapp.drive


import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.services.drive.DriveScopes


object DriveAuth {
    private val scopes = listOf(DriveScopes.DRIVE_FILE)


    fun getGoogleSignInClient(context: Context) =
        com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(
            context,
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(Scope(DriveScopes.DRIVE_FILE))
                .build()
        )


    /**
     * 토큰을 가져오며, 최초 호출 시 사용자에게 Drive 권한 동의를 요청할 수 있습니다.
     */
    fun getAccessToken(context: Context): String {
        val account = GoogleSignIn.getLastSignedInAccount(context)
            ?: error("Not signed in")
        val credential = GoogleAccountCredential.usingOAuth2(context, scopes).apply {
            selectedAccount = account.account
        }
        return credential.token // blocking; Worker/IO 스레드에서 호출할 것
    }
}
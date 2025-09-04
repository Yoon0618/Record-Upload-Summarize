plugins {
    id("com.android.application") version "8.13.0"
    id("org.jetbrains.kotlin.android") version "2.0.20"
}


android {
    namespace = "com.example.myapp"
    compileSdk = 35


    defaultConfig {
        applicationId = "com.example.myapp"
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }


    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}


dependencies {
// Google Sign-In (Play Services Auth)
    implementation("com.google.android.gms:play-services-auth:21.2.0")


// Google OAuth token helper (GoogleAccountCredential)
    implementation("com.google.api-client:google-api-client-android:2.8.1")


// WorkManager (백그라운드 업로드)
    implementation("androidx.work:work-runtime-ktx:2.10.1")


// OkHttp (Drive REST 업로드)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")


// AndroidX
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
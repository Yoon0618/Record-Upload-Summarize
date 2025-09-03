plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.myapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.recorderdrive"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        // Worker에서 포그라운드 알림 채널 ID 전달
        buildConfigField("String", "UPLOAD_CHANNEL_ID", "\"upload_worker\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlinOptions { jvmTarget = "21" }

    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.activity:activity-ktx:1.9.2")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // WorkManager (KTX). 2.10.3는 2025-08 기준 안정판.
    implementation("androidx.work:work-runtime-ktx:2.10.3") // :contentReference[oaicite:2]{index=2}

    // SAF 편의
    implementation("androidx.documentfile:documentfile:1.0.1")
}

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.smallgamerecord.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.smallgamerecord.app"
        minSdk = 23
        targetSdk = 35
        // Explicit versioning: every release bumps versionCode by hand, so
        // the update feed and the system installer can always tell which
        // build is newer — independent of where the APK was built.
        versionCode = 201
        versionName = "2.0.1"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

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
        // Each GitHub Actions build gets a higher versionCode, so Android can
        // install it over the previous build without uninstalling the app.
        versionCode = (System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 2)
        versionName = "1.0." + (System.getenv("GITHUB_RUN_NUMBER") ?: "2")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

plugins {
    alias(libs.plugins.android.application)
}

android {
    // Watch Face Format faces are resource-only: no Kotlin or Java code.
    enableKotlin = false
    namespace = "com.jimlin.pixelface.watchface"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.jimlin.pixelface.watchface"
        // WFF version 2 needs Wear OS 5 (API 34). Pixel Watch 3 ships with it.
        minSdk = 34
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = false
            signingConfig = signingConfigs.getByName("debug")
        }
    }
}

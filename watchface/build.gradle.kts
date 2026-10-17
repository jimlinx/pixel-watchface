import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

// Release key lives outside the repo. Default: ~/.android/pixelface-release.properties
// (keys: storeFile, storePassword, keyAlias, keyPassword). Override with -PpixelfaceSigning=<path>.
// Without it, release builds fall back to the debug key so anyone can build.
val signingProps = Properties().apply {
    val path = (findProperty("pixelfaceSigning") as String?)
        ?: "${System.getProperty("user.home")}/.android/pixelface-release.properties"
    file(path).takeIf { it.exists() }?.inputStream()?.use { load(it) }
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

    signingConfigs {
        if (signingProps.isNotEmpty()) {
            create("release") {
                storeFile = file(signingProps.getProperty("storeFile"))
                storePassword = signingProps.getProperty("storePassword")
                keyAlias = signingProps.getProperty("keyAlias")
                keyPassword = signingProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = false
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
        }
    }
}

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
    namespace = "com.jimlin.pixelface.companion"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.jimlin.pixelface.companion"
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
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(libs.complications.data.source.ktx)
    implementation(libs.health.services)
    implementation(libs.listenablefuture)
}

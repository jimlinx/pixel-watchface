plugins {
    alias(libs.plugins.android.application)
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

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
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

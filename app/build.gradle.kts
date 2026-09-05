plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// Only present when the release keystore secrets are available (the Play Store
// release workflow, or a local release build). Without them, release builds
// fall back to the debug key.
val keystorePath: String? = System.getenv("KEYSTORE_PATH")

android {
    namespace = "com.andys8.kidsviewer"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.andys8.kidsviewer"
        minSdk = 26
        targetSdk = 36
        // CI supplies these so each build is a real update with a version you can read back
        // under Settings > Apps. Local builds fall back to a placeholder.
        versionCode = (System.getenv("BUILD_NUMBER") ?: "1").toInt()
        versionName = System.getenv("BUILD_STAMP") ?: "dev"
    }

    signingConfigs {
        getByName("debug") {
            // A fixed debug key, checked in on purpose. Without it every CI run generates a new
            // one, and Android refuses to install a build signed with a different key over the
            // previous install. Debug-only and not a secret: the password is the Android default.
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }

        if (keystorePath != null) {
            create("release") {
                storeFile = file(keystorePath)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.findByName("release")
                ?: signingConfigs.getByName("debug")
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.foundation)
    implementation(libs.androidx.material3)

    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)

    implementation(libs.coil.compose)
    implementation(libs.kotlinx.coroutines.android)
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

/*
 * The release signing key, supplied by CI from repository secrets. When it is absent -- a local
 * build, or a fork without the secrets -- release builds fall back to the checked-in debug key
 * so the build never breaks; the workflow says which key a build was signed with.
 */
val releaseKeystore: String? = System.getenv("KEYSTORE_PATH")?.takeIf { it.isNotBlank() }

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
        if (releaseKeystore != null) {
            create("release") {
                storeFile = file(releaseKeystore)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
                // v1 is obsolete; v2/v3 are what modern Android verifies, and v3 is what lets
                // the key be rotated later without every install having to be removed first.
                enableV1Signing = false
                enableV2Signing = true
                enableV3Signing = true
            }
        }

        getByName("debug") {
            // A fixed debug key, checked in on purpose. Without it every CI run generates a new
            // one, and Android refuses to install a build signed with a different key over the
            // previous install. Debug-only and not a secret: the password is the Android default.
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.findByName("release")
                ?: signingConfigs.getByName("debug")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    // WebP is compressed already. Packing it again costs build time and a little size, and
    // costs the phone a decompression pass every time a picture is opened.
    androidResources {
        noCompress += "webp"
    }

    // A single APK that installs on any device, which is what direct downloads need. There is no
    // native code here, so there is nothing to split on and nothing gained by splitting.
    splits {
        abi { isEnable = false }
        density { isEnable = false }
    }

    // Strip the dependency metadata block Google Play uses; it is signed with a Google key and
    // has no purpose in an APK distributed outside Play.
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
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

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Per-app settings: -P flag ya environment se override ho sakti hain.
fun prop(name: String, default: String): String =
    (project.findProperty(name) as String?)?.takeIf { it.isNotBlank() } ?: default

val appName = prop("APP_NAME", "GitHub Uploader")
val appId = prop("APP_ID", "com.nh.githubuploader")
val appVersionName = prop("VERSION_NAME", "1.0.0")
// GitHub Actions par run number use hota hai, taaki har build ka versionCode badhta rahe.
val appVersionCode = prop("VERSION_CODE", System.getenv("GITHUB_RUN_NUMBER") ?: "1").toIntOrNull() ?: 1

// Release signing: keystore tabhi use hoga jab environment variables set hon.
val keystorePath: String? = System.getenv("KEYSTORE_PATH")
val hasKeystore = keystorePath != null && file(keystorePath).exists()

android {
    namespace = "com.nh.githubuploader"
    compileSdk = 35

    defaultConfig {
        applicationId = appId
        minSdk = 24
        targetSdk = 35
        versionCode = appVersionCode
        versionName = appVersionName

        val safeName = appName
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace("'", "\\'")
        resValue("string", "app_name", safeName)
    }

    signingConfigs {
        // Fixed debug key: har build same signature => update install ho jata hai.
        getByName("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
        if (hasKeystore) {
            create("release") {
                storeFile = file(keystorePath!!)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = if (hasKeystore) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
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
        buildConfig = true
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.activity:activity-compose:1.9.3")

    implementation(platform("androidx.compose:compose-bom:2024.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")

    implementation("androidx.documentfile:documentfile:1.0.1")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    debugImplementation("androidx.compose.ui:ui-tooling")
}

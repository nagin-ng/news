plugins {
    id("com.android.application")
}

android {
    namespace = "com.example.agylauncher"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.agylauncher"
        minSdk = 24
        targetSdk = 34
        versionCode = 11
        versionName = "7.0"

        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64", "x86")
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

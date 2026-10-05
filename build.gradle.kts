
buildscript {
    ext {
        compose_version = "1.5.4"
        room_version = "2.6.1"
        kotlin_version = "1.9.20"
        nav_version = "2.7.5"
    }
}
plugins {
    id("com.android.application") version "8.2.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.20" apply false
    id("com.google.devtools.ksp") version "1.9.20-1.0.14" apply false
}

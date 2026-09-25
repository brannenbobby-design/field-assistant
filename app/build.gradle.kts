plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "com.brannen.aamirrorprobe"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.brannen.aamirrorprobe"
        minSdk = 28
        targetSdk = 35
        versionCode = 2
        versionName = "0.2"
    }
}
dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
}

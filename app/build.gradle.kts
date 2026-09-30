plugins { id("com.android.application") }

android {
    namespace = "com.brannenservices.floridamanunsupervised"
    compileSdk = 35

    signingConfigs {
        create("release") {
            storeFile = rootProject.file(".github/keys/android-test.keystore")
            storePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD") ?: "android"
            keyAlias = System.getenv("ANDROID_KEY_ALIAS") ?: "android"
            keyPassword = System.getenv("ANDROID_KEY_PASSWORD") ?: "android"
        }
    }

    buildTypes {
        getByName("release") {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = false
        }
    }

    defaultConfig {
        applicationId = "com.brannenservices.floridamanunsupervised"
        minSdk = 23
        targetSdk = 35
        versionCode = 17
        versionName = "1.0.5"
    }
}

plugins { id("com.android.application") }
android {
 namespace = "com.brannenservices.phonecontrol"
 compileSdk = 35
 defaultConfig { applicationId = "com.brannenservices.phonecontrol"; minSdk = 26; targetSdk = 35; versionCode = 12; versionName = "1.2.0" }
 buildTypes { getByName("release") { isMinifyEnabled = false } }
}

dependencies { implementation("com.google.mlkit:text-recognition:16.0.1") }

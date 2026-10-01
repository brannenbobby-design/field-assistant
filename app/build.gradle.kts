plugins { id("com.android.application") }
android {
 namespace = "com.brannenservices.phonecontrol"
 compileSdk = 35
 defaultConfig { applicationId = "com.brannenservices.phonecontrol"; minSdk = 26; targetSdk = 35; versionCode = 5; versionName = "0.5.0" }
 buildTypes { getByName("release") { isMinifyEnabled = false } }
}

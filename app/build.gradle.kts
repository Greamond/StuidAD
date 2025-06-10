plugins {
    alias(libs.plugins.androidApplication)
}

android {
    namespace = "com.example.stuid"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.stuid"
        minSdk = 28
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    implementation("com.github.bumptech.glide:glide:4.12.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.12.0")
    implementation("com.google.android.material:material:1.6.0")
    implementation ("com.squareup.okhttp3:okhttp:4.9.3")
    implementation ("com.google.code.gson:gson:2.8.9")
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    implementation(libs.swiperefreshlayout)
    testImplementation(libs.junit)
    testImplementation ("org.mockito:mockito-android:5.7.0")
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation ("org.mockito:mockito-android:5.7.0")
    androidTestImplementation(libs.espresso.core)
}
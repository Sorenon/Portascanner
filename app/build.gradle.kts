plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.portascanner"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.portascanner"
        minSdk = 29
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
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {

    implementation("androidx.core:core-ktx:1.10.1")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.9.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    implementation("org.tensorflow:tensorflow-lite-task-vision-play-services:0.4.2")

    implementation("com.github.MikeOrtiz:TouchImageView:3.6")

    // CameraX core library using the camera2 implementation
    val cameraxVersion = "1.4.0-alpha04"
    // The following line is optional, as the core library is included indirectly by camera-camera2
    implementation("androidx.camera:camera-core:${cameraxVersion}")
    implementation("androidx.camera:camera-camera2:${cameraxVersion}")
    // If you want to additionally use the CameraX Lifecycle library
    implementation("androidx.camera:camera-lifecycle:${cameraxVersion}")
    // If you want to additionally use the CameraX VideoCapture library
    implementation("androidx.camera:camera-video:${cameraxVersion}")
    // If you want to additionally use the CameraX View class
    implementation("androidx.camera:camera-view:${cameraxVersion}")
    // If you want to additionally add CameraX ML Kit Vision Integration
    implementation("androidx.camera:camera-mlkit-vision:${cameraxVersion}")
    // If you want to additionally use the CameraX Extensions library
    implementation("androidx.camera:camera-extensions:${cameraxVersion}")

    val pytochVersion = "2.1.0"
    implementation("org.pytorch:pytorch_android_lite:${pytochVersion}")
    implementation("org.pytorch:pytorch_android_torchvision_lite:${pytochVersion}")
}
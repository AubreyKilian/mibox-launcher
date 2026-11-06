plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "one.aubrey.launcher"
    compileSdk = 34

    defaultConfig {
        applicationId = "one.aubrey.launcher"
        minSdk = 21
        // The Mi Box S runs Android 11 (API 30). Android applies the behaviour of the OS it
        // runs on, so a newer target adds no restrictions there.
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isDebuggable = false
            // Personal sideload build: sign with the local debug key.
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    testImplementation("junit:junit:4.13.2")
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    // Required by the Samsung Health Data SDK.
    alias(libs.plugins.kotlin.parcelize)
}

android {
    namespace = "com.arjunvision.arjunfitai"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.arjunvision.arjunfitai"
        // Samsung Health (and so the Data SDK) runs on Android 10 / API 29 and later.
        minSdk = 29
        targetSdk = 36
        versionCode = 2
        versionName = "0.1.1"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        // The Samsung Health Data SDK supports Java 17 or later.
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

// The Samsung Health Data SDK is distributed only as an AAR download from
// https://developer.samsung.com/health/data/process.html. Matches both
// "samsung-health-data-api.aar" and versioned names such as "samsung-health-data-api-1.1.0.aar".
val samsungHealthAar = fileTree(mapOf("dir" to "libs", "include" to listOf("samsung-health-data-api*.aar")))

val checkSamsungHealthSdk by tasks.registering {
    val aarFiles = samsungHealthAar
    doLast {
        if (aarFiles.isEmpty) {
            throw GradleException(
                "Samsung Health Data SDK not found. Download it from " +
                    "https://developer.samsung.com/health/data/process.html and copy " +
                    "samsung-health-data-api*.aar into app/libs/."
            )
        }
    }
}
tasks.named("preBuild") { dependsOn(checkSamsungHealthSdk) }

dependencies {
    implementation(samsungHealthAar)
    implementation(libs.gson)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.coroutines.android)
    debugImplementation(libs.compose.ui.tooling)

    testImplementation(libs.junit)
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    // Required by the Samsung Health Data SDK.
    alias(libs.plugins.kotlin.parcelize)
    // Room code generation for the in-app food, water and weight log.
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.arjunvision.arjunfitai"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.arjunvision.arjunfitai"
        // Samsung Health (and so the Data SDK) runs on Android 10 / API 29 and later.
        minSdk = 29
        targetSdk = 36
        versionCode = 12
        versionName = "0.9.0-alpha1"
    }

    // Two builds of the same app (same applicationId, same code in src/main):
    // - samsung: includes the Samsung Health Data SDK (needs the AAR in app/libs, see README).
    // - open: no Samsung SDK, so anyone can build it (students, the public repo, GitHub Actions).
    flavorDimensions += "health"
    productFlavors {
        create("samsung") {
            dimension = "health"
            isDefault = true
        }
        create("open") {
            dimension = "health"
            versionNameSuffix = "-open"
        }
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
    packaging {
        // The Anthropic SDK's dependencies ship duplicate licence/metadata files.
        resources.excludes += setOf("META-INF/*.kotlin_module", "META-INF/versions/**", "META-INF/DEPENDENCIES", "META-INF/LICENSE*", "META-INF/NOTICE*")
    }
    buildFeatures {
        compose = true
        // Profile › About shows BuildConfig.VERSION_NAME.
        buildConfig = true
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
// Only the "samsung" build needs the AAR; the "open" build never touches it.
tasks.matching { it.name.startsWith("preSamsung") && it.name.endsWith("Build") }.configureEach {
    dependsOn(checkSamsungHealthSdk)
}

dependencies {
    "samsungImplementation"(samsungHealthAar)
    // Used by the Samsung SDK and by the exercise library.
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
    implementation(libs.androidx.navigation.compose)
    // In-app settings and goals (Samsung Health stays read-only).
    implementation(libs.datastore.preferences)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.anthropic.java)
    debugImplementation(libs.compose.ui.tooling)

    testImplementation(libs.junit)
}

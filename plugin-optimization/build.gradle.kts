plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.svbooster.plugin.optimization"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.svbooster.plugin.optimization.packagebuilder"
        minSdk = 26
        targetSdk = 35
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
}

dependencies {
    // API and Compose are supplied by the host at runtime; they must not be duplicated in plugin DEX.
    compileOnly(project(":plugin-api"))
    compileOnly(platform("androidx.compose:compose-bom:2024.12.01"))
    compileOnly("androidx.compose.material3:material3")
    compileOnly("androidx.compose.foundation:foundation")
    compileOnly("androidx.compose.ui:ui")
}

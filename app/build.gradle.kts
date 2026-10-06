plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.cy.languagereader.mobile"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.cy.languagereader.mobile"
        minSdk = 26
        targetSdk = 35
        versionCode = 134
        versionName = "5.4-sentence-context-engine"
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.fragment:fragment-ktx:1.8.5")
    implementation("androidx.fragment:fragment-compose:1.8.5")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")

    // Mature EPUB rendering, navigation, selection and locator persistence.
    // 3.1.0 matches this project's compileSdk 35 and Kotlin 2.1 toolchain.
    implementation("org.readium.kotlin-toolkit:readium-shared:3.1.0")
    implementation("org.readium.kotlin-toolkit:readium-streamer:3.1.0")
    implementation("org.readium.kotlin-toolkit:readium-navigator:3.1.0")

    implementation("com.tom-roush:pdfbox-android:2.0.27.0")
}

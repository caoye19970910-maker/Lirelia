@file:Suppress("DEPRECATION")

import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.devtools.ksp")
    kotlin("plugin.serialization")
}

val localProperties = Properties()
val localPropertiesFile: File = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(FileInputStream(localPropertiesFile))
}

android {
    namespace = "com.yugentech.quill"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.yugentech.quill"
        minSdk = 26
        targetSdk = 36

        versionCode = 11
        versionName = "3.2.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val webClientId = localProperties.getProperty("WEB_CLIENT_ID") ?: ""
        buildConfigField("String", "WEB_CLIENT_ID", "\"$webClientId\"")

        val geminiApiKey = localProperties.getProperty("GEMINI_API_KEY") ?: ""
        buildConfigField("String", "GEMINI_API_KEY", "\"$geminiApiKey\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }


}


tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    exclude("**/auth/**")
    exclude("**/billing/**")
    exclude("**/bookDetails/**")
    exclude("**/cloud/**")
    exclude("**/insghts/**")
    exclude("**/notification/**")
    exclude("**/quota/**")
    exclude("**/sources/discover/**")
    exclude("**/sources/gutenberg/**")
    exclude("**/sources/standard/**")
    exclude("**/storage/**")
    exclude("**/user/**")

    exclude("**/di/modules/access/**")
    exclude("**/di/modules/notification/**")
    exclude("**/di/modules/books/IndexingModule.kt")
    exclude("**/di/modules/books/StorageModule.kt")
    exclude("**/di/modules/config/SettingsModule.kt")
    exclude("**/di/modules/core/CloudModule.kt")
    exclude("**/di/modules/core/FirebaseModule.kt")
    exclude("**/di/modules/core/NetworkModule.kt")
    exclude("**/di/modules/core/WorkerModule.kt")
    exclude("**/di/modules/shared/AiraModule.kt")
    exclude("**/di/modules/shared/BookDetailsModule.kt")

    exclude("**/navigation/navgraph/AboutGraph.kt")
    exclude("**/navigation/navgraph/AccountGraph.kt")
    exclude("**/navigation/navgraph/AuthGraph.kt")
    exclude("**/navigation/navgraph/BookFeatureGraph.kt")
    exclude("**/navigation/navgraph/SettingsGraph.kt")
    exclude("**/navigation/navgraph/SourceGraph.kt")
    exclude("**/navigation/screen/BottomBarScreen.kt")

    exclude("**/ui/about/**")
    exclude("**/ui/access/**")
    exclude("**/ui/config/**")
    exclude("**/ui/info/**")
    exclude("**/ui/shared/**")
    exclude("**/ui/sources/**")
    exclude("**/ui/tabs/discoverScreen/**")
    exclude("**/ui/tabs/moreScreen/**")

    exclude("**/ui/main/components/LogoutConfirmationDialog.kt")
    exclude("**/ui/main/components/SectionHeader.kt")
    exclude("**/ui/main/components/ToastMessage.kt")
    exclude("**/ui/main/components/TopBar.kt")
    exclude("**/util/ErrorUtils.kt")
    exclude("**/utils/ReleaseTree.kt")
}

configurations.all {
    resolutionStrategy {
        force("androidx.concurrent:concurrent-futures:1.2.0")
        force("androidx.concurrent:concurrent-futures-ktx:1.2.0")
    }
}

dependencies {
    implementation(project(":database"))
    implementation(project(":theme"))
    implementation(project(":reader"))

    implementation(libs.androidx.graphics.shapes)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.material3)
    coreLibraryDesugaring(libs.desugar.jdk.libs)
    implementation(libs.readium.shared)
    implementation(libs.readium.streamer)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.lottie.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.runtime)
    implementation(libs.google.material)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.compose.material3.window.size.class1)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.accompanist.navigation.animation)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.compose.animation.graphics)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.datastore.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)
    implementation(libs.coil.compose)
    implementation(libs.haze)
    implementation(libs.timber)
    implementation(libs.reorderable)
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
import com.android.sdklib.AndroidVersion.VersionCodes

plugins {
    id("com.android.application")
    id("com.jaredsburrows.license")
    kotlin("android")
    kotlin("kapt")
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.21"
}

val isReleaseBuildInvocation: Boolean = gradle.startParameter.taskNames.any { it.contains("Release", ignoreCase = true) }

val appVersionName: String by project
val appVersionCode: String by project

apply(plugin = "androidx.navigation.safeargs.kotlin")
apply(plugin = "dagger.hilt.android.plugin")

android {
    namespace = "com.app.galleryx"
    compileSdk = VersionCodes.BAKLAVA

    defaultConfig {
        applicationId = "com.app.galleryx"
        minSdk = VersionCodes.P
        targetSdk = VersionCodes.BAKLAVA

        versionCode = appVersionCode.toInt()
        versionName = appVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        javaCompileOptions {
            annotationProcessorOptions {
                arguments += "room.incremental" to "true"
                arguments += "room.schemaLocation" to "$projectDir/schemas"
            }
        }

        base {
            archivesName = "GalleryX-$versionName"
        }

        ndk {
            abiFilters.clear()
            abiFilters.add("arm64-v8a")
        }
    }

    flavorDimensions += "distribution"
    productFlavors {
        create("play") {
            dimension = "distribution"
            if (!isReleaseBuildInvocation) {
                applicationIdSuffix = ".play"
                versionNameSuffix = "-play-debug"
            }
        }

        create("foss") {
            dimension = "distribution"
            if (!isReleaseBuildInvocation) {
                applicationIdSuffix = ".foss"
                versionNameSuffix = "-foss-debug"
            }
        }
    }

    buildTypes {
        getByName("debug") {
            isDebuggable = true
        }

        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        dataBinding = true
        compose = true
        buildConfig = true
    }

    compileOptions {
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    lint {
        lintConfig = file("$rootDir/gradle/lint.xml")
        checkReleaseBuilds = false
        abortOnError = false
    }
}

licenseReport {
    generateCsvReport = false
    generateHtmlReport = true
    generateJsonReport = false
    generateTextReport = false
    copyHtmlReportToAssets = true
    useVariantSpecificAssetDirs = true
}

fun DependencyHandler.playImplementation(dependencyNotation: Any) {
    add("fossImplementation", dependencyNotation)
}

fun DependencyHandler.fossImplementation(dependencyNotation: Any) {
    add("fossImplementation", dependencyNotation)
}

dependencies {
    // Architectural Components
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.10.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.10.0")

    // Room
    implementation("androidx.room:room-runtime:2.8.4")
    implementation("androidx.room:room-paging:2.8.4")
    implementation("androidx.room:room-ktx:2.8.4")
    kapt("androidx.room:room-compiler:2.8.4")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")

    // Navigation Components
    implementation("androidx.navigation:navigation-fragment-ktx:2.9.6")
    implementation("androidx.navigation:navigation-ui-ktx:2.9.6")

    // Paging 3
    implementation("androidx.paging:paging-runtime-ktx:3.3.6")

    // Timber Logging
    implementation("com.jakewharton.timber:timber:5.0.1")

    // Dagger Core & Hilt
    implementation("com.google.dagger:dagger:2.57.2")
    kapt("com.google.dagger:dagger-compiler:2.57.2")
    implementation("com.google.dagger:hilt-android:2.57.2")
    kapt("com.google.dagger:hilt-android-compiler:2.57.2")
    implementation("androidx.hilt:hilt-navigation-compose:1.3.0")
    kapt("androidx.hilt:hilt-compiler:1.3.0")

    // Activity & UI
    implementation("androidx.activity:activity-ktx:1.12.0")
    implementation("androidx.activity:activity:1.12.0")
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")

    // Compose Core
    implementation(platform("androidx.compose:compose-bom:2025.11.01"))
    implementation("androidx.compose.material3:material3:1.4.0")
    implementation("androidx.compose.material:material-icons-extended:1.7.6")
    implementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.activity:activity-compose")
    implementation("androidx.compose.foundation:foundation-layout:1.10.2")

    // Security & Biometrics
    implementation("org.mindrot", "jbcrypt", "0.4")
    implementation("androidx.biometric:biometric:1.1.0")

    // Data Parsing & Processing
    implementation("com.google.code.gson", "gson", "2.13.1")
    implementation("androidx.exifinterface", "exifinterface", "1.4.1")

    // Media: VLC Player
    implementation("org.videolan.android:libvlc-all:3.5.1")

    // Media: Glide
    implementation("com.github.bumptech.glide:glide:4.16.0")
    kapt("com.github.bumptech.glide:compiler:4.16.0")
    implementation("com.github.bumptech.glide:compose:1.0.0-beta01") // Jetpack Compose support

    // Media: Coil (Retained strictly for telephoto zoomable image support for now)
    val coilVersion = "2.7.0"
    implementation("me.saket.telephoto:zoomable-image-coil:0.18.0")
    implementation("io.coil-kt:coil-compose:$coilVersion")
    implementation("io.coil-kt:coil-gif:$coilVersion")
    implementation("io.coil-kt:coil-video:$coilVersion")

    implementation(fileTree("libs").matching { include("*.jar") })

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
}
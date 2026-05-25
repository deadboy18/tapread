plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.tapread.nfc"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.tapread.nfc"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
    }

    signingConfigs {
        create("release") {
            storeFile = file("${rootProject.projectDir}/keystore/tapread.jks")
            storePassword = "deadboy"
            keyAlias = "tapread"
            keyPassword = "deadboy"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    // Ignore Windows desktop.ini files that break resource merging
    androidResources {
        ignoreAssetsPattern = "!.svn:!.git:!.ds_store:!*.scc:!CVS:!thumbs.db:!picasa.ini:!*~:desktop.ini"
    }

    // Keep Kotlin source in kotlin/ directory
    sourceSets {
        getByName("main") {
            java.srcDirs("src/main/kotlin")
        }
    }

    // Custom APK naming: tapread-v1.0.0-release.apk
    applicationVariants.all {
        val variant = this
        variant.outputs.all {
            val output = this as com.android.build.gradle.internal.api.BaseVariantOutputImpl
            output.outputFileName = "tapread-v${variant.versionName}-${variant.buildType.name}.apk"
        }
    }
}

dependencies {
    // EMV NFC library (does all the heavy parsing)
    implementation("com.github.devnied.emvnfccard:library:3.1.0")

    // AndroidX / Material
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.activity:activity-ktx:1.9.0")
    implementation("androidx.fragment:fragment-ktx:1.7.1")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // Logging bridge for EMV library's slf4j calls
    implementation("org.slf4j:slf4j-api:2.0.13")
    implementation("com.github.tony19:logback-android:3.0.0")

    // JSON serialization for card data persistence
    implementation("com.google.code.gson:gson:2.11.0")
}

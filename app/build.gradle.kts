plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.morgunbaen.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.morgunbaen.app"
        minSdk = 26
        targetSdk = 35
        // Fylgir BREYTINGAR.md og v-taggunum sem release.yml byggir a.
        // versionCode er versionName an punkts (0.93 -> 93) svo tolurnar
        // tvaer geti ekki rekid i sundur: v0.92 var gefid ut med
        // versionCode 1 og versionName "1.0", sem sagdi hvorugt satt.
        versionCode = 93
        versionName = "0.93"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")

    // Compose
    implementation(platform("androidx.compose:compose-bom:2024.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Bakgrunnsvinna - sækir bænina
    implementation("androidx.work:work-runtime-ktx:2.9.1")

    // Netsamskipti vid RUV
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Hljodspilun (raedur baedi vid MP3 og HLS-streymi)
    implementation("androidx.media3:media3-exoplayer:1.4.1")
    implementation("androidx.media3:media3-exoplayer-hls:1.4.1")

    // Kotlin coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // Einingaprof - keyra a tolvunni, engan sima tarf
    testImplementation("junit:junit:4.13.2")
}

import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Undirritunarlykillinn byr ALDREI i repo-inu. Tveir stadir eru lesnir:
// keystore.properties fyrir heimavinnslu (.gitignore heldur henni uti) og
// umhverfisbreytur fyrir CI, sem les tar leyndarmal ur GitHub.
//
// Finnist hvorugt er signingConfig EKKI settur. Tad er visvitandi: hver sem
// er a ad geta klonad verkefnid og keyrt ./gradlew test og assembleDebug an
// tess ad eiga lykilinn. assembleRelease skilar tha ounderritudu APK-i, og
// release.yml stodvar sig adur en ad tvi kemur.
val keystoreProps = Properties().apply {
    rootProject.file("keystore.properties")
        .takeIf { it.exists() }
        ?.inputStream()
        ?.use { load(it) }
}

fun signingValue(prop: String, env: String): String? =
    keystoreProps.getProperty(prop) ?: System.getenv(env)

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

    signingConfigs {
        signingValue("storeFile", "KEYSTORE_FILE")?.let { path ->
            create("release") {
                storeFile = file(path)
                storePassword = signingValue("storePassword", "KEYSTORE_PASSWORD")
                keyAlias = signingValue("keyAlias", "KEY_ALIAS")
                keyPassword = signingValue("keyPassword", "KEY_PASSWORD")

                // Skemun sett SKYRT frekar en ad treysta sjalfgildum AGP:
                // uppfaersla a byggingartolunum ma ekki breyta undirritun
                // utgafa i kyrrtei. v1 (JAR) tarf ekki - hann er fyrir
                // Android undir API 24 og minSdk her er 26. v3 er med tvi
                // hann er forsenda tess ad geta skipt um lykil sidar.
                enableV1Signing = false
                enableV2Signing = true
                enableV3Signing = true
            }
        }
    }

    buildTypes {
        release {
            // R8 er VILJANDI slokkt. Compose, media3, WorkManager og OkHttp
            // reida sig oll a endurskin ad einhverju leyti; klippi R8 of
            // naerri birtist tad sem vekjari sem hringir ekki - tholalega
            // og ad morgni. Tad a heima i eigin ferd med eigin profun,
            // ekki i somu ferd og undirritunin.
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // null tegar enginn lykill er til stadar - sja athugasemd ad ofan.
            signingConfig = signingConfigs.findByName("release")
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

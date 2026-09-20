plugins {
    // Frá AGP 9 fylgir Kotlin-stuðningur með Android-plugginu sjálfu og
    // `org.jetbrains.kotlin.android` er hvorki nauðsynlegur né leyfður.
    // Compose-þýðandinn þarf hins vegar enn sitt eigið plugin.
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    // Namespace og applicationId eru VILJANDI ekki eins.
    //
    // Auðkenni appsins gagnvart Android er "is.aeh.heimvisir" — það er
    // varanlegt, sést í stillingum símans og ræður því hvort uppfærsla
    // telst sama app. En "is" er lykilorð í Kotlin, svo pakkanafn sem
    // byrjar á því krefðist bakstrika í hverri einustu skrá:
    //
    //     package `is`.aeh.heimvisir.core
    //
    // Namespace ræður aðeins hvar R-klasinn og BuildConfig verða til, svo
    // hann má vera annar. Kóðinn situr því í aeh.heimvisir og notandinn
    // sér is.aeh.heimvisir.
    namespace = "aeh.heimvisir"
    compileSdk = 37

    defaultConfig {
        applicationId = "is.aeh.heimvisir"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "0.1"
    }

    buildTypes {
        release {
            // R8 slökkt í bili. Appið er lítið og ekkert græðist á
            // styttingu fyrr en einhver undirritar það raunverulega —
            // þá á það heima í eigin ferð með eigin prófun.
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    testOptions {
        unitTests {
            // android.util.Log er ekki til í einingaprófum. Án þessa
            // kastar hver einasta Log-lína "not mocked".
            isReturnDefaultValues = true
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.kotlinx.coroutines.test)
}

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.ashrafwebdev.sikkanam"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.ashrafwebdev.sikkanam"
        minSdk = 24
        targetSdk = 35
        // CI sets this to the run number, so each build is newer than the last. Local builds use 1.
        versionCode = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 1
        versionName = "1.0"
    }

    // CI sets SIKKANAM_KEYSTORE to a keystore kept in GitHub secrets. Signing with the same key
    // every time lets each new build install over the previous one. Without it, the default
    // debug key is used, as on a local machine.
    val sharedKeystore: String? = System.getenv("SIKKANAM_KEYSTORE")?.takeIf { it.isNotBlank() }
    val sharedPassword: String? = System.getenv("SIKKANAM_KEYSTORE_PASSWORD")?.takeIf { it.isNotBlank() }

    buildTypes {
        getByName("debug") {
            if (sharedKeystore != null && sharedPassword != null) {
                signingConfig = signingConfigs.create("shared") {
                    storeFile = file(sharedKeystore)
                    storePassword = sharedPassword
                    keyAlias = "sikkanam"
                    keyPassword = sharedPassword
                    storeType = "pkcs12"
                }
            }
        }
        release {
            isMinifyEnabled = false
        }
    }

    // Two builds from one codebase. "github" keeps every feature, including the in-app updater
    // and payment alerts. "play" leaves out what Google Play policy restricts: installing other
    // APKs, self-updating, and reading other apps' notifications.
    flavorDimensions += "distribution"
    productFlavors {
        create("github") {
            dimension = "distribution"
            buildConfigField("boolean", "SELF_UPDATE", "true")
            buildConfigField("boolean", "PAYMENT_ALERTS", "true")
        }
        create("play") {
            dimension = "distribution"
            buildConfigField("boolean", "SELF_UPDATE", "false")
            buildConfigField("boolean", "PAYMENT_ALERTS", "false")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")

    implementation("androidx.biometric:biometric:1.1.0")
    // Pinned newer than the biometric library's own fragment, so the file pickers
    // (backup, restore, receipts, CSV) can use request codes that FragmentActivity accepts.
    implementation("androidx.fragment:fragment:1.8.5")
    implementation("androidx.work:work-runtime-ktx:2.9.1")
    implementation("androidx.glance:glance-appwidget:1.1.0")
    implementation("androidx.core:core-ktx:1.13.1")

    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    debugImplementation("androidx.compose.ui:ui-tooling")

    testImplementation("junit:junit:4.13.2")
}

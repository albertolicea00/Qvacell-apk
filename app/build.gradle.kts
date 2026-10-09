plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.qvacell.app"
    // Capped at 36 (AGP 8.11.0's max supported API level) rather than 37 — needed to stay on
    // an AGP version the installed Android Studio (2025.1 Meerkat) actually supports syncing.
    compileSdk = 36

    defaultConfig {
        applicationId = "com.qvacell.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
    }

    // "unlocked" (GitHub/sideload) gets the dashboard data-capture pipeline — silent USSD capture,
    // SMS reading, call-log estimation — and the sensitive permissions/receivers it needs.
    // "store" (Google Play) ships without any of that, so the store build never has to clear
    // Play's restricted-permissions review for RECEIVE_SMS/READ_SMS/READ_CALL_LOG.
    flavorDimensions += "distribution"
    productFlavors {
        create("unlocked") {
            dimension = "distribution"
            buildConfigField("boolean", "DASHBOARD_CAPTURE_ENABLED", "true")
        }
        create("store") {
            dimension = "distribution"
            buildConfigField("boolean", "DASHBOARD_CAPTURE_ENABLED", "false")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
            freeCompilerArgs.add("-opt-in=androidx.compose.material3.ExperimentalMaterial3Api")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

}

// TODO: the legacy `applicationVariants`/`BaseVariantOutputImpl` API this used to rename output
// APKs to "qvacell-<variant>-v<versionName>.apk" is gone under AGP 9's variant API, and the
// direct outputFileName-mutation replacement doesn't compile against this AGP/AGP-recipes'
// current shape either — needs AGP 9's "listenToArtifacts" recipe, not a mechanical port.
// Dropped for now (default AGP output naming applies) rather than block this build; unrelated
// to the AGP bump's actual goal (unblocking compilation for the new dashboard-pipeline code).

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.09.03")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    // Pinned below their latest releases: androidx.core 1.19.x / lifecycle 2.11.x /
    // activity-compose 1.13.x / navigation-compose 2.10.x all bake an AAR-metadata requirement of
    // compileSdk 37 + AGP 9.1.0+/9.2.0+ — incompatible with the AGP 8.11.0 ceiling the installed
    // Android Studio (2025.1 Meerkat) supports. These versions predate that requirement.
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.0")
    implementation("androidx.activity:activity-compose:1.10.1")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.material3:material3")

    implementation("androidx.navigation:navigation-compose:2.9.0")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")

    implementation("androidx.room:room-runtime:2.8.5")
    implementation("androidx.room:room-ktx:2.8.5")
    ksp("androidx.room:room-compiler:2.8.5")

    implementation("androidx.sqlite:sqlite:2.7.1")
    implementation("androidx.sqlite:sqlite-framework:2.7.1")

    implementation("androidx.security:security-crypto:1.1.0")

    implementation("androidx.datastore:datastore-preferences:1.2.1")

    implementation("com.google.mlkit:text-recognition:16.0.1")
    implementation("androidx.camera:camera-core:1.4.1")
    implementation("androidx.camera:camera-camera2:1.4.1")
    implementation("androidx.camera:camera-lifecycle:1.4.1")
    implementation("androidx.camera:camera-view:1.4.1")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // First-ever test infra for this project (dashboard-pipeline parser/DAO/repository tests).
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.13")
    testImplementation("androidx.test:core:1.6.1")
    testImplementation("androidx.room:room-testing:2.8.5")
    testImplementation("app.cash.turbine:turbine:1.1.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.11.0")
}

// Robolectric 4.13's bundled ASM can't parse class files compiled for newer JDKs ("Unsupported
// class file major version 69" = JDK 25) — it crashes instrumenting android.webkit.RoboCookieManager
// during test teardown even when the test body itself passed. Force unit tests onto JDK 17
// (already required by compileOptions/kotlin.jvmTarget above) regardless of which JDK launched
// Gradle, so this doesn't depend on the invoking machine's default `java`.
tasks.withType<Test>().configureEach {
    javaLauncher.set(
        project.extensions.getByType<JavaToolchainService>().launcherFor {
            languageVersion.set(JavaLanguageVersion.of(17))
        }
    )
}

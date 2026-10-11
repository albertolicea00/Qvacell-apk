import java.security.MessageDigest
import java.util.Properties

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
        // Target Android 14 (API 34) for broad OEM compatibility (e.g. Xiaomi HyperOS/MIUI
        // package installer rejection on API 36) while satisfying Google Play requirements.
        targetSdk = 34
        versionCode = 3
        versionName = "1.0.2"
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

    val releasePropsFile = rootProject.file("keys/release.properties")
    val releaseProps = Properties()
    if (releasePropsFile.exists()) {
        releasePropsFile.inputStream().use { releaseProps.load(it) }
    }

    signingConfigs {
        create("release") {
            storeFile = rootProject.file("keys/release.jks")
            storePassword = releaseProps.getProperty("storePassword")
            keyAlias = releaseProps.getProperty("keyAlias", "albertolicea00")
            keyPassword = releaseProps.getProperty("keyPassword")
            enableV1Signing = true
            enableV2Signing = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (releasePropsFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
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

    applicationVariants.all {
        val variant = this
        val apkFileName = "qvacell-v${variant.versionName}-${variant.buildType.name}-${variant.flavorName}.apk"
        outputs.all {
            val outputImpl = this as? com.android.build.gradle.internal.api.BaseVariantOutputImpl
            outputImpl?.outputFileName = apkFileName
        }
        assembleProvider.configure {
            doLast {
                variant.outputs.forEach { output ->
                    val apkFile = (output as? com.android.build.gradle.internal.api.BaseVariantOutputImpl)?.outputFile
                    if (apkFile != null && apkFile.exists()) {
                        // Export copy to app/build/outputs/apk/{debug|release}/
                        val targetDir = File(layout.buildDirectory.get().asFile, "outputs/apk/${variant.buildType.name}")
                        targetDir.mkdirs()
                        val destApk = File(targetDir, apkFile.name)
                        apkFile.copyTo(destApk, overwrite = true)

                        // Solo las de debug deben tener el SHA-256
                        if (variant.buildType.name == "debug") {
                            val digest = MessageDigest.getInstance("SHA-256")
                            val hashBytes = digest.digest(apkFile.readBytes())
                            val sha256 = hashBytes.joinToString("") { b -> "%02x".format(b) }
                            val shaContent = "$sha256  ${apkFile.name}\n"
                            File(apkFile.parentFile, "${apkFile.name}.sha256").writeText(shaContent)
                            File(targetDir, "${apkFile.name}.sha256").writeText(shaContent)
                        }
                    }
                }
            }
        }
    }
}

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

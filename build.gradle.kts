plugins {
    // AGP 9.0+ has built-in Kotlin support (Gradle 9.6+ removed the internal API AGP 8.x's
    // com.android.internal.application relies on, so AGP 8.x cannot run under Gradle 9.7.1 at
    // all — see https://docs.gradle.org/9.7.1/userguide/upgrading_version_9.html#agp_8x_incompatible).
    // Applying `org.jetbrains.kotlin.android` alongside it now conflicts (duplicate `kotlin`
    // extension), so that plugin is removed — AGP registers Kotlin itself.
    id("com.android.application") version "9.1.1" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.10" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.2.10" apply false
    id("com.google.devtools.ksp") version "2.2.10-2.0.2" apply false
}

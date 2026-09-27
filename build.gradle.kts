plugins {
    // Pinned to AGP 8.11.0 (Gradle 8.13, max API 36) to match the maximum AGP the installed
    // Android Studio (2025.1 Meerkat, build 251.25410) supports — that Studio build refuses to
    // sync anything newer. AGP 8.x needs Gradle < 9.6 (see Gradle's own AGP-8.x-incompatible note
    // for 9.6+), so the wrapper is pinned to 8.13 accordingly (gradle/wrapper/gradle-wrapper.properties).
    // AGP 8.x has no built-in Kotlin support (unlike AGP 9+), so `org.jetbrains.kotlin.android`
    // is applied explicitly again here.
    id("com.android.application") version "8.11.0" apply false
    // Kotlin 2.2.10 (not the 2.1.20 AGP 8.11's own release notes mention) — kotlinx-serialization-json
    // 1.11.0 and Room 2.8.5's KSP-generated code expect a newer Kotlin stdlib metadata version than
    // 2.1.20 provides ("Module was compiled with an incompatible version of Kotlin" / Room-generated
    // DAO impls failing to resolve stdlib functions). Kotlin/AGP compatibility is forward-tolerant —
    // this combo already compiled clean earlier this session, unrelated to the AGP 8.x/9.x split.
    id("org.jetbrains.kotlin.android") version "2.2.10" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.10" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.2.10" apply false
    id("com.google.devtools.ksp") version "2.2.10-2.0.2" apply false
}

// Root build for the single Gradle multi-project build (replaces the former
// Maven reactor). Plugin versions are declared once here with `apply false`
// so every subproject applies the SAME version — in particular the Kotlin
// Gradle plugin, which is one artifact shared by the `:domain` (multiplatform)
// and `:backend:implementation` (jvm) modules and must not diverge.
plugins {
    // Kotlin is one shared plugin artifact across :domain (multiplatform) and
    // :backend:implementation (jvm), so the version is unified here. It must
    // match the kotlin-stdlib the Quarkus 3.35.1 BOM enforces (2.3.21):
    // a 2.1.x compiler cannot read 2.3.x stdlib metadata (ICE).
    kotlin("multiplatform") version "2.3.21" apply false
    kotlin("jvm") version "2.3.21" apply false
    kotlin("plugin.allopen") version "2.3.21" apply false
    kotlin("plugin.noarg") version "2.3.21" apply false
    id("io.quarkus") version "3.35.1" apply false
    id("io.gitlab.arturbosch.detekt") version "1.23.8" apply false
    id("com.github.node-gradle.node") version "7.1.0" apply false
}

allprojects {
    group = "io.github.theestimator"
    version = "1.0.0-SNAPSHOT"
}

// Single entry point for every static-analysis tool across all modules: detekt
// over the Kotlin modules plus svelte-check and ESLint over the frontend. It
// runs no tests and needs no Docker — that keeps it a fast, standalone lint
// pass (the full test suite stays separate via `./gradlew test`).
tasks.register("staticAnalysis") {
    group = "verification"
    description = "Runs all static analysis: detekt over the Kotlin modules " +
        "plus svelte-check and ESLint over the frontend. No tests, no Docker."
    dependsOn(
        ":domain:detekt",
        ":backend:implementation:detekt",
        ":frontend:npmCheck",
        ":frontend:npmLintReport",
        // Consolidated HTML + merged SARIF over all of the above.
        ":frontend:sarifHtmlReport"
    )
}

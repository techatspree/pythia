// Root build for the single Gradle multi-project build (replaces the former
// Maven reactor). Plugin versions are declared once here with `apply false`
// so every subproject applies the SAME version — in particular the Kotlin
// Gradle plugin, which is one artifact shared by the `:domain` (multiplatform)
// and `:backend:implementation` (jvm) modules and must not diverge.
plugins {
    // Kotlin is one shared plugin artifact across :domain (multiplatform) and
    // :backend:implementation (jvm), so the version is unified here. It must
    // match the kotlin-stdlib the Quarkus 3.40.1 BOM enforces (2.4.10):
    // a 2.1.x compiler cannot read 2.3.x stdlib metadata (ICE).
    kotlin("multiplatform") version "2.4.10" apply false
    kotlin("jvm") version "2.4.10" apply false
    kotlin("plugin.allopen") version "2.4.10" apply false
    kotlin("plugin.noarg") version "2.4.10" apply false
    id("io.quarkus") version "3.40.1" apply false
    id("io.gitlab.arturbosch.detekt") version "1.23.8" apply false
    id("com.github.node-gradle.node") version "7.1.0" apply false
    // CycloneDX SBOM (task-206). Applied to :backend:implementation and
    // :domain only; scripts/sbom-finalize.mjs turns their raw output into the
    // one application SBOM, build/sbom/pythia.cdx.json.
    id("org.cyclonedx.bom") version "3.5.1" apply false
}

allprojects {
    group = "io.pythia"
    // Single source: `version` in the root gradle.properties, changed only by
    // scripts/release.sh (task-193).
    version = providers.gradleProperty("version").get()

    // Kotlin/JS (:domain and its sub-projects, plus the root) would otherwise
    // download the Kotlin Gradle plugin's own Node default (24.10.0 when this was
    // written, task-197) next to the frontend's. Its Yarn 1 calls url.parse(), which Node
    // 24+ reports as DEP0169 on every build. One version for the whole build,
    // read from gradle.properties, so the two toolchains cannot drift (task-197).
    plugins.withType<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsPlugin> {
        the<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsEnvSpec>()
            .version.set(providers.gradleProperty("nodeVersion"))
    }

    // Stamp the licence onto every produced jar manifest. These are constants —
    // nothing time- or environment-derived — so the reproducible Jib image
    // (src/backend/implementation/src/main/resources/application.properties)
    // stays byte-deterministic.
    tasks.withType<Jar>().configureEach {
        manifest {
            attributes(
                "Implementation-Title" to project.name,
                "Implementation-Version" to project.version,
                "SPDX-License-Identifier" to "Apache-2.0",
            )
        }
    }
}

// Single entry point for every static-analysis tool across all modules: detekt
// over the Kotlin modules plus svelte-check and ESLint over the frontend. It
// runs no tests and needs no Docker — that keeps it a fast, standalone lint
// pass (the full test suite stays separate via `./gradlew test`).
// Pythia's one application SBOM (task-206): build/sbom/pythia.cdx.json, assembled
// by :frontend:finalizeSbom and verified against what ships by
// scripts/check-sbom.sh. Part of `build`, because the compliance check (task-207)
// reads it. A release attaches it as pythia-<tag>.cdx.json (ci.yaml, `publish`).
val sbom = tasks.register<Exec>("sbom") {
    group = "verification"
    description = "Builds and checks the application SBOM, build/sbom/pythia.cdx.json."
    dependsOn(":frontend:finalizeSbom")
    commandLine(layout.projectDirectory.file("scripts/check-sbom.sh").asFile.path)
}
// The root project has no `build` task (no `base` plugin, whose `clean` would also
// wipe the Kotlin/JS tooling under the root build/), so `./gradlew build` reaches
// the SBOM through :frontend:build, which runs in every full build.
project(":frontend").tasks.matching { it.name == "build" }.configureEach { dependsOn(sbom) }

tasks.register("staticAnalysis") {
    group = "verification"
    description = "Runs all static analysis: detekt over the Kotlin modules " +
        "plus svelte-check and ESLint over the frontend, and Redocly over the " +
        "OpenAPI document. No tests, no Docker."
    dependsOn(
        // Every domain module, not just the aggregator: since task-143 most of
        // the Kotlin lives in :domain:core and the two method modules, so
        // listing only :domain would silently stop analysing almost all of it.
        ":domain:detekt",
        ":domain:core:detekt",
        ":domain:method-threepoint:detekt",
        ":domain:method-bucketsampled:detekt",
        ":backend:implementation:detekt",
        ":frontend:npmCheck",
        ":frontend:npmLintReport",
        // Unlike the two above, this one FAILS on findings (task-149).
        ":frontend:checkDesignTokens",
        // Blocking as well: the OpenAPI document, Redocly recommended-strict (task-204).
        ":frontend:lintOpenApi",
        // Consolidated HTML + merged SARIF over all of the above.
        ":frontend:sarifHtmlReport"
        // Deliberately ABSENT: :frontend:checkOssCompliance (task-207). It reads the
        // application SBOM, so it needs quarkusBuild and npmBuild — the whole
        // application — and this task is a fast lint pass. It runs in `build`.
    )
}

import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("multiplatform")
    kotlin("plugin.allopen")
    id("io.gitlab.arturbosch.detekt")
}

repositories {
    mavenCentral()
}

kotlin {
    jvm {
        // Kotlin 2.3.x removed the legacy `kotlinOptions` DSL in favor of
        // the typed `compilerOptions`.
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
    }

    js {
        // Pin the JS output module name to "domain" so the emitted files stay
        // `domain.mjs` / `domain.d.mts` (adapter.ts + CLAUDE.md depend on this).
        // Without this the KMP module name would inherit the root project name
        // ("pythia-domain") now that domain is a subproject.
        outputModuleName.set("domain")
        useEsModules()
        browser()
        generateTypeScriptDefinitions()
        binaries.library()
    }

    sourceSets {
        getByName("commonMain") {
            kotlin.srcDir("src/main/kotlin")
            dependencies {
                // MUST be `api`, never `implementation` (task-143).
                // :backend:implementation depends on this aggregator and imports
                // io.pythia.model.* in ~13 files; `implementation`
                // dependencies do not reach a consumer's compile classpath, so
                // downgrading these would break the backend build wholesale.
                api(project(":domain:core"))
                api(project(":domain:method-threepoint"))
                api(project(":domain:method-bucketsampled"))
                // `api`, not `implementation` (task-106): EstimatorBucketAssignment exposes
                // kotlinx.datetime.Instant in its public API and the backend has to
                // construct one, so the type must reach a consumer's compile classpath.
                api("org.jetbrains.kotlinx:kotlinx-datetime:0.6.2")
                implementation("io.github.oshai:kotlin-logging:7.0.7")
            }
        }
        getByName("jvmTest") {
            kotlin.srcDir("src/test/kotlin")
            dependencies {
                implementation(kotlin("test-junit5"))
                implementation("org.junit.jupiter:junit-jupiter:5.11.4")
                // kotlin-logging delegates to slf4j on the JVM target; the
                // backend supplies a provider via Quarkus' JBoss LogManager
                // bridge, but the domain's own JVM tests need one too.
                runtimeOnly("org.slf4j:slf4j-simple:2.0.16")
            }
        }
    }
}

allOpen {
    annotation("io.pythia.model.DomainEntity")
}

// Create .d.mts companions for .mjs files so TypeScript bundler mode resolves types correctly.
// Kotlin 2.3.x emits `.d.mts` directly (e.g. domain.d.mts); copy those through.
// For any .mjs lacking a .d.mts (older-style .d.ts, or dependency stubs), derive one.
val prepareTypescriptArtifacts = tasks.register<Copy>("prepareTypescriptArtifacts") {
    description = "prepare TS artifacts for KMP"
    dependsOn("jsBrowserProductionLibraryDistribution")
    from(layout.buildDirectory.dir("dist/js/productionLibrary")) {
        include("*.mjs", "*.d.ts", "*.d.mts", "*.js", "*.map", "package.json")
    }
    into(layout.buildDirectory.dir("typescript-prep"))

    doLast {
        val prepDir = layout.buildDirectory.dir("typescript-prep").get().asFile
        prepDir.listFiles { f -> f.extension == "mjs" }?.forEach { mjs ->
            val stem = mjs.nameWithoutExtension
            val dmts = File(prepDir, "$stem.d.mts")
            if (!dmts.exists()) {
                val dts = File(prepDir, "$stem.d.ts")
                if (dts.exists()) dmts.writeText(dts.readText())
                else dmts.writeText("export {};\n")
            }
        }
        // The Kotlin/JS toolchain pins its own `typescript` devDependency (e.g.
        // 5.9.3) into the emitted package.json. The frontend consumes the .mjs /
        // .d.mts files directly with its OWN TypeScript, so that pin is unused
        // here and only trips IDE "version doesn't match" warnings against the
        // frontend's typescript. Strip it so the generated manifest carries no
        // conflicting version.
        val pkg = File(prepDir, "package.json")
        if (pkg.exists()) {
            @Suppress("UNCHECKED_CAST")
            val json = groovy.json.JsonSlurper().parse(pkg) as MutableMap<String, Any?>
            (json["devDependencies"] as? MutableMap<String, Any?>)?.let { dev ->
                dev.remove("typescript")
                if (dev.isEmpty()) json.remove("devDependencies")
            }
            pkg.writeText(groovy.json.JsonOutput.prettyPrint(groovy.json.JsonOutput.toJson(json)))
        }
    }
}

// Package the JS production library as a zip
val packageTypescript = tasks.register<Zip>("packageTypescript") {
    description = "package TS code for KMP"
    dependsOn(prepareTypescriptArtifacts)
    from(layout.buildDirectory.dir("typescript-prep"))
    archiveFileName.set("domain-${version}-typescript.zip")
    destinationDirectory.set(layout.buildDirectory.dir("typescript"))
}

tasks.named("build") {
    dependsOn(packageTypescript)
}

// --- Static analysis: detekt over the KMP common/jvm source set. ---
// Reports are informational; `ignoreFailures = true` keeps the Gradle
// build green so the Maven reactor stays green too.
detekt {
    config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
    buildUponDefaultConfig = true
    ignoreFailures = true
    // KMP layout: point at the common source dir; `source.from(...)`
    // can't be set on a Configurable property here, so override the
    // source on the task instead (below).
}

tasks.named<io.gitlab.arturbosch.detekt.Detekt>("detekt") {
    setSource(files("$projectDir/src/main/kotlin"))
    include("**/*.kt", "**/*.kts")
    reports {
        xml.required.set(true)
        xml.outputLocation.set(layout.buildDirectory.file("reports/detekt/detekt.xml"))
        html.required.set(true)
        html.outputLocation.set(layout.buildDirectory.file("reports/detekt/detekt.html"))
        sarif.required.set(true)
        sarif.outputLocation.set(layout.buildDirectory.file("reports/detekt/detekt.sarif"))
    }
}

// Expose the packaged TypeScript library zip as a consumable configuration so
// the :frontend project can depend on it directly (replaces the former Maven
// `zip:typescript` classifier artifact + maven-dependency-plugin unpack).
val typescriptDist = configurations.create("typescriptDist") {
    isCanBeResolved = false
    isCanBeConsumed = true
}

artifacts {
    add(typescriptDist.name, packageTypescript)
}

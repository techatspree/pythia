plugins {
    kotlin("multiplatform") version "2.1.21"
    kotlin("plugin.allopen") version "2.1.21"
    id("maven-publish")
    id("io.gitlab.arturbosch.detekt") version "1.23.8"
}

group = "io.github.theestimator"
version = "1.0.0-SNAPSHOT"

repositories {
    mavenCentral()
}

kotlin {
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "21"
        }
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
    }

    js {
        useEsModules()
        browser()
        generateTypeScriptDefinitions()
        binaries.library()
    }

    sourceSets {
        val commonMain by getting {
            kotlin.srcDir("src/main/kotlin")
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.2")
                implementation("io.github.oshai:kotlin-logging:7.0.7")
            }
        }
        val jvmTest by getting {
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
    annotation("io.github.theestimator.model.DomainEntity")
}

// Create .d.mts companions for .mjs files so TypeScript bundler mode resolves types correctly.
// domain.mjs → domain.d.mts (copy of domain.d.ts); all other .mjs → empty stub.
val prepareTypescriptArtifacts by tasks.registering(Copy::class) {
    dependsOn("jsBrowserProductionLibraryDistribution")
    from(layout.buildDirectory.dir("dist/js/productionLibrary")) {
        include("*.mjs", "*.d.ts", "*.js", "*.map", "package.json")
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
    }
}

// Package the JS production library as a zip for Maven consumption
val packageTypescript by tasks.registering(Zip::class) {
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
    config.setFrom(files("$rootDir/../../config/detekt/detekt.yml"))
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
    }
}

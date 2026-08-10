import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// One Gradle module per estimation method (task-143). Mirrors the aggregator
// :domain build, minus the JS packaging: only :domain pins the output module
// name and produces the `domain` TypeScript bundle the frontend consumes.
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
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
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
        getByName("commonMain") {
            kotlin.srcDir("src/main/kotlin")
            dependencies {
                // `api`, not `implementation`: consumers of a method module must
                // see the core model types it exposes.
                api(project(":domain:core"))
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
                runtimeOnly("org.slf4j:slf4j-simple:2.0.16")
            }
        }
    }
}

// The concrete leaves are @DomainEntity data classes; without allOpen here they
// would silently be final in this module while open everywhere else.
allOpen {
    annotation("io.github.theestimator.model.DomainEntity")
}

detekt {
    config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
    buildUponDefaultConfig = true
    ignoreFailures = true
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

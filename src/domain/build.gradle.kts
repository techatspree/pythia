plugins {
    kotlin("multiplatform") version "2.1.21"
    kotlin("plugin.allopen") version "2.1.21"
    id("maven-publish")
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
            }
        }
        val jvmTest by getting {
            kotlin.srcDir("src/test/kotlin")
            dependencies {
                implementation(kotlin("test-junit5"))
                implementation("org.junit.jupiter:junit-jupiter:5.11.4")
            }
        }
    }
}

allOpen {
    annotation("io.github.theestimator.model.DomainEntity")
}

// Package the JS production library as a zip for Maven consumption
val packageTypescript by tasks.registering(Zip::class) {
    dependsOn("jsBrowserProductionLibraryDistribution")
    from(layout.buildDirectory.dir("dist/js/productionLibrary"))
    include("*.js", "*.mjs", "*.d.ts", "package.json")
    archiveFileName.set("domain-${version}-typescript.zip")
    destinationDirectory.set(layout.buildDirectory.dir("typescript"))
}

tasks.named("build") {
    dependsOn(packageTypescript)
}

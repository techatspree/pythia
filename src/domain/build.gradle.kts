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

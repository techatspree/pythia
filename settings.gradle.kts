rootProject.name = "the-estimator"

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

include(":domain", ":backend:implementation", ":backend:end2end", ":frontend")

// The on-disk layout lives under src/, so remap each logical project path.
project(":domain").projectDir = file("src/domain")
project(":backend").projectDir = file("src/backend")
project(":backend:implementation").projectDir = file("src/backend/implementation")
project(":backend:end2end").projectDir = file("src/backend/end2end")
project(":frontend").projectDir = file("src/frontend")

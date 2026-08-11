rootProject.name = "pythia"

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

include(
    ":domain",
    // One Gradle module per estimation method (task-143), so the method
    // boundary is compiler-enforced rather than a convention: :domain:core
    // holds the model + SPI and depends on no method, each method depends only
    // on core, and :domain aggregates all three for the backend and frontend.
    ":domain:core",
    ":domain:method-threepoint",
    ":domain:method-bucketsampled",
    ":backend:implementation",
    ":backend:end2end",
    ":frontend"
)

// The on-disk layout lives under src/, so remap each logical project path.
project(":domain").projectDir = file("src/domain")
project(":domain:core").projectDir = file("src/domain/core")
project(":domain:method-threepoint").projectDir = file("src/domain/method-threepoint")
project(":domain:method-bucketsampled").projectDir = file("src/domain/method-bucketsampled")
project(":backend").projectDir = file("src/backend")
project(":backend:implementation").projectDir = file("src/backend/implementation")
project(":backend:end2end").projectDir = file("src/backend/end2end")
project(":frontend").projectDir = file("src/frontend")

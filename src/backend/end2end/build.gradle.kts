plugins {
    java
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testImplementation("io.rest-assured:rest-assured:5.5.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

// The end-to-end tests run against a RUNNING backend, so they are NOT part of
// the default build (mirrors the former Maven `skipITs=true`). The sources
// still compile under `build`; run the suite explicitly with `e2eTest`.
tasks.test {
    enabled = false
}

tasks.register<Test>("e2eTest") {
    description = "Runs the end-to-end tests against a running backend."
    group = "verification"
    useJUnitPlatform()
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
}

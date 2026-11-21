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
// `smokeTest` (task-087) runs only the read-only `@Tag("smoke")` suite, which
// `e2eTest` excludes; neither is wired into `build`/`check`.
tasks.test {
    enabled = false
}

tasks.register<Test>("e2eTest") {
    description = "Runs the end-to-end tests against a running backend."
    group = "verification"
    useJUnitPlatform { excludeTags("smoke") }
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
}

tasks.register<Test>("smokeTest") {
    description = "Runs the READ-ONLY smoke suite against a running stack " +
        "(-Dbackend.url, -Dfrontend.url; optional SMOKE_TOKEN). See docs/development.md."
    group = "verification"
    useJUnitPlatform { includeTags("smoke") }
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    systemProperty("backend.url", providers.systemProperty("backend.url").getOrElse("http://localhost:18080"))
    systemProperty("frontend.url", providers.systemProperty("frontend.url").getOrElse("http://localhost:18081"))
    environment("SMOKE_TOKEN", providers.environmentVariable("SMOKE_TOKEN").getOrElse(""))
    outputs.upToDateWhen { false }
}

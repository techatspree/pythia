import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm")
    kotlin("plugin.allopen")
    kotlin("plugin.noarg")
    id("io.quarkus")
    id("io.gitlab.arturbosch.detekt")
}

repositories {
    mavenCentral()
    mavenLocal()
}

val quarkusPlatformGroupId = project.property("quarkusPlatformGroupId") as String
val quarkusPlatformArtifactId = project.property("quarkusPlatformArtifactId") as String
val quarkusPlatformVersion = project.property("quarkusPlatformVersion") as String

dependencies {
    implementation(enforcedPlatform("$quarkusPlatformGroupId:$quarkusPlatformArtifactId:$quarkusPlatformVersion"))

    // Shared KMP domain (JVM target), the single source of truth.
    implementation(project(":domain"))
    // The KMP domain logs via kotlin-logging, which on the JVM delegates to
    // slf4j (captured by Quarkus' JBoss LogManager bridge). The project
    // dependency does not propagate the domain's Gradle deps, so declare it.
    implementation("io.github.oshai:kotlin-logging-jvm:7.0.7")

    implementation("io.quarkus:quarkus-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("io.quarkus:quarkus-hibernate-orm-panache-kotlin")
    implementation("io.quarkus:quarkus-smallrye-health")
    implementation("io.quarkus:quarkus-flyway")
    implementation("io.quarkus:quarkus-smallrye-openapi")
    implementation("io.quarkus:quarkus-jdbc-postgresql")
    implementation("io.quarkus:quarkus-container-image-jib")
    implementation("io.quarkus:quarkus-rest-jackson")
    implementation("io.quarkus:quarkus-websockets-next")
    implementation("io.quarkus:quarkus-jackson")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("io.quarkus:quarkus-oidc")
    implementation("io.quarkus:quarkus-micrometer-registry-prometheus")
    implementation("io.quarkus:quarkus-hibernate-validator")
    implementation("io.quarkus:quarkus-logging-json")
    implementation("io.quarkus:quarkus-arc")
    implementation("org.apache.poi:poi-ooxml:5.4.1")
    // Read-only, used by MerlinImporter via plain java.sql.DriverManager to read
    // an uploaded Merlin .mproject SQLite (Core Data) document — NOT a Quarkus
    // datasource (task-131). The app DB is PostgreSQL.
    implementation("org.xerial:sqlite-jdbc:3.50.1.0")

    testImplementation("io.quarkus:quarkus-junit5")
    testImplementation("io.rest-assured:rest-assured")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.4.0")
}

// Open classes for the annotations Quarkus/Hibernate/JAX-RS require (mirrors
// the former kotlin-maven-plugin all-open / no-arg configuration verbatim).
allOpen {
    annotation("jakarta.ws.rs.Path")
    annotation("jakarta.enterprise.context.ApplicationScoped")
    annotation("jakarta.enterprise.context.RequestScoped")
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("io.quarkus.test.junit.QuarkusTest")
}

noArg {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
        javaParameters.set(true)
    }
}

tasks.test {
    useJUnitPlatform()
    // Quarkus requires the JBoss LogManager during tests.
    systemProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager")
    // Keep the test build hermetic: pin the auth provider to `dev` as a system
    // property (SmallRye ordinal 400) so an ambient `APP_AUTH_PROVIDER` env var
    // (ordinal 300) — e.g. from a developer's Entra shell that also exports
    // ENTRA_TENANT_ID — cannot flip @QuarkusTest onto the `entra` path. That
    // path's beans (EntraAuthFilter) inject a JsonWebToken which OIDC — disabled
    // in %test — does not provide, which otherwise fails Arc's build-time
    // validation with an UnsatisfiedResolutionException.
    systemProperty("app.auth.provider", "dev")
}

// Expose the SmallRye-generated OpenAPI document as a consumable configuration
// so :frontend can depend on it directly (task-114), mirroring the way :domain
// publishes its packaged TypeScript through `typescriptDist`. This is what keeps
// the committed `src/frontend/src/lib/api/openapi.json` from drifting: the
// frontend resolves the artifact instead of relying on a manual `cp`.
//
// SmallRye writes the file during the Quarkus augmentation performed by
// `quarkusBuild`, to `target/openapi/` (see
// `quarkus.smallrye-openapi.store-schema-directory` in application.properties).
val openapiFile = layout.projectDirectory.file("target/openapi/openapi.json")

// SmallRye writes the document as a SIDE EFFECT of Quarkus augmentation, into
// `target/openapi/` — a path no Quarkus task declares as an output. Gradle
// therefore cannot tell that deleting it invalidates anything, so a cached or
// up-to-date augmentation leaves it missing/stale. Declare it on the task that
// actually augments (`quarkusAppPartsBuild`) so staleness is detected.
tasks.named("quarkusAppPartsBuild") {
    outputs.file(openapiFile)
}

// Snapshot the document into `build/` immediately after augmentation. Both the
// PROD augmentation (`quarkusBuild`) and the TEST augmentation (@QuarkusTest)
// write to the same `target/openapi/openapi.json`, and they do not agree — the
// prod profile has OIDC active (`%prod.app.auth.provider=entra`) and therefore
// emits `securitySchemes`, while `%test` (dev auth, no OIDC) does not. Whichever
// ran last would win, making the consumed contract depend on task ordering.
// Snapshotting pins the PROD document — the one that describes the deployed API.
val openApiSchemaArtifact = tasks.register<Copy>("openApiSchemaArtifact") {
    description = "Snapshots the generated OpenAPI document for consumption by :frontend."
    dependsOn(tasks.named("quarkusAppPartsBuild"))
    from(openapiFile)
    into(layout.buildDirectory.dir("openapi"))
    // A `Copy` whose source is missing silently produces nothing, which would
    // hand :frontend a stale schema — the very failure this wiring prevents.
    doFirst {
        check(openapiFile.asFile.exists()) {
            "The Quarkus augmentation did not produce ${openapiFile.asFile}. " +
                "Re-run with `./gradlew :backend:implementation:quarkusAppPartsBuild --rerun-tasks`."
        }
    }
}

// …and make sure the tests cannot clobber `target/openapi/` before the snapshot
// is taken, so a single `./gradlew build` is deterministic.
tasks.named("test") {
    mustRunAfter(openApiSchemaArtifact)
}

val openapiSchema = configurations.create("openapiSchema") {
    isCanBeResolved = false
    isCanBeConsumed = true
}

artifacts {
    add(openapiSchema.name, layout.buildDirectory.file("openapi/openapi.json")) {
        builtBy(openApiSchemaArtifact)
    }
}

// Static analysis: detekt over the backend Kotlin sources. Reports are
// informational; the build stays green regardless of findings.
detekt {
    config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
    buildUponDefaultConfig = true
    ignoreFailures = true
    source.setFrom(files("src/main/kotlin"))
}

tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    reports {
        xml.required.set(true)
        xml.outputLocation.set(layout.buildDirectory.file("reports/detekt/detekt.xml"))
        html.required.set(true)
        html.outputLocation.set(layout.buildDirectory.file("reports/detekt/detekt.html"))
        sarif.required.set(true)
        sarif.outputLocation.set(layout.buildDirectory.file("reports/detekt/detekt.sarif"))
    }
}

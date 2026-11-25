import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm")
    kotlin("plugin.allopen")
    kotlin("plugin.noarg")
    id("io.quarkus")
    id("io.gitlab.arturbosch.detekt")
    id("org.cyclonedx.bom")
}

// Raw SBOM input for the backend part of build/sbom/pythia.cdx.json (task-206).
// The Quarkus production runtime classpath is the closest configuration to what
// ships, but not identical (BOM/umbrella `type=pom` entries, artifacts Quarkus
// strips, `-jvm` jar names), so scripts/sbom-finalize.mjs trims it to the jars
// in build/quarkus-app/lib. The default (all configurations) would list test and
// build tooling. Quarkus' own quarkus-cyclonedx extension was rejected: in a
// Gradle build it emits no license data.
tasks.named<org.cyclonedx.gradle.CyclonedxDirectTask>("cyclonedxDirectBom") {
    includeConfigs.set(listOf("quarkusProdRuntimeClasspathConfiguration"))
    includeBomSerialNumber.set(false)
}

repositories {
    mavenCentral()
    mavenLocal()
}

// Pushing implies multi-arch (task-196). A registry can hold a multi-platform
// index; a load into the Docker daemon takes a single image, so the daemon
// default in application.properties stays linux/amd64 and only a push widens it.
// Two ways to push, both covered: `imageBuild -Dquarkus.container-image.push=true`
// (the documented form) and the plugin's own `imagePush` task, which forces the
// push internally where no system property is visible.
// Precedence, verified from the plugin's EffectiveConfig (task-196, re-verified
// unchanged for Quarkus 3.40.1 in task-198): forced task properties (600) > task properties (500) >
// system properties (400) > environment (300) > quarkus { set() } (290) >
// gradle.properties (280) > application.properties (250). So this set()
// overrides the application.properties default, and an explicit
// -Dquarkus.jib.platforms=… still wins over both.
val pushesImage = providers.systemProperty("quarkus.container-image.push").orNull == "true" ||
    gradle.startParameter.taskNames.any { it.endsWith("imagePush") }
if (pushesImage) {
    quarkus {
        set("jib.platforms", "linux/amd64,linux/arm64")
    }
}

// Licence material in the image (task-219): /licenses/pythia/ holds Pythia's
// LICENSE, NOTICE and CONTRIBUTORS and this image's part of the SBOM. Quarkus'
// Jib extension copies src/main/jib/ into the image root, so it is staged there
// (generated, gitignored).
//
// THE CYCLE: the SBOM part needs build/sbom/pythia.cdx.json, which needs this
// project's quarkusBuild (the jars in quarkus-app/lib); but the image is built
// INSIDE that same augmentation (quarkusAppPartsBuild with container-image
// build/push). One invocation cannot both feed the augmentation and depend on
// its output, so staging is a SEPARATE invocation before the image build, and
// quarkusAppPartsBuild must NOT depend on stageImageLicenses.
//
// THE --rerun RULE: the staging invocation has just run the augmentation
// without an image, and the container-image properties are not among its
// inputs. The image invocation must therefore force it with
// `quarkusAppPartsBuild --rerun`, or it is UP-TO-DATE and builds no image while
// reporting success. scripts/build-backend-image.sh owns both invocations; build
// the backend image only through it (docs/deployment.md, "How the images are
// built").
val imageLicensesDir = layout.projectDirectory.dir("src/main/jib/licenses/pythia")

val stageImageLicenses = tasks.register<Sync>("stageImageLicenses") {
    description = "Stages /licenses/pythia/ for the backend image into src/main/jib (task-219)."
    // The SBOM part and THIRD-PARTY-LICENSES.txt (task-208) are made by :frontend,
    // where the build's pinned Node runs.
    dependsOn(":frontend:writeBackendImageSbomPart", ":frontend:generateBackendThirdPartyLicenses")
    from(rootProject.files("LICENSE", "NOTICE", "CONTRIBUTORS"))
    from(layout.buildDirectory.file("image-licenses/sbom.cdx.json"))
    from(rootProject.layout.buildDirectory.file("third-party-licenses/pythia-backend/THIRD-PARTY-LICENSES.txt"))
    into(imageLicensesDir)
}

// True only when THIS project's image is built or pushed. Not for
// :frontend:imageBuild, which reaches quarkusAppPartsBuild through
// finalizeSbom -> quarkusBuild without building a backend image. Task names are
// checked because the plugin's imageBuild forces container-image.build itself
// (application.properties sets it false), with no system property visible.
val backendImageRequested =
    listOf("quarkus.container-image.build", "quarkus.container-image.push").any {
        providers.systemProperty(it).orNull == "true"
    } || gradle.startParameter.taskNames.map { it.trimStart(':') }.any {
        it in setOf("backend:implementation:imageBuild", "backend:implementation:imagePush", "imageBuild", "imagePush")
    }

// The guard: an image built without staging would ship without its licence.
// A task of its own that always runs, so it fires even when the augmentation
// itself would be UP-TO-DATE (a doFirst there would be skipped).
val verifyImageLicensesStaged = tasks.register("verifyImageLicensesStaged") {
    description = "Fails a backend image build whose /licenses/pythia/ was not staged (task-219)."
    val sbomPart = imageLicensesDir.file("sbom.cdx.json").asFile
    outputs.upToDateWhen { false }
    doLast {
        if (!sbomPart.exists()) {
            throw GradleException(
                "$sbomPart is missing, so the image would ship without /licenses/pythia/. " +
                    "Build the backend image with scripts/build-backend-image.sh: the SBOM it carries " +
                    "needs this augmentation's output, so it is staged in a separate Gradle invocation " +
                    "first (docs/deployment.md, \"How the images are built\")."
            )
        }
    }
}
if (backendImageRequested) {
    tasks.named("quarkusAppPartsBuild") { dependsOn(verifyImageLicensesStaged) }
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
    // Drives the session-socket heartbeat (task-147); @Scheduled is inert without it.
    implementation("io.quarkus:quarkus-scheduler")
    implementation("io.quarkus:quarkus-jackson")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("io.quarkus:quarkus-oidc")
    implementation("io.quarkus:quarkus-micrometer-registry-prometheus")
    implementation("io.quarkus:quarkus-hibernate-validator")
    implementation("io.quarkus:quarkus-logging-json")
    implementation("io.quarkus:quarkus-arc")
    // Read-only MCP server (task-218), Streamable HTTP at /api/mcp. Versioned by
    // the Quarkus PLATFORM's own MCP BOM, which moves with quarkusPlatformVersion
    // (3.40.1 pins 2.0.1). Not a hand-picked Quarkiverse version: the newer 2.0.4
    // is built against Quarkus 3.33, and 2.1.0 is only a CR.
    implementation(enforcedPlatform("$quarkusPlatformGroupId:quarkus-mcp-server-bom:$quarkusPlatformVersion"))
    implementation("io.quarkiverse.mcp:quarkus-mcp-server-http")
    implementation("org.apache.poi:poi-ooxml:5.4.1")
    // Read-only, used by MerlinImporter via plain java.sql.DriverManager to read
    // an uploaded Merlin .mproject SQLite (Core Data) document — NOT a Quarkus
    // datasource (task-131). The app DB is PostgreSQL.
    implementation("org.xerial:sqlite-jdbc:3.50.1.0")

    testImplementation("io.quarkus:quarkus-junit5")
    testImplementation("io.rest-assured:rest-assured")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.4.0")
    // McpAssured, the MCP test client (task-218); version from the MCP BOM above.
    testImplementation("io.quarkiverse.mcp:quarkus-mcp-server-test")
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
// prod profile has OIDC active (`%prod` leaves `quarkus.oidc.enabled` at its
// default `true`, and the Entra module is in every non-dev build, task-194) and
// therefore emits `securitySchemes`, while `%test` (dev auth, no OIDC) does not. Whichever
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

import com.github.gradle.node.npm.task.NpmTask
import com.github.gradle.node.task.NodeTask
import com.google.cloud.tools.jib.api.Containerizer
import com.google.cloud.tools.jib.api.Credential
import com.google.cloud.tools.jib.api.DockerDaemonImage
import com.google.cloud.tools.jib.api.ImageReference
import com.google.cloud.tools.jib.api.Jib
import com.google.cloud.tools.jib.api.LogEvent
import com.google.cloud.tools.jib.api.RegistryImage
import com.google.cloud.tools.jib.api.buildplan.AbsoluteUnixPath
import com.google.cloud.tools.jib.api.buildplan.FileEntriesLayer
import com.google.cloud.tools.jib.api.buildplan.Platform
import com.google.cloud.tools.jib.api.buildplan.Port
import com.google.cloud.tools.jib.frontend.CredentialRetrieverFactory
import java.nio.file.Files

// jib-core assembles the frontend image in-process, like Quarkus/Jib does for the
// backend: no Docker daemon to build or push (task-195). Maven Central only.
buildscript {
    repositories { mavenCentral() }
    dependencies { classpath("com.google.cloud.tools:jib-core:0.28.2") }
}

plugins {
    base
    id("com.github.node-gradle.node")
}

node {
    // The build's one Node.js version, shared with Kotlin/JS (gradle.properties).
    version.set(providers.gradleProperty("nodeVersion"))
    npmVersion.set("10.9.2")
    download.set(true)
    nodeProjectDir.set(layout.projectDirectory)
    // CI installs EXACTLY the lockfile (task-195). The image used to be built by
    // the Dockerfile's `npm ci`, which guaranteed that; `npm install` may resolve
    // differently and rewrite package-lock.json. `CI` is set by GitHub Actions and
    // GitLab CI alike, the same signal `openApiGate` uses. Local work keeps
    // `install`.
    npmInstallCommand.set(if (providers.environmentVariable("CI").isPresent) "ci" else "install")
}

// Consume the :domain project's packaged TypeScript library (the KMP/JS
// output + .d.ts), replacing the former Maven `zip:typescript` artifact.
val domainTypescript = configurations.create("domainTypescript") {
    isCanBeConsumed = false
    isCanBeResolved = true
}

// Consume the backend's generated OpenAPI document (task-114) so the committed
// copy — and the typed client generated from it — can never drift from the
// actual REST contract.
val backendOpenapi = configurations.create("backendOpenapi") {
    isCanBeConsumed = false
    isCanBeResolved = true
}

dependencies {
    domainTypescript(project(mapOf("path" to ":domain", "configuration" to "typescriptDist")))
    backendOpenapi(
        project(mapOf("path" to ":backend:implementation", "configuration" to "openapiSchema"))
    )
}

// Unpack the domain TypeScript zip into src/lib/domain so the SvelteKit app
// can import it (adapter.ts). Lazy `from` keeps the dependency on
// :domain:packageTypescript via the resolved configuration.
val unpackDomainTypescript = tasks.register<Sync>("unpackDomainTypescript") {
    from(domainTypescript.elements.map { locs -> locs.map { zipTree(it.asFile) } })
    into(layout.projectDirectory.dir("src/lib/domain"))
}

tasks.npmInstall {
    args.set(listOf("--legacy-peer-deps"))
}

// Refresh the committed OpenAPI document from the backend build. Deliberately a
// `Copy`, NOT a `Sync`: `src/lib/api` also holds hand-written `fetch.ts` /
// `types.ts` and the generated `schema.d.ts`, all of which a `Sync` would delete.
val syncBackendOpenapi = tasks.register<Copy>("syncBackendOpenapi") {
    description = "Copies the backend-generated OpenAPI document into src/lib/api."
    from(backendOpenapi.elements.map { locs -> locs.map { it.asFile } })
    into(layout.projectDirectory.dir("src/lib/api"))
    rename { "openapi.json" }
}

// `gen:api` regenerates schema.d.ts FROM the committed openapi.json, so it must
// run after the sync. This couples :frontend:check to the backend's
// quarkusBuild, which is accepted: it gives correct ordering for free, and when
// the backend is unchanged quarkusBuild is UP-TO-DATE and costs ~nothing.
val genApi = tasks.register<NpmTask>("genApi") {
    dependsOn(tasks.npmInstall, syncBackendOpenapi)
    args.set(listOf("run", "gen:api"))
}

val npmBuild = tasks.register<NpmTask>("npmBuild") {
    dependsOn(tasks.npmInstall, unpackDomainTypescript, genApi)
    args.set(listOf("run", "build"))
}

val npmCheck = tasks.register<NpmTask>("npmCheck") {
    dependsOn(tasks.npmInstall, unpackDomainTypescript, genApi)
    args.set(listOf("run", "check"))
}

// Static analysis: ESLint over ts/svelte/html. `lint:report` always exits 0,
// so the build stays green regardless of findings (informational). The e2e
// subset is the exception: `lintE2e` below runs it again and FAILS the build.
val npmLintReport = tasks.register<NpmTask>("npmLintReport") {
    dependsOn(tasks.npmInstall)
    args.set(listOf("run", "lint:report"))
}

// Consolidate detekt (Kotlin) + ESLint (frontend) reports into one HTML plus a
// merged SARIF, via a dependency-free Node script run through the managed Node.
val sarifHtmlReport = tasks.register<NodeTask>("sarifHtmlReport") {
    dependsOn(":domain:detekt", ":backend:implementation:detekt", npmLintReport)
    script.set(rootProject.file("scripts/sarif-to-html.mjs"))
    val outDir = rootProject.layout.buildDirectory.dir("reports/static-analysis")
    args.set(
        listOf(
            "--out", outDir.get().file("static-analysis.html").asFile.absolutePath,
            "--sarif", outDir.get().file("static-analysis.sarif").asFile.absolutePath,
            "--detekt", rootProject.file("src/domain/build/reports/detekt/detekt.sarif").absolutePath,
            "--detekt", rootProject.file("src/backend/implementation/build/reports/detekt/detekt.sarif").absolutePath,
            "--eslint", rootProject.file("src/frontend/reports/eslint.json").absolutePath
        )
    )
}

tasks.named("assemble") {
    dependsOn(npmBuild)
}

// Drift gate (task-114): fail when the committed OpenAPI client differs from
// what the backend just produced. Compares against HEAD, not the index — a bare
// `git diff` would be satisfied by `git add`-ing a stale schema.
val verifyOpenApiSchemaCommitted = tasks.register<Exec>("verifyOpenApiSchemaCommitted") {
    group = "verification"
    description = "Fails if the committed OpenAPI client has drifted from the backend contract."
    dependsOn(genApi)
    workingDir = rootProject.projectDir
    commandLine = listOf(
        "git", "diff", "--exit-code", "--stat", "HEAD", "--",
        "src/frontend/src/lib/api/openapi.json",
        "src/frontend/src/lib/api/schema.d.ts"
    )
    // Skip where there is no git working tree (e.g. a source-tarball build).
    // Must be `onlyIf`: disabling inside `doFirst` would be too late.
    onlyIf { rootProject.file(".git").exists() }
    doFirst {
        logger.lifecycle(
            "Verifying the committed OpenAPI client is in sync with the backend; " +
                "on failure run `./gradlew build` and commit " +
                "src/lib/api/openapi.json + src/lib/api/schema.d.ts."
        )
    }
}

// OPT-IN only. Wiring this into `check` unconditionally would deadlock ordinary
// development: any change to a REST DTO regenerates the two files, so the build
// would fail until they were committed — while the task workflow requires a
// green build BEFORE the commit. CI builds an already-committed tree, so there
// the gate is exactly the right check.
val openApiGate = providers.environmentVariable("CI").isPresent ||
    providers.gradleProperty("openApiGate").isPresent

// Design-token gate (task-149). Deliberately UNLIKE npmLintReport/detekt, which
// are informational and always exit 0: this one FAILS the build on an arbitrary
// colour value (`bg-[#abc]`), because a token rule that only warns decays. It is
// enforceable only because task-149 first brought the count to zero. The script
// resolves its own scan root, so it behaves identically under Gradle's cwd and
// a manual run from the repo root.
val checkDesignTokens = tasks.register<NodeTask>("checkDesignTokens") {
    dependsOn(tasks.npmInstall)
    script.set(file("scripts/check-design-tokens.mjs"))
    // Deliberately NO declared inputs/outputs: the scanned `src` tree also holds
    // generated output (`src/lib/api` from syncBackendOpenapi, `src/lib/domain`
    // from unpackDomainTypescript), so declaring it as an input makes Gradle
    // demand a dependency on every generator. The scan takes ~50 ms, so always
    // running it is cheaper than the coupling.
    outputs.upToDateWhen { false }
}

// e2e lint gate (task-191). Deliberately UNLIKE npmLintReport, which is
// informational and always exits 0: this one FAILS the build on any ESLint
// finding under `e2e/`, which is what makes the translated-label ban (task-189,
// task-191) actually enforced rather than a report entry nobody reads. It is
// enforceable only because the e2e baseline is zero; the rest of the frontend
// keeps the informational policy because its legacy baseline is not. Every rule
// configured for `e2e/**` is gated, not only the ban — one config, one source
// of truth for the e2e rules.
val lintE2e = tasks.register<NpmTask>("lintE2e") {
    dependsOn(tasks.npmInstall)
    args.set(listOf("run", "lint:e2e"))
}

tasks.named("check") {
    dependsOn(npmCheck, npmLintReport, checkDesignTokens, lintE2e)
    if (openApiGate) dependsOn(verifyOpenApiSchemaCommitted)
}

tasks.named<Delete>("clean") {
    delete(
        layout.projectDirectory.dir(".svelte-kit"),
        layout.projectDirectory.dir("build"),
        layout.projectDirectory.dir("src/lib/domain")
    )
}

// The frontend container image, built with jib-core (task-195): no Docker daemon,
// no second `npm run build` inside a container, a digest-pinned base, and a direct
// registry push. Named after `:backend:implementation:imageBuild`.
//
// The image takes no build-time configuration (task-162). VITE_* used to be
// inlined into the bundle, which made the IMAGE the configuration — a stage
// needed its own build and an image could not be promoted. The auth coordinates
// now arrive at runtime from /config.json, so one image serves every stage.

// Pinned by digest like the backend's Jib base (`quarkus.jib.base-jvm-image`): a
// tag floats, so the same commit could otherwise ship a different nginx. This is
// the multi-arch index of nginx 1.27.5-alpine; upgrading nginx is a deliberate
// edit of this line, never a side effect of rebuilding.
val frontendBaseImage =
    "nginx:1.27-alpine@sha256:65645c7bb6a0661892a8b03b89d0743208a18dd2f3f17a54ef4b76fb8e2f2a10"

// Targets come from Gradle properties without dots, so each can also be given as
// ORG_GRADLE_PROJECT_<name> — which keeps a password off the command line.
val frontendImagePush = providers.gradleProperty("frontendImagePush").map(String::toBoolean).orElse(false)
val frontendImageRegistry = providers.gradleProperty("frontendImageRegistry")
val frontendImageTag = providers.gradleProperty("frontendImageTag").orElse(project.version.toString())
val frontendImageUsername = providers.gradleProperty("frontendImageUsername")
val frontendImagePassword = providers.gradleProperty("frontendImagePassword")

tasks.register("imageBuild") {
    group = "build"
    description = "Builds the frontend image with jib-core: into the local Docker daemon, " +
        "or straight to a registry with -PfrontendImagePush=true."
    dependsOn(npmBuild)
    // The image is a side effect, not a declared output, so this task must never
    // be up to date or cached — the same trap ci.yaml describes for the backend's
    // augmentation.
    outputs.upToDateWhen { false }
    val spaDir = layout.projectDirectory.dir("build").asFile.toPath()
    val nginxConf = layout.projectDirectory.file("nginx.conf").asFile.toPath()
    doLast {
        val push = frontendImagePush.get()
        val registry = frontendImageRegistry.orNull
        check(Files.isRegularFile(spaDir.resolve("index.html"))) {
            "No SPA build at $spaDir — npmBuild should have produced build/index.html"
        }
        check(!push || registry != null) {
            "-PfrontendImagePush=true needs -PfrontendImageRegistry=<host[/prefix]>"
        }
        val ref = listOfNotNull(registry, "pythia/pythia-frontend:${frontendImageTag.get()}").joinToString("/")
        val log: (LogEvent) -> Unit = { logger.info(it.message) }

        // Where the Dockerfile put it: the base image's entrypoint renders
        // /etc/nginx/templates/*.template (and sets NGINX_LOCAL_RESOLVERS).
        val configLayer = FileEntriesLayer.builder().setName("nginx-config")
            .addEntry(nginxConf, AbsoluteUnixPath.get("/etc/nginx/templates/default.conf.template"))
            .build()
        val htmlRoot = AbsoluteUnixPath.get("/usr/share/nginx/html")
        val spaLayer = FileEntriesLayer.builder().setName("spa")
        Files.walk(spaDir).use { paths ->
            paths.filter { Files.isRegularFile(it) }.sorted().forEach { file ->
                val rel = spaDir.relativize(file)
                // `npm run dev`'s predev writes static/config.json = {"authProvider":
                // "dev"} (task-162), and Vite copies static/ into build/. Baking it in
                // would ship a dev-auth config that wins wherever no ConfigMap mounts
                // over it. Before task-195 the Docker ignore file guarded this; now
                // this line does — do not undo it.
                if (rel.toString() == "config.json") return@forEach
                spaLayer.addEntry(file, htmlRoot.resolve(rel.joinToString("/")))
            }
        }

        val target = if (push) {
            val image = RegistryImage.named(ref)
            val user = frontendImageUsername.orNull
            val password = frontendImagePassword.orNull
            if (user != null && password != null) {
                image.addCredential(user, password)
            } else {
                val credentials = CredentialRetrieverFactory.forImage(ImageReference.parse(ref), log)
                image.addCredentialRetriever(credentials.dockerConfig())
                image.addCredentialRetriever(credentials.wellKnownCredentialHelpers())
            }
            Containerizer.to(image)
        } else {
            Containerizer.to(DockerDaemonImage.named(ref))
        }
        // jib-core has NO localhost exception: plain HTTP needs this flag. Allow it
        // for a local registry only (a throwaway registry:2 in a test), never for a
        // real one. Credentials over HTTP stay off, which is Jib's default.
        val registryHost = ImageReference.parse(ref).registry.substringBefore(':')
        target.setAllowInsecureRegistries(push && registryHost in setOf("localhost", "127.0.0.1"))
            .setToolName("pythia")
            .addEventHandler(LogEvent::class.java) { logger.info(it.message) }

        // A registry holds a multi-platform index, so a push builds amd64 + arm64
        // (task-196) and every consumer pulls its native variant. A daemon load
        // takes one image, so that path stays linux/amd64 — a fixed platform, not
        // the host's, so the image never depends on the machine that built it.
        val platforms = if (push) {
            setOf(Platform("amd64", "linux"), Platform("arm64", "linux"))
        } else {
            setOf(Platform("amd64", "linux"))
        }
        val platformList = platforms.joinToString { "${it.os}/${it.architecture}" }
        logger.lifecycle("Building frontend image $ref on $frontendBaseImage (push=$push, platforms=$platformList)")
        // Entrypoint and CMD are deliberately NOT set: inheriting nginx's
        // /docker-entrypoint.sh is what renders the template and runs the
        // local-resolvers script. Creation and modification times stay at Jib's
        // fixed defaults, so the image does not depend on WHEN it is built.
        val container = Jib.from(frontendBaseImage)
            .setPlatforms(platforms)
            .setFileEntriesLayers(listOf(configLayer, spaLayer.build()))
            .addEnvironmentVariable("NGINX_ENTRYPOINT_LOCAL_RESOLVERS", "1")
            .addExposedPort(Port.tcp(80))
            .containerize(target)
        logger.lifecycle("Frontend image $ref: ${container.digest}")
    }
}

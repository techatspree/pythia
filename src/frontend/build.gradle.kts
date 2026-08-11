import com.github.gradle.node.npm.task.NpmTask
import com.github.gradle.node.task.NodeTask

plugins {
    base
    id("com.github.node-gradle.node")
}

node {
    version.set("22.14.0")
    npmVersion.set("10.9.2")
    download.set(true)
    nodeProjectDir.set(layout.projectDirectory)
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
// so the build stays green regardless of findings (informational).
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

tasks.named("check") {
    dependsOn(npmCheck, npmLintReport, checkDesignTokens)
    if (openApiGate) dependsOn(verifyOpenApiSchemaCommitted)
}

tasks.named<Delete>("clean") {
    delete(
        layout.projectDirectory.dir(".svelte-kit"),
        layout.projectDirectory.dir("build"),
        layout.projectDirectory.dir("src/lib/domain")
    )
}

// Build the frontend container image (used on the minikube deploy path,
// replacing the former dev-minikube Maven profile's exec:docker build).
tasks.register<Exec>("dockerBuildImage") {
    group = "build"
    description = "Builds the frontend Docker image."
    dependsOn(npmBuild)
    workingDir = projectDir
    // VITE_* are build-time (Vite inlines them), so forward them from the
    // invoking shell into the Docker build. `providers.environmentVariable`
    // reads the client environment (not a stale Gradle daemon snapshot).
    val viteVarNames = listOf(
        "VITE_AUTH_PROVIDER",
        "VITE_ENTRA_TENANT_ID",
        "VITE_ENTRA_SPA_CLIENT_ID",
        "VITE_ENTRA_API_CLIENT_ID",
        "VITE_ENTRA_REDIRECT_URI"
    )
    val viteVars = viteVarNames.associateWith { providers.environmentVariable(it) }
    val imageTag = "theestimator/estimation-frontend:${project.version}"
    doFirst {
        val buildArgs = viteVars.flatMap { (name, provider) ->
            val value = provider.getOrElse("")
            if (value.isNotEmpty()) listOf("--build-arg", "$name=$value") else emptyList()
        }
        commandLine(listOf("docker", "build") + buildArgs + listOf("-t", imageTag, "."))
    }
}

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

dependencies {
    domainTypescript(project(mapOf("path" to ":domain", "configuration" to "typescriptDist")))
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

val genApi = tasks.register<NpmTask>("genApi") {
    dependsOn(tasks.npmInstall)
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

tasks.named("check") {
    dependsOn(npmCheck, npmLintReport)
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

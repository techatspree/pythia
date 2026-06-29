import com.github.gradle.node.npm.task.NpmTask

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
val domainTypescript by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
}

dependencies {
    domainTypescript(project(mapOf("path" to ":domain", "configuration" to "typescriptDist")))
}

// Unpack the domain TypeScript zip into src/lib/domain so the SvelteKit app
// can import it (adapter.ts). Lazy `from` keeps the dependency on
// :domain:packageTypescript via the resolved configuration.
val unpackDomainTypescript by tasks.registering(Sync::class) {
    from(domainTypescript.elements.map { locs -> locs.map { zipTree(it.asFile) } })
    into(layout.projectDirectory.dir("src/lib/domain"))
}

tasks.npmInstall {
    args.set(listOf("--legacy-peer-deps"))
}

val genApi by tasks.registering(NpmTask::class) {
    dependsOn(tasks.npmInstall)
    args.set(listOf("run", "gen:api"))
}

val npmBuild by tasks.registering(NpmTask::class) {
    dependsOn(tasks.npmInstall, unpackDomainTypescript, genApi)
    args.set(listOf("run", "build"))
}

val npmCheck by tasks.registering(NpmTask::class) {
    dependsOn(tasks.npmInstall, unpackDomainTypescript, genApi)
    args.set(listOf("run", "check"))
}

// Static analysis: ESLint over ts/svelte/html. `lint:report` always exits 0,
// so the build stays green regardless of findings (informational).
val npmLintReport by tasks.registering(NpmTask::class) {
    dependsOn(tasks.npmInstall)
    args.set(listOf("run", "lint:report"))
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
    commandLine("docker", "build", "-t", "theestimator/estimation-frontend:${project.version}", ".")
}

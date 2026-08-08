package io.github.theestimator.method

import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Singleton registry mapping each [EstimationMethod] to its
 * [EstimationMethodModule] and its [EstimationMethodSessionSupport].
 * [require] / [requireSession] throw for an unregistered method so a
 * misconfigured build fails loudly rather than silently defaulting.
 *
 * **The registry does NOT self-populate.** It used to install the standard
 * methods from an `init {}` block, but that required naming the concrete
 * modules — and since task-143 each method is its own Gradle module that
 * depends on `:domain:core`, so referencing them here would be a dependency
 * cycle. Installation moved to `io.github.theestimator.StandardMethods.installAll()`
 * in the aggregator `:domain`, and every entry point bootstraps explicitly
 * (backend: `MethodRegistryBootstrap` on `StartupEvent`; Kotlin/JS: the
 * `DomainFactory` entry points; tests: a direct call). Do not reintroduce the
 * `init {}`.
 */
object EstimationMethodRegistry {
    private val modules = mutableMapOf<EstimationMethod, EstimationMethodModule>()
    private val sessionSupports = mutableMapOf<EstimationMethod, EstimationMethodSessionSupport>()

    fun register(module: EstimationMethodModule) {
        modules[module.method] = module
        logger.debug { "Registered estimation method module: ${module.method}" }
    }

    fun get(method: EstimationMethod): EstimationMethodModule? = modules[method]

    fun require(method: EstimationMethod): EstimationMethodModule =
        modules[method] ?: error("No estimation method module registered for $method")

    fun all(): List<EstimationMethodModule> = modules.values.toList()

    fun registerSessionSupport(support: EstimationMethodSessionSupport) {
        sessionSupports[support.method] = support
        logger.debug { "Registered estimation method session support: ${support.method}" }
    }

    fun getSession(method: EstimationMethod): EstimationMethodSessionSupport? = sessionSupports[method]

    fun requireSession(method: EstimationMethod): EstimationMethodSessionSupport =
        sessionSupports[method] ?: error("No estimation method session support registered for $method")

    /**
     * Test-only: clear all registrations so tests start from a clean registry.
     *
     * Public rather than `internal` only because the tests that use it live in
     * sibling Gradle modules since task-143, and `internal` is module-scoped.
     * Production code must never call it.
     */
    fun clear() {
        modules.clear()
        sessionSupports.clear()
    }
}

package io.github.theestimator.method

import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Singleton registry mapping each [EstimationMethod] to its
 * [EstimationMethodModule]. It starts empty; concrete method modules register
 * themselves (task-098 registers PERT, task-102 registers bucket+sampled).
 * [require] throws for an unregistered method so a misconfigured build fails
 * loudly rather than silently defaulting.
 */
object EstimationMethodRegistry {
    private val modules = mutableMapOf<EstimationMethod, EstimationMethodModule>()

    fun register(module: EstimationMethodModule) {
        modules[module.method] = module
        logger.debug { "Registered estimation method module: ${module.method}" }
    }

    fun get(method: EstimationMethod): EstimationMethodModule? = modules[method]

    fun require(method: EstimationMethod): EstimationMethodModule =
        modules[method] ?: error("No estimation method module registered for $method")

    fun all(): List<EstimationMethodModule> = modules.values.toList()

    /** Test-only: clear all registrations so tests start from a clean registry. */
    internal fun clear() {
        modules.clear()
    }
}

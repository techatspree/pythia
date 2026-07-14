package io.github.theestimator.method

import io.github.theestimator.method.bucketsampled.BucketMethodModule
import io.github.theestimator.method.threepoint.ThreePointMethodModule
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Singleton registry mapping each [EstimationMethod] to its
 * [EstimationMethodModule]. [installStandardMethods] registers the built-in
 * modules; it runs from the object's initialization so the registry self-populates the
 * first time it is touched. [require] throws for an unregistered method so a
 * misconfigured build fails loudly rather than silently defaulting.
 */
object EstimationMethodRegistry {
    private val modules = mutableMapOf<EstimationMethod, EstimationMethodModule>()

    // Eager self-registration: the object's init runs on first access (e.g. the
    // export writers' require(...)), so the standard methods are always present.
    // This deliberately couples the otherwise method-agnostic registry to the
    // concrete modules — an accepted trade-off for zero-config registration.
    init {
        installStandardMethods()
    }

    fun register(module: EstimationMethodModule) {
        modules[module.method] = module
        logger.debug { "Registered estimation method module: ${module.method}" }
    }

    fun get(method: EstimationMethod): EstimationMethodModule? = modules[method]

    fun require(method: EstimationMethod): EstimationMethodModule =
        modules[method] ?: error("No estimation method module registered for $method")

    fun all(): List<EstimationMethodModule> = modules.values.toList()

    /** Register the built-in method modules. Idempotent (register overwrites). */
    fun installStandardMethods() {
        register(ThreePointMethodModule())
        register(BucketMethodModule())
        logger.info { "Installed standard estimation methods: ${modules.keys}" }
    }

    /** Test-only: clear all registrations so tests start from a clean registry. */
    internal fun clear() {
        modules.clear()
    }
}

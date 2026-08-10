package io.github.theestimator

import io.github.theestimator.method.EstimationMethodRegistry
import io.github.theestimator.method.bucketsampled.BucketMethodModule
import io.github.theestimator.method.bucketsampled.BucketSampledSessionSupport
import io.github.theestimator.method.threepoint.ThreePointMethodModule
import io.github.theestimator.method.threepoint.ThreePointSessionSupport
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Installs the built-in estimation methods into [EstimationMethodRegistry].
 *
 * This lives in the **aggregator** `:domain` rather than in the registry itself
 * (task-143): the registry is part of `:domain:core`, which deliberately depends
 * on no method module, so it cannot name `ThreePointMethodModule` or
 * `BucketMethodModule` without recreating the dependency cycle the module split
 * exists to prevent. Only the aggregator sees every method.
 *
 * The registry therefore no longer self-populates from an `init {}` block, so
 * every entry point must bootstrap explicitly:
 *  - **Backend** — `MethodRegistryBootstrap` on Quarkus' `StartupEvent`.
 *  - **Kotlin/JS** — the `DomainFactory` entry points call [installAll] before
 *    building anything, so the frontend needs no explicit call.
 *  - **Tests** — call [installAll] directly (or register the module under test).
 */
object StandardMethods {

    /**
     * Idempotent: registration overwrites by [io.github.theestimator.method.EstimationMethod],
     * so calling this repeatedly (per factory call, per test) is safe and cheap.
     */
    fun installAll() {
        EstimationMethodRegistry.register(ThreePointMethodModule())
        EstimationMethodRegistry.register(BucketMethodModule())
        EstimationMethodRegistry.registerSessionSupport(ThreePointSessionSupport())
        EstimationMethodRegistry.registerSessionSupport(BucketSampledSessionSupport())
        logger.debug { "Installed standard estimation methods" }
    }
}
